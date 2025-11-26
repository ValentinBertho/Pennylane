package fr.mismo.pennylane.dto.planItem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanItem {

    @JsonProperty("number")
    private String number;

    @JsonProperty("label")
    private String label;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("vat_rate")
    private String vatRate;

    @JsonProperty("country_alpha2")
    private String countryAlpha2;

    @JsonProperty("description")
    private String description;

}
