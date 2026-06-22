package app.zylos.catalog.domain.model;

import java.io.Serial;

import app.zylos.catalog.domain.exception.CatalogDomainException;

/**
 * Raised when a product is asked to move between two lifecycle states the {@link ProductStatus}
 * state machine does not permit (e.g. re-publishing a discontinued product). Co-located with the
 * status enum so the {@code exception} package stays free of any {@code model} dependency (no
 * package cycle).
 */
public class IllegalStatusTransitionException extends CatalogDomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ProductStatus from;
    private final ProductStatus to;

    public IllegalStatusTransitionException(ProductStatus from, ProductStatus to) {
        super("Illegal product status transition: %s -> %s".formatted(from, to));
        this.from = from;
        this.to = to;
    }

    public ProductStatus from() {
        return from;
    }

    public ProductStatus to() {
        return to;
    }
}
