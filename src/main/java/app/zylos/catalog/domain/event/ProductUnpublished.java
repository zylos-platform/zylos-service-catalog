package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;

/**
 * A product transitioned to {@code UNPUBLISHED} and is no longer buyer-visible. Emitted both for an
 * explicit vendor unpublish and for the automatic demotion that fires when a published product's
 * last purchasable variant becomes unavailable.
 */
public record ProductUnpublished(ProductId productId, long version) implements DomainEvent {}
