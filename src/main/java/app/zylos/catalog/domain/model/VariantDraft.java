package app.zylos.catalog.domain.model;

import java.util.Objects;

import app.zylos.catalog.domain.vo.Money;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.Sku;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * Creation input for a new variant. Identity is supplied by the application layer
 * ({@code VariantId.newId()}), keeping the aggregate factory deterministic. A drafted variant always
 * becomes {@code ACTIVE}; non-negativity of the price is enforced when the variant is built.
 */
public record VariantDraft(VariantId id, Sku sku, Money listPrice, ProductAttributes attributes) {

    public VariantDraft {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(listPrice, "listPrice must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
    }
}
