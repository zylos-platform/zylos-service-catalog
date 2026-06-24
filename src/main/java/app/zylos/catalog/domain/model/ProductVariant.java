package app.zylos.catalog.domain.model;

import java.util.Objects;

import app.zylos.catalog.domain.exception.CatalogDomainException;
import app.zylos.catalog.domain.vo.Money;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.Sku;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * A purchasable variant within a {@code Product} aggregate.
 *
 * <p>An entity, so equality is identity-based on its {@link VariantId}, which is permanently
 * immutable — it is the join key used by carts, orders, and history, so it must never change.
 * Business fields (SKU, list price, attributes) and availability ({@link VariantStatus}) are
 * mutable, but only through package-private operations invoked by the {@code Product} root: external
 * callers holding a reference returned from {@code Product#variants()} cannot mutate it, so every
 * change stays subject to the root's invariants (SKU uniqueness, auto-demote).
 */
public final class ProductVariant {

    private final VariantId id;
    private Sku sku;
    private Money listPrice;
    private ProductAttributes attributes;
    private VariantStatus status;

    private ProductVariant(VariantId id, Sku sku, Money listPrice, ProductAttributes attributes, VariantStatus status) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sku = Objects.requireNonNull(sku, "sku must not be null");
        this.listPrice = requireNonNegative(listPrice);
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /**
     * Fresh variant in {@code ACTIVE} status. Root-only: variants are created via the aggregate.
     */
    static ProductVariant create(VariantId id, Sku sku, Money listPrice, ProductAttributes attributes) {
        return new ProductVariant(id, sku, listPrice, attributes, VariantStatus.ACTIVE);
    }

    /**
     * Rehydrates a variant from persisted state. For repository adapters.
     */
    public static ProductVariant reconstitute(
            VariantId id, Sku sku, Money listPrice, ProductAttributes attributes, VariantStatus status) {
        return new ProductVariant(id, sku, listPrice, attributes, status);
    }

    private static Money requireNonNegative(Money listPrice) {
        Objects.requireNonNull(listPrice, "listPrice must not be null");

        if (listPrice.isNegative()) {
            throw new CatalogDomainException("listPrice must not be negative: " + listPrice);
        }
        return listPrice;
    }

    void changeBusinessDetails(Sku sku, Money listPrice, ProductAttributes attributes) {
        this.sku = Objects.requireNonNull(sku, "sku must not be null");
        this.listPrice = requireNonNegative(listPrice);
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
    }

    void transitionTo(VariantStatus target) {
        status.ensureCanTransitionTo(target);
        this.status = target;
    }

    public VariantId id() {
        return id;
    }

    public Sku sku() {
        return sku;
    }

    public Money listPrice() {
        return listPrice;
    }

    public ProductAttributes attributes() {
        return attributes;
    }

    public VariantStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductVariant other)) {
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
        return "ProductVariant[id=%s, sku=%s, status=%s]".formatted(id, sku, status);
    }
}
