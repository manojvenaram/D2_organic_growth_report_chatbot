package com.example.excelviz.controller;

import com.example.excelviz.service.ExcelParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Handles Excel file upload and parsing.
 * POST /api/excel/upload
 */
@RestController
@RequestMapping("/api/excel")
public class ExcelUploadController {

    private static final Logger log = LoggerFactory.getLogger(ExcelUploadController.class);

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private com.example.excelviz.service.ExcelCacheService excelCacheService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadExcel(
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please upload a non-empty Excel file"));
        }

        String name = file.getOriginalFilename();
        if (name == null || (!name.endsWith(".xlsx") && !name.endsWith(".xls"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only Excel files (.xlsx, .xls) are supported"));
        }

        log.info("Processing upload: {} ({} bytes)", name, file.getSize());

        excelParserService.parseExcel(file);

        int siteCount = excelCacheService.getOwningSites() != null ? excelCacheService.getOwningSites().size() : 0;
        log.info("Upload complete: {} ({} sites loaded)", name, siteCount);
        return ResponseEntity.ok(Map.of("message", "Excel uploaded and processed successfully (" + siteCount + " sites loaded)"));
    }
}