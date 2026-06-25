package app.zylos.catalog.adapter.out.persistence.mongo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.f4b6a3.uuid.UuidCreator;

import app.zylos.catalog.domain.event.DomainEvent;
import app.zylos.catalog.domain.model.Product;
import app.zylos.catalog.domain.model.ProductVariant;
import app.zylos.catalog.domain.vo.AttributeValue;
import app.zylos.catalog.domain.vo.AttributeValue.BooleanValue;
import app.zylos.catalog.domain.vo.AttributeValue.NumberValue;
import app.zylos.catalog.domain.vo.AttributeValue.TextValue;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.contracts.catalog.v1.AttributeEntry;
import app.zylos.contracts.catalog.v1.CatalogProductEvent;
import app.zylos.contracts.catalog.v1.ProductState;
import app.zylos.contracts.catalog.v1.VariantState;
import app.zylos.contracts.common.v1.Money;
import app.zylos.contracts.common.v1.Producer;

import io.confluent.kafka.serializers.KafkaAvroSerializer;

/**
 * Builds outbox records from an aggregate's recorded domain events. Each thin domain event is enriched
 * into a {@link CatalogProductEvent} envelope carrying a full ECST {@link ProductState} snapshot (built
 * once per command), which is serialized to Avro/Confluent-wire-format bytes and stored in the outbox.
 */
@Component
public class OutboxEventFactory {

    private static final String AGGREGATE_TYPE = "product";
    private static final int EVENT_SCHEMA_VERSION = 1;

    private final KafkaAvroSerializer serializer;
    private final String eventTopic;
    private final String producerService;
    private final String producerVersion;

    public OutboxEventFactory(
            KafkaAvroSerializer catalogEventAvroSerializer,
            @Value("${zylos.catalog.event-topic:catalog.product.events.v1}") String eventTopic,
            @Value("${spring.application.name:zylos-service-catalog}") String producerService,
            @Value("${zylos.service.version:0.0.0}") String producerVersion) {
        this.serializer = catalogEventAvroSerializer;
        this.eventTopic = eventTopic;
        this.producerService = producerService;
        this.producerVersion = producerVersion;
    }

    private static Money money(app.zylos.catalog.domain.vo.Money money) {
        return Money.newBuilder()
                .setMinorUnits(money.minorUnits())
                .setCurrency(money.currency().getCurrencyCode())
                .build();
    }

    private static List<AttributeEntry> attributes(ProductAttributes attributes) {
        List<AttributeEntry> entries = new ArrayList<>();
        attributes.values().forEach((name, value) -> entries.add(toAttributeEntry(name, value)));
        return entries;
    }

    private static AttributeEntry toAttributeEntry(String name, AttributeValue value) {
        return switch (value) {
            case TextValue t ->
                AttributeEntry.newBuilder()
                        .setName(name)
                        .setType("TEXT")
                        .setValue(t.value())
                        .build();
            case NumberValue n ->
                AttributeEntry.newBuilder()
                        .setName(name)
                        .setType("NUMBER")
                        .setValue(n.value().toString())
                        .build();
            case BooleanValue b ->
                AttributeEntry.newBuilder()
                        .setName(name)
                        .setType("BOOLEAN")
                        .setValue(Boolean.toString(b.value()))
                        .build();
        };
    }

    public List<OutboxEventDocument> from(Product product, List<DomainEvent> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        ProductState state = productState(product);
        Producer producer = Producer.newBuilder()
                .setService(producerService)
                .setVersion(producerVersion)
                .build();
        Instant occurredAt = Instant.now();
        String aggregateId = product.id().value().toString();

        List<OutboxEventDocument> records = new ArrayList<>(events.size());

        for (DomainEvent event : events) {
            String eventId = UuidCreator.getTimeOrderedEpoch().toString();
            String eventType = event.getClass().getSimpleName();

            CatalogProductEvent envelope = CatalogProductEvent.newBuilder()
                    .setEventId(eventId)
                    .setEventType(eventType)
                    .setEventVersion(EVENT_SCHEMA_VERSION)
                    .setAggregateId(aggregateId)
                    .setAggregateType(AGGREGATE_TYPE)
                    .setOccurredAt(occurredAt)
                    .setCorrelationId(null)
                    .setCausationId(null)
                    .setProducer(producer)
                    .setPayload(state)
                    .build();

            byte[] payload = serializer.serialize(eventTopic, envelope);
            records.add(new OutboxEventDocument(
                    eventId, eventType, aggregateId, AGGREGATE_TYPE, occurredAt.toEpochMilli(), payload));
        }
        return records;
    }

    private ProductState productState(Product product) {
        List<VariantState> variants = new ArrayList<>();

        for (ProductVariant variant : product.variants()) {
            variants.add(VariantState.newBuilder()
                    .setVariantId(variant.id().value().toString())
                    .setSku(variant.sku().value())
                    .setListPrice(money(variant.listPrice()))
                    .setAttributes(attributes(variant.attributes()))
                    .setStatus(variant.status().name())
                    .build());
        }

        return ProductState.newBuilder()
                .setProductId(product.id().value().toString())
                .setSellerId(product.sellerId().value().toString())
                .setName(product.name())
                .setDescription(product.description())
                .setCategoryId(product.categoryId().value().toString())
                .setAttributes(attributes(product.attributes()))
                .setStatus(product.status().name())
                .setVersion(product.version())
                .setVariants(variants)
                .build();
    }
}
