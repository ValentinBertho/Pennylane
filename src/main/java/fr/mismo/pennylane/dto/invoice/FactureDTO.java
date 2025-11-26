package fr.mismo.pennylane.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureDTO {

    // Facture principale
    private String noVFacture;       // Numéro de la facture
    private String chronoVFacture;   // Référence unique de la facture
    private String codSite;          // Code du site
    private String codEtat;          // État de la facture
    private Date dateFacture;        // Date de la facture
    private Double mttHt;            // Montant HT
    private Double mttTtc;           // Montant TTC
    private Double netAPayer;        // Net à payer
    private String objet;            // Objet de la facture
    private String noVLFacture;     // Numéro de la ligne de facture
    private Integer noLigne;        // Numéro de la ligne
    private String typeLigne;       // Type de ligne (Produit, Service, etc.)
    private Integer noProduit;      // Numéro du produit
    private Integer idProduit;      // Id du produit
    private String desCom;          // Description du produit ou service
    private String codTaxe;          // code taxe du produit
    private Double tauxTaxe;        // Taux de taxe
    private Integer qteFac;         // Quantité facturée
    private Double puvb;            // Prix unitaire
    private Double puNet;           // Prix unitaire
    private Double totalNet;        // Prix unitaire
    private Double totalHT;         // Total HT de la ligne
    private Integer noSociete;      // No societe
    private String customerPennylaneId;     // pennylane ID
    private String invoicePennylaneId;     // pennylane ID
    private String cpte;            // compte comptable
    private Date startDate;        // Date de début LF
    private Date endDate;        // Date de de fin LF

}

