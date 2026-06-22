package app.zylos.catalog.domain.exception;

import java.io.Serial;

/**
 * Base type for all domain-rule violations raised within the Catalog bounded context.
 *
 * <p>Unchecked by design: the domain signals rule violations by throwing, and inbound adapters
 * translate these into protocol-specific responses (HTTP 4xx, gRPC status). The domain itself
 * stays free of any transport or framework concern.
 */
public class CatalogDomainException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CatalogDomainException(String message) {
        super(message);
    }

    public CatalogDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
