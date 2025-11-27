package fr.mismo.pennylane.service;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.api.InvoiceApi;
import fr.mismo.pennylane.api.SupplierApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.dto.invoice.SupplierInvoiceResponse;
import fr.mismo.pennylane.dto.supplier.Supplier;
import fr.mismo.pennylane.settings.WsDocumentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour InvoiceService
 * Couvre les méthodes utilitaires et la logique métier critique
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - InvoiceService")
class InvoiceServiceTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private LogRepository logRepository;

    @Mock
    private SupplierApi supplierApi;

    @Mock
    private InvoiceApi invoiceApi;

    @Mock
    private AccountsApi accountsApi;

    @Mock
    private WsDocumentProperties wsDocumentProperties;

    @Mock
    private LogHelper logHelper;

    @InjectMocks
    private InvoiceService invoiceService;

    private SiteEntity testSite;

    @BeforeEach
    void setUp() {
        testSite = new SiteEntity();
        testSite.setId(1);
        testSite.setCode("TEST_SITE");

        // Mock pour LogHelper
        when(logHelper.startTraitement(anyString())).thenReturn(System.currentTimeMillis());
    }

    @Test
    @DisplayName("syncInvoice - Doit retourner immédiatement si invoice est null")
    void syncInvoice_shouldReturnImmediately_whenInvoiceIsNull() {
        // When
        invoiceService.syncInvoice(null, testSite, Collections.emptyList());

        // Then
        verify(logHelper).error(eq("SYNC_INVOICE"), anyString(), isNull());
        verify(supplierApi, never()).retrieveSupplier(anyString(), any());
    }

    @Test
    @DisplayName("syncInvoice - Doit retourner immédiatement si site est null")
    void syncInvoice_shouldReturnImmediately_whenSiteIsNull() {
        // Given
        SupplierInvoiceResponse.SupplierInvoiceItem invoice = new SupplierInvoiceResponse.SupplierInvoiceItem();
        invoice.setId(123L);

        // When
        invoiceService.syncInvoice(invoice, null, Collections.emptyList());

        // Then
        verify(logHelper).error(eq("SYNC_INVOICE"), anyString(), isNull());
        verify(supplierApi, never()).retrieveSupplier(anyString(), any());
    }

    @Test
    @DisplayName("updateInvoice - Doit retourner immédiatement si aFacture est null")
    void updateInvoice_shouldReturnImmediately_whenFactureIsNull() {
        // When
        invoiceService.updateInvoice(null, testSite);

        // Then
        verify(logHelper).error(eq("UPDATE_INVOICE"), anyString(), isNull());
        verify(invoiceApi, never()).getSupplierInvoiceById(any(), anyString());
    }

    @Test
    @DisplayName("updateInvoice - Doit retourner immédiatement si aSite est null")
    void updateInvoice_shouldReturnImmediately_whenSiteIsNull() {
        // When
        invoiceService.updateInvoice("123", null);

        // Then
        verify(logHelper).error(eq("UPDATE_INVOICE"), anyString(), isNull());
        verify(invoiceApi, never()).getSupplierInvoiceById(any(), anyString());
    }

    @Test
    @DisplayName("updateReglements - Doit retourner immédiatement si aFacture est null")
    void updateReglements_shouldReturnImmediately_whenFactureIsNull() {
        // When
        invoiceService.updateReglements(null, testSite);

        // Then
        verify(logHelper).error(eq("UPDATE_REGLEMENTS"), anyString(), isNull());
        verify(invoiceApi, never()).getCustomerInvoiceById(any(), anyString());
    }

    @Test
    @DisplayName("updateReglementsV2 - Doit retourner immédiatement si aFacture est null")
    void updateReglementsV2_shouldReturnImmediately_whenFactureIsNull() {
        // When
        invoiceService.updateReglementsV2(null, testSite);

        // Then
        verify(logHelper).error(eq("UPDATE_REGLEMENTS_V2"), anyString(), isNull());
        verify(invoiceApi, never()).getCustomerInvoiceById(any(), anyString());
    }

    @Test
    @DisplayName("parseDoubleSafe - Doit retourner la valeur par défaut si value est null")
    void parseDoubleSafe_shouldReturnDefault_whenValueIsNull() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("parseDoubleSafe", Object.class, double.class);
        method.setAccessible(true);

        // When
        double result = (double) method.invoke(null, null, 10.0);

        // Then
        assertEquals(10.0, result, 0.001);
    }

    @Test
    @DisplayName("parseDoubleSafe - Doit parser correctement un double valide")
    void parseDoubleSafe_shouldParseValidDouble() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("parseDoubleSafe", Object.class, double.class);
        method.setAccessible(true);

        // When
        double result1 = (double) method.invoke(null, "123.45", 0.0);
        double result2 = (double) method.invoke(null, 678.90, 0.0);

        // Then
        assertEquals(123.45, result1, 0.001);
        assertEquals(678.90, result2, 0.001);
    }

    @Test
    @DisplayName("parseDoubleSafe - Doit retourner la valeur par défaut pour une chaîne invalide")
    void parseDoubleSafe_shouldReturnDefault_whenStringIsInvalid() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("parseDoubleSafe", Object.class, double.class);
        method.setAccessible(true);

        // When
        double result = (double) method.invoke(null, "not_a_number", 10.0);

        // Then
        assertEquals(10.0, result, 0.001);
    }

    @Test
    @DisplayName("parseDoubleSafe - Doit gérer les espaces dans la chaîne")
    void parseDoubleSafe_shouldHandleWhitespace() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("parseDoubleSafe", Object.class, double.class);
        method.setAccessible(true);

        // When
        double result1 = (double) method.invoke(null, "  123.45  ", 0.0);
        double result2 = (double) method.invoke(null, "   ", 10.0);

        // Then
        assertEquals(123.45, result1, 0.001);
        assertEquals(10.0, result2, 0.001); // Chaîne vide après trim
    }

    @Test
    @DisplayName("computePaymentStatus - Doit retourner fully_paid si remaining est 0")
    void computePaymentStatus_shouldReturnFullyPaid_whenRemainingIsZero() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("computePaymentStatus", boolean.class, double.class, double.class);
        method.setAccessible(true);

        // When
        String result1 = (String) method.invoke(null, true, 0.0, 100.0);
        String result2 = (String) method.invoke(null, false, 0.0, 100.0);

        // Then
        assertEquals("fully_paid", result1);
        assertEquals("fully_paid", result2);
    }

    @Test
    @DisplayName("computePaymentStatus - Doit gérer les arrondis pour fully_paid")
    void computePaymentStatus_shouldHandleRoundingForFullyPaid() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("computePaymentStatus", boolean.class, double.class, double.class);
        method.setAccessible(true);

        // When
        String result1 = (String) method.invoke(null, true, 0.001, 100.0);
        String result2 = (String) method.invoke(null, true, 0.009, 100.0);
        String result3 = (String) method.invoke(null, true, -0.005, 100.0);

        // Then
        assertEquals("fully_paid", result1);
        assertEquals("fully_paid", result2);
        assertEquals("fully_paid", result3);
    }

    @Test
    @DisplayName("computePaymentStatus - Doit retourner partially_paid si 0 < remaining < total")
    void computePaymentStatus_shouldReturnPartiallyPaid_whenPartiallyPaid() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("computePaymentStatus", boolean.class, double.class, double.class);
        method.setAccessible(true);

        // When
        String result1 = (String) method.invoke(null, false, 50.0, 100.0);
        String result2 = (String) method.invoke(null, true, 1.0, 100.0);
        String result3 = (String) method.invoke(null, false, 99.0, 100.0);

        // Then
        assertEquals("partially_paid", result1);
        assertEquals("partially_paid", result2);
        assertEquals("partially_paid", result3);
    }

    @Test
    @DisplayName("computePaymentStatus - Doit retourner to_be_solded si total = remaining et isPaid")
    void computePaymentStatus_shouldReturnToBeSolded_whenTotalEqualsRemainingAndPaid() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("computePaymentStatus", boolean.class, double.class, double.class);
        method.setAccessible(true);

        // When
        String result1 = (String) method.invoke(null, true, 100.0, 100.0);
        String result2 = (String) method.invoke(null, true, 100.005, 100.0); // Avec arrondi

        // Then
        assertEquals("to_be_solded", result1);
        assertEquals("to_be_solded", result2);
    }

    @Test
    @DisplayName("computePaymentStatus - Doit retourner to_be_processed par défaut")
    void computePaymentStatus_shouldReturnToBeProcessed_byDefault() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("computePaymentStatus", boolean.class, double.class, double.class);
        method.setAccessible(true);

        // When
        String result1 = (String) method.invoke(null, false, 100.0, 100.0);
        String result2 = (String) method.invoke(null, false, 150.0, 100.0); // Remaining > total
        String result3 = (String) method.invoke(null, true, -100.0, -50.0); // Total négatif

        // Then
        assertEquals("to_be_processed", result1);
        assertEquals("to_be_processed", result2);
        assertEquals("to_be_processed", result3);
    }

    @Test
    @DisplayName("computePaymentStatus - Doit gérer les montants négatifs")
    void computePaymentStatus_shouldHandleNegativeAmounts() throws Exception {
        // Given
        Method method = InvoiceService.class.getDeclaredMethod("computePaymentStatus", boolean.class, double.class, double.class);
        method.setAccessible(true);

        // When
        String result = (String) method.invoke(null, false, 50.0, -100.0);

        // Then
        assertEquals("to_be_processed", result);
    }

    @Test
    @DisplayName("processError - Ne doit pas lever d'exception avec invoice null")
    void processError_shouldNotThrowException_whenInvoiceIsNull() {
        // When & Then
        assertDoesNotThrow(() -> invoiceService.processError(null, new RuntimeException("Test")));
        verify(logHelper).error(eq("PROCESS_ERROR"), anyString(), any());
    }

    @Test
    @DisplayName("processError - Ne doit pas lever d'exception avec exception null")
    void processError_shouldNotThrowException_whenExceptionIsNull() {
        // Given
        SupplierInvoiceResponse.SupplierInvoiceItem invoice = new SupplierInvoiceResponse.SupplierInvoiceItem();
        invoice.setId(123L);

        // When & Then
        assertDoesNotThrow(() -> invoiceService.processError(invoice, null));
    }
}
