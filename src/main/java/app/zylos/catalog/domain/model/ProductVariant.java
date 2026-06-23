package app.zylos.catalog.domain.model;

import java.util.Objects;

import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.Sku;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * A purchasable variant within a {@code Product} aggregate, identified by its own {@link VariantId}
 * and carrying a unique {@link Sku} plus the attributes that differentiate it (e.g. size, color).
 *
 * <p>This is an entity, not a value object: identity is its {@code VariantId}, so equality is
 * identity-based. It is immutable in this phase — variants are added, not edited; mutators will be
 * introduced when update operations are required.
 */
public final class ProductVariant {

    private final VariantId id;
    private final Sku sku;
    private final ProductAttributes attributes;

    private ProductVariant(VariantId id, Sku sku, ProductAttributes attributes) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sku = Objects.requireNonNull(sku, "sku must not be null");
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
    }

    public static ProductVariant create(VariantId id, Sku sku, ProductAttributes attributes) {
        return new ProductVariant(id, sku, attributes);
    }

    public VariantId id() {
        return id;
    }

    public Sku sku() {
        return sku;
    }

    public ProductAttributes attributes() {
        return attributes;
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
        return "ProductVariant[id=%s, sku=%s]".formatted(id, sku);
    }
}
