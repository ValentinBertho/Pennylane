package fr.mismo.pennylane.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransactionsReference {

    @JsonProperty("banking_provider")
    private String bankingProvider;

    @JsonProperty("provider_field_name")
    private String providerFieldName;

    @JsonProperty("provider_field_value")
    private String providerFieldValue;
}
