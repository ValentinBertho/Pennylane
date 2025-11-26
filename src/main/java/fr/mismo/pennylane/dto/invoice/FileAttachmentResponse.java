package fr.mismo.pennylane.dto.invoice;

import lombok.Data;

@Data
public class FileAttachmentResponse {
    private Long id;
    private String url;
    private String filename;
    private String created_at;
    private String updated_at;
}
