package pl.tomaszko.s04e02.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HubClientConfig {

    @Bean
    RestClient hubRestClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder.build();
    }
}
