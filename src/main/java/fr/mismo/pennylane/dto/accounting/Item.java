package fr.mismo.pennylane.dto.accounting;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Item {

    @JsonProperty("id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer id;

    @JsonProperty("label")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String label;

    @JsonProperty("number")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String number;

    @JsonProperty("vat_rate")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String vatRate;

    @JsonProperty("country_alpha2")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String countryAlpha2;

    @JsonProperty("enabled")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enabled;
}
