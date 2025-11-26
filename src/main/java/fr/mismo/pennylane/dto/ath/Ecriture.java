package fr.mismo.pennylane.dto.ath;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Ecriture {

    @JsonProperty("ECRITURE_LOT")
    private Integer ecritureLot;

    @JsonProperty("NO_ECRITURE_PIECE")
    private Integer noEcriturePiece;

    @JsonProperty("NO_ECRITURE_LIGNE")
    private Integer noEcritureLigne;

    @JsonProperty("DATE_ECRITURE")
    private LocalDate dateEcriture;

    @JsonProperty("CODE_JOURNAL")
    private String codeJournal;

    @JsonProperty("COMPTE_GENERAL")
    private String compteGeneral;

    @JsonProperty("NUM")
    private String num;

    @JsonProperty("NUMERO_DE_PIECE")
    private String numeroDePiece;

    @JsonProperty("REFERENCE")
    private String reference;

    @JsonProperty("COMPTE_TIERS")
    private String compteTiers;

    @JsonProperty("LIBELLE")
    private String libelle;

    @JsonProperty("DATE_ECHEANCE")
    private LocalDate dateEcheance;

    @JsonProperty("SENS")
    private String sens;

    @JsonProperty("MONTANT")
    private Double montant;

    @JsonProperty("TYPE")
    private String type;

    @JsonProperty("SECTION_AXE")
    private String sectionAxe;

    @JsonProperty("SECTION")
    private String section;

    @JsonProperty("NO_A_FACTURE")
    private Integer noAFacture;

    @JsonProperty("NO_V_FACTURE")
    private Integer noVFacture;
}
