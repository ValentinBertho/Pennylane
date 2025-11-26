package fr.mismo.pennylane.dto;

import lombok.Data;

@Data
public class Category {
    private String created_at;
    private String direction;
    private Long id;
    private String label;
    private String updated_at;
    private String analytical_code;
    private CategoryGroup category_group;
}

