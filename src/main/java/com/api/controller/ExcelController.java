package com.api.controller;

import com.api.dto.ExcelUploadResponse;
import com.api.model.ExcelUpload;
import com.api.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    /**
     * POST /api/excel/upload
     * Upload an .xlsx or .xls file. Requires a valid JWT in Authorization header.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ExcelUploadResponse response = excelService.uploadAndParseExcel(file, userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/excel/history
     * Returns all uploads made by the authenticated user.
     */
    @GetMapping("/history")
    public ResponseEntity<List<ExcelUpload>> getUploadHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                excelService.getUserUploads(userDetails.getUsername()));
    }

    /**
     * GET /api/excel/{id}
     * Returns metadata for a specific upload (only if owned by the caller).
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUploadById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ExcelUpload upload = excelService.getUploadById(id, userDetails.getUsername());
            return ResponseEntity.ok(upload);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
