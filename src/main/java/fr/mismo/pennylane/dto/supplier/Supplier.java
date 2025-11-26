package fr.mismo.pennylane.dto.supplier;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mismo.pennylane.dto.Address;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class Supplier {

    private Long id;
    private String name;

    @JsonProperty("establishment_no")
    private String establishmentNo;

    @JsonProperty("vat_number")
    private String vatNumber;

    @JsonProperty("ledger_account")
    private LedgerAccount ledgerAccount;

    private List<String> emails;

    private String iban;

    @JsonProperty("postal_address")
    private Address postalAddress;

    @JsonProperty("supplier_payment_method")
    private String supplierPaymentMethod;

    @JsonProperty("supplier_due_date_delay")
    private Integer supplierDueDateDelay;

    @JsonProperty("supplier_due_date_rule")
    private String supplierDueDateRule;

    @JsonProperty("external_reference")
    private String externalReference;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;
}
