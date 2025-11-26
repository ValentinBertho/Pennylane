package fr.mismo.pennylane.dto.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LineItem {
    @JsonProperty("product_id")
    private int productId;

    @JsonProperty("label")
    private String label;

    @JsonProperty("quantity")
    private int quantity;

    @JsonProperty("ledger_account_id")
    private String ledgerAccountId;

    @JsonProperty("currency_amount")
    private String currencyAmount;  // Changement vers Double

    @JsonProperty("currency_tax")
    private String currencyTax;

    @JsonProperty("raw_currency_unit_price")
    private String rawCurrencyUnitPrice;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("vat_rate")
    private String vatRate;

    @JsonProperty("imputation_dates")
    private ImputationDates imputationDates;
}