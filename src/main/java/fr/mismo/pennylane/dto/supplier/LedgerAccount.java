package fr.mismo.pennylane.dto.supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LedgerAccount {
    private Long id;
    private String number;
}
