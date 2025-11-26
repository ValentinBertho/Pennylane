package fr.mismo.pennylane.dto.planItem;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ResponsePlanItem {

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("current_page")
    private Integer currentPage;

    @JsonProperty("total_plan_items")
    private Integer totalPlanItems;

    @JsonProperty("plan_items")
    private List<PlanItem> planItems;

}
