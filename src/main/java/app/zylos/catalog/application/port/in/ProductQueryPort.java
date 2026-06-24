package app.zylos.catalog.application.port.in;

import java.util.Optional;

import app.zylos.catalog.application.query.ProductView;
import app.zylos.catalog.domain.vo.ProductId;

/**
 * Inbound (driving) port for product read use cases. This phase exposes read-your-writes lookup by
 * identity against the write model; seller-scoped, category, paginated, and faceted browse queries
 * arrive with the query adapters and OpenSearch read model.
 */
public interface ProductQueryPort {

    Optional<ProductView> findById(ProductId productId);
}
