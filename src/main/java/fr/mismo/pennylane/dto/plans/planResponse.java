package fr.mismo.pennylane.dto.plans;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mismo.pennylane.dto.accounting.Item;
import lombok.Data;

import java.util.List;

@Data
public class planResponse {

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("current_page")
    private int currentPage;

    @JsonProperty("total_items")
    private int totalItems;

    @JsonProperty("per_page")
    private int perPage;

    @JsonProperty("plan_items")
    private List<planItem> items;
}

