package com.bhadabazaar.BhadaBazaar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    /**
     * RestTemplate for outbound calls to third-party APIs (Cloudflare Images, Turnstile).
     * A plain {@code new RestTemplate()} has no timeouts, so a single slow/hung upstream can pin
     * Tomcat threads indefinitely until the pool is exhausted and the whole app stops responding.
     * Bounding connect + read time guarantees those threads are always released.
     */
    @Bean
    public RestTemplate externalApiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);  // max 5s to establish the TCP connection
        factory.setReadTimeout(15_000);    // max 15s waiting for the response
        return new RestTemplate(factory);
    }
}
