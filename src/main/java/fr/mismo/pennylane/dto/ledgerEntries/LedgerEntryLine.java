package fr.mismo.pennylane.dto.ledgerEntries;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LedgerEntryLine {

    @JsonProperty("id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id; // Corrigé de Integer à Long

    @JsonProperty("debit")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String debit;

    @JsonProperty("credit")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String credit;

    @JsonProperty("ledger_account_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long ledgerAccountId; // Corrigé de Integer à Long

    @JsonProperty("label")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String label;
}
