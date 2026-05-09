package com.free.easyLearn.dto.document;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAccessResponse {
    private List<DocumentAccessDTO> data;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;
}
