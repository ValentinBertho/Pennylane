package fr.mismo.pennylane.Scheduler;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.api.InvoiceApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.EcritureRepository;
import fr.mismo.pennylane.dao.repository.SiteRepository;
import fr.mismo.pennylane.dto.Category;
import fr.mismo.pennylane.dto.invoice.SupplierInvoiceResponse;
import fr.mismo.pennylane.service.CategoryCacheService;
import fr.mismo.pennylane.service.InvoiceService;
import fr.mismo.pennylane.settings.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerPurchasesTest {

    @Mock
    SiteRepository siteRepository;
    @Mock
    InvoiceService invoiceService;
    @Mock
    InvoiceApi invoiceApi;
    @Mock
    Config config;
    @Mock
    EcritureRepository ecritureRepository;
    @Mock
    AccountsApi accountsApi;
    @Mock
    CategoryCacheService categoryCacheService;

    @InjectMocks
    schedulerPurchases schedulerPurchases;

    @Test
    @DisplayName("SyncPurchases continue les autres sites si une catégorie est manquante")
    void syncPurchases_shouldContinueWhenOneSiteHasMissingCategory() {
        SiteEntity siteAth = new SiteEntity();
        siteAth.setCode("ATH");
        SiteEntity siteOk = new SiteEntity();
        siteOk.setCode("OK");

        Category ach = new Category();
        ach.setLabel("ACH");
        ach.setId(99L);

        SupplierInvoiceResponse.SupplierInvoiceItem invoiceItem = new SupplierInvoiceResponse.SupplierInvoiceItem();
        invoiceItem.setId(123L);
        invoiceItem.setPaymentStatus("to_be_paid");

        when(siteRepository.findAllByPennylaneAchatTrue()).thenReturn(List.of(siteAth, siteOk));
        when(config.getDaysBackward()).thenReturn("10");
        when(config.getStatusAFiltrer()).thenReturn(List.of("to_be_paid"));
        when(config.getCategoriesAFiltrer()).thenReturn(List.of("ACH"));

        when(categoryCacheService.getCategories(siteAth)).thenReturn(List.of());
        when(categoryCacheService.getCategories(siteOk)).thenReturn(List.of(ach));
        when(invoiceApi.listAllSupplierInvoices(eq(siteOk), eq(List.of(99L)), any()))
                .thenReturn(List.of(invoiceItem));

        schedulerPurchases.SyncPurchases();

        verify(invoiceService, times(1)).syncInvoice(eq(invoiceItem), eq(siteOk), eq(List.of(99L)));
        verify(invoiceApi, never()).listAllSupplierInvoices(eq(siteAth), any(), any());
    }
}
