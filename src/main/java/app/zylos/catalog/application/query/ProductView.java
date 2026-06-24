package app.zylos.catalog.application.query;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import app.zylos.catalog.domain.model.ProductStatus;
import app.zylos.catalog.domain.vo.CategoryId;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.ProductId;
import app.zylos.catalog.domain.vo.SellerId;

/**
 * Read view of a product for read-your-writes against the write model.
 * Faithfully mirrors aggregate state.
 */
public record ProductView(
        ProductId id,
        SellerId sellerId,
        String name,
        @Nullable String description,
        CategoryId categoryId,
        ProductAttributes attributes,
        ProductStatus status,
        long version,
        List<ProductVariantView> variants) {

    public ProductView {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sellerId, "sellerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(variants, "variants must not be null");
        variants = List.copyOf(variants);
    }
}
