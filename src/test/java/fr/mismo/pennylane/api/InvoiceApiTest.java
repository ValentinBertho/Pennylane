package fr.mismo.pennylane.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dto.Category;
import fr.mismo.pennylane.dto.CategoryListResponse;
import fr.mismo.pennylane.dto.invoice.Invoice;
import fr.mismo.pennylane.dto.invoice.InvoiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - InvoiceApi")
class InvoiceApiTest {

    @Mock
    private RestTemplate restTemplate;

    private InvoiceApi invoiceApi;
    private SiteEntity site;

    @BeforeEach
    void setUp() {
        invoiceApi = new InvoiceApi(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(invoiceApi, "apiUrlV2", "https://app.pennylane.com/api/external/v2/");
        ReflectionTestUtils.setField(invoiceApi, "apiUrl", "https://app.pennylane.com/api/external/v2/");

        site = new SiteEntity();
        site.setPennylaneToken("token-test");
    }

    @Test
    @DisplayName("listAllCategories - doit paginer jusqu'à has_more=false et retourner toutes les catégories")
    void listAllCategories_shouldPaginateAndReturnAllCategories() {
        Category c1 = new Category();
        c1.setId(1L);
        c1.setLabel("ACHATS");

        Category c2 = new Category();
        c2.setId(2L);
        c2.setLabel("VENTES");

        CategoryListResponse page1 = new CategoryListResponse();
        page1.setItems(List.of(c1));
        page1.setHas_more(true);
        page1.setNext_cursor("cursor-1");

        CategoryListResponse page2 = new CategoryListResponse();
        page2.setItems(List.of(c2));
        page2.setHas_more(false);

        when(restTemplate.exchange(contains("categories?limit={limit}"), eq(HttpMethod.GET), any(), eq(CategoryListResponse.class), any(Map.class)))
                .thenReturn(new ResponseEntity<>(page1, HttpStatus.OK))
                .thenReturn(new ResponseEntity<>(page2, HttpStatus.OK));

        List<Category> categories = invoiceApi.listAllCategories(site);

        assertEquals(2, categories.size());
        assertEquals(List.of("ACHATS", "VENTES"), categories.stream().map(Category::getLabel).toList());
        verify(restTemplate, times(2))
                .exchange(contains("categories?limit={limit}"), eq(HttpMethod.GET), any(), eq(CategoryListResponse.class), any(Map.class));
    }

    @Test
    @DisplayName("listAllCategories - utilise l'endpoint v2")
    void listAllCategories_shouldUseV2Endpoint() {
        CategoryListResponse page = new CategoryListResponse();
        page.setItems(List.of());
        page.setHas_more(false);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(CategoryListResponse.class), any(Map.class)))
                .thenReturn(new ResponseEntity<>(page, HttpStatus.OK));

        invoiceApi.listAllCategories(site);

        verify(restTemplate).exchange(startsWith("https://app.pennylane.com/api/external/v2/categories"),
                eq(HttpMethod.GET), any(), eq(CategoryListResponse.class), any(Map.class));
    }

    @Test
    @DisplayName("createInvoice - doit retourner ALREADY_EXISTS sur 422 external_reference dupliquée")
    void createInvoice_shouldReturnAlreadyExistsWhenExternalReferenceTaken() {
        Invoice invoice = new Invoice();
        invoice.setExternalReference("MIS26TO0100067");

        HttpClientErrorException duplicateException = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable Entity",
                null,
                "{\"status\":422,\"error\":\"External reference has already been taken\"}".getBytes(),
                null
        );

        when(restTemplate.exchange(contains("customer_invoices/import"), eq(HttpMethod.POST), any(), eq(InvoiceResponse.class)))
                .thenThrow(duplicateException);

        String existingInvoiceBody = "{\"items\":[{\"id\":987654,\"external_reference\":\"MIS26TO0100067\"}],\"has_more\":false}";
        when(restTemplate.exchange(contains("customer_invoices?limit={limit}"), eq(HttpMethod.GET), any(), eq(String.class), any(Map.class)))
                .thenReturn(new ResponseEntity<>(existingInvoiceBody, HttpStatus.OK));

        InvoiceResponse response = invoiceApi.createInvoice(invoice, site, false);

        assertEquals("ALREADY_EXISTS", response.getResponseStatus());
        assertEquals(987654L, response.getId());
    }

}
