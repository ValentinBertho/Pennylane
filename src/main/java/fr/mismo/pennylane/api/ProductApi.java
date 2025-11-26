package fr.mismo.pennylane.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dto.product.Product;
import fr.mismo.pennylane.dto.product.ResponseProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ProductApi {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.url_v1}")
    private String apiUrl;

    @Value("${api.url_v2}")
    private String apiUrlV2;

    @Autowired
    public ProductApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Product createProduct(Product product, SiteEntity site) {
        String url = apiUrl + "products";

        product.setId(null);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("content-type", "application/json");

            HttpEntity<Product> requestEntity = new HttpEntity<>(product, headerBuilder(site.getPennylaneToken()));

            ResponseEntity<Product> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Product.class
            );

            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);

            Product apiResponse = response.getBody();
            return apiResponse != null ? apiResponse : null;
        } catch (Exception e) {
            handleException("createProduct", url, e);
            return null;
        }
    }

      public List<Product> listAllProducts(SiteEntity site) {
        String baseUrl = apiUrlV2 + "products";
        List<Product> allProducts = new ArrayList<>();
        String cursor = null;
        boolean hasMore = true;

        while (hasMore) {
            String url = buildUrl(baseUrl, cursor);
            ResponseProduct response = getProductPage(url, site);

            if (response == null) break;

            allProducts.addAll(response.getItems());
            hasMore = response.isHasMore();
            cursor = response.getNextCursor();
        }
        return allProducts;
    }

    private String buildUrl(String baseUrl, String cursor) {
        String url = baseUrl + "?limit=100";
        if (cursor != null && !cursor.isEmpty()) {
            url += "&cursor=" + cursor;
        }
        return url;
    }

    private ResponseProduct getProductPage(String url, SiteEntity site) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(headerBuilder(site.getPennylaneToken()));
            ResponseEntity<ResponseProduct> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ResponseProduct.class
            );

            Thread.sleep(1100); // Respect des limites de débit
            return response.getBody();
        } catch (Exception e) {
            handleException("getProductPage", url, e);
            return null;
        }
    }

    public Product retrieveProduct(String productId, SiteEntity site) {
        String url = apiUrl + "products/" + productId;

        try {
            ResponseEntity<Product> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headerBuilder(site.getPennylaneToken())),
                    Product.class
            );

            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);

            Product apiResponse = response.getBody();
            return apiResponse != null ? apiResponse : null;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else {
                handleException("retrieveProduct", url, e);
            }
        } catch (Exception e) {
            handleException("retrieveProduct", url, e);
        }
        return null;
    }

    public Product updateProduct(Product product, SiteEntity site) {
        String url = apiUrl + "products/" + product.getId();
        product.setId(null);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("content-type", "application/json");

            HttpEntity<Product> requestEntity = new HttpEntity<>(product, headerBuilder(site.getPennylaneToken()));

            ResponseEntity<Product> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    Product.class
            );

            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);

            Product apiResponse = response.getBody();
            return apiResponse != null ? apiResponse : null;
        } catch (Exception e) {
            handleException("updateProduct", url, e);
            return null;
        }
    }

    private void handleException(String methodName, String url, Exception e) {
        if (e instanceof IOException) {
            log.error("Méthode: {}, URL: {}, Erreur d'entrée/sortie: {}", methodName, url, e.getMessage(), e);
        } else if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpException = (HttpClientErrorException) e;
            log.error("Méthode: {}, URL: {}, Erreur HTTP: {} - {}", methodName, url, httpException.getStatusCode(), httpException.getStatusText(), e);
        } else {
            log.error("Méthode: {}, URL: {}, Erreur lors de l'appel à l'API: {}", methodName, url, e.getMessage(), e);
        }
    }

    private HttpHeaders headerBuilder(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
