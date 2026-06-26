package app.zylos.catalog.adapter.out.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.util.Currency;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import app.zylos.catalog.application.port.out.ProductRepository;
import app.zylos.catalog.domain.model.Product;
import app.zylos.catalog.domain.model.VariantDraft;
import app.zylos.catalog.domain.vo.*;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

@Testcontainers
@DataMongoTest
class MongoProductRepositoryIT {

    private static final Currency USD = Currency.getInstance("USD");
    private static final String TOPIC = "catalog.product.events.v1";

    @Container
    @ServiceConnection
    static MongoDBContainer mongo =
            new MongoDBContainer("mongodb/mongodb-community-server:8.0.5-ubuntu2204").withReplicaSet();

    @Autowired
    private MongoDatabaseFactory databaseFactory;

    private MongoTemplate mongoTemplate;
    private MongoTransactionManager transactionManager;
    private OutboxEventFactory outboxEventFactory;

    private static Product sampleDraftProduct() {
        return Product.create(
                ProductId.newId(),
                SellerId.newId(),
                "Wireless Headphones",
                "Over-ear",
                CategoryId.newId(),
                ProductAttributes.empty().with("brand", AttributeValue.of("Acme")),
                List.of(new VariantDraft(
                        VariantId.newId(), Sku.of("SKU-1"), Money.ofMinor(19999, USD), ProductAttributes.empty())));
    }

    @BeforeEach
    void setUp() {
        mongoTemplate = new MongoTemplate(databaseFactory);
        transactionManager = new MongoTransactionManager(databaseFactory);
        KafkaAvroSerializer serializer =
                new KafkaAvroSerializer(new MockSchemaRegistryClient(), Map.of("schema.registry.url", "mock://test"));
        outboxEventFactory = new OutboxEventFactory(serializer, TOPIC, "zylos-service-catalog", "test");
        mongoTemplate.getDb().drop();
    }

    @Test
    void savePersistsAggregateAndOutboxEventTogether() {
        ProductRepository repository =
                new MongoProductRepository(mongoTemplate, transactionManager, outboxEventFactory);
        Product product = sampleDraftProduct();

        repository.save(product);

        assertThat(mongoTemplate.findById(product.id().value().toString(), ProductDocument.class))
                .isNotNull();

        List<OutboxEventDocument> events = mongoTemplate.findAll(OutboxEventDocument.class);
        assertThat(events).hasSize(1);
        OutboxEventDocument event = events.getFirst();
        assertThat(event.eventType()).isEqualTo("ProductCreated");
        assertThat(event.aggregateType()).isEqualTo("product");
        assertThat(event.aggregateId()).isEqualTo(product.id().value().toString());
        assertThat(event.payload()).isNotEmpty();
    }

    @Test
    void saveWritesNeitherWhenOutboxInsertFails() {
        MongoTemplate spyTemplate = spy(new MongoTemplate(databaseFactory));
        doThrow(new IllegalStateException("simulated outbox failure"))
                .when(spyTemplate)
                .insert(any(), eq(OutboxEventDocument.class));
        ProductRepository repository = new MongoProductRepository(spyTemplate, transactionManager, outboxEventFactory);
        Product product = sampleDraftProduct();

        assertThatThrownBy(() -> repository.save(product)).isInstanceOf(IllegalStateException.class);

        assertThat(mongoTemplate.findById(product.id().value().toString(), ProductDocument.class))
                .isNull();
        assertThat(mongoTemplate.findAll(OutboxEventDocument.class)).isEmpty();
    }
}
