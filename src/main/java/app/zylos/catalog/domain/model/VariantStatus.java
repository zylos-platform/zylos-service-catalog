package app.zylos.catalog.domain.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import app.zylos.catalog.domain.exception.IllegalStatusTransitionException;

/**
 * Availability of a {@code ProductVariant}.
 *
 * <ul>
 *   <li>{@code ACTIVE} — purchasable.
 *   <li>{@code INACTIVE} — temporarily paused; can be reactivated.
 *   <li>{@code DISCONTINUED} — terminal; permanently soft-deleted.
 * </ul>
 *
 * <p>A new variant starts {@code ACTIVE}. When every variant of a product becomes non-purchasable
 * ({@code INACTIVE} or {@code DISCONTINUED}), the {@code Product} aggregate auto-demotes itself to
 * {@code UNPUBLISHED} so a live product never displays with zero buyable options.
 */
public enum VariantStatus {
    ACTIVE,
    INACTIVE,
    DISCONTINUED;

    private static final Map<VariantStatus, Set<VariantStatus>> ALLOWED_TARGETS = new EnumMap<>(VariantStatus.class);

    static {
        ALLOWED_TARGETS.put(ACTIVE, Set.of(INACTIVE, DISCONTINUED));
        ALLOWED_TARGETS.put(INACTIVE, Set.of(ACTIVE, DISCONTINUED));
        ALLOWED_TARGETS.put(DISCONTINUED, Set.of());
    }

    public boolean isPurchasable() {
        return this == ACTIVE;
    }

    public boolean canTransitionTo(VariantStatus target) {
        return ALLOWED_TARGETS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return ALLOWED_TARGETS.getOrDefault(this, Set.of()).isEmpty();
    }

    public void ensureCanTransitionTo(VariantStatus target) {
        Objects.requireNonNull(target, "target must not be null");

        if (!canTransitionTo(target)) {
            throw new IllegalStatusTransitionException(this, target);
        }
    }
}
