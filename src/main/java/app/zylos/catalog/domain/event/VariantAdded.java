package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * A variant was added to a product.
 */
public record VariantAdded(ProductId productId, VariantId variantId, long version) implements DomainEvent {}
