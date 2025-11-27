# Améliorations fonctionnelles recommandées du code métier

## État actuel

L'infrastructure est en place (Resilience4j configuré, Actuator, sécurité), mais le **code métier doit être modifié** pour utiliser ces patterns et corriger certains problèmes.

---

## 🔴 Priorité 1 - Appliquer Resilience4j dans le code (1 semaine)

### Problème actuel

Les appels API ne sont **pas protégés** malgré la configuration Resilience4j.

### Exemple actuel (InvoiceApi.java:70)

```java
// ❌ Pas de protection
public InvoiceResponse createInvoice(Invoice invoice, SiteEntity site, Boolean withVerif) {
    try {
        ResponseEntity<InvoiceResponse> response = restTemplate.exchange(
            url, HttpMethod.POST, requestEntity, InvoiceResponse.class
        );
        return response.getBody();
    } catch (HttpClientErrorException e) {
        // Gestion basique
    }
}
```

### Solution recommandée

```java
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@CircuitBreaker(name = "pennylane-api", fallbackMethod = "createInvoiceFallback")
@Retry(name = "pennylane-api")
@RateLimiter(name = "pennylane-api")
public InvoiceResponse createInvoice(Invoice invoice, SiteEntity site, Boolean withVerif) {
    // Le code reste le même
    // Resilience4j gère automatiquement retry, circuit breaker et rate limiting
    ResponseEntity<InvoiceResponse> response = restTemplate.exchange(
        url, HttpMethod.POST, requestEntity, InvoiceResponse.class
    );
    return response.getBody();
}

// Méthode de fallback en cas d'échec
private InvoiceResponse createInvoiceFallback(Invoice invoice, SiteEntity site,
                                               Boolean withVerif, Exception e) {
    log.error("Circuit breaker activé pour createInvoice après plusieurs échecs", e);
    InvoiceResponse errorResponse = new InvoiceResponse();
    errorResponse.setResponseStatus("CIRCUIT_OPEN");
    errorResponse.setResponseMessage("Service temporairement indisponible, réessayez plus tard");
    return errorResponse;
}
```

### Fichiers à modifier

- `src/main/java/fr/mismo/pennylane/api/InvoiceApi.java`
- `src/main/java/fr/mismo/pennylane/api/CustomerApi.java`
- `src/main/java/fr/mismo/pennylane/api/ProductApi.java`
- `src/main/java/fr/mismo/pennylane/api/SupplierApi.java`
- `src/main/java/fr/mismo/pennylane/api/AccountsApi.java`
- `src/main/java/fr/mismo/pennylane/service/WsDocumentService.java`

### Impact
- ✅ Retry automatique (4 tentatives avec backoff exponentiel)
- ✅ Protection circuit breaker (évite les cascades de pannes)
- ✅ Rate limiting (2 req/s) **sans Thread.sleep bloquant**
- ✅ Fallback en cas d'échec total

---

## 🔴 Priorité 2 - Supprimer Thread.sleep() (2 jours)

### Problème actuel

`Thread.sleep()` bloque le thread, ce qui est inefficace.

### Exemple actuel (InvoiceApi.java, CustomerApi.java, etc.)

```java
// ❌ MAUVAIS - Bloque le thread
try {
    Thread.sleep(600);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}

// ou
try {
    Thread.sleep(1100);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### Solution

Utiliser `@RateLimiter` de Resilience4j à la place :

```java
// ✅ BON - Non bloquant, géré par Resilience4j
@RateLimiter(name = "pennylane-api")
public List<InvoiceResponse> listSupplierInvoices(SiteEntity site, ...) {
    // Plus besoin de Thread.sleep()
    // Le RateLimiter limite automatiquement à 2 req/s
}
```

### Recherche dans le code

```bash
# Trouver tous les Thread.sleep()
grep -r "Thread.sleep" src/main/java/
```

**Résultats attendus** : ~15-20 occurrences dans les classes API

### Impact
- ✅ Meilleure utilisation des threads
- ✅ Performance améliorée
- ✅ Respect du rate limit Pennylane (2 req/s)

---

## 🟠 Priorité 3 - Nettoyer le code dupliqué V1/V2 (3 jours)

### Problème actuel

Deux versions de plusieurs méthodes existent :

**schedulerPurchases.java** :
- `SyncPurchases()` - Version V1
- `SyncPurchasesV2()` - Version V2 (utilise changelog)
- `UpdatePurchaseReglement()` - Version V1
- `UpdatePurchaseReglementV2()` - Version V2 (transactions détaillées)

**CustomerApi.java** :
- `listCustomers()` - Code commenté
- Nouvelle version active

### Solution recommandée

1. **Décider quelle version garder** (généralement V2)
2. **Supprimer l'ancienne version**
3. **Renommer V2 → nom normal**
4. **Documenter le choix**

### Exemple

**Avant** :
```java
@Scheduled(cron = "${cron.Purchases}")
public void SyncPurchases() {
    // Ancienne version - récupère toutes les factures
}

@Scheduled(cron = "${cron.PurchasesV2}")
public void SyncPurchasesV2() {
    // Nouvelle version - utilise changelog (plus efficace)
}
```

**Après** :
```java
@Scheduled(cron = "${cron.Purchases}")
public void syncPurchases() {
    // Version optimisée avec changelog
    // Anciennement SyncPurchasesV2
}

// SyncPurchases (V1) supprimée
```

### Impact
- ✅ Code plus simple
- ✅ Maintenance facilitée
- ✅ Moins de confusion

---

## 🟠 Priorité 4 - Améliorer la gestion d'erreurs (3 jours)

### Problème actuel

Le traitement continue malgré certaines erreurs, risquant des **incohérences**.

### Exemple problématique (AccountingService.java)

```java
try {
    // Créer produit
    productService.createOrUpdateProduct(product, site);
} catch (Exception e) {
    log.error("Erreur création produit", e);
    // ❌ Continue quand même !
}

try {
    // Créer client
    customerService.createOrUpdateCustomer(customer, site);
} catch (Exception e) {
    log.error("Erreur création client", e);
    // ❌ Continue quand même !
}

// Créer facture - peut échouer si produit/client manquant
invoiceApi.createInvoice(invoice, site);
```

### Solution recommandée

```java
@Transactional
public void syncEntry(Entry entry, SiteEntity site) {
    try {
        // 1. Créer produit (obligatoire)
        ProductResponse product = productService.createOrUpdateProduct(entry.getProduct(), site);
        if (product.getStatus().equals("ERROR")) {
            throw new ServiceException("Impossible de créer le produit : " + product.getMessage());
        }

        // 2. Créer client (obligatoire)
        CustomerResponse customer = customerService.createOrUpdateCustomer(entry.getCustomer(), site);
        if (customer.getStatus().equals("ERROR")) {
            throw new ServiceException("Impossible de créer le client : " + customer.getMessage());
        }

        // 3. Créer facture
        InvoiceResponse invoice = invoiceApi.createInvoice(entry.getInvoice(), site);
        if (invoice.getStatus().equals("ERROR")) {
            throw new ServiceException("Impossible de créer la facture : " + invoice.getMessage());
        }

        // 4. Marquer comme traité
        markAsProcessed(entry);

    } catch (ServiceException e) {
        log.error("Échec synchronisation entry {}: {}", entry.getId(), e.getMessage());
        markAsFailed(entry, e.getMessage());
        // Rollback automatique grâce à @Transactional
        throw e;
    }
}
```

### Impact
- ✅ Garantit la cohérence des données
- ✅ Rollback en cas d'échec partiel
- ✅ Meilleure traçabilité des erreurs

---

## 🟡 Priorité 5 - Supprimer le code commenté (1 jour)

### Problème actuel

Code commenté partout dans la codebase.

### Exemples

**CustomerApi.java** :
```java
/*public List<Customer> listCustomers(SiteEntity site) {
    // 50+ lignes de code commenté
    // OLD CODE
}*/
```

**InvoiceService.java** :
```java
// TODO OBSOLETE
public void oldMethod() {
    // Méthode marquée obsolète mais toujours présente
}
```

### Solution

```bash
# Rechercher tout le code commenté
grep -r "TODO OBSOLETE" src/main/java/
grep -r "OLD CODE" src/main/java/

# Supprimer manuellement après vérification
```

### Règle

- ✅ **Git est votre historique** - Pas besoin de garder du vieux code
- ✅ Supprimer tout code commenté de plus de 2 semaines
- ✅ Si vraiment nécessaire, documenter dans un fichier CHANGELOG

---

## 🟡 Priorité 6 - Extraire les constantes magiques (2 jours)

### Problème actuel

```java
Thread.sleep(600);
Thread.sleep(1100);
private static final int taillePaquetMax = 512000;
```

### Solution

Créer une classe de constantes :

```java
package fr.mismo.pennylane.settings;

public final class Constants {

    private Constants() {} // Prevent instantiation

    // API Rate Limiting
    public static final int PENNYLANE_RATE_LIMIT_FAST_MS = 600;
    public static final int PENNYLANE_RATE_LIMIT_SLOW_MS = 1100;

    // WSDocument
    public static final int WSDOCUMENT_MAX_PACKET_SIZE = 512 * 1024; // 512KB

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE = 500;

    // Timeouts
    public static final int HTTP_CONNECTION_TIMEOUT_MS = 10000; // 10s
    public static final int HTTP_READ_TIMEOUT_MS = 30000;       // 30s
}
```

Puis utiliser :

```java
import static fr.mismo.pennylane.settings.Constants.*;

Thread.sleep(PENNYLANE_RATE_LIMIT_FAST_MS); // Plus clair !
```

---

## 🟡 Priorité 7 - Uniformiser le nommage (2 jours)

### Problème actuel

Mélange français/anglais :

```java
String noVFacture;           // ❌ Français
Integer customerId;          // ✅ Anglais
String idPennylane;          // ❌ Mélange
LocalDate dateEcheance;      // ❌ Français
BigDecimal montantTTC;       // ❌ Français
```

### Solution recommandée

**Choisir une langue** (recommandation : **anglais**) et s'y tenir.

```java
// ✅ Tout en anglais
String invoiceNumber;
Integer customerId;
String pennylaneId;
LocalDate dueDate;
BigDecimal totalAmountIncludingTax;
```

### Stratégie de migration

1. **Ne pas tout renommer d'un coup** (risque de régression)
2. **Renommer au fur et à mesure** des modifications
3. **Utiliser l'IDE** (refactoring automatique)
4. **Mettre à jour les tests** en même temps

---

## 🟡 Priorité 8 - Optimiser les N+1 queries (3 jours)

### Problème actuel

Dans les boucles, appels individuels au lieu de batch.

### Exemple (InvoiceService.java)

```java
// ❌ N appels API
for (SupplierInvoice invoice : invoices) {
    Category category = categoryApi.getCategory(invoice.getCategoryId());
    Supplier supplier = supplierApi.getSupplier(invoice.getSupplierId());
    // Traitement...
}
```

### Solution 1 : Pre-loading

```java
// ✅ 1 seul appel pour toutes les catégories
Map<String, Category> categories = categoryApi.getAllCategories(site);
Map<String, Supplier> suppliers = supplierApi.getAllSuppliers(site);

for (SupplierInvoice invoice : invoices) {
    Category category = categories.get(invoice.getCategoryId());
    Supplier supplier = suppliers.get(invoice.getSupplierId());
    // Traitement...
}
```

### Solution 2 : Cache (déjà en place partiellement)

```java
@Cacheable(value = "categories", key = "#site.id")
public Map<String, Category> getAllCategories(SiteEntity site) {
    // Résultat mis en cache
}
```

---

## 🔴 Priorité 9 - Ajouter des tests (2 semaines)

### État actuel

**0% de couverture de tests**

### Tests minimums recommandés

#### 1. Tests unitaires des Mappers (priorité HAUTE)

```java
@Test
void testInvoiceMapper_shouldMapCorrectly() {
    // Given
    FactureEntity entity = createTestFacture();

    // When
    Invoice dto = InvoiceMapper.toDTO(entity);

    // Then
    assertThat(dto.getInvoiceNumber()).isEqualTo(entity.getNoVFacture());
    assertThat(dto.getTotalAmount()).isEqualTo(entity.getMontantTTC());
}
```

#### 2. Tests unitaires des Services (priorité HAUTE)

```java
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceApi invoiceApi;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void testSyncPurchases_shouldCreateInvoice() {
        // Given
        when(invoiceApi.listSupplierInvoices(...)).thenReturn(mockInvoices);

        // When
        invoiceService.syncPurchases(site);

        // Then
        verify(invoiceRepository).save(any());
    }
}
```

#### 3. Tests d'intégration des API (priorité MOYENNE)

```java
@SpringBootTest
@AutoConfigureWireMock // Mock des appels HTTP
class InvoiceApiIntegrationTest {

    @Autowired
    private InvoiceApi invoiceApi;

    @Test
    void testCreateInvoice_withValidData_shouldSucceed() {
        // Given
        stubFor(post(urlEqualTo("/customer_invoices/import"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"id\":\"123\"}")));

        // When
        InvoiceResponse response = invoiceApi.createInvoice(invoice, site, true);

        // Then
        assertThat(response.getId()).isEqualTo("123");
    }
}
```

### Objectif

- **Court terme** : 30% de couverture (mappers + services critiques)
- **Moyen terme** : 50% de couverture
- **Long terme** : 70% de couverture

---

## 📊 Récapitulatif des améliorations fonctionnelles

| Priorité | Amélioration | Effort | Impact | Fichiers concernés |
|----------|--------------|--------|--------|-------------------|
| 🔴 P1 | Appliquer Resilience4j | 1 semaine | HAUTE | 6 classes API |
| 🔴 P2 | Supprimer Thread.sleep | 2 jours | HAUTE | 6 classes API |
| 🟠 P3 | Nettoyer code V1/V2 | 3 jours | MOYENNE | 2 schedulers |
| 🟠 P4 | Améliorer gestion erreurs | 3 jours | HAUTE | Services |
| 🟡 P5 | Supprimer code commenté | 1 jour | BASSE | Toutes classes |
| 🟡 P6 | Extraire constantes | 2 jours | BASSE | Toutes classes |
| 🟡 P7 | Uniformiser nommage | 2 jours | BASSE | DTOs, entities |
| 🟡 P8 | Optimiser N+1 queries | 3 jours | MOYENNE | Services |
| 🔴 P9 | Ajouter tests | 2 semaines | CRITIQUE | Toutes classes |

**Total effort estimé** : ~4 semaines pour une personne

---

## 🎯 Stratégie de mise en œuvre recommandée

### Sprint 1 (1 semaine)
1. Appliquer Resilience4j dans les API (P1)
2. Supprimer Thread.sleep (P2)
3. Ajouter tests unitaires basiques des Mappers

### Sprint 2 (1 semaine)
1. Améliorer gestion erreurs (P4)
2. Nettoyer code V1/V2 (P3)
3. Ajouter tests unitaires des Services

### Sprint 3 (1 semaine)
1. Optimiser N+1 queries (P8)
2. Supprimer code commenté (P5)
3. Ajouter tests d'intégration

### Sprint 4 (1 semaine)
1. Extraire constantes (P6)
2. Uniformiser nommage (P7)
3. Compléter les tests (objectif 50% couverture)

---

## ✅ Validation

### Checklist de fin

- [ ] Toutes les API utilisent @CircuitBreaker, @Retry, @RateLimiter
- [ ] Thread.sleep() complètement supprimé
- [ ] Code V1 supprimé, V2 renommé
- [ ] Gestion d'erreurs avec rollback transactionnel
- [ ] Aucun code commenté > 2 semaines
- [ ] Constantes extraites et nommées
- [ ] Nommage cohérent (tout anglais ou tout français)
- [ ] N+1 queries optimisées
- [ ] Tests : > 50% de couverture
- [ ] Tests passent tous au vert

---

**Conclusion** : Ces améliorations transformeront le code d'un état "fonctionnel mais fragile" vers un état "robuste et maintenable".
