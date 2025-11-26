package fr.mismo.pennylane.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dto.accounting.AccountingResponse;
import fr.mismo.pennylane.dto.accounting.Item;
import fr.mismo.pennylane.dto.invoice.CategoryResponse;
import fr.mismo.pennylane.dto.invoice.FileAttachmentResponse;
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

import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@Component
public class AccountsApi {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.url_v2}")
    private String apiUrlV2;

    @Autowired
    public AccountsApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }



    public List<Item> listAllLedgerAccounts(SiteEntity site) {
        List<Item> allLedgerAccounts = new ArrayList<>();
        int currentPage = 1;
        int totalPages;

        do {
            String url = apiUrlV2 + "ledger_accounts?page=" + currentPage + "&per_page=1000";
            try {
                ResponseEntity<AccountingResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headerBuilder(site.getPennylaneToken())),
                        AccountingResponse.class
                );

                AccountingResponse apiResponse = response.getBody();
                if (apiResponse != null) {
                    allLedgerAccounts.addAll(apiResponse.getItems());
                    totalPages = apiResponse.getTotalPages();
                    currentPage++;
                } else {
                    break;
                }

                // Pause pour respecter la limite de 2 requêtes par seconde
                Thread.sleep(600);
            } catch (Exception e) {
                handleException("listAllLedgerAccounts", url, e);
                break;
            }
        } while (currentPage <= totalPages);

        return allLedgerAccounts;
    }

    public Item getLedgerAccountById(String id, SiteEntity site) {
        String url = apiUrlV2 + "ledger_accounts/" + id;

        try {
            ResponseEntity<Item> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headerBuilder(site.getPennylaneToken())),
                    Item.class
            );
            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);
            return response.getBody();
        } catch (Exception e) {
            handleException("getLedgerAccountById", url, e);
            return null;
        }
    }

    public Item getLedgerAccountByNumber(String number, SiteEntity site) {
        try {
            String filterJson = String.format(
                    "[{\"field\": \"number\", \"operator\": \"start_with\", \"value\": \"%s\"}]", number
            );

            String url = apiUrlV2 + "ledger_accounts?filter={filter}";

            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<AccountingResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    AccountingResponse.class,
                    filterJson
            );

            AccountingResponse apiResponse = response.getBody();

            Thread.sleep(1100); // Respect rate limit

            if (apiResponse != null && apiResponse.getItems() != null) {
                for (Item item : apiResponse.getItems()) {
                    if (item.getNumber().equals(number)) {
                        return item;
                    }
                }
                log.warn("Aucun ledger account EXACT avec le number: {}", number);
            }

            return null;

        } catch (Exception e) {
            handleException("getLedgerAccountByNumber", apiUrlV2 + "ledger_accounts", e);
            return null;
        }
    }



    public Item createLedgerAccount(Item newAccount, SiteEntity site) {
        String url = apiUrlV2 + "ledger_accounts";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("content-type", "application/json");
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());

            HttpEntity<Item> requestEntity = new HttpEntity<>(newAccount, headers);

            ResponseEntity<Item> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Item.class
            );
            Thread.sleep(600);
            return response.getBody();
        } catch (Exception e) {
            handleException("createLedgerAccount", url, e);
            return null;
        }
    }

    public FileAttachmentResponse uploadFileAttachment(Resource file, SiteEntity site) {
        String url = apiUrlV2 + "file_attachments";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Préparer le corps multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<FileAttachmentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    FileAttachmentResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            handleException("uploadFileAttachment", url, e);
            return null;
        }
    }

    public CategoryResponse getCategoryByUrl(String url, SiteEntity site) {
        try {
            ResponseEntity<CategoryResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headerBuilder(site.getPennylaneToken())),
                    CategoryResponse.class
            );
            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);
            return response.getBody();
        } catch (Exception e) {
            handleException("getCategoryByUrl", url, e);
            return null;
        }
    }



    private void handleException(String methodName, String url, Exception e) {
        if (e instanceof IOException) {
            log.error("Méthode: {}, URL: {}, Erreur d'entrée/sortie : {}", methodName, url, e.getMessage(), e);
        } else if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpException = (HttpClientErrorException) e;
            log.error("Méthode: {}, URL: {}, Erreur HTTP : {} - {}", methodName, url, httpException.getStatusCode(), httpException.getStatusText(), e);
        } else {
            log.error("Méthode: {}, URL: {}, Erreur lors de l'appel à l'API : {}", methodName, url, e.getMessage(), e);
        }
    }

    private HttpHeaders headerBuilder(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
