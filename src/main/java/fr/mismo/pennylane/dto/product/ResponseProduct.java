package fr.mismo.pennylane.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mismo.pennylane.dto.customer.Customer;
import lombok.Data;

import java.util.List;

@Data
public class ResponseProduct{

    @JsonProperty("has_more")
    private boolean hasMore;

    @JsonProperty("next_cursor")
    private String nextCursor;

    @JsonProperty("items")
    private List<Product> items;

}
