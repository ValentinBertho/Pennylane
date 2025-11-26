package fr.mismo.pennylane.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dto.supplier.ResponseSupplier;
import fr.mismo.pennylane.dto.supplier.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class SupplierApi {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.url_v1}")
    private String apiUrl;

    @Value("${api.url_v2}")
    private String apiUrlV2;

    @Autowired
    public SupplierApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Supplier createSupplier(Supplier supplier, SiteEntity site) {
        String url = apiUrl + "suppliers";
        try {
            HttpHeaders headers = createHeaders(site.getPennylaneToken());
            HttpEntity<Supplier> requestEntity = new HttpEntity<>(supplier, headers);

            ResponseEntity<Supplier> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Supplier.class
            );

            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);

            return response.getBody();
        } catch (Exception e) {
            handleException("createSupplier", url, e);
            return null;
        }
    }
    public Supplier retrieveSupplier(String supplierId, SiteEntity site) {
        String url = apiUrlV2 + "suppliers/" + supplierId;
        try {
            ResponseEntity<Supplier> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(site.getPennylaneToken())),
                    Supplier.class
            );

            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else {
                handleException("retrieveSupplier", url, e);
                return null;
            }
        } catch (Exception e) {
            handleException("retrieveSupplier", url, e);
            return null;
        }
    }

    private void handleException(String methodName, String url, Exception e) {
        if (e instanceof IOException) {
            log.error("Méthode: {}, URL: {}, Erreur d'entrée/sortie: {}", methodName, url, e.getMessage(), e);
        } else if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpException = (HttpClientErrorException) e;
            log.error("Méthode: {}, URL: {}, Erreur HTTP: {} - {}", methodName, url, httpException.getStatusCode(), httpException.getStatusText(), e);
        } else if (e instanceof HttpStatusCodeException) {
            HttpStatusCodeException statusCodeException = (HttpStatusCodeException) e;
            log.error("Méthode: {}, URL: {}, Code de statut HTTP: {} - {}", methodName, url, statusCodeException.getStatusCode(), statusCodeException.getStatusText(), e);
        } else {
            log.error("Méthode: {}, URL: {}, Erreur inconnue: {}", methodName, url, e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
