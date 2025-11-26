package fr.mismo.pennylane.dto.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Mandate {

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("source_id")
    private String sourceId;

}
