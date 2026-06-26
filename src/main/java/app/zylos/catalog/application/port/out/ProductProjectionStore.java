package app.zylos.catalog.application.port.out;

import app.zylos.catalog.application.projection.ProductProjection;

/**
 * Outbound port for the catalog read model. Implementations perform an idempotent, last-writer-wins
 * upsert keyed by {@code productId} and gated by {@link ProductProjection#version()} via the store's
 * native external versioning, so a stale or duplicate version is silently ignored. Discontinuation is
 * modeled as an upsert with {@code visibility=DISCONTINUED}, so no delete operation is required.
 */
public interface ProductProjectionStore {

    void upsert(ProductProjection projection);
}
