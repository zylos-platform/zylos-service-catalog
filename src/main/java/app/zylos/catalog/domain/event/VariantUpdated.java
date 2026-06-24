package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * A variant's business fields (SKU, list price, or attributes) were updated.
 */
public record VariantUpdated(ProductId productId, VariantId variantId, long version) implements DomainEvent {}
