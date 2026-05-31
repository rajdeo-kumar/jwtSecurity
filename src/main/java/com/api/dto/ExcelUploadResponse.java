package com.api.dto;

import com.api.model.ExcelUpload;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ExcelUploadResponse {
    private Long uploadId;
    private String originalFileName;
    private long fileSizeBytes;
    private int totalSheets;
    private int totalRows;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private ExcelUpload.UploadStatus status;
    private String message;

    // Preview of parsed data (first sheet, first N rows)
    private List<String> headers;
    private List<Map<String, Object>> previewRows;
    private int previewRowCount;
}
