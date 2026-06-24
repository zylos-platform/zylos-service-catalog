package app.zylos.catalog.application.query;

import java.util.Objects;

import app.zylos.catalog.domain.model.VariantStatus;
import app.zylos.catalog.domain.vo.Money;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.Sku;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * Read view of a variant, mirroring the write model (no derived fields).
 */
public record ProductVariantView(
        VariantId id, Sku sku, Money listPrice, ProductAttributes attributes, VariantStatus status) {

    public ProductVariantView {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(listPrice, "listPrice must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
