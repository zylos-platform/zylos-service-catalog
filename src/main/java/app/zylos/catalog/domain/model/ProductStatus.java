package app.zylos.catalog.domain.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import app.zylos.catalog.domain.exception.IllegalStatusTransitionException;

/**
 * Lifecycle state of a {@code Product} aggregate and the rules governing transitions.
 *
 * <ul>
 *   <li>{@code DRAFT} — never been live; a one-way on-ramp. Reworking a live product is done via
 *       {@code UNPUBLISHED}, never by returning to {@code DRAFT}.
 *   <li>{@code PUBLISHED} — visible on the storefront.
 *   <li>{@code UNPUBLISHED} — temporarily hidden; can be re-published.
 *   <li>{@code DISCONTINUED} — terminal; permanently dead (soft-deleted).
 * </ul>
 */
public enum ProductStatus {
    DRAFT,
    PUBLISHED,
    UNPUBLISHED,
    DISCONTINUED;

    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED_TARGETS = new EnumMap<>(ProductStatus.class);

    static {
        ALLOWED_TARGETS.put(DRAFT, Set.of(PUBLISHED, DISCONTINUED));
        ALLOWED_TARGETS.put(PUBLISHED, Set.of(UNPUBLISHED, DISCONTINUED));
        ALLOWED_TARGETS.put(UNPUBLISHED, Set.of(PUBLISHED, DISCONTINUED));
        ALLOWED_TARGETS.put(DISCONTINUED, Set.of());
    }

    public boolean canTransitionTo(ProductStatus target) {
        return ALLOWED_TARGETS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return ALLOWED_TARGETS.getOrDefault(this, Set.of()).isEmpty();
    }

    public void ensureCanTransitionTo(ProductStatus target) {
        Objects.requireNonNull(target, "target must not be null");

        if (!canTransitionTo(target)) {
            throw new IllegalStatusTransitionException(this, target);
        }
    }
}
