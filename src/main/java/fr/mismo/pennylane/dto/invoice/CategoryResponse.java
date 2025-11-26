package fr.mismo.pennylane.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CategoryResponse {
    private Integer id;
    private String label;
    private String direction;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;

    @JsonProperty("category_group")
    private CategoryGroup categoryGroup;

    @JsonProperty("analytical_code")
    private String analyticalCode;

    @Data
    public static class CategoryGroup {
        private Integer id;
    }
}
