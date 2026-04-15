package fr.mismo.pennylane.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dto.accounting.AccountingResponse;
import fr.mismo.pennylane.dto.accounting.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - AccountsApi")
class AccountsApiTest {

    @Mock
    private RestTemplate restTemplate;

    private AccountsApi accountsApi;
    private SiteEntity site;

    @BeforeEach
    void setUp() {
        accountsApi = new AccountsApi(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(accountsApi, "apiUrlV2", "https://api.pennylane.test/");

        site = new SiteEntity();
        site.setPennylaneToken("token-test");
    }

    @Test
    @DisplayName("createLedgerAccount - Doit retourner le compte existant quand l'API répond 422 already exists")
    void createLedgerAccount_shouldReturnExisting_whenApiReturnsAlreadyExists422() {
        Item newAccount = new Item();
        newAccount.setNumber("707001");
        newAccount.setLabel("Compte test");

        HttpClientErrorException alreadyExistsException = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable Entity",
                org.springframework.http.HttpHeaders.EMPTY,
                "{\"error\":\"This ledger account already exists\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(
                eq("https://api.pennylane.test/ledger_accounts"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Item.class)
        )).thenThrow(alreadyExistsException);

        AccountingResponse accountingResponse = new AccountingResponse();
        Item existing = new Item();
        existing.setId(123L);
        existing.setNumber("707001");
        existing.setLabel("Compte déjà existant");
        accountingResponse.setItems(List.of(existing));
        accountingResponse.setTotalPages(1);

        when(restTemplate.exchange(
                eq("https://api.pennylane.test/ledger_accounts?filter={filter}"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(AccountingResponse.class),
                ArgumentMatchers.<Object>any()
        )).thenReturn(ResponseEntity.ok(accountingResponse));

        Item result = accountsApi.createLedgerAccount(newAccount, site);

        assertNotNull(result);
        assertEquals("707001", result.getNumber());
        assertEquals(123L, result.getId());
        verify(restTemplate, times(1)).exchange(
                eq("https://api.pennylane.test/ledger_accounts"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Item.class)
        );
        verify(restTemplate, times(1)).exchange(
                eq("https://api.pennylane.test/ledger_accounts?filter={filter}"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(AccountingResponse.class),
                ArgumentMatchers.<Object>any()
        );
    }
}
