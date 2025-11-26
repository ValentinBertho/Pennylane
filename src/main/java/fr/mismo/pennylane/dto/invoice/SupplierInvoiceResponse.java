package fr.mismo.pennylane.dto.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierInvoiceResponse {

    @JsonProperty("has_more")
    private boolean hasMore;

    @JsonProperty("next_cursor")
    private String nextCursor;

    private List<SupplierInvoiceItem> items;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SupplierInvoiceItem {
        private Long id;
        private String label;

        @JsonProperty("invoice_number")
        private String invoiceNumber;

        private String currency;
        private String amount;

        @JsonProperty("currency_amount")
        private String currencyAmount;

        @JsonProperty("currency_amount_before_tax")
        private String currencyAmountBeforeTax;

        @JsonProperty("exchange_rate")
        private String exchangeRate;

        private String date;
        private String deadline;

        @JsonProperty("currency_tax")
        private String currencyTax;

        private String tax;
        private Boolean reconciled;
        private String filename;

        @JsonProperty("public_file_url")
        private String publicFileUrl;

        @JsonProperty("remaining_amount_with_tax")
        private String remainingAmountWithTax;

        @JsonProperty("remaining_amount_without_tax")
        private String remainingAmountWithoutTax;

        @JsonProperty("ledger_entry")
        private LedgerEntry ledgerEntry;

        private Supplier supplier;

        @JsonProperty("invoice_lines")
        private UrlWrapper invoiceLines;

        private UrlWrapper categories;

        @JsonProperty("transaction_reference")
        private TransactionReference transactionReference;

        @JsonProperty("payment_status")
        private String paymentStatus;

        private UrlWrapper payments;

        @JsonProperty("matched_transactions")
        private UrlWrapper matchedTransactions;

        @JsonProperty("external_reference")
        private String externalReference;

        @JsonProperty("archived_at")
        private OffsetDateTime archivedAt;

        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
    }

    // Classes internes communes

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LedgerEntry {
        private Long id;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Supplier {
        private Long id;
        private String url;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UrlWrapper {
        private String url;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TransactionReference {
        @JsonProperty("banking_provider")
        private String bankingProvider;

        @JsonProperty("provider_field_name")
        private String providerFieldName;

        @JsonProperty("provider_field_value")
        private String providerFieldValue;
    }
}
