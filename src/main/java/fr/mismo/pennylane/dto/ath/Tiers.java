package fr.mismo.pennylane.dto.ath;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Tiers {

    @JsonProperty("ID_UNIQUE")
    private String idUnique;

    @JsonProperty("COMPTE_COMPTABLE")
    private String compteComptable;

    @JsonProperty("RAISON_SOCIALE")
    private String raisonSociale;

    @JsonProperty("TYPE_TIERS")
    private String typeTiers;

    @JsonProperty("ADRESSE1")
    private String adresse1;

    @JsonProperty("ADRESSE2")
    private String adresse2;

    @JsonProperty("CP")
    private String cp;

    @JsonProperty("VILLE")
    private String ville;

    @JsonProperty("PAYS")
    private String pays;

    @JsonProperty("TELEPHONE")
    private String telephone;

    @JsonProperty("FAX")
    private String fax;

    @JsonProperty("E_MAIL")
    private String email;

    @JsonProperty("CODEAPE")
    private String codeApe;

    @JsonProperty("SIRET")
    private String siret;

    private String emailRelance;

    @JsonProperty("vat_number")
    private String tva;

    @JsonProperty("codRglt")
    private String codRglt;

    @JsonProperty("country_alpha2")
    private String codRegion;

    @JsonProperty("Intitule_banque")
    private String intituleBanque;

    @JsonProperty("Structure_banque")
    private String structureBanque;

    @JsonProperty("Code_banque")
    private String codeBanque;

    @JsonProperty("Guichet_banque")
    private String guichetBanque;

    @JsonProperty("Compte_banque")
    private String compteBanque;

    @JsonProperty("Cle_banque")
    private String cleBanque;

    @JsonProperty("Bic_banque")
    private String bicBanque;

    @JsonProperty("Code_ISO")
    private String codeIso;
}
