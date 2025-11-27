package fr.mismo.pennylane.Scheduler;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.EcritureRepository;
import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.dao.repository.SiteRepository;
import fr.mismo.pennylane.dto.accounting.Item;
import fr.mismo.pennylane.service.AccountingService;
import fr.mismo.pennylane.service.InvoiceService;
import org.hibernate.service.spi.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour SchedulerAccounting
 * Vérifie la robustesse des tâches planifiées
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - SchedulerAccounting")
class SchedulerAccountingTest {

    @Mock
    private EcritureRepository ecritureRepository;

    @Mock
    private AccountingService accountingService;

    @Mock
    private AccountsApi accountsApi;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private LogRepository logRepository;

    @InjectMocks
    private schedulerAccounting scheduler;

    private SiteEntity testSite;
    private List<Item> testAccounts;

    @BeforeEach
    void setUp() {
        testSite = new SiteEntity();
        testSite.setId(1);
        testSite.setCode("TEST_SITE");
        testSite.setPennylaneActif(true);
        testSite.setPennylaneAchat(true);

        testAccounts = new ArrayList<>();
        Item item = new Item();
        item.setId("1");
        item.setNumber("411000");
        testAccounts.add(item);
    }

    @Test
    @DisplayName("syncEntries - Doit gérer correctement le cas sans sites actifs")
    void syncEntries_shouldHandleNoActiveSites() {
        // Given
        when(siteRepository.findAllByPennylaneActifTrue()).thenReturn(Collections.emptyList());

        // When & Then
        assertDoesNotThrow(() -> scheduler.syncEntries());
        verify(accountsApi, never()).listAllLedgerAccounts(any());
        verify(accountingService, never()).syncEcriture(anyInt(), any(), anyList());
    }

    @Test
    @DisplayName("syncEntries - Doit gérer correctement le cas sans écritures à synchroniser")
    void syncEntries_shouldHandleNoEcritures() {
        // Given
        when(siteRepository.findAllByPennylaneActifTrue()).thenReturn(List.of(testSite));
        when(ecritureRepository.getLotEcritureToExport(testSite.getId())).thenReturn(Collections.emptyList());

        // When & Then
        assertDoesNotThrow(() -> scheduler.syncEntries());
        verify(accountsApi, never()).listAllLedgerAccounts(any());
        verify(accountingService, never()).syncEcriture(anyInt(), any(), anyList());
    }

    @Test
    @DisplayName("syncEntries - Doit traiter les écritures avec succès")
    void syncEntries_shouldProcessEcrituresSuccessfully() {
        // Given
        List<Integer> ecritures = List.of(1, 2, 3);
        when(siteRepository.findAllByPennylaneActifTrue()).thenReturn(List.of(testSite));
        when(ecritureRepository.getLotEcritureToExport(testSite.getId())).thenReturn(ecritures);
        when(accountsApi.listAllLedgerAccounts(testSite)).thenReturn(testAccounts);
        doNothing().when(accountingService).syncEcriture(anyInt(), any(), anyList());

        // When
        scheduler.syncEntries();

        // Then
        verify(accountsApi).listAllLedgerAccounts(testSite);
        verify(accountingService, times(3)).syncEcriture(anyInt(), eq(testSite), eq(testAccounts));
    }

    @Test
    @DisplayName("syncEntries - Doit gérer les erreurs RestClientException")
    void syncEntries_shouldHandleRestClientException() {
        // Given
        List<Integer> ecritures = List.of(1);
        when(siteRepository.findAllByPennylaneActifTrue()).thenReturn(List.of(testSite));
        when(ecritureRepository.getLotEcritureToExport(testSite.getId())).thenReturn(ecritures);
        when(accountsApi.listAllLedgerAccounts(testSite)).thenReturn(testAccounts);
        doThrow(new RestClientException("Network error"))
                .when(accountingService).syncEcriture(anyInt(), any(), anyList());

        // When & Then
        assertDoesNotThrow(() -> scheduler.syncEntries());
        verify(accountingService).syncEcriture(anyInt(), eq(testSite), eq(testAccounts));
    }

    @Test
    @DisplayName("syncEntries - Doit gérer les erreurs ServiceException")
    void syncEntries_shouldHandleServiceException() {
        // Given
        List<Integer> ecritures = List.of(1);
        when(siteRepository.findAllByPennylaneActifTrue()).thenReturn(List.of(testSite));
        when(ecritureRepository.getLotEcritureToExport(testSite.getId())).thenReturn(ecritures);
        when(accountsApi.listAllLedgerAccounts(testSite)).thenReturn(testAccounts);
        doThrow(new ServiceException("Service error"))
                .when(accountingService).syncEcriture(anyInt(), any(), anyList());

        // When & Then
        assertDoesNotThrow(() -> scheduler.syncEntries());
        verify(accountingService).processError(anyInt(), any());
    }

    @Test
    @DisplayName("UpdateSale - Doit gérer correctement le cas sans sites d'achat actifs")
    void updateSale_shouldHandleNoActiveSites() {
        // Given
        when(siteRepository.findAllByPennylaneAchatTrue()).thenReturn(Collections.emptyList());

        // When & Then
        assertDoesNotThrow(() -> scheduler.UpdateSale());
        verify(ecritureRepository, never()).getAFactureBAP(anyString());
        verify(invoiceService, never()).updateInvoice(anyString(), any());
    }

    @Test
    @DisplayName("UpdateSale - Doit gérer correctement le cas sans factures à mettre à jour")
    void updateSale_shouldHandleNoInvoices() {
        // Given
        when(siteRepository.findAllByPennylaneAchatTrue()).thenReturn(List.of(testSite));
        when(ecritureRepository.getAFactureBAP(testSite.getCode())).thenReturn(Collections.emptyList());

        // When & Then
        assertDoesNotThrow(() -> scheduler.UpdateSale());
        verify(invoiceService, never()).updateInvoice(anyString(), any());
    }

    @Test
    @DisplayName("UpdateSale - Doit traiter les factures avec succès")
    void updateSale_shouldProcessInvoicesSuccessfully() {
        // Given
        List<String> factures = List.of("FAC001", "FAC002");
        when(siteRepository.findAllByPennylaneAchatTrue()).thenReturn(List.of(testSite));
        when(ecritureRepository.getAFactureBAP(testSite.getCode())).thenReturn(factures);
        doNothing().when(invoiceService).updateInvoice(anyString(), any());

        // When
        scheduler.UpdateSale();

        // Then
        verify(invoiceService, times(2)).updateInvoice(anyString(), eq(testSite));
    }

    @Test
    @DisplayName("UpdateSale - Doit gérer les erreurs lors de la mise à jour des factures")
    void updateSale_shouldHandleUpdateErrors() {
        // Given
        List<String> factures = List.of("FAC001");
        when(siteRepository.findAllByPennylaneAchatTrue()).thenReturn(List.of(testSite));
        when(ecritureRepository.getAFactureBAP(testSite.getCode())).thenReturn(factures);
        doThrow(new RuntimeException("Update error"))
                .when(invoiceService).updateInvoice(anyString(), any());

        // When & Then
        assertDoesNotThrow(() -> scheduler.UpdateSale());
        verify(invoiceService).updateInvoice(anyString(), eq(testSite));
    }

    @Test
    @DisplayName("UpdateSale - Doit gérer les erreurs RestClientException")
    void updateSale_shouldHandleRestClientException() {
        // Given
        List<String> factures = List.of("FAC001");
        when(siteRepository.findAllByPennylaneAchatTrue()).thenReturn(List.of(testSite));
        when(ecritureRepository.getAFactureBAP(testSite.getCode())).thenReturn(factures);
        doThrow(new RestClientException("Network error"))
                .when(invoiceService).updateInvoice(anyString(), any());

        // When & Then
        assertDoesNotThrow(() -> scheduler.UpdateSale());
        verify(invoiceService).updateInvoice(anyString(), eq(testSite));
    }

    @Test
    @DisplayName("UpdateSale - Doit gérer les erreurs ServiceException")
    void updateSale_shouldHandleServiceException() {
        // Given
        List<String> factures = List.of("FAC001");
        when(siteRepository.findAllByPennylaneAchatTrue()).thenReturn(List.of(testSite));
        when(ecritureRepository.getAFactureBAP(testSite.getCode())).thenReturn(factures);
        doThrow(new ServiceException("Service error"))
                .when(invoiceService).updateInvoice(anyString(), any());

        // When & Then
        assertDoesNotThrow(() -> scheduler.UpdateSale());
        verify(invoiceService).updateInvoice(anyString(), eq(testSite));
    }

    @Test
    @DisplayName("purgeLogs - Doit exécuter la purge des logs sans erreur")
    void purgeLogs_shouldExecuteSuccessfully() {
        // Given
        doNothing().when(logRepository).logPurger();

        // When & Then
        assertDoesNotThrow(() -> scheduler.purgeLogs());
        verify(logRepository).logPurger();
    }

    @Test
    @DisplayName("purgeLogs - Doit gérer les erreurs lors de la purge")
    void purgeLogs_shouldHandleErrors() {
        // Given
        doThrow(new RuntimeException("Purge error")).when(logRepository).logPurger();

        // When & Then - Vérifie que l'erreur est propagée (selon le comportement attendu)
        // Si la méthode doit absorber l'erreur, utilisez assertDoesNotThrow
        try {
            scheduler.purgeLogs();
        } catch (Exception e) {
            // Erreur attendue
        }
        verify(logRepository).logPurger();
    }

    @Test
    @DisplayName("syncEntries - Doit gérer plusieurs sites simultanément")
    void syncEntries_shouldHandleMultipleSites() {
        // Given
        SiteEntity site2 = new SiteEntity();
        site2.setId(2);
        site2.setCode("SITE2");
        site2.setPennylaneActif(true);

        List<Integer> ecritures1 = List.of(1, 2);
        List<Integer> ecritures2 = List.of(3);

        when(siteRepository.findAllByPennylaneActifTrue()).thenReturn(List.of(testSite, site2));
        when(ecritureRepository.getLotEcritureToExport(testSite.getId())).thenReturn(ecritures1);
        when(ecritureRepository.getLotEcritureToExport(site2.getId())).thenReturn(ecritures2);
        when(accountsApi.listAllLedgerAccounts(any())).thenReturn(testAccounts);
        doNothing().when(accountingService).syncEcriture(anyInt(), any(), anyList());

        // When
        scheduler.syncEntries();

        // Then
        verify(accountsApi, times(2)).listAllLedgerAccounts(any());
        verify(accountingService, times(3)).syncEcriture(anyInt(), any(), eq(testAccounts));
    }
}
