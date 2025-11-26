package fr.mismo.pennylane.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
public class LedgerAttachmentResponse {

    @JsonProperty("url")
    private String url;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("filename")
    private String filename;
}
