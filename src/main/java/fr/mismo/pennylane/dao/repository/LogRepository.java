package fr.mismo.pennylane.dao.repository;
import fr.mismo.pennylane.dao.entity.LogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface LogRepository extends CrudRepository<LogEntity, Integer> {

    @Modifying
    @Transactional
    @Query
    (value = "EXEC sp_LOG_Enregistrer @NIVEAU = :niveau, @TRAITEMENT = :traitement, @INITIATEUR = :initiateur, @MESSAGE = :message", nativeQuery = true)
    void logEnregistrer(
            @Param("niveau") String niveau,
            @Param("traitement") String traitement,
            @Param("initiateur") String initiateur,
            @Param("message") String message
    );

    // Log de la procédure 'SP_PENNYLANE_TRAITER_LOT'
    @Modifying
    @Transactional
    @Query(value = "EXEC SP_PENNYLANE_TRAITER_LOT @NO_ECRITURE_LOT = :noEcritureLot, @MESSAGE = :message, @SUCCES = :succes", nativeQuery = true)
    void traiterLot(
            @Param("noEcritureLot") Integer noEcritureLot,
            @Param("message") String message,
            @Param("succes") Boolean succes
    );

    // Log de la procédure 'SP_PENNYLANE_TRAITER_LOT'
    @Modifying
    @Transactional
    @Query(value = "EXEC SP_PENNYLANE_TRAITER_FACTURE @NO_V_FACTURE = :noVFacture, @ID_PENNYLANE = :idPennylane, @ID_PENNYLANE_V2 = :idPennylaneV2, @SUCCES = :succes", nativeQuery = true)
    void traiterFacture(
            @Param("noVFacture") Integer noVFacture,
            @Param("idPennylane") String idPennylane,
            @Param("idPennylaneV2") String idPennylaneV2,
            @Param("succes") Boolean succes
    );

    // Log de la procédure 'SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP'
    @Modifying
    @Transactional
    @Query(value = "EXEC SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP @ID_A_FACTURE = :idFacture, @MESSAGE = :message, @SUCCES = :succes", nativeQuery = true)
    void traiterSupplierInvoiceBap(
            @Param("idFacture") String idFacture,
            @Param("message") String message,
            @Param("succes") Boolean succes
    );

    @Modifying
    @Transactional
    @Query(value = "EXEC SP_PENNYLANE_MAJ_SOCIETE @CODE_COMPTABLE = :codeComptable, @ID_PENNYLANE = :idPennylane, @ID_PENNYLANE_V2 = :idPennylaneV2, @COD_SITE = :codSite", nativeQuery = true)
    void majSociete(
            @Param("codeComptable") String codeComptable,
            @Param("idPennylane") String idPennylane,
            @Param("idPennylaneV2") String idPennylaneV2,
            @Param("codSite") String codSite
    );

    @Transactional
    @Query(value = "DECLARE @RESULT_OUTPUT INT; " +
            "EXEC SP_PENNYLANE_SUPPLIER_INVOICE_CREER_REGLEMENT " +
            "    @INVOICE_ID = :invoiceId, " +
            "    @TRANSACTION_ID = :transactionId, " +
            "    @MONTANT = :montant, " +
            "    @DATE_REGLEMENT = :dateReglement, " +
            "    @RESULT_OUTPUT = @RESULT_OUTPUT OUTPUT; " +
            "SELECT @RESULT_OUTPUT",
            nativeQuery = true)
    int creerReglement(
            @Param("invoiceId") String invoiceId,
            @Param("transactionId") Long transactionId,
            @Param("montant") Double montant,
            @Param("dateReglement") String dateReglement
    );

    @Transactional
    @Query(value = "DECLARE @RESULT_OUTPUT INT; " +
            "EXEC SP_PENNYLANE_SUPPLIER_INVOICE_PURGE_OBSOLETE_TRANSACTIONS " +
            "    @INVOICE_ID = :invoiceId, " +
            "    @VALID_TRANSACTION_IDS_CSV = :validTransactionIdsCsv, " +
            "    @RESULT_OUTPUT = @RESULT_OUTPUT OUTPUT; " +
            "SELECT @RESULT_OUTPUT",
            nativeQuery = true)
    int purgeObsoleteTransactions(
            @Param("invoiceId") String invoiceId,
            @Param("validTransactionIdsCsv") String validTransactionIdsCsv
    );


    @Modifying
    @Transactional
    @Query(value = "EXEC SP_PENNYLANE_MAJ_PRODUITS @NO_PRODUIT = :noProduit, @ID_PENNYLANE = :idPennylane", nativeQuery = true)
    void majProduit(
            @Param("noProduit") int noProduit,
            @Param("idPennylane") String idPennylane
    );

    @Transactional
    @Query(value = "DECLARE @RESULT_OUTPUT INT; " +
            "EXEC SP_PENNYLANE_SUPPLIER_INVOICE_CREER " +
            "    @NO_SOCIETE = :noSociete, " +
            "    @ID_PENNYLANE_FOURN = :idPennylaneFourn, " +
            "    @ID_PENNYLANE_FOURN_V2 = :idPennylaneFournV2, " +
            "    @INVOICE_ID = :invoiceId, " +
            "    @INVOICE_ID_V2 = :invoiceIdV2, " +
            "    @OBJET = :objet, " +
            "    @DATE_FACTURE = :dateFacture, " +
            "    @COD_SITE = :codSite, " +
            "    @COD_DIRECTION = :codDirection, " +
            "    @COD_AGENCE = :codAgence, " +
            "    @COD_ETAT = :codEtat, " +
            "    @TOTAL_HT = :totalHT, " +
            "    @TOTAL_TTC = :totalTTC, " +
            "    @TOTAL_TVA = :totalTVA, " +
            "    @INVOICE_NUMBER = :invoiceNumber, " +
            "    @DEVISE = :devise, " +
            "    @NOM_SOCIETE = :nomSociete, " +
            "    @IMPORT_MESSAGE = :importMessage, " +
            "    @RESULT_OUTPUT = @RESULT_OUTPUT OUTPUT; " +
            "SELECT @RESULT_OUTPUT",
            nativeQuery = true)
    int creerSupplierInvoice(
            @Param("noSociete") String noSociete,
            @Param("idPennylaneFourn") String idPennylaneFourn,
            @Param("idPennylaneFournV2") String idPennylaneFournV2,
            @Param("invoiceId") String invoiceId,
            @Param("invoiceIdV2") String invoiceIdV2,
            @Param("objet") String objet,
            @Param("dateFacture") String dateFacture,
            @Param("codSite") String codSite,
            @Param("codDirection") String codDirection,
            @Param("codAgence") String codAgence,
            @Param("codEtat") String codEtat,
            @Param("totalHT") String totalHT,
            @Param("totalTTC") String totalTTC,
            @Param("totalTVA") String totalTVA,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("devise") String devise,
            @Param("nomSociete") String nomSociete,
            @Param("importMessage") String importMessage);



    @Transactional
    @Query(value = "DECLARE @RESULT_OUTPUT INT; " +
            "EXEC SP_PENNYLANE_SUPPLIER_INVOICE_MAJ " +
            "    @NO_SOCIETE = :noSociete, " +
            "    @INVOICE_ID = :invoiceId, " +
            "    @OBJET = :objet, " +
            "    @DATE_FACTURE = :dateFacture, " +
            "    @COD_SITE = :codSite, " +
            "    @TOTAL_HT = :totalHT, " +
            "    @TOTAL_TTC = :totalTTC, " +
            "    @TOTAL_TVA = :totalTVA, " +
            "    @INVOICE_NUMBER = :invoiceNumber, " +
            "    @NOM_SOCIETE = :nomSociete, " +
            "    @RESULT_OUTPUT = @RESULT_OUTPUT OUTPUT; " +
            "SELECT @RESULT_OUTPUT",
            nativeQuery = true)
    int majSupplierInvoice(
            @Param("noSociete") String noSociete,
            @Param("invoiceId") String invoiceId,
            @Param("objet") String objet,
            @Param("dateFacture") String dateFacture,
            @Param("codSite") String codSite,
            @Param("totalHT") String totalHT,
            @Param("totalTTC") String totalTTC,
            @Param("totalTVA") String totalTVA,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("nomSociete") String nomSociete);

    // Log de la procédure 'SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS'
    @Transactional
    @Query(value = "DECLARE @RESULT_OUTPUT INT; " +
            "EXEC SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS " +
            "    @PAID = :paid, " +
            "    @PAYMENT_STATUS = :paymentStatus, " +
            "    @REMAINING_AMOUNT = :remainingAmount, " +
            "    @FULLY_PAID_AT = :fullyPaidAt, " +
            "    @CURRENCY_AMOUNT = :currencyAmount, " +
            "    @INVOICE_ID = :invoiceId, " +
            "    @COD_SITE = :codSite, " +
            "    @RESULT_OUTPUT = @RESULT_OUTPUT OUTPUT; " +
            "SELECT @RESULT_OUTPUT",
            nativeQuery = true)
    int majSupplierInvoiceReglement(
            @Param("paid") Boolean paid,
            @Param("paymentStatus") String paymentStatus,
            @Param("remainingAmount") Double remainingAmount,
            @Param("fullyPaidAt") String fullyPaidAt,
            @Param("currencyAmount") Double currencyAmount,
            @Param("invoiceId") String invoiceId,
            @Param("codSite") String codSite);

    @Modifying
    @Transactional
    @Query(
            value = "EXEC SP_PENNYLANE_AJOUT_FORUM_LIGNE @ENTITE = :entite, @NO_ENTITE = :noEntite, @MESSAGE = :message, @NIVEAU = :niveau",
            nativeQuery = true
    )
    void ajouterLigneForum(
            @Param("entite") String entite,
            @Param("noEntite") String noEntite,
            @Param("message") String message,
            @Param("niveau") Integer niveau
    );

    @Modifying
    @Transactional
    @Query
            (value = "EXEC SP_LOG_PURGER", nativeQuery = true)
    void logPurger();


    @Transactional
    @Query(value = "EXEC SP_PENNYLANE_SUPPLIER_INVOICE_EXIST @INVOICE_ID = :invoiceId", nativeQuery = true)
    Integer checkIfSupplierInvoiceExists(@Param("invoiceId") String invoiceId);

    // Recherche par niveau
    Page<LogEntity> findByNiveau(String niveau, Pageable pageable);

    // Recherche par période
    Page<LogEntity> findByDateLogBetween(Date dateDebut, Date dateFin, Pageable pageable);

    // Recherche par traitement
    Page<LogEntity> findByTraitementContainingIgnoreCase(String traitement, Pageable pageable);

    // Recherche par initiateur
    Page<LogEntity> findByInitiateurContainingIgnoreCase(String initiateur, Pageable pageable);

    // Recherche par application
    Page<LogEntity> findByApplication(String application, Pageable pageable);

    // Recherche multicritères
    @Query("SELECT l FROM LogEntity l WHERE " +
            "(:niveau IS NULL OR :niveau = '' OR l.niveau = :niveau) AND " +
            "(:traitement IS NULL OR :traitement = '' OR LOWER(l.traitement) LIKE LOWER(CONCAT('%', :traitement, '%'))) AND " +
            "(:initiateur IS NULL OR :initiateur = '' OR LOWER(l.initiateur) LIKE LOWER(CONCAT('%', :initiateur, '%'))) AND " +
            "(:application IS NULL OR :application = '' OR l.application = :application) AND " +
            "(:environnement IS NULL OR :environnement = '' OR l.environnement = :environnement) AND " +
            "(:dateDebut IS NULL OR l.dateLog >= :dateDebut) AND " +
            "(:dateFin IS NULL OR l.dateLog <= :dateFin) AND " +
            "(:message IS NULL OR :message = '' OR LOWER(l.message) LIKE LOWER(CONCAT('%', :message, '%')))")
    Page<LogEntity> rechercheAvancee(
            @Param("niveau") String niveau,
            @Param("traitement") String traitement,
            @Param("initiateur") String initiateur,
            @Param("application") String application,
            @Param("environnement") String environnement,
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin,
            @Param("message") String message,
            Pageable pageable
    );

    // Statistiques par niveau
    @Query("SELECT l.niveau, COUNT(l) FROM LogEntity l WHERE l.dateLog >= :dateDebut GROUP BY l.niveau")
    List<Object[]> getStatistiquesParNiveau(@Param("dateDebut") Date dateDebut);

    // Statistiques par application
    @Query("SELECT l.application, COUNT(l) FROM LogEntity l WHERE l.dateLog >= :dateDebut GROUP BY l.application")
    List<Object[]> getStatistiquesParApplication(@Param("dateDebut") Date dateDebut);

    // Logs avec erreurs (codes HTTP >= 400)
    @Query("SELECT l FROM LogEntity l WHERE l.niveau = 'ERROR' AND l.dateLog >= :dateDebut")
    Page<LogEntity> findLogsAvecErreurs(@Param("dateDebut") Date dateDebut, Pageable pageable);

    // Logs lents (durée > seuil)
    @Query("SELECT l FROM LogEntity l WHERE l.dureeMs > :seuilMs AND l.dateLog >= :dateDebut")
    Page<LogEntity> findLogsLents(@Param("seuilMs") Long seuilMs, @Param("dateDebut") Date dateDebut, Pageable pageable);

    // Liste des applications distinctes
    @Query("SELECT DISTINCT l.application FROM LogEntity l WHERE l.application IS NOT NULL ORDER BY l.application")
    List<String> findDistinctApplications();

    // Liste des environnements distincts
    @Query("SELECT DISTINCT l.environnement FROM LogEntity l WHERE l.environnement IS NOT NULL ORDER BY l.environnement")
    List<String> findDistinctEnvironnements();

    // Liste des niveaux distincts
    @Query("SELECT DISTINCT l.niveau FROM LogEntity l WHERE l.niveau IS NOT NULL ORDER BY l.niveau")
    List<String> findDistinctNiveaux();


    @Modifying
    @Transactional
    @Query(value = "EXEC SP_FACTUREX_TRAITER_FACTURE " +
            "@NO_V_FACTURE = :noVFacture, " +
            "@FACTURE_ID = :factureId, " +
            "@FACTURE_ETAT = :factureEtat, " +
            "@FACTURE_ACTION = :factureAction, " +
            "@FACTURE_COMMENTAIRE = :factureCommentaire, " +
            "@SUCCES = :succes",
            nativeQuery = true)
    void traiterFacture(
            @Param("noVFacture") Integer noVFacture,
            @Param("factureId") String factureId,
            @Param("factureEtat") String factureEtat,
            @Param("factureAction") String factureAction,
            @Param("factureCommentaire") String factureCommentaire,
            @Param("succes") Boolean succes
    );

    /*@Modifying
    @Transactional
    @Query(
            value = "EXEC SP_FACTUREX_AJOUT_FORUM_LIGNE @ENTITE = :entite, @NO_ENTITE = :noEntite, @MESSAGE = :message, @NIVEAU = :niveau",
            nativeQuery = true
    )
    void ajouterLigneForum(
            @Param("entite") String entite,
            @Param("noEntite") String noEntite,
            @Param("message") String message,
            @Param("niveau") Integer niveau
    );*/


}

