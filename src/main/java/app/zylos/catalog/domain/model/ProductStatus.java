package app.zylos.catalog.domain.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lifecycle state of a {@code Product} aggregate and the rules governing transitions.
 *
 * <ul>
 *   <li>{@code DRAFT} — created, not yet buyer-visible; may be published or abandoned.
 *   <li>{@code PUBLISHED} — live and browsable; may be discontinued.
 *   <li>{@code DISCONTINUED} — terminal; no further transitions permitted.
 * </ul>
 */
public enum ProductStatus {
    DRAFT,
    PUBLISHED,
    DISCONTINUED;

    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED_TARGETS = new EnumMap<>(ProductStatus.class);

    static {
        ALLOWED_TARGETS.put(DRAFT, Set.of(PUBLISHED, DISCONTINUED));
        ALLOWED_TARGETS.put(PUBLISHED, Set.of(DISCONTINUED));
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
