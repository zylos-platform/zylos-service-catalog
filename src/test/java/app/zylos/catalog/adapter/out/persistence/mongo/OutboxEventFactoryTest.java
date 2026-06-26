package app.zylos.catalog.adapter.out.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.model.Product;
import app.zylos.catalog.domain.model.VariantDraft;
import app.zylos.catalog.domain.vo.*;
import app.zylos.contracts.catalog.v1.CatalogProductEvent;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

class OutboxEventFactoryTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final String TOPIC = "catalog.product.events.v1";

    private final MockSchemaRegistryClient srClient = new MockSchemaRegistryClient();
    private final KafkaAvroSerializer serializer = new KafkaAvroSerializer(
            srClient, Map.of("schema.registry.url", "mock://test", "avro.remove.java.properties", true));
    private final OutboxEventFactory factory =
            new OutboxEventFactory(serializer, TOPIC, "zylos-service-catalog", "test");
    private final KafkaAvroDeserializer deserializer = new KafkaAvroDeserializer(
            srClient, Map.of("schema.registry.url", "mock://test", "specific.avro.reader", true));

    @Test
    void buildsAndSerializesEnvelopeWithFullSnapshot() {
        Product product = Product.create(
                ProductId.newId(),
                SellerId.newId(),
                "Wireless Headphones",
                "Over-ear",
                CategoryId.newId(),
                ProductAttributes.empty().with("warrantyYears", AttributeValue.of(new BigDecimal("2"))),
                List.of(new VariantDraft(
                        VariantId.newId(),
                        Sku.of("SKU-1"),
                        Money.ofMinor(19999, USD),
                        ProductAttributes.empty().with("color", AttributeValue.of("black")))));

        List<OutboxEventDocument> records = factory.from(product, product.pullDomainEvents());

        assertThat(records).hasSize(1);
        OutboxEventDocument recordEvent = records.getFirst();
        assertThat(recordEvent.eventType()).isEqualTo("ProductCreated");
        assertThat(recordEvent.aggregateType()).isEqualTo("product");
        assertThat(recordEvent.aggregateId()).isEqualTo(product.id().value().toString());
        assertThat(recordEvent.payload()).isNotEmpty();

        CatalogProductEvent envelope = (CatalogProductEvent) deserializer.deserialize(TOPIC, recordEvent.payload());
        assertThat(envelope.getEventType()).isEqualTo("ProductCreated");
        assertThat(envelope.getProducer().getService()).isEqualTo("zylos-service-catalog");
        assertThat(envelope.getPayload().getSellerId())
                .isEqualTo(product.sellerId().value().toString());
        assertThat(envelope.getPayload().getVersion()).isEqualTo(1L);
        assertThat(envelope.getPayload().getStatus()).isEqualTo("DRAFT");
        assertThat(envelope.getPayload().getVariants()).hasSize(1);
        assertThat(envelope.getPayload().getVariants().getFirst().getListPrice().getMinorUnits())
                .isEqualTo(19999L);
        assertThat(envelope.getPayload().getVariants().getFirst().getStatus()).isEqualTo("ACTIVE");
        assertThat(envelope.getPayload().getAttributes()).anySatisfy(a -> {
            assertThat(a.getName()).isEqualTo("warrantyyears");
            assertThat(a.getType()).isEqualTo("NUMBER");
            assertThat(a.getValue()).isEqualTo("2");
        });
    }
}
