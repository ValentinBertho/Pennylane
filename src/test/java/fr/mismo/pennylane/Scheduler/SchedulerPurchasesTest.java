package fr.mismo.pennylane.Scheduler;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.api.InvoiceApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.EcritureRepository;
import fr.mismo.pennylane.dao.repository.SiteRepository;
import fr.mismo.pennylane.dto.Category;
import fr.mismo.pennylane.dto.invoice.SupplierInvoiceResponse;
import fr.mismo.pennylane.logging.FlowLogger;
import fr.mismo.pennylane.service.CategoryCacheService;
import fr.mismo.pennylane.service.InvoiceService;
import fr.mismo.pennylane.settings.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - SchedulerPurchases")
class SchedulerPurchasesTest {

    @Mock
    private SiteRepository siteRepository;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private InvoiceApi invoiceApi;
    @Mock
    private Config config;
    @Mock
    private EcritureRepository ecritureRepository;
    @Mock
    private AccountsApi accountsApi;
    @Mock
    private CategoryCacheService categoryCacheService;
    @Mock
    private FlowLogger flowLogger;

    @InjectMocks
    private schedulerPurchases scheduler;

    private SiteEntity siteAth;
    private SiteEntity siteOk;

    @BeforeEach
    void setUp() {
        siteAth = new SiteEntity();
        siteAth.setId(1);
        siteAth.setCode("ATH");
        siteAth.setPennylaneAchat(true);

        siteOk = new SiteEntity();
        siteOk.setId(2);
        siteOk.setCode("PAR");
        siteOk.setPennylaneAchat(true);

        when(config.getDaysBackward()).thenReturn("7");
        when(config.getStatusAFiltrer()).thenReturn(List.of("to_be_paid"));
        when(config.getCategoriesAFiltrer()).thenReturn(List.of("ACHATS"));
        when(flowLogger.startSyncAchats(anyInt(), anyList(), anyList())).thenReturn("corr-id");
    }

    @Test
    @DisplayName("SyncPurchases - Doit continuer sur les autres sites si un site a une incohérence de catégories")
    void syncPurchases_shouldContinueWhenCategoriesMismatchOnOneSite() {
        when(siteRepository.findAllByPennylaneAchatTrue()).thenReturn(List.of(siteAth, siteOk));

        when(categoryCacheService.getCategories(siteAth)).thenReturn(Collections.emptyList());

        Category achats = new Category();
        achats.setId(11L);
        achats.setLabel("ACHATS");
        when(categoryCacheService.getCategories(siteOk)).thenReturn(List.of(achats));

        SupplierInvoiceResponse.SupplierInvoiceItem invoice = new SupplierInvoiceResponse.SupplierInvoiceItem();
        invoice.setId(99L);
        invoice.setPaymentStatus("to_be_paid");
        when(invoiceApi.listAllSupplierInvoices(eq(siteOk), eq(List.of(11L)), any())).thenReturn(List.of(invoice));

        InvoiceService.SyncResult syncResult = new InvoiceService.SyncResult();
        syncResult.setCreated(true);
        when(invoiceService.syncInvoice(invoice, siteOk, List.of(11L))).thenReturn(syncResult);

        assertDoesNotThrow(() -> scheduler.SyncPurchases());

        verify(invoiceApi).listAllSupplierInvoices(eq(siteOk), eq(List.of(11L)), any());
        verify(invoiceService).syncInvoice(invoice, siteOk, List.of(11L));
        verify(config).setLastInsertPurchases(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("SyncPurchases - Doit ignorer le site si l'API retourne null")
    void syncPurchases_shouldSkipSiteWhenApiReturnsNull() {
        when(siteRepository.findAllByPennylaneAchatTrue()).thenReturn(List.of(siteOk));

        Category achats = new Category();
        achats.setId(11L);
        achats.setLabel("ACHATS");
        when(categoryCacheService.getCategories(siteOk)).thenReturn(List.of(achats));

        when(invoiceApi.listAllSupplierInvoices(eq(siteOk), eq(List.of(11L)), any())).thenReturn(null);

        assertDoesNotThrow(() -> scheduler.SyncPurchases());

        verify(invoiceService, never()).syncInvoice(any(), any(), anyList());
        verify(config, never()).setLastInsertPurchases(any());
    }
}
