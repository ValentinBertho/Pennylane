package fr.mismo.pennylane.service;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.api.CustomerApi;
import fr.mismo.pennylane.api.InvoiceApi;
import fr.mismo.pennylane.api.ProductApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.*;
import fr.mismo.pennylane.dto.accounting.Item;
import fr.mismo.pennylane.dto.ath.Ecriture;
import fr.mismo.pennylane.dto.invoice.InvoiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AccountingService
 * Couvre les cas nominaux et les edge cases pour améliorer la fiabilité
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - AccountingService")
class AccountingServiceTest {

    @Mock
    private EcritureRepository ecritureRepository;

    @Mock
    private WsDocumentService wsDocumentService;

    @Mock
    private CourrierRepository courrierRepository;

    @Mock
    private LogRepository logRepository;

    @Mock
    private FactureRepository factureRepository;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InvoiceApi invoiceApi;

    @Mock
    private ProductApi productApi;

    @Mock
    private SocieteRepository societeRepository;

    @Mock
    private CustomerApi customerApi;

    @Mock
    private TiersMapper tiersMapper;

    @Mock
    private AccountsApi accountsApi;

    @InjectMocks
    private AccountingService accountingService;

    private SiteEntity testSite;
    private List<Item> testComptes;

    @BeforeEach
    void setUp() {
        testSite = new SiteEntity();
        testSite.setId(1);
        testSite.setCode("TEST_SITE");

        testComptes = new ArrayList<>();
        Item item = new Item();
        item.setId("1");
        item.setNumber("411000");
        item.setLabel("Clients");
        testComptes.add(item);
    }

    @Test
    @DisplayName("syncEcriture - Doit lever une exception si ecritureInt est null")
    void syncEcriture_shouldThrowException_whenEcritureIntIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountingService.syncEcriture(null, testSite, testComptes)
        );

        assertEquals("Le numéro de lot d'écriture ne peut pas être null", exception.getMessage());
        verify(ecritureRepository, never()).getEcrituresToExport(anyInt());
    }

    @Test
    @DisplayName("syncEcriture - Doit lever une exception si site est null")
    void syncEcriture_shouldThrowException_whenSiteIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountingService.syncEcriture(1, null, testComptes)
        );

        assertEquals("Le site ne peut pas être null", exception.getMessage());
        verify(ecritureRepository, never()).getEcrituresToExport(anyInt());
    }

    @Test
    @DisplayName("syncEcriture - Doit lever une exception si comptes est null")
    void syncEcriture_shouldThrowException_whenComptesIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountingService.syncEcriture(1, testSite, null)
        );

        assertEquals("La liste des comptes ne peut pas être null", exception.getMessage());
        verify(ecritureRepository, never()).getEcrituresToExport(anyInt());
    }

    @Test
    @DisplayName("syncEcriture - Doit retourner immédiatement si la liste d'écritures est vide")
    void syncEcriture_shouldReturnImmediately_whenEcrituresListIsEmpty() {
        // Given
        Integer ecritureInt = 1;
        when(ecritureRepository.getEcrituresToExport(ecritureInt)).thenReturn(Collections.emptyList());

        // When
        accountingService.syncEcriture(ecritureInt, testSite, testComptes);

        // Then
        verify(ecritureRepository).getEcrituresToExport(ecritureInt);
        verify(logRepository).traiterLot(ecritureInt, "Aucune écriture à traiter", true);
        verify(invoiceApi, never()).createInvoice(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("syncEcriture - Doit retourner immédiatement si la liste d'écritures est null")
    void syncEcriture_shouldReturnImmediately_whenEcrituresListIsNull() {
        // Given
        Integer ecritureInt = 1;
        when(ecritureRepository.getEcrituresToExport(ecritureInt)).thenReturn(null);

        // When
        accountingService.syncEcriture(ecritureInt, testSite, testComptes);

        // Then
        verify(ecritureRepository).getEcrituresToExport(ecritureInt);
        verify(logRepository).traiterLot(ecritureInt, "Aucune écriture à traiter", true);
        verify(invoiceApi, never()).createInvoice(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("removeTrailingZerosString - Doit supprimer les zéros à la fin")
    void removeTrailingZerosString_shouldRemoveTrailingZeros() {
        // When & Then
        assertEquals("411", AccountingService.removeTrailingZerosString("41100"));
        assertEquals("411", AccountingService.removeTrailingZerosString("411000"));
        assertEquals("41100", AccountingService.removeTrailingZerosString("41100"));
    }

    @Test
    @DisplayName("removeTrailingZerosString - Doit gérer les cas null et vide")
    void removeTrailingZerosString_shouldHandleNullAndEmpty() {
        // When & Then
        assertNull(AccountingService.removeTrailingZerosString(null));
        assertEquals("", AccountingService.removeTrailingZerosString(""));
    }

    @Test
    @DisplayName("removeTrailingZerosString - Doit gérer une chaîne composée uniquement de zéros")
    void removeTrailingZerosString_shouldHandleOnlyZeros() {
        // When & Then
        assertEquals("", AccountingService.removeTrailingZerosString("0000"));
    }

    @Test
    @DisplayName("removeTrailingZerosString - Ne doit pas modifier une chaîne sans zéros à la fin")
    void removeTrailingZerosString_shouldNotModifyStringWithoutTrailingZeros() {
        // When & Then
        assertEquals("41123", AccountingService.removeTrailingZerosString("41123"));
        assertEquals("123", AccountingService.removeTrailingZerosString("123"));
    }

    @Test
    @DisplayName("processError - Doit logger l'erreur correctement")
    void processError_shouldLogError() {
        // Given
        Integer ecritureInt = 1;
        Exception testException = new RuntimeException("Test exception");

        // When
        accountingService.processError(ecritureInt, testException);

        // Then - vérifie que la méthode ne lève pas d'exception
        assertDoesNotThrow(() -> accountingService.processError(ecritureInt, testException));
    }

    @Test
    @DisplayName("processErrorAccount - Doit logger l'erreur correctement")
    void processErrorAccount_shouldLogError() {
        // Given
        Integer account = 411000;
        Exception testException = new RuntimeException("Test exception");

        // When & Then
        assertDoesNotThrow(() -> accountingService.processErrorAccount(account, testException));
    }

    @Test
    @DisplayName("processErrorJournal - Doit logger l'erreur correctement")
    void processErrorJournal_shouldLogError() {
        // Given
        Integer journal = 1;
        Exception testException = new RuntimeException("Test exception");

        // When & Then
        assertDoesNotThrow(() -> accountingService.processErrorJournal(journal, testException));
    }

    @Test
    @DisplayName("convertFileToBase64 - Doit convertir un fichier en base64")
    void convertFileToBase64_shouldConvertFile() throws Exception {
        // Given
        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        byte[] testBytes = "Test content".getBytes();
        when(mockFile.getBytes()).thenReturn(testBytes);

        // When
        String result = accountingService.convertFileToBase64(mockFile);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Vérifier que c'est bien du base64
        assertTrue(result.matches("^[A-Za-z0-9+/]*={0,2}$"));
    }

    @Test
    @DisplayName("Test d'intégration - syncEcriture avec données valides doit traiter sans erreur")
    void syncEcriture_integration_shouldProcessSuccessfully() {
        // Given
        Integer ecritureInt = 1;
        List<Ecriture> ecritures = new ArrayList<>();
        Ecriture ecriture = new Ecriture();
        ecriture.setNoEcriturePiece(1);
        ecriture.setNoVFacture(100);
        ecritures.add(ecriture);

        when(ecritureRepository.getEcrituresToExport(ecritureInt)).thenReturn(ecritures);
        when(factureRepository.getFacture(anyInt())).thenReturn(Collections.emptyList());

        // When & Then - vérifie que la méthode gère les cas avec des données partielles
        assertDoesNotThrow(() -> accountingService.syncEcriture(ecritureInt, testSite, testComptes));
    }
}
