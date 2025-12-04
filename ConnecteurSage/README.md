# Connecteur ATHENEO ↔ SAGE

## 📋 Description

Connecteur bidirectionnel entre ATHENEO et SAGE 100c développé en .NET Framework 4.8 / C#. Ce connecteur permet de synchroniser les données comptables et commerciales entre les deux systèmes.

## 🎯 Fonctionnalités

### ✅ Export (ATHENEO → SAGE)
- **Factures de vente** : Export des factures clients depuis Athénéo vers Sage
- Mapping automatique des données (clients, produits, lignes de facture)
- Gestion des TVA et des montants
- Traçabilité complète des exports

### ✅ Import (SAGE → ATHENEO)
- **Factures d'achat** : Import des factures fournisseurs depuis Sage
- **Règlements** : Import des règlements clients et fournisseurs
- Mise à jour automatique des statuts de paiement
- Détection des doublons par référence externe

## 🏗️ Architecture

```
ConnecteurSage/
├── Models/                      # Modèles de données
│   ├── FactureAtheneo.cs       # Facture Athénéo (export)
│   ├── FactureSage.cs          # Facture Sage (export)
│   ├── FactureAchatSage.cs     # Facture achat depuis Sage
│   ├── FactureAchatAtheneo.cs  # Facture achat pour Athénéo
│   ├── ReglementSage.cs        # Règlement depuis Sage
│   └── Ligne*.cs               # Lignes de facture
│
├── Services/                    # Services métier
│   ├── SageConnector.cs        # Connecteur COM Interop Sage
│   ├── AtheneoReader.cs        # Lecture données Athénéo
│   ├── FactureMapper.cs        # Mapping données
│   ├── ExportFacturesService.cs          # Export factures vente
│   ├── ImportFacturesAchatsService.cs    # Import factures achats
│   ├── ImportReglementsService.cs        # Import règlements
│   └── Logger.cs               # Gestion des logs
│
├── Utils/                       # Utilitaires
│   └── ConfigurationManager.cs # Gestion configuration
│
├── SQL/                         # Procédures stockées
│   ├── SP_EXPORT_FACTURE.sql
│   ├── SP_GET_FACTURE.sql
│   ├── SP_IMPORT_FACTURE_ACHAT.sql
│   ├── SP_IMPORT_LIGNE_FACTURE_ACHAT.sql
│   └── SP_IMPORT_REGLEMENT.sql
│
└── Config/
    └── AppSettings.json         # Configuration
```

## ⚙️ Configuration

### AppSettings.json

```json
{
  "ConnectionStrings": {
    "AtheneoDb": "Server=localhost;Database=AtheneoDB;User Id=sa;Password=***;TrustServerCertificate=True;"
  },
  "Sage": {
    "ProgID": "Sage100c.SDO.Application",
    "CompanyName": "NomSociete",
    "Username": "Utilisateur",
    "Password": "MotDePasse",
    "ConnectionTimeout": 30
  },
  "Sync": {
    "BatchSize": 50,
    "EnableDryRun": false,
    "LogLevel": "Information",
    "RetryAttempts": 3,
    "RetryDelaySeconds": 5
  },
  "Filters": {
    "ExportFactures": {
      "StatutFacture": "2",
      "DateDebutExport": null
    },
    "ImportFacturesAchats": {
      "DateDebutImport": null,
      "JoursRetroactifs": 30
    },
    "ImportReglements": {
      "DateDebutImport": null,
      "JoursRetroactifs": 7
    }
  }
}
```

### Paramètres Sage

- **ProgID** : Identifiant COM de Sage (vérifier dans la documentation Sage)
  - Sage 100c : `Sage100c.SDO.Application`
  - Sage 1000 : `Sage.CRM.Application`
- **CompanyName** : Nom du dossier/société dans Sage
- **Username/Password** : Identifiants de connexion Sage

## 🚀 Utilisation

### Ligne de commande

```bash
# Export des factures de vente (Athénéo → Sage)
AtheneoSageSync.exe ExportFactures

# Import des factures d'achat (Sage → Athénéo)
AtheneoSageSync.exe ImportFacturesAchat

# Import des règlements (Sage → Athénéo)
AtheneoSageSync.exe ImportReglements

# Mode simulation (sans modification réelle)
AtheneoSageSync.exe ExportFactures --dry-run
```

### Planification (Task Scheduler Windows)

**Exemple de planification quotidienne** :

1. Ouvrir le Planificateur de tâches Windows
2. Créer une nouvelle tâche
3. Déclencheur : Tous les jours à 08:00
4. Action : Lancer `AtheneoSageSync.exe ExportFactures`
5. Répéter pour les autres modes

## 📊 Logs

Les logs sont générés dans le répertoire `logs/` avec rotation quotidienne :

```
logs/
├── export-sage-20241204.log
├── export-sage-20241205.log
└── ...
```

Format des logs :
```
[2024-12-04 08:00:00 INF] ═══════════════════════════════════════════════════
[2024-12-04 08:00:00 INF] 🚀 EXPORT FACTURES VENTE : Athénéo → Sage
[2024-12-04 08:00:00 INF] ═══════════════════════════════════════════════════
[2024-12-04 08:00:01 INF] 📥 5 factures à exporter
[2024-12-04 08:00:02 INF] 🔄 Export facture vente FA-2024-001 vers Sage
[2024-12-04 08:00:03 INF] ✅ Facture FA-2024-001 exportée vers Sage avec succès
```

## 🗄️ Base de données

### Tables utilisées

**Athénéo** :
- `V_FACTURE` / `V_L_FACTURE` : Factures de vente
- `A_FACTURE` / `A_L_FACTURE` : Factures d'achat
- `REGLEMENT` : Règlements
- `SYNCHRO_MARQUAGE` : Traçabilité des synchronisations
- `SOCIETE` : Clients/Fournisseurs
- `PRODUITS` : Articles

### Procédures stockées

| Procédure | Description |
|-----------|-------------|
| `SP_EXPORT_FACTURE` | Liste les factures à exporter |
| `SP_GET_FACTURE` | Récupère une facture et ses lignes |
| `SP_GET_FACTURE_LINES` | Récupère les lignes d'une facture |
| `SP_TRAITER_FACTURE` | Marque une facture comme exportée |
| `SP_IMPORT_FACTURE_ACHAT` | Importe une facture d'achat |
| `SP_IMPORT_LIGNE_FACTURE_ACHAT` | Importe une ligne de facture |
| `SP_IMPORT_REGLEMENT` | Importe un règlement |

### Pattern de personnalisation

Toutes les procédures stockées supportent le pattern de personnalisation :

```sql
IF EXISTS (SELECT * FROM sysobjects WHERE id = OBJECT_ID(N'[spe_SP_NOM_PROCEDURE]'))
BEGIN
    EXEC spe_SP_NOM_PROCEDURE @params
END
ELSE
BEGIN
    -- Implémentation par défaut
END
```

Pour personnaliser, créer une procédure préfixée par `spe_`.

## 🔧 Développement

### Prérequis

- .NET Framework 4.8 SDK
- Visual Studio 2019 ou supérieur
- SQL Server 2016 ou supérieur
- Sage 100c installé (avec SDK COM)

### Build

```bash
# Restauration des packages NuGet
nuget restore AtheneoSageSync.sln

# Compilation
msbuild AtheneoSageSync.sln /p:Configuration=Release
```

### Dépendances NuGet

- **Dapper 2.0.123** : Micro-ORM pour accès SQL
- **Newtonsoft.Json 13.0.3** : Gestion JSON
- **Serilog 4.3.0** : Logging structuré
- **System.Data.SqlClient 4.9.0** : Accès SQL Server

## 🔍 Dépannage

### Erreur de connexion Sage

```
❌ Erreur connexion Sage (HRESULT: 0x80040154): Class not registered
```

**Solution** :
- Vérifier que Sage est installé
- Vérifier le ProgID dans AppSettings.json
- Exécuter en tant qu'administrateur
- Vérifier que l'utilisateur a les droits Sage

### Erreur COM "Unknown name"

```
❌ Erreur COM export facture (HRESULT: 0x80020006): Unknown name
```

**Solution** :
- La méthode/propriété n'existe pas dans votre version de Sage
- Consulter la documentation de l'API Sage pour votre version
- Adapter le code dans `SageConnector.cs`

### Procédure stockée introuvable

```
Could not find stored procedure 'SP_IMPORT_FACTURE_ACHAT'
```

**Solution** :
- Exécuter les scripts SQL du répertoire `SQL/`
- Vérifier les permissions de l'utilisateur SQL

## 📈 Performance

### Optimisations implémentées

- **Transactions SQL** : Garantit l'intégrité des données
- **Batch processing** : Traitement par lot configurable
- **Connection pooling** : Réutilisation des connexions DB
- **Lazy loading** : Chargement à la demande
- **Dispose pattern** : Libération correcte des ressources COM

### Recommandations

- Exécuter en heures creuses pour les gros volumes
- Ajuster `BatchSize` selon la mémoire disponible
- Surveiller les logs pour détecter les ralentissements
- Indexer les colonnes `REFERENCE_EXTERNE` dans les tables

## 🔐 Sécurité

### Bonnes pratiques

✅ **À faire** :
- Stocker les mots de passe dans un gestionnaire sécurisé
- Utiliser des comptes dédiés avec droits minimaux
- Activer TrustServerCertificate uniquement en dev
- Surveiller les logs pour détecter des anomalies

❌ **À éviter** :
- Commit des mots de passe dans le contrôle de source
- Exécution avec des comptes administrateurs
- Désactivation complète des logs

### Chiffrement

Pour chiffrer les sections sensibles d'AppSettings.json :

```bash
# Utiliser aspnet_regiis.exe ou un outil de chiffrement personnalisé
aspnet_regiis -pef "ConnectionStrings" "C:\Path\To\Config"
```

## 📝 TODO / Améliorations futures

- [ ] Mode serveur Windows Service pour synchronisation continue
- [ ] Interface web de monitoring
- [ ] Export des règlements depuis Athénéo
- [ ] Support de Sage API REST (si disponible)
- [ ] Tests unitaires avec moq
- [ ] Métriques de performance (StatsD/Prometheus)
- [ ] Support multi-société
- [ ] Notifications par email en cas d'erreur

## 📞 Support

Pour toute question ou problème :

1. Consulter les logs dans `logs/`
2. Vérifier la configuration dans `AppSettings.json`
3. Consulter la documentation Sage API
4. Contacter l'équipe technique

## 📜 Licence

Propriétaire - Tous droits réservés

---

**Version** : 1.0.0
**Date** : Décembre 2024
**Auteur** : Valentin Bertho
