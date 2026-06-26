package app.zylos.catalog.adapter.out.projection.opensearch;

import java.net.URI;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenSearch client for the catalog read model. Local/dev runs the security plugin disabled, so this
 * connects over plain HTTP with no credentials; production (AWS OpenSearch Service).
 */
@Configuration(proxyBeanMethods = false)
public class CatalogOpenSearchConfig {

    @Value("${zylos.opensearch.uri}")
    String uri;

    @Bean
    OpenSearchClient catalogOpenSearchClient() {
        URI parsed = URI.create(uri);
        HttpHost host = new HttpHost(parsed.getScheme(), parsed.getHost(), parsed.getPort());

        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder.builder(host)
                .setMapper(new JacksonJsonpMapper())
                .build();
        return new OpenSearchClient(transport);
    }
}
