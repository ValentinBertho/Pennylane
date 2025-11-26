package fr.mismo.pennylane.dto;

import lombok.Data;

import java.util.List;

@Data
public class CategoryListResponse {
    private List<Category> items;
    private boolean has_more;
    private String next_cursor;
}
