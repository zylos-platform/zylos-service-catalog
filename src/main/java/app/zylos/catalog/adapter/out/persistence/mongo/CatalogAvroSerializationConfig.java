package app.zylos.catalog.adapter.out.persistence.mongo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.confluent.kafka.serializers.KafkaAvroSerializer;

@Configuration(proxyBeanMethods = false)
public class CatalogAvroSerializationConfig {

    @Value("${zylos.kafka.schema-registry-url}")
    private String schemaRegistryUrl;

    @Value("${zylos.kafka.auto-register-schemas:true}")
    private boolean autoRegisterSchemas;

    @Bean
    KafkaAvroSerializer catalogEventAvroSerializer() {
        Map<String, Object> config = new HashMap<>();
        config.put("schema.registry.url", schemaRegistryUrl);
        config.put("auto.register.schemas", autoRegisterSchemas);
        config.put("avro.remove.java.properties", true);

        KafkaAvroSerializer serializer = new KafkaAvroSerializer();
        serializer.configure(config, false);
        return serializer;
    }
}
