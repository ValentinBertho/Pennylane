package fr.mismo.pennylane.configuration;

import fr.mismo.pennylane.api.RequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestClientConfig {

    private final RequestInterceptor requestInterceptor; // ✅ Injection Spring

    // ✅ Constructeur pour injecter l'intercepteur
    @Autowired
    public RestClientConfig(RequestInterceptor requestInterceptor) {
        this.requestInterceptor = requestInterceptor;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }

        // ✅ Utiliser l'instance injectée par Spring (avec LogHelper déjà injecté)
        interceptors.add(requestInterceptor);

        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}