package com.example.excelviz.controller;

import com.example.excelviz.model.ReportRequest;
import com.example.excelviz.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Handles PDF report generation.
 * POST /api/report/generate
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateReport(@RequestBody ReportRequest request) throws Exception {

        log.info("Report generation requested for site: {}", request.getOwningSite());

        byte[] pdfBytes = reportService.generateReport(request);

        String filename = "DocViz_Report_" +
                (request.getOwningSite() != null
                        ? request.getOwningSite().replaceAll("[^a-zA-Z0-9]", "_") + "_"
                        : "") +
                LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMMyyy")) +
                ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build()
        );
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}