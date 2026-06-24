package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * A single variant was permanently discontinued by explicit vendor action. The cascade triggered by
 * discontinuing the whole product does not emit this per variant — the {@code ProductDiscontinued}
 * snapshot already reflects the cascaded variant states.
 */
public record VariantDiscontinued(ProductId productId, VariantId variantId, long version) implements DomainEvent {}
