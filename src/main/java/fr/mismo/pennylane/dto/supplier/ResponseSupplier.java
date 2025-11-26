package fr.mismo.pennylane.dto.supplier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ResponseSupplier {

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("current_page")
    private Integer currentPage;

    @JsonProperty("total_suppliers")
    private Integer totalSuppliers;

    @JsonProperty("suppliers")
    private List<Supplier> suppliers;
}
