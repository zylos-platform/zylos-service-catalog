package app.zylos.catalog.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import app.zylos.catalog.domain.event.*;
import app.zylos.catalog.domain.exception.CatalogDomainException;
import app.zylos.catalog.domain.vo.*;

/**
 * Product aggregate root for the Catalog bounded context.
 *
 * <p>Consistency boundary: every invariant holds within a single instance and is enforced on each
 * mutating command. A successful command increments the monotonic {@code version} by exactly one and
 * records one or more thin {@link DomainEvent}s, all carrying the resulting version; that version is
 * the last-writer-wins key for the read projection.
 *
 * <p>Two orthogonal lifecycle axes are coordinated here:
 *
 * <ul>
 *   <li>{@link ProductStatus} — storefront status of the product.
 *   <li>{@link VariantStatus} — availability of each variant.
 * </ul>
 *
 * <p>Domain rules across the two axes: discontinuing a product cascades {@code DISCONTINUED} to all
 * its variants; deactivating or discontinuing the last purchasable variant of a {@code PUBLISHED}
 * product auto-demotes the product to {@code UNPUBLISHED}. The reverse — buyer-facing buyability,
 * where product status overrides variant availability — is a read/projection concern and is
 * deliberately not modeled here, to avoid a second source of truth.
 *
 * <p>Per bounded-context discipline, Catalog owns only the seller-set list price (per variant);
 * effective/discounted pricing belongs to Pricing and availability to Inventory.
 */
public final class Product {

    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;
    private static final int MAX_VARIANTS = 100;

    private final ProductId id;
    private final SellerId sellerId;
    private final List<ProductVariant> variants;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private String name;
    private @Nullable String description;
    private CategoryId categoryId;
    private ProductAttributes attributes;
    private ProductStatus status;
    private long version;

    private Product(
            ProductId id,
            SellerId sellerId,
            String name,
            @Nullable String description,
            CategoryId categoryId,
            ProductAttributes attributes,
            ProductStatus status,
            List<ProductVariant> variants,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
        this.name = normaliseName(name);
        this.description = normaliseDescription(description);
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(variants, "variants must not be null");

        if (variants.isEmpty()) {
            throw new CatalogDomainException("A product must have at least one variant");
        }
        requireUniqueSkus(variants);
        this.variants = new ArrayList<>(variants);
        this.version = version;
    }

    /**
     * Creates a new product in {@code DRAFT} at version 1 with at least one ({@code ACTIVE}) variant,
     * owned by {@code sellerId}, recording {@link ProductCreated}.
     */
    public static Product create(
            ProductId id,
            SellerId sellerId,
            String name,
            @Nullable String description,
            CategoryId categoryId,
            ProductAttributes attributes,
            List<VariantDraft> initialVariants) {

        Objects.requireNonNull(initialVariants, "initialVariants must not be null");

        List<ProductVariant> built = initialVariants.stream()
                .map(d -> ProductVariant.create(d.id(), d.sku(), d.listPrice(), d.attributes()))
                .toList();

        Product product =
                new Product(id, sellerId, name, description, categoryId, attributes, ProductStatus.DRAFT, built, 1L);
        product.recordEvent(new ProductCreated(id, product.version));
        return product;
    }

    /**
     * Rehydrates a product from persisted state without recording events. For repository adapters.
     */
    public static Product reconstitute(
            ProductId id,
            SellerId sellerId,
            String name,
            @Nullable String description,
            CategoryId categoryId,
            ProductAttributes attributes,
            ProductStatus status,
            List<ProductVariant> variants,
            long version) {

        if (version < 1L) {
            throw new CatalogDomainException("version must be >= 1, was " + version);
        }
        return new Product(id, sellerId, name, description, categoryId, attributes, status, variants, version);
    }

    private static String normaliseName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        String trimmed = name.trim();

        if (trimmed.isEmpty()) {
            throw new CatalogDomainException("name must not be blank");
        }

        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new CatalogDomainException("name must be at most %d characters".formatted(MAX_NAME_LENGTH));
        }
        return trimmed;
    }

    private static @Nullable String normaliseDescription(@Nullable String description) {
        if (description == null) {
            return null;
        }

        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new CatalogDomainException(
                    "description must be at most %d characters".formatted(MAX_DESCRIPTION_LENGTH));
        }
        return trimmed;
    }

    private static void requireUniqueSkus(List<ProductVariant> variants) {
        long distinctSkus =
                variants.stream().map(ProductVariant::sku).distinct().count();

        if (distinctSkus != variants.size()) {
            throw new CatalogDomainException("Variants must have unique SKUs");
        }
    }

    public void updateDetails(
            String name, @Nullable String description, CategoryId categoryId, ProductAttributes attributes) {
        ensureProductMutable();
        this.name = normaliseName(name);
        this.description = normaliseDescription(description);
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
        bumpVersion();
        recordEvent(new ProductUpdated(id, version));
    }

    public ProductVariant addVariant(VariantDraft draft) {
        ensureProductMutable();
        Objects.requireNonNull(draft, "draft must not be null");

        if (variants.size() >= MAX_VARIANTS) {
            throw new CatalogDomainException("A product may have at most %d variants".formatted(MAX_VARIANTS));
        }

        ensureSkuUniqueForNew(draft.sku());
        ProductVariant variant = ProductVariant.create(draft.id(), draft.sku(), draft.listPrice(), draft.attributes());
        variants.add(variant);
        bumpVersion();
        recordEvent(new VariantAdded(id, variant.id(), version));
        return variant;
    }

    public void updateVariant(VariantId variantId, Sku sku, Money listPrice, ProductAttributes attributes) {
        ensureProductMutable();
        ProductVariant variant = requireVariant(variantId);

        if (variant.status() == VariantStatus.DISCONTINUED) {
            throw new CatalogDomainException("Cannot update a discontinued variant: " + variantId);
        }

        Objects.requireNonNull(sku, "sku must not be null");
        ensureSkuUniqueExcluding(variantId, sku);
        variant.changeBusinessDetails(sku, listPrice, attributes);
        bumpVersion();
        recordEvent(new VariantUpdated(id, variantId, version));
    }

    public void activateVariant(VariantId variantId) {
        ensureProductMutable();
        ProductVariant variant = requireVariant(variantId);
        variant.transitionTo(VariantStatus.ACTIVE);
        bumpVersion();
        recordEvent(new VariantActivated(id, variantId, version));
    }

    public void deactivateVariant(VariantId variantId) {
        ensureProductMutable();
        ProductVariant variant = requireVariant(variantId);
        variant.transitionTo(VariantStatus.INACTIVE);
        bumpVersion();
        recordEvent(new VariantDeactivated(id, variantId, version));
        autoDemoteIfNoPurchasableVariant();
    }

    public void discontinueVariant(VariantId variantId) {
        ensureProductMutable();
        ProductVariant variant = requireVariant(variantId);
        variant.transitionTo(VariantStatus.DISCONTINUED);
        bumpVersion();
        recordEvent(new VariantDiscontinued(id, variantId, version));
        autoDemoteIfNoPurchasableVariant();
    }

    public void publish() {
        status.ensureCanTransitionTo(ProductStatus.PUBLISHED);

        if (variants.stream().noneMatch(v -> v.status().isPurchasable())) {
            throw new CatalogDomainException("Cannot publish a product with no purchasable (ACTIVE) variant: " + id);
        }

        status = ProductStatus.PUBLISHED;
        bumpVersion();
        recordEvent(new ProductPublished(id, version));
    }

    public void unpublish() {
        status.ensureCanTransitionTo(ProductStatus.UNPUBLISHED);
        status = ProductStatus.UNPUBLISHED;
        bumpVersion();
        recordEvent(new ProductUnpublished(id, version));
    }

    public void discontinue() {
        status.ensureCanTransitionTo(ProductStatus.DISCONTINUED);
        status = ProductStatus.DISCONTINUED;

        for (ProductVariant variant : variants) {
            if (variant.status() != VariantStatus.DISCONTINUED) {
                variant.transitionTo(VariantStatus.DISCONTINUED);
            }
        }

        bumpVersion();
        recordEvent(new ProductDiscontinued(id, version));
    }

    /**
     * Returns and clears the events recorded since the last pull.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    public ProductId id() {
        return id;
    }

    public SellerId sellerId() {
        return sellerId;
    }

    public String name() {
        return name;
    }

    public @Nullable String description() {
        return description;
    }

    public CategoryId categoryId() {
        return categoryId;
    }

    public ProductAttributes attributes() {
        return attributes;
    }

    public ProductStatus status() {
        return status;
    }

    public List<ProductVariant> variants() {
        return List.copyOf(variants);
    }

    public long version() {
        return version;
    }

    private void autoDemoteIfNoPurchasableVariant() {
        if (status == ProductStatus.PUBLISHED
                && variants.stream().noneMatch(v -> v.status().isPurchasable())) {
            status = ProductStatus.UNPUBLISHED;
            recordEvent(new ProductUnpublished(id, version)); // shares the current command's version
        }
    }

    private ProductVariant requireVariant(VariantId variantId) {
        Objects.requireNonNull(variantId, "variantId must not be null");
        return variants.stream()
                .filter(v -> v.id().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new CatalogDomainException("Variant not found: " + variantId));
    }

    private void ensureSkuUniqueForNew(Sku sku) {
        if (variants.stream().anyMatch(v -> v.sku().equals(sku))) {
            throw new CatalogDomainException("Duplicate SKU within product: " + sku);
        }
    }

    private void ensureSkuUniqueExcluding(VariantId excluded, Sku sku) {
        boolean clash = variants.stream()
                .filter(v -> !v.id().equals(excluded))
                .anyMatch(v -> v.sku().equals(sku));

        if (clash) {
            throw new CatalogDomainException("Duplicate SKU within product: " + sku);
        }
    }

    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    private void bumpVersion() {
        version = Math.incrementExact(version);
    }

    private void ensureProductMutable() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new CatalogDomainException("Cannot modify a discontinued product: " + id);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Product other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Product[id=%s, sellerId=%s, status=%s, version=%d, variants=%d]"
                .formatted(id, sellerId, status, version, variants.size());
    }
}
