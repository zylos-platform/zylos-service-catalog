package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;

/**
 * A product transitioned to the terminal {@code DISCONTINUED} state.
 */
public record ProductDiscontinued(ProductId productId, long version) implements DomainEvent {}
