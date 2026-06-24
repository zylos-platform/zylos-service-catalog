package app.zylos.catalog.application.command;

import java.util.Objects;

import app.zylos.catalog.domain.vo.*;

/**
 * Request to update a variant's mutable business fields (SKU, list price, attributes).
 */
public record UpdateVariantCommand(
        ProductId productId, VariantId variantId, Sku sku, Money listPrice, ProductAttributes attributes) {

    public UpdateVariantCommand {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(variantId, "variantId must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(listPrice, "listPrice must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
    }
}
