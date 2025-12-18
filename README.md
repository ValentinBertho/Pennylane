# Interface ATHENEO ↔ Pennylane

Application Spring Boot permettant la synchronisation bidirectionnelle des données comptables et de facturation entre le système ERP interne ATHENEO et le logiciel de comptabilité cloud Pennylane.

## 📋 Objectif du projet

Cette interface middleware automatise la synchronisation des données entre deux systèmes :
- **ATHENEO** : ERP/Système comptable interne
- **Pennylane** : Logiciel de comptabilité en ligne

L'application synchronise automatiquement :
- Les écritures comptables
- Les factures et achats
- Les clients et fournisseurs (tiers)
- Les produits
- Les documents associés (PDF)

## 🔧 Fonctionnement général

L'application fonctionne comme un pont de synchronisation :

1. **Tâches planifiées** (cron jobs) qui s'exécutent à intervalles réguliers
2. **Récupération des données** depuis ATHENEO (base SQL Server)
3. **Transformation et validation** des données
4. **Envoi vers Pennylane** via API REST
5. **Journalisation** de toutes les opérations pour traçabilité

### Architecture

```
ATHENEO (SQL Server)
        ↓
  Interface Pennylane (Spring Boot)
  - Schedulers (tâches automatiques)
  - Services métier
  - API clients
  - Mappers de données
        ↓
  Pennylane (API REST)
```

## 🛠️ Technologies utilisées

### Stack principal
- **Java 21**
- **Spring Boot 3.3.0**
- **Maven** (gestionnaire de dépendances)
- **SQL Server** (base de données)

### Dépendances clés
- **Spring Data JPA** : ORM et accès base de données
- **Spring Web** : API REST et contrôleurs
- **Spring WebFlux** : Client HTTP réactif
- **Spring Security** : Authentification/Autorisation
- **Spring WS** : Services web SOAP
- **iTextPDF** : Génération de documents PDF
- **Lombok** : Réduction du code boilerplate
- **JUnit** : Tests unitaires (51 tests)

## 📦 Installation

### Prérequis
- Java 21 ou supérieur
- Maven 3.6+ (ou utiliser le wrapper Maven inclus)
- Accès à une base SQL Server
- Clés API Pennylane

## 📁 Structure du projet

```
src/
├── main/java/fr/mismo/pennylane/
│   ├── Scheduler/          # Tâches planifiées (cron)
│   ├── api/                # Clients API Pennylane
│   ├── controller/         # Contrôleurs REST
│   ├── dao/                # Accès aux données
│   │   ├── entity/         # Entités JPA (12 classes)
│   │   └── repository/     # Repositories Spring Data
│   ├── dto/                # Data Transfer Objects (39 fichiers)
│   ├── service/            # Logique métier (12 services)
│   ├── configuration/      # Configuration Spring
│   └── util/               # Utilitaires
│
├── main/resources/
│   ├── application.yml     # Configuration principale
│   ├── templates/          # Templates Thymeleaf
│   └── wsdocument/         # Définitions WSDL
│
└── test/java/              # Tests unitaires (51 tests)

ConnecteurSage/             # Connecteur C# .NET (composant séparé)
structure/                  # Scripts de base de données
deploy/                     # Configuration de déploiement
```

## 🔑 Composants principaux

### Services métier
- **AccountingService** : Synchronisation des écritures comptables
- **InvoiceService** : Synchronisation des factures
- **DocumentService** : Gestion des documents PDF
- **LogsService** : Journalisation centralisée

### Schedulers (Tâches automatiques)
- **schedulerAccounting** : Synchronise les écritures toutes les 10 secondes
- **schedulerPurchases** : Synchronise les achats

### API Clients
- **InvoiceApi** : Gestion des factures Pennylane
- **CustomerApi** : Gestion des clients/fournisseurs
- **AccountsApi** : Comptes du plan comptable
- **ProductApi** : Gestion des produits

### Mappers
- **InvoiceMapper** : Transformation des données de factures
- **TiersMapper** : Transformation des données clients/fournisseurs
- **ProductMapper** : Transformation des données produits