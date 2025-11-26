package fr.mismo.pennylane.dto.ledgerEntries;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LedgerEntryRequest {

    @JsonProperty("ledger_entry_lines")
    private List<LedgerEntryLine> ledgerEntryLines = new ArrayList<>();

    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonProperty("label")
    private String label;

    @JsonProperty("journal_id")
    private Integer journalId;

    @JsonProperty("ledger_attachment_id")
    private Integer ledgerAttachmentId;
}

