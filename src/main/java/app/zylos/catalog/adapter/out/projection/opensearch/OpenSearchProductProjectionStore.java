package app.zylos.catalog.adapter.out.projection.opensearch;

import java.io.IOException;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.VersionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import app.zylos.catalog.application.port.out.ProductProjectionStore;
import app.zylos.catalog.application.projection.ProductProjection;

/**
 * Writes the catalog read model to OpenSearch as an idempotent, last-writer-wins upsert. The aggregate
 * {@code version} is used as the document's external version, so OpenSearch rejects any stale or
 * duplicate event with a 409, which is treated as a successful no-op. All operations target the
 * {@code catalog-products} write alias.
 */
@Component
public class OpenSearchProductProjectionStore implements ProductProjectionStore {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchProductProjectionStore.class);

    private final OpenSearchClient client;
    private final String alias;

    public OpenSearchProductProjectionStore(
            OpenSearchClient catalogOpenSearchClient,
            @Value("${zylos.opensearch.alias:catalog-products}") String alias) {
        this.client = catalogOpenSearchClient;
        this.alias = alias;
    }

    @Override
    public void upsert(ProductProjection projection) {
        try {
            client.index(i -> i.index(alias)
                    .id(projection.productId())
                    .document(projection)
                    .version(projection.version())
                    .versionType(VersionType.External));
        } catch (OpenSearchException e) {
            if (e.status() == 409) {
                log.debug(
                        "Ignoring stale projection for product {} at version {} (version conflict)",
                        projection.productId(),
                        projection.version());
                return;
            }
            throw new ProjectionException("Failed to upsert projection for product " + projection.productId(), e);
        } catch (IOException e) {
            throw new ProjectionException("I/O error upserting projection for product " + projection.productId(), e);
        }
    }
}
