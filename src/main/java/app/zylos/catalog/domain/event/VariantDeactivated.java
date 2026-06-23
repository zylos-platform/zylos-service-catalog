package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * A variant was temporarily paused ({@code INACTIVE}).
 */
public record VariantDeactivated(ProductId productId, VariantId variantId, long version) implements DomainEvent {}
