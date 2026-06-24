package app.zylos.catalog.application.command;

import java.util.Objects;

import app.zylos.catalog.domain.vo.Money;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.Sku;

/**
 * Per-variant input for product creation. Carries no {@code VariantId}: identity is assigned by the
 * application layer ({@code VariantId.newId()}) when mapping to a domain {@code VariantDraft}.
 */
public record NewVariant(Sku sku, Money listPrice, ProductAttributes attributes) {

    public NewVariant {
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(listPrice, "listPrice must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
    }
}
