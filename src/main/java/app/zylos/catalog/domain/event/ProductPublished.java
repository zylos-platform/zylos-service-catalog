package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;

/**
 * A product transitioned to {@code PUBLISHED} and is now buyer-visible.
 */
public record ProductPublished(ProductId productId, long version) implements DomainEvent {}
