package app.zylos.catalog.application.port.in;

import app.zylos.catalog.application.command.AddVariantCommand;
import app.zylos.catalog.application.command.CreateProductCommand;
import app.zylos.catalog.application.command.UpdateProductDetailsCommand;
import app.zylos.catalog.application.command.UpdateVariantCommand;
import app.zylos.catalog.domain.vo.ProductId;
import app.zylos.catalog.domain.vo.VariantId;

/**
 * Inbound (driving) port for product write use cases. Implemented by an application service
 * and invoked by driving adapters (REST/gRPC). Each method corresponds to a single
 * aggregate command and runs in its own transaction; the implementation enriches and publishes the
 * resulting domain events via the transactional outbox.
 */
public interface ProductCommandPort {

    ProductId createProduct(CreateProductCommand command);

    void updateProductDetails(UpdateProductDetailsCommand command);

    VariantId addVariant(AddVariantCommand command);

    void updateVariant(UpdateVariantCommand command);

    void activateVariant(ProductId productId, VariantId variantId);

    void deactivateVariant(ProductId productId, VariantId variantId);

    void discontinueVariant(ProductId productId, VariantId variantId);

    void publishProduct(ProductId productId);

    void unpublishProduct(ProductId productId);

    void discontinueProduct(ProductId productId);
}
