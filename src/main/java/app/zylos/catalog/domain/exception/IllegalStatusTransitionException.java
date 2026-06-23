package app.zylos.catalog.domain.exception;

import java.io.Serial;

/**
 * Raised when an aggregate or entity is asked to move between two lifecycle states its state machine
 * does not permit (e.g. re-publishing a discontinued product, reactivating a discontinued variant).
 */
public class IllegalStatusTransitionException extends CatalogDomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Enum<?> from;
    private final Enum<?> to;

    public IllegalStatusTransitionException(Enum<?> from, Enum<?> to) {
        super("Illegal %s transition: %s -> %s".formatted(from.getClass().getSimpleName(), from.name(), to.name()));
        this.from = from;
        this.to = to;
    }

    public Enum<?> from() {
        return from;
    }

    public Enum<?> to() {
        return to;
    }
}
