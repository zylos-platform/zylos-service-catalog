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
 * <p>Consistency boundary: every invariant (non-blank name, non-negative list price, SKU uniqueness
 * across variants, lifecycle transition rules) holds within a single instance and is enforced on
 * each mutating command. A successful command increments the monotonic {@code version} by one and
 * records a thin {@link DomainEvent}; that version is the last-writer-wins key used by the read
 * projection.
 *
 * <p>Per bounded-context discipline, Catalog owns only the seller-set list price — effective and
 * discounted pricing belong to Pricing, availability to Inventory.
 */
public final class Product {

    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;
    private static final int MAX_VARIANTS = 100;

    private final ProductId id;
    private final List<ProductVariant> variants;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private String name;
    private @Nullable String description;
    private CategoryId categoryId;
    private Money listPrice;
    private ProductAttributes attributes;
    private ProductStatus status;
    private long version;

    private Product(
            ProductId id,
            String name,
            @Nullable String description,
            CategoryId categoryId,
            Money listPrice,
            ProductAttributes attributes,
            ProductStatus status,
            List<ProductVariant> variants,
            long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = normaliseName(name);
        this.description = normaliseDescription(description);
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.listPrice = requireNonNegativePrice(listPrice);
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.variants = new ArrayList<>(requireUniqueSkus(variants));
        this.version = version;
    }

    /**
     * Creates a new product in {@code DRAFT} at version 1, recording {@link ProductCreated}.
     */
    public static Product create(
            ProductId id,
            String name,
            @Nullable String description,
            CategoryId categoryId,
            Money listPrice,
            ProductAttributes attributes) {
        Product product = new Product(
                id, name, description, categoryId, listPrice, attributes, ProductStatus.DRAFT, List.of(), 1L);
        product.record(new ProductCreated(id, product.version));
        return product;
    }

    /**
     * Rehydrates a product from persisted state without recording events or altering the version.
     * For exclusive use by repository adapters.
     */
    public static Product reconstitute(
            ProductId id,
            String name,
            @Nullable String description,
            CategoryId categoryId,
            Money listPrice,
            ProductAttributes attributes,
            ProductStatus status,
            List<ProductVariant> variants,
            long version) {
        if (version < 1L) {
            throw new CatalogDomainException("version must be >= 1, was " + version);
        }
        return new Product(id, name, description, categoryId, listPrice, attributes, status, variants, version);
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

    private static Money requireNonNegativePrice(Money listPrice) {
        Objects.requireNonNull(listPrice, "listPrice must not be null");

        if (listPrice.isNegative()) {
            throw new CatalogDomainException("listPrice must not be negative: " + listPrice);
        }
        return listPrice;
    }

    private static List<ProductVariant> requireUniqueSkus(List<ProductVariant> variants) {
        Objects.requireNonNull(variants, "variants must not be null");
        long distinctSkus =
                variants.stream().map(ProductVariant::sku).distinct().count();

        if (distinctSkus != variants.size()) {
            throw new CatalogDomainException("Variants must have unique SKUs");
        }
        return variants;
    }

    public void updateDetails(
            String name,
            @Nullable String description,
            CategoryId categoryId,
            Money listPrice,
            ProductAttributes attributes) {
        ensureNotDiscontinued();
        this.name = normaliseName(name);
        this.description = normaliseDescription(description);
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.listPrice = requireNonNegativePrice(listPrice);
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
        bumpVersion();
        record(new ProductUpdated(id, version));
    }

    public ProductVariant addVariant(VariantId variantId, Sku sku, ProductAttributes variantAttributes) {
        ensureNotDiscontinued();
        Objects.requireNonNull(sku, "sku must not be null");

        if (variants.size() >= MAX_VARIANTS) {
            throw new CatalogDomainException("A product may have at most %d variants".formatted(MAX_VARIANTS));
        }

        if (variants.stream().anyMatch(v -> v.sku().equals(sku))) {
            throw new CatalogDomainException("Duplicate SKU within product: " + sku);
        }

        ProductVariant variant = ProductVariant.create(variantId, sku, variantAttributes);
        variants.add(variant);
        bumpVersion();
        record(new VariantAdded(id, variant.id(), version));
        return variant;
    }

    public void publish() {
        status.ensureCanTransitionTo(ProductStatus.PUBLISHED);

        if (variants.isEmpty()) {
            throw new CatalogDomainException("Cannot publish a product with no variants: " + id);
        }

        status = ProductStatus.PUBLISHED;
        bumpVersion();
        record(new ProductPublished(id, version));
    }

    public void discontinue() {
        status.ensureCanTransitionTo(ProductStatus.DISCONTINUED);
        status = ProductStatus.DISCONTINUED;
        bumpVersion();
        record(new ProductDiscontinued(id, version));
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

    public String name() {
        return name;
    }

    public @Nullable String description() {
        return description;
    }

    public CategoryId categoryId() {
        return categoryId;
    }

    public Money listPrice() {
        return listPrice;
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

    private void record(DomainEvent event) {
        domainEvents.add(event);
    }

    private void bumpVersion() {
        version = Math.incrementExact(version);
    }

    private void ensureNotDiscontinued() {
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
        return "Product[id=%s, status=%s, version=%d, variants=%d]".formatted(id, status, version, variants.size());
    }
}
