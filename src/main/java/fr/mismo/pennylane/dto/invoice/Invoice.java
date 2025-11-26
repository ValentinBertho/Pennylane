package fr.mismo.pennylane.dto.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.*;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Invoice {

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

    // Utilisation de LocalDate ou LocalDateTime selon le besoin
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date; // Si seule la date est nécessaire

    // Si l'heure est aussi nécessaire, utiliser LocalDateTime ou OffsetDateTime
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private OffsetDateTime date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate deadline; // Même chose pour le deadline

    @JsonProperty("currency_tax")
    private String currencyTax;

    private String tax;

    private String language;

    private Boolean paid;

    private String status;

    private Discount discount;

    @JsonProperty("ledger_entry")
    private LedgerEntry ledgerEntry;

    @JsonProperty("public_file_url")
    private String publicFileUrl;

    private String filename;

    @JsonProperty("file_attachment_id")
    private String fileId;

    @JsonProperty("remaining_amount_with_tax")
    private String remainingAmountWithTax;

    @JsonProperty("remaining_amount_without_tax")
    private String remainingAmountWithoutTax;

    private Boolean draft;

    @JsonProperty("special_mention")
    private String specialMention;

    @JsonProperty("customer_id")
    private String customerId;
    //private Customer customer;

    private Supplier supplier;

    @JsonProperty("invoice_line_sections")
    private UrlWrapper invoiceLineSections;

    private UrlWrapper categories;

    @JsonProperty("pdf_invoice_free_text")
    private String pdfInvoiceFreeText;

    @JsonProperty("pdf_invoice_subject")
    private String pdfInvoiceSubject;

    @JsonProperty("pdf_description")
    private String pdfDescription;

    @JsonProperty("billing_subscription")
    private IdWrapper billingSubscription;

    @JsonProperty("credited_invoice")
    private CreditedInvoice creditedInvoice;

    @JsonProperty("customer_invoice_template")
    private IdWrapper customerInvoiceTemplate;

    @JsonProperty("transaction_reference")
    private TransactionReference transactionReference;

    @JsonProperty("payment_status")
    private String paymentStatus;

    private UrlWrapper payments;

    @JsonProperty("matched_transactions")
    private UrlWrapper matchedTransactions;

    private UrlWrapper appendices;

    private IdWrapper estimate;

    @JsonProperty("external_reference")
    private String externalReference;

    private OffsetDateTime archivedAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @JsonProperty("invoice_lines")
    private List<LineItem> invoiceLinesList;

    // Les autres classes internes restent inchangées
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Discount {
        private String type;
        private String value;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LedgerEntry {
        private Long id;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Customer {
        private String id;
        private String url;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Supplier {
        private String id;
        private String url;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UrlWrapper {
        private String url;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IdWrapper {
        private Long id;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreditedInvoice {
        private Long id;
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
