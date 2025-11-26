package fr.mismo.pennylane.dto.ledgerEntries;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class LedgerEntryResponse {

    @JsonProperty("id")
    private Long id; // Corrigé de Integer à Long

    @JsonProperty("label")
    private String label;

    @JsonProperty("date")
    private String date;

    @JsonProperty("journal_id")
    private Integer journalId;

    @JsonProperty("ledger_attachment_filename")
    private String ledgerAttachmentFilename;

    @JsonProperty("ledger_attachment_id")
    private Integer ledgerAttachmentId;

    @JsonProperty("ledger_entry_lines")
    private List<LedgerEntryLine> ledgerEntryLines;
}
