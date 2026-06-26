package app.zylos.catalog.adapter.out.projection.opensearch;

/**
 * Raised when a projection write fails for a non-recoverable-here reason; propagated so the consumer's
 * error handler can retry and ultimately route to the DLQ.
 */
public class ProjectionException extends RuntimeException {

    public ProjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
