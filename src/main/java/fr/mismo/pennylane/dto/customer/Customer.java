package fr.mismo.pennylane.dto.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.mismo.pennylane.dto.Address;
import fr.mismo.pennylane.dto.supplier.LedgerAccount;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Customer {

    private Long id;
    private String name;

    @JsonProperty("billing_iban")
    private String billingIban;

    @JsonProperty("payment_conditions")
    private String paymentConditions;

    @JsonProperty("recipient")
    private String recipient = "";

    private String phone;
    private String reference;
    private String notes;

    @JsonProperty("vat_number")
    private String vatNumber;

    @JsonProperty("reg_no")
    private String regNo;

    @JsonProperty("ledger_account")
    private LedgerAccount ledgerAccount;

    private List<String> emails;

    @JsonProperty("billing_address")
    private Address billingAddress;

    @JsonProperty("delivery_address")
    private Address deliveryAddress;

    @JsonProperty("external_reference")
    private String externalReference;

    @JsonProperty("billing_language")
    private String billingLanguage;
}
