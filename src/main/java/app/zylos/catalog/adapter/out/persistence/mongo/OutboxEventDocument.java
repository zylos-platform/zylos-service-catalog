package app.zylos.catalog.adapter.out.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outbox")
public record OutboxEventDocument(
        @Id String eventId,
        String eventType,
        String aggregateId,
        String aggregateType,
        long occurredAt,
        byte[] payload) {}
