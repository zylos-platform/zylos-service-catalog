package app.zylos.catalog.domain.event;

import app.zylos.catalog.domain.vo.ProductId;

/**
 * A fact that has happened to a Catalog aggregate. Domain events are intentionally thin: they carry
 * the aggregate identity and the version they produced, plus only data intrinsic to the intent.
 * Full event-carried state-transfer snapshots for the wire are assembled by the outbox adapter
 * from current aggregate state, keeping the domain free of serialization and envelope
 * concerns.
 */
public sealed interface DomainEvent
        permits ProductCreated,
                ProductUpdated,
                ProductPublished,
                ProductUnpublished,
                ProductDiscontinued,
                VariantAdded,
                VariantUpdated,
                VariantActivated,
                VariantDeactivated,
                VariantDiscontinued {

    ProductId productId();

    long version();
}
