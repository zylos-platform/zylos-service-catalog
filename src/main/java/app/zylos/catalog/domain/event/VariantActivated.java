package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * A variant became purchasable ({@code ACTIVE}).
 */
public record VariantActivated(ProductId productId, VariantId variantId, long version) implements DomainEvent {}
