package com.muczynski.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000); // 60 seconds
        factory.setReadTimeout(60000); // 60 seconds
        restTemplate.setRequestFactory(factory);

        // Add User-Agent header for all requests to comply with sites like Wikimedia
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            request.getHeaders().set("User-Agent", "library.muczynskifamily.com");
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(Collections.singletonList(interceptor));

        return restTemplate;
    }
}
