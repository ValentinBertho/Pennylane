# Interface ATHENEO ↔ PENNYLANE

Application Java Spring Boot pour la synchronisation bidirectionnelle des données comptables entre ATHENEO (ERP) et Pennylane (plateforme SaaS).

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Proprietary-red)]()

## 📋 Table des matières

- [Fonctionnalités](#-fonctionnalités)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Utilisation](#-utilisation)
- [Architecture](#-architecture)
- [Sécurité](#-sécurité)
- [Monitoring](#-monitoring)
- [Développement](#-développement)
- [Troubleshooting](#-troubleshooting)
- [Documentation](#-documentation)

## 🚀 Fonctionnalités

### Synchronisation ATHENEO → Pennylane
- ✅ Export automatique des factures de vente
- ✅ Création/mise à jour des clients
- ✅ Création/mise à jour des produits
- ✅ Upload des PDF de factures
- ✅ Synchronisation des écritures comptables

### Synchronisation Pennylane → ATHENEO
- ✅ Import des factures d'achat fournisseurs
- ✅ Téléchargement automatique des PDF
- ✅ Synchronisation des règlements (globale et détaillée)
- ✅ Mise à jour des statuts de paiement

### Fonctionnalités transverses
- 📊 Interface web de consultation des logs
- 🔄 Tâches planifiées configurables
- 🛡️ Résilience (Circuit Breaker, Retry, Rate Limiting)
- 📈 Monitoring avec Actuator et Prometheus
- 🔒 Authentification et sécurité

## 📦 Prérequis

### Logiciels requis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **SQL Server** (accès à la base ATHENEO)
- **Accès réseau** :
  - API Pennylane (https://app.pennylane.com)
  - Service SOAP WSDocument (ATHENEO)

### Credentials nécessaires

- Identifiants base de données SQL Server
- Token API Pennylane (par site)
- Credentials WSDocument SOAP
- (Optionnel) Utilisateur/mot de passe pour l'authentification HTTP Basic

## 🔧 Installation

### 1. Cloner le projet

```bash
git clone <repository-url>
cd Pennylane
```

### 2. Configurer les variables d'environnement

Copier le fichier d'exemple et le personnaliser :

```bash
cp .env.example .env
```

Éditer `.env` avec vos credentials :

```bash
# Base de données
DB_URL=jdbc:sqlserver://SERVER\\INSTANCE;databaseName=ATHENEO_MISMO;encrypt=false
DB_USERNAME=votre_utilisateur
DB_PASSWORD=votre_mot_de_passe

# WSDocument
WSDOCUMENT_URI=http://server:8081/WSDocumentAth/WSDocumentAth.svc
WSDOCUMENT_LOGIN=votre_login
WSDOCUMENT_PASSWORD=votre_mot_de_passe

# Sécurité (changer le mot de passe par défaut !)
SECURITY_USERNAME=admin
SECURITY_PASSWORD=changez_ce_mot_de_passe
```

### 3. Compiler le projet

```bash
# Compiler et créer le JAR
mvn clean package -DskipTests

# Le JAR est créé dans : target/interface-pennylane.jar
```

### 4. Configurer application.yml

Copier le template et adapter si nécessaire :

```bash
cp deploy/application-template.yml deploy/application.yml
# Éditer deploy/application.yml pour personnaliser la configuration
```

### 5. Lancer l'application

#### En développement

```bash
# Avec Maven
mvn spring-boot:run

# Avec le JAR
java -jar target/interface-pennylane.jar

# Avec profil spécifique
java -jar target/interface-pennylane.jar --spring.profiles.active=dev
```

#### En production

```bash
# Avec variables d'environnement
export DB_URL="jdbc:sqlserver://..."
export DB_USERNAME="prod_user"
export DB_PASSWORD="secure_password"

java -jar target/interface-pennylane.jar \
  --spring.config.location=file:./deploy/application.yml \
  --server.port=8093
```

#### Avec systemd (Linux)

Créer un fichier `/etc/systemd/system/pennylane-interface.service` :

```ini
[Unit]
Description=Interface ATHENEO-PENNYLANE
After=network.target

[Service]
Type=simple
User=pennylane
WorkingDirectory=/opt/pennylane-interface
Environment="DB_URL=jdbc:sqlserver://..."
Environment="DB_USERNAME=prod_user"
Environment="DB_PASSWORD=secure_password"
Environment="SECURITY_USERNAME=admin"
Environment="SECURITY_PASSWORD=your_secure_password"
ExecStart=/usr/bin/java -jar /opt/pennylane-interface/interface-pennylane.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Activer et démarrer :

```bash
sudo systemctl daemon-reload
sudo systemctl enable pennylane-interface
sudo systemctl start pennylane-interface
sudo systemctl status pennylane-interface
```

## ⚙️ Configuration

### Configuration des tâches planifiées

Les tâches sont configurées via les propriétés `cron.*` :

```yaml
cron:
  # Export écritures ATHENEO → Pennylane (toutes les 10 secondes)
  Entries: "*/10 * * * * *"

  # Import factures achats Pennylane → ATHENEO (toutes les 30 minutes)
  Purchases: "0 */30 * * * *"

  # Sync règlements (toutes les heures)
  PurchaseReglementV2: "0 0 * * * *"

  # Purge logs anciens (tous les jours à 2h)
  PurgeLog: "0 0 2 * * *"

  # Désactiver une tâche
  Customer: "-"
```

**Format** : `secondes minutes heures jour mois jour_semaine`

### Configuration de la sécurité

#### Mode production (recommandé)

```yaml
security:
  basic:
    enabled: true
  user:
    name: ${SECURITY_USERNAME:admin}
    password: ${SECURITY_PASSWORD}
```

#### Mode développement

```yaml
security:
  basic:
    enabled: false
```

⚠️ **Attention** : Ne JAMAIS désactiver la sécurité en production !

### Configuration de la résilience

La configuration par défaut est optimisée pour la production. Pour personnaliser :

```yaml
resilience4j:
  circuitbreaker:
    instances:
      pennylane-api:
        slidingWindowSize: 20          # Fenêtre d'observation
        failureRateThreshold: 40        # % d'erreurs pour ouvrir
        waitDurationInOpenState: 30s    # Attente avant retry

  retry:
    instances:
      pennylane-api:
        maxAttempts: 4                  # Nombre de tentatives
        waitDuration: 2s                # Délai entre tentatives
        enableExponentialBackoff: true  # Backoff exponentiel

  ratelimiter:
    instances:
      pennylane-api:
        limitForPeriod: 2               # 2 requêtes
        limitRefreshPeriod: 1s          # par seconde
```

## 🎯 Utilisation

### Interface web de logs

Accéder à l'interface : `http://localhost:8093/api/v1/logs`

Fonctionnalités :
- 📊 Dashboard avec statistiques
- 🔍 Recherche multicritères
- ⚠️ Consultation des erreurs
- 🐌 Détection des traitements lents
- 📄 Export PDF

### Endpoints API

| Endpoint | Description | Authentification |
|----------|-------------|------------------|
| `/actuator/health` | État de santé | Non |
| `/actuator/metrics` | Métriques | Oui |
| `/actuator/prometheus` | Métriques Prometheus | Oui |
| `/logs` | Interface de logs | Oui |
| `/logs/errors` | Logs d'erreurs | Oui |
| `/logs/slow` | Traitements lents | Oui |

### Exemples d'appels

#### Health check

```bash
curl http://localhost:8093/api/v1/actuator/health
```

#### Métriques (avec authentification)

```bash
curl -u admin:password http://localhost:8093/api/v1/actuator/metrics
```

#### Logs d'erreurs

```bash
curl -u admin:password http://localhost:8093/api/v1/logs/errors
```

## 🏗️ Architecture

### Structure du projet

```
pennylane/
├── src/
│   ├── main/
│   │   ├── java/fr/mismo/pennylane/
│   │   │   ├── api/              # Clients REST (Pennylane API)
│   │   │   ├── configuration/    # Configuration Spring
│   │   │   ├── controller/       # Contrôleurs web
│   │   │   ├── dao/              # Repositories et entités JPA
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── Scheduler/        # Tâches planifiées
│   │   │   ├── service/          # Services métier
│   │   │   └── settings/         # Paramètres applicatifs
│   │   └── resources/
│   │       ├── templates/        # Templates Thymeleaf
│   │       ├── wsdocument/       # WSDL pour SOAP
│   │       └── application.yml   # Configuration
│   └── test/                     # Tests (à développer)
├── deploy/
│   ├── application.yml           # Config déploiement
│   └── application-template.yml  # Template config
├── .env.example                  # Exemple variables d'env
├── pom.xml                       # Configuration Maven
├── README.md                     # Ce fichier
└── DOCUMENTATION_FONCTIONNELLE.md # Doc fonctionnelle complète
```

### Stack technique

**Backend** :
- Java 21
- Spring Boot 3.3.0 (Web, Security, JPA, Cache)
- Hibernate / JPA
- SQL Server JDBC Driver

**API** :
- REST Client (Pennylane API)
- SOAP Client (WSDocument)
- Spring WebFlux

**Résilience** :
- Resilience4j (Circuit Breaker, Retry, Rate Limiter)

**Monitoring** :
- Spring Boot Actuator
- Micrometer
- Prometheus

**Sécurité** :
- Spring Security
- BCrypt password encoding

**Autres** :
- Lombok (réduction code boilerplate)
- iTextPDF (génération PDF)
- Thymeleaf (templates web)

## 🔒 Sécurité

### Recommandations de sécurité

#### ✅ À FAIRE

1. **Externaliser les credentials**
   - Utiliser des variables d'environnement
   - Ne JAMAIS committer de mots de passe dans Git
   - Utiliser un vault en production (Vault, AWS Secrets Manager, etc.)

2. **Sécuriser l'accès**
   - Changer le mot de passe par défaut
   - Utiliser des mots de passe forts (min 16 caractères)
   - Activer l'authentification en production

3. **Réseau**
   - Utiliser HTTPS en production
   - Restreindre l'accès réseau (firewall)
   - Utiliser un reverse proxy (nginx, Apache)

4. **Logs**
   - Éviter de logger des données sensibles
   - Purger régulièrement les anciens logs
   - Protéger l'accès aux logs

#### ❌ À NE PAS FAIRE

- ❌ Committer des credentials dans Git
- ❌ Désactiver CSRF en production
- ❌ Exposer directement l'application sur Internet
- ❌ Utiliser le mot de passe par défaut
- ❌ Désactiver la sécurité en production

### Checklist de sécurité avant mise en production

- [ ] Variables d'environnement configurées
- [ ] Mot de passe admin changé
- [ ] Authentification activée (`security.basic.enabled=true`)
- [ ] HTTPS activé sur le reverse proxy
- [ ] Firewall configuré
- [ ] Logs sensibles masqués
- [ ] Backup de la base de données configuré
- [ ] Plan de disaster recovery documenté

## 📊 Monitoring

### Métriques disponibles

#### Métriques applicatives

- `http_server_requests_seconds` : Latence des requêtes HTTP
- `jvm_memory_used_bytes` : Utilisation mémoire JVM
- `system_cpu_usage` : Utilisation CPU
- `process_uptime_seconds` : Uptime de l'application

#### Métriques de résilience

- `resilience4j_circuitbreaker_state` : État des circuit breakers
- `resilience4j_circuitbreaker_failure_rate` : Taux d'échec
- `resilience4j_retry_calls_total` : Nombre de retry
- `resilience4j_ratelimiter_available_permissions` : Permissions rate limiter

### Intégration Prometheus

1. **Exposer les métriques** :
   - Endpoint : `http://server:8093/api/v1/actuator/prometheus`

2. **Configurer Prometheus** (`prometheus.yml`) :

```yaml
scrape_configs:
  - job_name: 'pennylane-interface'
    metrics_path: '/api/v1/actuator/prometheus'
    basic_auth:
      username: 'admin'
      password: 'your_password'
    static_configs:
      - targets: ['server:8093']
```

3. **Démarrer Prometheus** :

```bash
./prometheus --config.file=prometheus.yml
```

### Dashboards Grafana

Métriques recommandées à surveiller :
- Taux d'erreur HTTP (> 5%)
- Latence P95 (> 5 secondes)
- État circuit breakers (ouvert)
- Nombre de retry (tendance croissante)
- Utilisation mémoire (> 80%)
- Nombre d'erreurs dans les logs

## 🛠️ Développement

### Prérequis développement

- IDE recommandé : IntelliJ IDEA, Eclipse, VS Code
- Plugin Lombok installé dans l'IDE
- Git
- Postman ou curl pour tester les API

### Compiler et lancer en mode développement

```bash
# Compiler
mvn clean compile

# Lancer avec rechargement automatique (devtools)
mvn spring-boot:run

# Désactiver la sécurité pour le dev
export SECURITY_ENABLED=false
mvn spring-boot:run
```

### Exécuter les tests

```bash
# Tous les tests
mvn test

# Tests spécifiques
mvn test -Dtest=NomDuTest

# Tests avec couverture de code
mvn clean test jacoco:report
# Rapport dans : target/site/jacoco/index.html
```

### Formater le code

```bash
# Vérifier le formatage
mvn spotless:check

# Appliquer le formatage
mvn spotless:apply
```

### Bonnes pratiques

- ✅ Toujours créer une branche pour une nouvelle fonctionnalité
- ✅ Écrire des tests unitaires et d'intégration
- ✅ Documenter les classes et méthodes (Javadoc)
- ✅ Logger les événements importants
- ✅ Gérer les exceptions proprement
- ✅ Utiliser les patterns de résilience (Retry, Circuit Breaker)

## 🐛 Troubleshooting

### Problème : L'application ne démarre pas

**Symptômes** : Erreur au démarrage

**Vérifications** :
1. Java 21+ installé : `java -version`
2. Variables d'environnement définies
3. Base de données accessible
4. Port 8093 disponible : `netstat -an | grep 8093`

**Solution** :
```bash
# Vérifier les logs
tail -f logs/spring.log

# Tester la connectivité DB
telnet db-server 1433
```

### Problème : Factures non synchronisées

**Symptômes** : Les factures ne sont pas exportées/importées

**Vérifications** :
1. Tâche planifiée activée : vérifier `cron.*`
2. Logs d'erreurs : `/logs/errors`
3. Circuit breaker ouvert : `/actuator/health`
4. Token Pennylane valide

**Solution** :
```bash
# Consulter les logs
curl -u admin:pass http://localhost:8093/api/v1/logs/errors

# Vérifier le health
curl http://localhost:8093/api/v1/actuator/health
```

### Problème : Erreur "Circuit breaker is OPEN"

**Symptômes** : Erreur dans les logs, appels API rejetés

**Cause** : Trop d'échecs consécutifs ont ouvert le circuit breaker

**Solution** :
1. Identifier la cause (logs d'erreurs)
2. Corriger le problème (réseau, API, credentials)
3. Attendre 30s (fermeture automatique en half-open)
4. Ou redémarrer l'application

### Problème : Performance dégradée

**Symptômes** : Traitements lents, timeouts

**Vérifications** :
1. Consulter "Traitements lents" : `/logs/slow`
2. Métriques : `/actuator/metrics`
3. Utilisation mémoire/CPU

**Solution** :
```bash
# Analyser les traitements lents
curl -u admin:pass http://localhost:8093/api/v1/logs/slow

# Vérifier la mémoire
curl -u admin:pass http://localhost:8093/api/v1/actuator/metrics/jvm.memory.used
```

### Problème : Erreur d'authentification Pennylane

**Symptômes** : HTTP 401 Unauthorized

**Cause** : Token Pennylane invalide ou expiré

**Solution** :
1. Vérifier le token dans `T_SITE.PENNYLANE_TOKEN`
2. Régénérer un token dans l'interface Pennylane
3. Mettre à jour la base de données
4. Redémarrer l'application

### Support

Pour obtenir de l'aide :
1. Consulter la [Documentation fonctionnelle](DOCUMENTATION_FONCTIONNELLE.md)
2. Consulter les logs détaillés
3. Vérifier le health check
4. Contacter le support technique

## 📚 Documentation

- **[Documentation fonctionnelle](DOCUMENTATION_FONCTIONNELLE.md)** : Guide complet des fonctionnalités
- **[Template de configuration](deploy/application-template.yml)** : Configuration complète commentée
- **[Variables d'environnement](.env.example)** : Exemple de configuration

### Documentation externe

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.3.0/reference/html/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Pennylane API Documentation](https://pennylane.readme.io/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)

## 📝 Changelog

### Version 1.10.2
- ✨ Ajout de la résilience avec Resilience4j
- 🔒 Amélioration de la sécurité (authentification, CSRF)
- 📊 Ajout du monitoring avec Actuator et Prometheus
- 📖 Documentation fonctionnelle et technique complète
- ⚙️ Externalisation des credentials via variables d'environnement
- 🐛 Corrections de bugs divers

### Version 1.10.x
- Export factures de vente ATHENEO → Pennylane
- Import factures d'achat Pennylane → ATHENEO
- Synchronisation des règlements
- Interface web de logs

## 🤝 Contribution

Pour contribuer au projet :
1. Créer une branche feature : `git checkout -b feature/ma-fonctionnalite`
2. Commiter les changements : `git commit -m "Ajout de ma fonctionnalité"`
3. Pusher la branche : `git push origin feature/ma-fonctionnalite`
4. Créer une Pull Request

## 📄 Licence

Proprietary - Tous droits réservés

---

**Développé par** : MISMO
**Version** : 1.10.2
**Dernière mise à jour** : 2025-11-26
