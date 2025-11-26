package fr.mismo.pennylane.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dto.Category;
import fr.mismo.pennylane.dto.CategoryListResponse;
import fr.mismo.pennylane.dto.invoice.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InvoiceApi {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.url_v1}")
    private String apiUrl;

    @Autowired
    public InvoiceApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public InvoiceResponse createInvoice(Invoice invoice, SiteEntity site, Boolean withVerif) {
        String url = apiUrl + "customer_invoices/import";

        InvoiceResponse exist = null;

        // Vérification si la facture existe déjà, uniquement si withVerif est true ET que l'ID est présent
        if (withVerif && invoice.getId() != null) {
            exist = checkInvoiceExists(site, String.valueOf(invoice.getId()));

            if (exist != null) {
                String errorMessage = String.format("La facture avec le numéro %s existe déjà.", invoice.getInvoiceNumber());
                log.warn(errorMessage);

                exist.setResponseStatus("ALREADY_EXISTS");
                exist.setResponseMessage(errorMessage);
                return exist;
            }
            else{
                invoice.setId(null);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/json");
        headers.set("content-type", "application/json");
        headers.set("Authorization", "Bearer " + site.getPennylaneToken());

        HttpEntity<Invoice> requestEntity = new HttpEntity<>(invoice, headers);

        try {
            ResponseEntity<InvoiceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    InvoiceResponse.class
            );

            return response.getBody();

        } catch (HttpClientErrorException e) {
            // Gestion du 422 ou autres 4xx
            log.warn("Erreur HTTP lors de la création de la facture : {}", e.getStatusCode());

            InvoiceResponse errorResponse = new InvoiceResponse();
            errorResponse.setResponseStatus("FAILED");
            errorResponse.setResponseMessage("Erreur HTTP : " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return errorResponse;

        } catch (Exception e) {
            // Autres erreurs (5xx, timeout, etc.)
            log.error("Erreur inattendue lors de l'appel à l'API", e);

            InvoiceResponse errorResponse = new InvoiceResponse();
            errorResponse.setResponseStatus("ERROR");
            errorResponse.setResponseMessage("Erreur inattendue : " + e.getMessage());
            return errorResponse;
        }
    }


    public InvoiceResponse getCustomerInvoiceById(SiteEntity site, String invoiceId) {
        String url = apiUrl + "customer_invoices/" + invoiceId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());

            log.trace("Fetching customer invoice with ID: {}", invoiceId);
            log.trace("Authorization Header: {}", headers.get("Authorization"));

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            Thread.sleep(600);

            log.trace("Response Status: {}", response.getStatusCode());
            log.trace("Response Body: {}", response.getBody());

            if (response.getStatusCode().is4xxClientError()) {
                throw new ApiException("Erreur lors de la récupération de la facture : " + response.getBody());
            }

            return objectMapper.readValue(response.getBody(), InvoiceResponse.class);
        } catch (HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Server Error: {} - {}", e.getStatusCode(), responseBody);
            return null;
        } catch (Exception e) {
            log.error("General Error: {}", e.getMessage());
            return null;
        }
    }

    public List<Transaction> getAllMatchedTransactions(SiteEntity site, String matchedTransactionsUrl) {
        List<Transaction> allTransactions = new ArrayList<>();
        String cursor = null;
        boolean hasMore = true;

        while (hasMore) {
            // Construction de l'URL avec le curseur si besoin
            String url = matchedTransactionsUrl + "?limit=100";
            if (cursor != null && !cursor.isEmpty()) {
                url += "&cursor=" + cursor;
            }

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("accept", "application/json");
                headers.set("Authorization", "Bearer " + site.getPennylaneToken());
                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                ResponseEntity<TransactionListResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        TransactionListResponse.class
                );

                Thread.sleep(1100); // Respect du rate limit

                TransactionListResponse body = response.getBody();
                if (body == null) break;

                allTransactions.addAll(body.getItems());
                hasMore = body.isHasMore();
                cursor = body.getNextCursor();

            } catch (Exception e) {
                break;
            }
        }
        return allTransactions;
    }

    public SupplierInvoiceResponse.SupplierInvoiceItem getSupplierInvoiceById(SiteEntity site, String invoiceId) {
        String url = apiUrl + "supplier_invoices/" + invoiceId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());

            log.debug("Fetching supplier invoice with ID: {}", invoiceId);
            log.debug("Authorization Header: {}", headers.get("Authorization"));

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            Thread.sleep(600);

            log.debug("Response Status: {}", response.getStatusCode());
            log.debug("Response Body: {}", response.getBody());

            if (response.getStatusCode().is4xxClientError()) {
                throw new ApiException("Erreur lors de la récupération de la facture fournisseur : " + response.getBody());
            }

            return objectMapper.readValue(response.getBody(), SupplierInvoiceResponse.SupplierInvoiceItem.class);
        } catch (HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Server Error: {} - {}", e.getStatusCode(), responseBody);
            throw new ApiException("Erreur serveur lors de la récupération de la facture fournisseur: " + extractErrorMessage(responseBody), e);
        } catch (Exception e) {
            log.error("General Error: {}", e.getMessage());
            throw new ApiException("Erreur générale lors de la récupération de la facture fournisseur", e);
        }
    }

    public boolean updateSupplierInvoicePaymentStatus(SiteEntity site, String invoiceId, String paymentStatus) {
        String url = apiUrl + "supplier_invoices/" + invoiceId + "/payment_status";

        HttpHeaders headers = headerBuilder(site.getPennylaneToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = String.format("{\"payment_status\": \"%s\"}", paymentStatus);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class
            );

            log.info("Mise à jour du statut de paiement de la facture {} : {}", invoiceId, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                log.warn("Facture {} : Un paiement correspondant est déjà rattaché. Le passage en \"{}\" est ignoré.",
                        invoiceId, paymentStatus);
                return true; // On considère l'opération comme réussie
            }

            log.error("Erreur client lors de la mise à jour du statut de paiement de la facture {} : {} - {}",
                    invoiceId, e.getStatusCode(), e.getResponseBodyAsString());
            return false;

        } catch (HttpServerErrorException e) {
            String errorMessage = extractErrorMessage(e.getResponseBodyAsString());
            log.error("Erreur serveur lors de la mise à jour du statut de la facture {} : {} - {}",
                    invoiceId, e.getStatusCode(), errorMessage);
            return false;

        } catch (Exception e) {
            log.error("Erreur inattendue lors de la mise à jour du statut de paiement de la facture {} : {}",
                    invoiceId, e.getMessage());
            return false;
        }
    }

    public List<Category> listAllCategories(SiteEntity site) {
        String baseUrl = apiUrl + "categories";
        List<Category> allCategories = new ArrayList<>();
        String cursor = null;
        boolean hasMore = true;

        while (hasMore) {
            String url = baseUrl;
            if (cursor != null && !cursor.isEmpty()) {
                url += "?cursor=" + cursor;
            }

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("accept", "application/json");
                headers.set("Authorization", "Bearer " + site.getPennylaneToken());

                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
                ResponseEntity<CategoryListResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        CategoryListResponse.class
                );

                Thread.sleep(1100); // Respect des limites de débit

                CategoryListResponse body = response.getBody();
                if (body == null) break;

                allCategories.addAll(body.getItems());
                hasMore = body.isHas_more();
                cursor = body.getNext_cursor();

            } catch (Exception e) {
                log.error("Erreur lors de la récupération des catégories", e);
                break;
            }
        }
        return allCategories;
    }


    public InvoiceResponse checkInvoiceExists(SiteEntity site, String invoiceId) {
        try {
            // Récupérer la facture en fonction de son ID
            InvoiceResponse invoiceResponse = getCustomerInvoiceById(site, invoiceId);
            return invoiceResponse;
        } catch (ApiException e) {
            if (e.getCause() instanceof HttpClientErrorException &&
                    ((HttpClientErrorException) e.getCause()).getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'existence de la facture: {}", e.getMessage());
            throw new ApiException("Erreur lors de la vérification de l'existence de la facture", e);
        }
    }

    public List<SupplierInvoiceResponse.SupplierInvoiceItem> listAllSupplierInvoices(
            SiteEntity site,
            List<Long> categoryIds,
            OffsetDateTime syncDateTime
    ) {

        log.debug("📤 Appel API listAllSupplierInvoices avec paramètres: site={}, categoryIds={}, syncDateTime={}",
                site, categoryIds, syncDateTime);

        List<SupplierInvoiceResponse.SupplierInvoiceItem> allInvoices = new ArrayList<>();
        String cursor = null;
        boolean hasMore = true;

        // Préparation du filtre JSON si nécessaire
        List<String> filterParts = new ArrayList<>();

        if (categoryIds != null && !categoryIds.isEmpty()) {
            String categoryIdsStr = categoryIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]"));
            filterParts.add(String.format("{\"field\": \"category_id\", \"operator\": \"in\", \"value\": %s}", categoryIdsStr));
        }

        if (syncDateTime != null) {
            String isoDate = syncDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            filterParts.add(String.format("{\"field\": \"date\", \"operator\": \"gt\", \"value\": \"%s\"}", isoDate));
        }

        String filterJson = filterParts.isEmpty() ? null : "[" + String.join(",", filterParts) + "]";

        try {
            // Préparation des headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            while (hasMore) {

                // Construction de l’URL avec des placeholders uniquement
                String url = apiUrl + "supplier_invoices?limit={limit}&sort={sort}"
                        + (cursor != null ? "&cursor={cursor}" : "")
                        + (filterJson != null ? "&filter={filter}" : "");

                Map<String, Object> uriVariables = new HashMap<>();
                uriVariables.put("limit", 100);
                uriVariables.put("sort", "-id");

                log.debug("URL de récupération des factures d'achats : " + url);

                if (cursor != null) {
                    uriVariables.put("cursor", cursor);
                }
                if (filterJson != null) {
                    log.debug("Filter JSON " + filterJson);
                    uriVariables.put("filter", filterJson);
                }

                // Appel API
                ResponseEntity<SupplierInvoiceResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        SupplierInvoiceResponse.class,
                        uriVariables
                );

                SupplierInvoiceResponse responseBody = response.getBody();
                if (responseBody == null) break;

                allInvoices.addAll(responseBody.getItems());
                hasMore = responseBody.isHasMore();
                cursor = responseBody.getNextCursor();
            }
            // Pause pour respecter la limite de 2 requêtes par seconde
            Thread.sleep(600);
        } catch (Exception e) {
            return null;
        }

        return allInvoices;
    }

    public List<ChangelogResponse.ChangelogItem> listAllSupplierInvoiceChangelogs(
            SiteEntity site,
            OffsetDateTime startDate
    ) {
        List<ChangelogResponse.ChangelogItem> allChangelogs = new ArrayList<>();
        String cursor = null;
        boolean hasMore = true;

        try {
            // Préparation des headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            while (hasMore) {
                // Construction de l’URL
                StringBuilder urlBuilder = new StringBuilder(apiUrl)
                        .append("changelogs/supplier_invoices")
                        .append("?limit={limit}");

                if (cursor != null) {
                    urlBuilder.append("&cursor={cursor}");
                }
                if (startDate != null) {
                    urlBuilder.append("&start_date={startDate}");
                }

                String url = urlBuilder.toString();

                Map<String, Object> uriVariables = new HashMap<>();
                uriVariables.put("limit", 1000);
                if (cursor != null) {
                    uriVariables.put("cursor", cursor);
                }
                if (startDate != null) {
                    String isoDate = startDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    uriVariables.put("startDate", isoDate);
                }

                ResponseEntity<ChangelogResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        ChangelogResponse.class,
                        uriVariables
                );

                ChangelogResponse body = response.getBody();
                if (body == null) break;

                allChangelogs.addAll(body.getItems());
                hasMore = Boolean.TRUE.equals(body.getHasMore());
                cursor = body.getNextCursor();

                // Respect de la limite d'appel
                Thread.sleep(600);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return allChangelogs;
    }




    private SupplierInvoiceResponse getInvoicePage(String url, SiteEntity site) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.set("Authorization", "Bearer " + site.getPennylaneToken());

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<SupplierInvoiceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    SupplierInvoiceResponse.class
            );

            Thread.sleep(1100); // Respect des limites de débit
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildUrl(String baseUrl, String cursor) {
        String url = baseUrl + "?limit=100&sort=-id";
        if (cursor != null && !cursor.isEmpty()) {
            url += "&cursor=" + cursor;
        }
        return url;
    }

    /**
     * Extrait le message d'erreur d'une réponse JSON
     * @param responseBody corps de la réponse contenant l'erreur
     * @return Le message d'erreur extrait ou un message par défaut
     */
    private String extractErrorMessage(String responseBody) {
        try {
            if (responseBody == null || responseBody.isEmpty()) {
                return "Aucune information d'erreur disponible";
            }

            // Vérifier si le corps est un JSON
            if (responseBody.trim().startsWith("{")) {
                Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);

                // Extraire le message d'erreur
                if (errorMap.containsKey("error")) {
                    return errorMap.get("error").toString();
                } else if (errorMap.containsKey("message")) {
                    return errorMap.get("message").toString();
                } else if (errorMap.containsKey("errors")) {
                    return errorMap.get("errors").toString();
                }
            }

            // Si pas de format JSON ou pas de champ d'erreur reconnu
            return responseBody;
        } catch (IOException e) {
            log.error("Erreur lors de l'extraction du message d'erreur: {}", e.getMessage());
            return responseBody; // Retourner le corps brut si impossible de parser
        }
    }

    private HttpHeaders headerBuilder(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}