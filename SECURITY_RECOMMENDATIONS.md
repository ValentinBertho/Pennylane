# 🔒 Recommandations de Sécurité - Interface ATHENEO ↔️ PENNYLANE

## ⚠️ PROBLÈMES CRITIQUES À CORRIGER IMMÉDIATEMENT

### 1. Credentials hardcodés dans application.yml

**Niveau de risque :** 🔴 **CRITIQUE**

#### Problème identifié

Le fichier `/home/user/Pennylane/src/main/resources/application.yml` contient des credentials en clair :

```yaml
# Lignes 14-16 : Database credentials
spring:
  datasource:
    url: jdbc:sqlserver://NA-ATH01.mismo.local\ATHENEO;databaseName=ATHENEO_MISMO
    username: atheneo_sql
    password: SQL19_4TH)sP3g{7  # ❌ CRITIQUE

# Lignes 102-103 : Web service credentials
wsdocument:
  login: ADMIN
  password: ADMIN  # ❌ CRITIQUE

# Lignes 10-12 : Credentials commentés mais exposés
# username: atheneo_sql
# password: SQL19_4TH)sP3g{7  # ❌ CRITIQUE même commenté
```

#### Impact
- ✗ Credentials exposés dans le repository Git
- ✗ Accès direct aux données sensibles si le code est compromis
- ✗ Violation des bonnes pratiques de sécurité
- ✗ Non-conformité RGPD potentielle

#### Solution recommandée

**Option 1 : Variables d'environnement (RECOMMANDÉ)**

Créer un fichier `application.yml` sans credentials :

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:sqlserver://localhost;databaseName=ATHENEO_MISMO}
    username: ${DATABASE_USERNAME:atheneo_sql}
    password: ${DATABASE_PASSWORD}  # Pas de valeur par défaut pour le password !

wsdocument:
  login: ${WS_LOGIN:ADMIN}
  password: ${WS_PASSWORD}  # Obligatoire via variable d'environnement
```

Définir les variables d'environnement :

```bash
# Linux/Mac
export DATABASE_PASSWORD="SQL19_4TH)sP3g{7"
export WS_PASSWORD="ADMIN"

# Windows
set DATABASE_PASSWORD=SQL19_4TH)sP3g{7
set WS_PASSWORD=ADMIN

# Docker
docker run -e DATABASE_PASSWORD="..." -e WS_PASSWORD="..." ...
```

**Option 2 : Spring Cloud Config Server**

Pour les déploiements multi-environnements :
- Externaliser la configuration dans un service dédié
- Chiffrer les credentials avec une clé de chiffrement
- Gérer les configurations par profil (dev, staging, prod)

**Option 3 : HashiCorp Vault**

Pour une sécurité maximale :
- Stockage sécurisé des secrets
- Rotation automatique des credentials
- Audit trail des accès
- Intégration Spring Vault

#### Actions immédiates

1. **Supprimer les credentials du repository**
   ```bash
   # Créer .gitignore
   echo "application-local.yml" >> .gitignore
   echo "application-prod.yml" >> .gitignore

   # Créer application-local.yml (NON versionné)
   # avec les credentials locaux
   ```

2. **Nettoyer l'historique Git**
   ```bash
   # Utiliser git-filter-repo ou BFG Repo-Cleaner
   # ATTENTION : opération destructive, backup recommandé
   ```

3. **Révoquer et changer tous les credentials exposés**
   - Base de données
   - Web service
   - Tokens API Pennylane

---

### 2. Sécurité complètement désactivée

**Niveau de risque :** 🔴 **CRITIQUE**

#### Problème identifié

`/home/user/Pennylane/src/main/java/fr/mismo/pennylane/configuration/SecurityConfig.java` :

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(CsrfConfigurer::disable)  // ❌ CSRF désactivé
        .authorizeHttpRequests(authz -> authz
            .anyRequest().permitAll());  // ❌ Tous les endpoints publics
    return http.build();
}
```

#### Impact
- ✗ **Aucune authentification** sur les endpoints
- ✗ **CSRF attacks** possibles
- ✗ Exposition du dashboard de logs (`/logs/*`)
- ✗ Accès direct aux données métier

#### Solution recommandée

**Configuration sécurisée avec authentification basique :**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authz -> authz
            // Endpoints publics (si nécessaire)
            .requestMatchers("/actuator/health").permitAll()

            // Endpoints de logs : rôle ADMIN requis
            .requestMatchers("/logs/**").hasRole("ADMIN")

            // Tous les autres endpoints : authentification requise
            .anyRequest().authenticated()
        )
        .httpBasic(Customizer.withDefaults())  // Authentification HTTP Basic
        .formLogin(form -> form
            .loginPage("/login")
            .permitAll()
        )
        // CSRF : activer pour les endpoints web
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/**")  // Désactiver seulement pour l'API
        );

    return http.build();
}

@Bean
public UserDetailsService userDetailsService() {
    // Utiliser une vraie base de données en production
    UserDetails admin = User.builder()
        .username("admin")
        .password(passwordEncoder().encode(System.getenv("ADMIN_PASSWORD")))
        .roles("ADMIN")
        .build();

    return new InMemoryUserDetailsManager(admin);
}

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Pour une sécurité renforcée (OAuth2/JWT) :**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults())
        )
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/logs/**").hasAuthority("SCOPE_admin")
            .anyRequest().authenticated()
        );

    return http.build();
}
```

---

### 3. Tokens API non chiffrés en base de données

**Niveau de risque :** 🟠 **ÉLEVÉ**

#### Problème identifié

`/home/user/Pennylane/src/main/java/fr/mismo/pennylane/dao/entity/SiteEntity.java` :

```java
@Column(name = "PENNYLANE_TOKEN")
private String pennylaneToken;  // ❌ Stocké en clair dans la BD
```

#### Impact
- ✗ Tokens Pennylane lisibles par quiconque accède à la BD
- ✗ Compromission possible si backup BD exposé
- ✗ Risque de vol de credentials API

#### Solution recommandée

**Option 1 : Chiffrement au niveau applicatif (JPA Converter)**

```java
@Entity
@Table(name = "T_SITE")
public class SiteEntity {

    @Column(name = "PENNYLANE_TOKEN")
    @Convert(converter = EncryptedStringConverter.class)
    private String pennylaneToken;
}

// Converter de chiffrement
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return encryptionService.decrypt(dbData);
    }
}
```

**Option 2 : Chiffrement au niveau base de données (SQL Server)**

```sql
-- Créer une clé de chiffrement
CREATE MASTER KEY ENCRYPTION BY PASSWORD = 'StrongPassword123!';

CREATE CERTIFICATE TokenEncryptCert
WITH SUBJECT = 'Pennylane Token Encryption';

CREATE SYMMETRIC KEY TokenEncryptKey
WITH ALGORITHM = AES_256
ENCRYPTION BY CERTIFICATE TokenEncryptCert;

-- Modifier la colonne
ALTER TABLE T_SITE
ADD PENNYLANE_TOKEN_ENCRYPTED VARBINARY(256);

-- Procédure de chiffrement
CREATE PROCEDURE SP_ENCRYPT_TOKEN
    @SiteId INT,
    @Token NVARCHAR(255)
AS
BEGIN
    OPEN SYMMETRIC KEY TokenEncryptKey
    DECRYPTION BY CERTIFICATE TokenEncryptCert;

    UPDATE T_SITE
    SET PENNYLANE_TOKEN_ENCRYPTED = EncryptByKey(Key_GUID('TokenEncryptKey'), @Token)
    WHERE ID = @SiteId;

    CLOSE SYMMETRIC KEY TokenEncryptKey;
END
```

---

### 4. Logging de données sensibles

**Niveau de risque :** 🟠 **ÉLEVÉ**

#### Problème identifié

`/home/user/Pennylane/src/main/java/fr/mismo/pennylane/api/InvoiceApi.java` ligne 109 :

```java
log.trace("Authorization Header: {}", headers.get("Authorization"));
// ❌ Bearer token loggé en clair
```

#### Impact
- ✗ Tokens API dans les logs applicatifs
- ✗ Risque si les logs sont centralisés (ELK, Splunk)
- ✗ Traces persistantes des credentials

#### Solution recommandée

**Masquage des données sensibles :**

```java
// Utiliser StringUtils.maskSensitive()
log.trace("Authorization Header: Bearer {}",
    StringUtils.maskSensitive(token));
// Output: "Authorization Header: Bearer ***e4f8"
```

**Configuration Logback pour masquer automatiquement :**

`logback-spring.xml` :

```xml
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
            </layout>
            <!-- Masquage automatique des patterns sensibles -->
            <charset>UTF-8</charset>
        </encoder>
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
            <evaluator class="ch.qos.logback.classic.boolex.GEventEvaluator">
                <expression>
                    message.contains("Bearer") || message.contains("password")
                </expression>
            </evaluator>
            <onMatch>DENY</onMatch>
        </filter>
    </appender>
</configuration>
```

---

### 5. Exposition des erreurs internes

**Niveau de risque :** 🟡 **MOYEN**

#### Problème identifié

`application.yml` lignes 77-81 :

```yaml
server:
  error:
    include-message: always       # ❌ Messages d'erreur exposés
    include-binding-errors: always  # ❌ Détails de validation exposés
    include-stacktrace: on_param  # ❌ Stack traces exposées via ?trace=true
```

#### Impact
- ✗ Révélation de l'architecture interne
- ✗ Stack traces exploitables par des attaquants
- ✗ Fuite d'informations (paths, versions, dépendances)

#### Solution recommandée

```yaml
server:
  error:
    include-message: never          # ✓ Messages génériques seulement
    include-binding-errors: never   # ✓ Pas de détails de validation
    include-stacktrace: never       # ✓ Jamais de stack trace
    whitelabel:
      enabled: false                # ✓ Page d'erreur personnalisée
```

**Gestionnaire d'erreurs personnalisé :**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        // Logger l'erreur complète (interne)
        log.error("Erreur interne", ex);

        // Retourner une réponse générique (externe)
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .error("Une erreur est survenue")
            .code("INTERNAL_ERROR")
            .build();

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.warn("Erreur API: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .error("Erreur lors de la communication avec Pennylane")
            .code(ex.getErrorCode())
            .build();

        return ResponseEntity
            .status(ex.getHttpStatusCode())
            .body(error);
    }
}
```

---

## 🛡️ BONNES PRATIQUES GÉNÉRALES

### 1. Principe du moindre privilège

#### Base de données
```sql
-- Créer un utilisateur dédié avec privilèges minimaux
CREATE LOGIN pennylane_app WITH PASSWORD = 'StrongPassword';
CREATE USER pennylane_app FOR LOGIN pennylane_app;

-- Accorder seulement les permissions nécessaires
GRANT SELECT, INSERT, UPDATE ON T_V_FACTURE TO pennylane_app;
GRANT SELECT, INSERT, UPDATE ON T_A_FACTURE_PENNYLANE TO pennylane_app;
GRANT EXECUTE ON SP_PENNYLANE_* TO pennylane_app;

-- REFUSER les permissions dangereuses
DENY DELETE ON DATABASE::ATHENEO_MISMO TO pennylane_app;
DENY CREATE TABLE TO pennylane_app;
DENY DROP TO pennylane_app;
```

#### API Pennylane
- Utiliser des tokens avec scope limité
- Un token par site/environnement
- Rotation régulière des tokens (90 jours)

### 2. Validation des entrées

```java
@Service
@Validated  // Activer la validation
public class InvoiceService {

    public void syncInvoice(
        @Valid @NotNull SupplierInvoiceItem invoice,
        @Valid @NotNull SiteEntity site
    ) {
        // Validation automatique via JSR-303
    }
}

// DTO avec validation
@Data
public class InvoiceDTO {
    @NotBlank(message = "Le numéro de facture est requis")
    @Size(max = 50)
    private String invoiceNumber;

    @NotNull
    @Positive
    private BigDecimal amount;

    @Email
    private String customerEmail;

    @Pattern(regexp = "^[A-Z0-9]+$")
    private String reference;
}
```

### 3. Protection contre les injections SQL

**DÉJÀ BIEN FAIT** ✅ : Utilisation de JPA et repositories Spring Data

```java
// ✓ Bon : Paramètres bindés automatiquement
@Query("SELECT e FROM Ecriture e WHERE e.noEcriture = :numero")
List<Ecriture> findByNumero(@Param("numero") Integer numero);

// ❌ Mauvais : Concaténation de String (n'existe pas dans le projet)
// String sql = "SELECT * FROM T_ECRITURE WHERE NO = " + numero;
```

### 4. Rate Limiting renforcé

**DÉJÀ IMPLÉMENTÉ** ✅ : Classe `RateLimiter.java`

Ajouter une protection supplémentaire au niveau HTTP :

```java
@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registration =
            new FilterRegistrationBean<>();

        registration.setFilter(new RateLimitFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);

        return registration;
    }
}

public class RateLimitFilter implements Filter {
    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 req/s

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {
        if (!rateLimiter.tryAcquire()) {
            ((HttpServletResponse) response).sendError(429, "Too Many Requests");
            return;
        }
        chain.doFilter(request, response);
    }
}
```

### 5. HTTPS obligatoire

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: pennylane
```

Redirection HTTP → HTTPS :

```java
@Configuration
public class HttpsRedirectConfig {
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };

        tomcat.addAdditionalTomcatConnectors(createHttpConnector());
        return tomcat;
    }

    private Connector createHttpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

### 6. Headers de sécurité HTTP

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'")
            )
            .frameOptions(FrameOptionsConfig::deny)
            .xssProtection(xss -> xss.block(true))
            .contentTypeOptions(Customizer.withDefaults())
            .referrerPolicy(referrer -> referrer
                .policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            )
        );

    return http.build();
}
```

### 7. Audit et monitoring

```java
@Aspect
@Component
public class SecurityAuditAspect {

    @Autowired
    private AuditRepository auditRepository;

    @AfterReturning("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public void auditSecuredMethod(JoinPoint joinPoint) {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String method = joinPoint.getSignature().toShortString();

        AuditLog log = AuditLog.builder()
            .timestamp(Instant.now())
            .username(username)
            .action(method)
            .status("SUCCESS")
            .build();

        auditRepository.save(log);
    }
}
```

---

## 📋 CHECKLIST DE SÉCURITÉ

### Avant la mise en production

- [ ] **Credentials externalisés** (variables d'environnement ou vault)
- [ ] **Sécurité activée** (authentification + CSRF)
- [ ] **Tokens chiffrés** en base de données
- [ ] **HTTPS activé** avec certificat valide
- [ ] **Logging sécurisé** (pas de credentials/tokens dans les logs)
- [ ] **Erreurs masquées** (pas de stack traces en production)
- [ ] **Validation des entrées** sur tous les endpoints
- [ ] **Rate limiting** activé
- [ ] **Headers de sécurité** configurés
- [ ] **Dépendances à jour** (scan de vulnérabilités)
- [ ] **Audit activé** (qui fait quoi, quand)
- [ ] **Backups sécurisés** (chiffrés)
- [ ] **Plan de réponse aux incidents** documenté

### Audits réguliers

- [ ] **Scan de vulnérabilités** (OWASP Dependency Check, Snyk)
- [ ] **Revue des accès** (qui a accès à quoi)
- [ ] **Rotation des secrets** (tous les 90 jours)
- [ ] **Revue des logs** (tentatives d'accès suspectes)
- [ ] **Tests de pénétration** (annuel)

---

## 🔧 OUTILS RECOMMANDÉS

### Scan de vulnérabilités
```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.4.0</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

```bash
# Exécuter le scan
mvn dependency-check:check

# Rapport généré dans target/dependency-check-report.html
```

### Git Secrets
```bash
# Installer git-secrets
brew install git-secrets  # Mac
# ou télécharger depuis https://github.com/awslabs/git-secrets

# Configurer
cd /home/user/Pennylane
git secrets --install
git secrets --register-aws

# Ajouter des patterns personnalisés
git secrets --add 'password\s*=\s*.+'
git secrets --add 'Bearer\s+[A-Za-z0-9_-]+'

# Scanner l'historique
git secrets --scan-history
```

### SonarQube
```yaml
# sonar-project.properties
sonar.projectKey=pennylane-interface
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
```

```bash
# Analyser
mvn clean verify sonar:sonar
```

---

## 📞 CONTACTS EN CAS D'INCIDENT

### Responsable sécurité
- **Email :** security@mismo.fr
- **Téléphone :** +33 X XX XX XX XX

### Procédure en cas de fuite de credentials
1. **Bloquer** immédiatement les accès compromis
2. **Notifier** le responsable sécurité
3. **Changer** tous les credentials
4. **Analyser** les logs d'accès
5. **Documenter** l'incident
6. **Implémenter** des mesures préventives

---

**Version :** 1.0
**Dernière mise à jour :** 2025-11-27
**Auteur :** Claude Code - Expert Sécurité
