package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;

/**
 * A product's core details were updated.
 */
public record ProductUpdated(ProductId productId, long version) implements DomainEvent {}
