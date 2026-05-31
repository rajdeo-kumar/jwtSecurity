package com.api.service;

import com.api.dto.ExcelUploadResponse;
import com.api.model.ExcelUpload;
import com.api.repository.ExcelUploadRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExcelService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);
    private static final int PREVIEW_ROWS = 10;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Autowired
    private ExcelUploadRepository uploadRepository;

    public ExcelUploadResponse uploadAndParseExcel(MultipartFile file, String username)
            throws IOException {

        validateFile(file);

        // Save file to disk
        String storedFileName = saveFileToDisk(file);
        String filePath = uploadDir + "/" + storedFileName;

        ExcelUpload upload = ExcelUpload.builder()
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .filePath(filePath)
                .fileSizeBytes(file.getSize())
                .uploadedBy(username)
                .uploadedAt(LocalDateTime.now())
                .status(ExcelUpload.UploadStatus.PROCESSING)
                .build();

        upload = uploadRepository.save(upload);

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = openWorkbook(file.getOriginalFilename(), inputStream);
            int totalRows = countTotalRows(workbook);
            int totalSheets = workbook.getNumberOfSheets();

            // Build preview from first sheet
            Sheet firstSheet = workbook.getSheetAt(0);
            List<String> headers = extractHeaders(firstSheet);
            List<Map<String, Object>> previewRows = extractPreviewRows(firstSheet, headers);

            upload.setTotalRows(totalRows);
            upload.setTotalSheets(totalSheets);
            upload.setStatus(ExcelUpload.UploadStatus.SUCCESS);
            upload = uploadRepository.save(upload);

            workbook.close();

            return ExcelUploadResponse.builder()
                    .uploadId(upload.getId())
                    .originalFileName(file.getOriginalFilename())
                    .fileSizeBytes(file.getSize())
                    .totalSheets(totalSheets)
                    .totalRows(totalRows)
                    .uploadedBy(username)
                    .uploadedAt(upload.getUploadedAt())
                    .status(ExcelUpload.UploadStatus.SUCCESS)
                    .message("File uploaded and parsed successfully.")
                    .headers(headers)
                    .previewRows(previewRows)
                    .previewRowCount(previewRows.size())
                    .build();

        } catch (Exception e) {
            upload.setStatus(ExcelUpload.UploadStatus.FAILED);
            upload.setErrorMessage(e.getMessage());
            uploadRepository.save(upload);
            logger.error("Failed to parse Excel file: {}", e.getMessage());
            throw new IOException("Failed to parse Excel file: " + e.getMessage(), e);
        }
    }

    public List<ExcelUpload> getUserUploads(String username) {
        return uploadRepository.findByUploadedByOrderByUploadedAtDesc(username);
    }

    public ExcelUpload getUploadById(Long id, String username) {
        return uploadRepository.findById(id)
                .filter(u -> u.getUploadedBy().equals(username))
                .orElseThrow(() -> new NoSuchElementException("Upload not found with id: " + id));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot upload an empty file.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null ||
                (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new IOException("Only .xlsx and .xls files are accepted.");
        }
    }

    private String saveFileToDisk(MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String stored = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = dir.resolve(stored);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return stored;
    }

    private Workbook openWorkbook(String filename, InputStream is) throws IOException {
        if (filename != null && filename.endsWith(".xls")) {
            return new HSSFWorkbook(is);
        }
        return new XSSFWorkbook(is);
    }

    private int countTotalRows(Workbook workbook) {
        int total = 0;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            // Subtract header row
            total += Math.max(0, sheet.getLastRowNum());
        }
        return total;
    }

    private List<String> extractHeaders(Sheet sheet) {
        List<String> headers = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return headers;

        for (Cell cell : headerRow) {
            headers.add(getCellStringValue(cell));
        }
        return headers;
    }

    private List<Map<String, Object>> extractPreviewRows(Sheet sheet, List<String> headers) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int lastRow = Math.min(sheet.getLastRowNum(), PREVIEW_ROWS);

        for (int i = 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j);
                rowMap.put(headers.get(j), cell == null ? null : getCellValue(cell));
            }
            rows.add(rowMap);
        }
        return rows;
    }

    private Object getCellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? cell.getNumericCellValue()
                    : cell.getStringCellValue();
            default      -> null;
        };
    }

    private String getCellStringValue(Cell cell) {
        Object val = getCellValue(cell);
        return val != null ? val.toString() : "";
    }
}
