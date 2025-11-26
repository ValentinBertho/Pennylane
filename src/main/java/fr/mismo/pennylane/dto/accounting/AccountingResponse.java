package fr.mismo.pennylane.dto.accounting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AccountingResponse {

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("current_page")
    private int currentPage;

    @JsonProperty("total_items")
    private int totalItems;

    @JsonProperty("per_page")
    private int perPage;

    @JsonProperty("items")
    private List<Item> items;
}

