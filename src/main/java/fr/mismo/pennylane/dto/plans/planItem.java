package fr.mismo.pennylane.dto.plans;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class planItem {

    @JsonProperty("number")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String number;

    @JsonProperty("label")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String label;

    @JsonProperty("enabled")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private boolean enabled;

    @JsonProperty("vat_rate")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String vatRate;

    @JsonProperty("country_alpha2")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String countryAlpha2;

    @JsonProperty("description")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;

    @JsonProperty("v2_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer v2Id;
}
