package fr.mismo.pennylane.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dto.customer.Customer;
import fr.mismo.pennylane.dto.customer.ResponseCustomer;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CustomerApi {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.url_v1}")
    private String apiUrl;

    @Value("${api.url_v2}")
    private String apiUrlV2;


    @Autowired
    public CustomerApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Customer createCustomer(Customer customer, SiteEntity site) {
        String url = apiUrl + "company_customers";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("content-type", "application/json");
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());

            HttpEntity<Customer> requestEntity = new HttpEntity<>(customer, headers);

            ResponseEntity<Customer> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Customer.class
            );
            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);

            return response.getBody();
        } catch (Exception e) {
            handleException("createCustomer", url, e);
            return null;
        }
    }

    //TODO OBSOLETE
    /*public List<Customer> listCustomers(SiteEntity site) {
        String url = apiUrl + "customers?page=1&perpage=200000";

        try {
            ResponseEntity<ResponseCustomer> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headerBuilder(site.getPennylaneToken())),
                    ResponseCustomer.class
            );

            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(1100);

            ResponseCustomer apiResponse = response.getBody();
            if (apiResponse != null) {
                return apiResponse.getItems();
            } else {
                log.warn("Réponse vide pour l'URL : {}", url);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            handleException("listCustomers", url, e);
            return Collections.emptyList();
        }
    }*/

    public ResponseCustomer findCustomerByLedgerAccount(SiteEntity site, Long ledgerAccountId) {
        try {
            // Construction du filtre JSON pour ledger_account_id
            String filterJson = String.format(
                    "[{\"field\": \"ledger_account_id\", \"operator\": \"eq\", \"value\": \"%s\"}]",
                    ledgerAccountId.toString()
            );

            String url = apiUrl + "customers?filter={filter}&sort=-id";

            // Appel à l'API avec paramètre filter
            ResponseEntity<ResponseCustomer> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headerBuilder(site.getPennylaneToken())),
                    ResponseCustomer.class,
                    filterJson  // Passé directement comme variable URI
            );

            Thread.sleep(1100); // Respect limite de requêtes

            ResponseCustomer apiResponse = response.getBody();
            if (apiResponse != null && !apiResponse.getItems().isEmpty()) {
                return apiResponse;
            } else {
                log.warn("Aucun client trouvé pour ledger_account_id: {}", ledgerAccountId);
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }



    public Customer retrieveCustomer(String customerId, SiteEntity site) {
        String url = apiUrl + "customers/" + customerId;

        try {
            ResponseEntity<Customer> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headerBuilder(site.getPennylaneToken())),
                    Customer.class
            );

            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("Client non trouvé pour l'ID : {}", customerId);
                return null;
            } else {
                handleException("retrieveCustomer", url, e);
                return null;
            }
        } catch (Exception e) {
            handleException("retrieveCustomer", url, e);
            return null;
        }
    }

    public Customer updateCustomer(Customer wrapper, SiteEntity site) {

        String url = apiUrl + "company_customers/" + wrapper.getId();

        wrapper.setLedgerAccount(null);

        // Ne pas envoyer le source ID lors d'un PUT.
        wrapper.setId(null);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("content-type", "application/json");
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());

            HttpEntity<Customer> requestEntity = new HttpEntity<>(wrapper, headers);

            ResponseEntity<Customer> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    Customer.class
            );


            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);

            return response.getBody();
        } catch (Exception e) {
            handleException("updateCustomer", url, e);
            return null;
        }
    }

    private void handleException(String methodName, String context, Exception e) {
        if (e instanceof IOException) {
            log.error("Méthode: {}, Contexte: {}, Erreur d'entrée/sortie : {}", methodName, context, e.getMessage(), e);
        } else if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpException = (HttpClientErrorException) e;
            log.error("Méthode: {}, Contexte: {}, Erreur HTTP : {} - {}", methodName, context, httpException.getStatusCode(), httpException.getStatusText(), e);
        } else {
            log.error("Méthode: {}, Contexte: {}, Erreur lors de l'appel à l'API : {}", methodName, context, e.getMessage(), e);
        }
    }

    private HttpHeaders headerBuilder(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
