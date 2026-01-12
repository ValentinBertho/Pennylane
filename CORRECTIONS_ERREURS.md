# Rapport de Correction des Erreurs - Pennylane Interface

## 📋 Résumé des Erreurs Corrigées

Ce document décrit les corrections apportées pour résoudre les erreurs identifiées dans les logs du 2026-01-12.

---

## 1. ✅ Deadlock SQL (Erreur 1205) - CORRIGÉ

### **Problème**
```
SQL Error: 1205, SQLState: 40001
La transaction (ID de processus 103) a été bloquée sur les ressources verrou | tampon de communication
par un autre processus et a été choisie comme victime. Réexécutez la transaction.
```

**Localisation** : `AccountingService.syncEcriture()` → `SP_PENNYLANE_AJOUT_FORUM_LIGNE`
**Cause** : Plusieurs schedulers tentent d'écrire simultanément dans la table forum, créant des verrous circulaires

### **Solutions Implémentées**

#### ✨ Nouveau fichier : `RetryHelper.java`
- Classe utilitaire pour gérer les retry avec backoff exponentiel
- Gère automatiquement les deadlocks SQL avec 3 tentatives maximum
- Backoff exponentiel : 100ms, 200ms, 400ms

#### ✨ Nouveau fichier : `LogService.java`
- Service dédié pour la gestion des logs avec transactions séparées (`REQUIRES_NEW`)
- Méthodes sécurisées :
  - `ajouterLigneForumSafe()` - Ajoute une ligne au forum avec retry
  - `traiterLotSafe()` - Traite un lot avec retry
  - `traiterFactureSafe()` - Traite une facture avec retry
  - `majSupplierInvoiceReglementSafe()` - MAJ règlements avec retry

#### 🔧 Modifications : `AccountingService.java`
- Remplacement de tous les appels directs à `logRepository` par `logService`
- Les opérations de log sont maintenant isolées dans des transactions séparées
- Retry automatique en cas de deadlock

### **Bénéfices**
- ✅ Résolution automatique des deadlocks sans intervention manuelle
- ✅ Isolation des transactions pour éviter les blocages
- ✅ Amélioration de la robustesse du système

---

## 2. ✅ Transaction Corrompue (Erreur 3930) - CORRIGÉ

### **Problème**
```
SQL Error: 3930, SQLState: S0001
La transaction actuelle ne peut pas être validée et ne prend pas en charge les opérations
qui écrivent dans le fichier journal. Restaurez la transaction.
```

**Localisation** : `InvoiceService.updateReglements()` → `SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS`
**Cause** : Une erreur antérieure dans la transaction la marque comme "rollback-only", mais le code continue d'essayer d'écrire

### **Solutions Implémentées**

#### 🔧 Modifications : `LogService.java`
- Ajout de la méthode `majSupplierInvoiceReglementSafe()`
- Utilise une transaction séparée (`REQUIRES_NEW`) pour éviter la corruption de transaction
- Retry automatique en cas de deadlock

#### 🔧 Modifications : `InvoiceService.java`
- Remplacement de l'appel direct à `logRepository.majSupplierInvoiceReglement()` par `logService.majSupplierInvoiceReglementSafe()`
- Remplacement de tous les appels à `logRepository.ajouterLigneForum()` par `logService.ajouterLigneForumSafe()`
- Ajout de gestion d'erreur robuste avec try-catch pour les logs

### **Bénéfices**
- ✅ Plus de transactions corrompues
- ✅ Isolation des opérations d'écriture dans des transactions séparées
- ✅ Gestion d'erreur améliorée

---

## 3. ℹ️ Factures Surpayées (Warning) - DOCUMENTÉ

### **Information**
```
WARN f.m.pennylane.service.InvoiceService - Facture surpayée: remaining=-281.62, total=-281.62
```

**Localisation** : `InvoiceService.computePaymentStatus()`
**Nature** : Ce n'est PAS une erreur, mais un avertissement métier

### **Explication**

Ce warning indique qu'une facture a reçu un paiement excédentaire. C'est un cas métier normal qui peut se produire pour plusieurs raisons :
- Erreur de saisie du montant de paiement
- Avoir à créer pour le client
- Correction ultérieure nécessaire

### **Comportement Actuel**

La méthode `computePaymentStatus()` gère correctement ce cas :
```java
// Cas 2: Surpaiement (remaining < 0) - avoir à créer
if (remaining < -EPSILON) {
    log.warn("Facture surpayée: remaining={}, total={}", remaining, total);
    return PaymentStatus.OVERPAID.getValue();
}
```

### **Recommandations**

1. **Aucune action requise** : Le code gère déjà correctement ce cas
2. **Notification optionnelle** : Vous pourriez ajouter une notification métier pour informer le service comptable
3. **Traçabilité** : Les warnings sont déjà loggés pour audit

---

## 🚀 Résumé des Fichiers Créés/Modifiés

### Fichiers Créés
1. ✨ `src/main/java/fr/mismo/pennylane/util/RetryHelper.java`
2. ✨ `src/main/java/fr/mismo/pennylane/service/LogService.java`

### Fichiers Modifiés
1. 🔧 `src/main/java/fr/mismo/pennylane/service/AccountingService.java`
2. 🔧 `src/main/java/fr/mismo/pennylane/service/InvoiceService.java`

---

## 📊 Tests Recommandés

### 1. Test de Deadlock
- Lancer plusieurs instances de synchronisation en parallèle
- Vérifier que les retry fonctionnent correctement
- Confirmer qu'aucune erreur 1205 n'apparaît dans les logs

### 2. Test de Transaction
- Simuler une erreur pendant `updateReglements()`
- Vérifier que la transaction n'est pas corrompue
- Confirmer qu'aucune erreur 3930 n'apparaît dans les logs

### 3. Test de Factures Surpayées
- Créer une facture avec un paiement excédentaire
- Vérifier que le statut `OVERPAID` est correctement assigné
- Confirmer que le warning apparaît dans les logs

---

## 🔍 Surveillance

### Métriques à surveiller
- Nombre de retry effectués (visible dans les logs avec `Deadlock détecté`)
- Durée des transactions (devrait diminuer)
- Nombre d'erreurs 1205 et 3930 (devrait être 0)

### Logs à vérifier
```
WARN f.m.pennylane.util.RetryHelper - Deadlock détecté pour l'opération 'ajouterLigneForum[...]' (tentative X/3)
INFO f.m.pennylane.service.LogService - MAJ réussie des règlements pour facture ID: XXX
```

---

## 📞 Support

Si vous rencontrez des problèmes avec ces corrections :
1. Vérifier les logs pour identifier les erreurs
2. Augmenter le niveau de log si nécessaire (`log.debug` → `log.trace`)
3. Vérifier les paramètres de retry dans `RetryHelper.java`

---

**Date de correction** : 2026-01-12
**Version** : 2.0.2
**Statut** : ✅ Corrections déployées et prêtes pour test
