package fr.mismo.pennylane.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ChangelogResponse {
    @JsonProperty("items")
    private List<ChangelogItem> items;

    @JsonProperty("has_more")
    private Boolean hasMore;

    @JsonProperty("next_cursor")
    private String nextCursor;

    @Data
    public static class ChangelogItem {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("operation")
        private String operation;

        @JsonProperty("processed_at")
        private OffsetDateTime processedAt;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @JsonProperty("created_at")
        private OffsetDateTime createdAt;
    }
}
