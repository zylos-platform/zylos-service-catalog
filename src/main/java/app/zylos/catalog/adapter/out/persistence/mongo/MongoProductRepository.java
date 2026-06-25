package app.zylos.catalog.adapter.out.persistence.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import app.zylos.catalog.application.port.out.ProductRepository;
import app.zylos.catalog.domain.event.DomainEvent;
import app.zylos.catalog.domain.model.Product;
import app.zylos.catalog.domain.vo.ProductId;

@Repository
public class MongoProductRepository implements ProductRepository {

    private final MongoTemplate mongoTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OutboxEventFactory outboxEventFactory;

    public MongoProductRepository(
            MongoTemplate mongoTemplate,
            MongoTransactionManager transactionManager,
            OutboxEventFactory outboxEventFactory) {
        this.mongoTemplate = mongoTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.outboxEventFactory = outboxEventFactory;
    }

    @Override
    public void save(Product product) {
        List<DomainEvent> events = product.pullDomainEvents();
        ProductDocument document = ProductDocumentMapper.toDocument(product);
        List<OutboxEventDocument> outboxEvents = outboxEventFactory.from(product, events);

        transactionTemplate.executeWithoutResult(_ -> {
            mongoTemplate.save(document);
            if (!outboxEvents.isEmpty()) {
                mongoTemplate.insert(outboxEvents, OutboxEventDocument.class);
            }
        });
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        ProductDocument document = mongoTemplate.findById(productId.value().toString(), ProductDocument.class);
        return Optional.ofNullable(document).map(ProductDocumentMapper::toDomain);
    }
}
