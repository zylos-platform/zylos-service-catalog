package app.zylos.catalog.application.command;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import app.zylos.catalog.domain.vo.CategoryId;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.ProductId;

/**
 * Request to update a product's core (non-variant) details.
 */
public record UpdateProductDetailsCommand(
        ProductId productId,
        String name,
        @Nullable String description,
        CategoryId categoryId,
        ProductAttributes attributes) {

    public UpdateProductDetailsCommand {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
    }
}
