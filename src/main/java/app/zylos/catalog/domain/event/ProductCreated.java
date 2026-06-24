package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;

/**
 * A new product was created (in {@code DRAFT}, at version 1).
 */
public record ProductCreated(ProductId productId, long version) implements DomainEvent {}
