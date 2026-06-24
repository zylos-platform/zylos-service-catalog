package app.zylos.catalog.application.command;

import java.util.Objects;

import app.zylos.catalog.domain.vo.Money;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.ProductId;
import app.zylos.catalog.domain.vo.Sku;

/**
 * Request to add a variant to an existing product. The {@code VariantId} is generated server-side.
 */
public record AddVariantCommand(ProductId productId, Sku sku, Money listPrice, ProductAttributes attributes) {

    public AddVariantCommand {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(listPrice, "listPrice must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
    }
}
