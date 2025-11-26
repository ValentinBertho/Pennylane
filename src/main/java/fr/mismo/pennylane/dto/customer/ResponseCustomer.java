package fr.mismo.pennylane.dto.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ResponseCustomer {

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("current_page")
    private Integer currentPage;

    @JsonProperty("total_customers")
    private Integer totalItems;

    @JsonProperty("items")
    private List<Customer> items;

}
