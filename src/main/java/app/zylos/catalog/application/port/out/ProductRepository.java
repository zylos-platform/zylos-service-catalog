package app.zylos.catalog.application.port.out;

import java.util.Optional;

import app.zylos.catalog.domain.model.Product;
import app.zylos.catalog.domain.vo.ProductId;

/**
 * Outbound (driven) port for product persistence. The MongoDB adapter implements this by writing the
 * aggregate document and the events the aggregate recorded to the transactional outbox atomically
 * within a single multi-document transaction. The application service depends on this interface,
 * never on the adapter.
 */
public interface ProductRepository {

    void save(Product product);

    Optional<Product> findById(ProductId productId);
}
