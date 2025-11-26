package fr.mismo.pennylane.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class TransactionListResponse {
    @JsonProperty("items")
    private List<Transaction> items;
    @JsonProperty("has_more")
    private boolean hasMore;
    @JsonProperty("next_cursor")
    private String nextCursor;
}
