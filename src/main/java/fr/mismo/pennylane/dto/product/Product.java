package fr.mismo.pennylane.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mismo.pennylane.dto.supplier.LedgerAccount;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("label")
    private String label;

    @JsonProperty("description")
    private String description;

    @JsonProperty("vat_rate")
    private String vatRate;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("external_reference")
    private String externalReference;

    @JsonProperty("price_before_tax")
    private String priceBeforeTax;

    @JsonProperty("ledger_account")
    private LedgerAccount ledgerAccount;

}
