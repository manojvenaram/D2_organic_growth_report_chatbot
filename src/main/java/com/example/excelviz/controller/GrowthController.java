package com.example.excelviz.controller;

import com.example.excelviz.model.ExecutiveSummaryResponse;
import com.example.excelviz.model.SiteGrowthData;
import com.example.excelviz.service.GrowthAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/growth")
public class GrowthController {

    private final GrowthAnalyticsService growthAnalyticsService;

    @Autowired
    public GrowthController(GrowthAnalyticsService growthAnalyticsService) {
        this.growthAnalyticsService = growthAnalyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ExecutiveSummaryResponse> getExecutiveSummary(
            @RequestParam(defaultValue = "CREATED") String docType) {
        return ResponseEntity.ok(growthAnalyticsService.getExecutiveSummary(docType));
    }

    @PostMapping("/analyze")
    public ResponseEntity<List<SiteGrowthData>> analyzeGrowth(@RequestBody List<SiteGrowthData> rawSiteData) {
        List<SiteGrowthData> processedData = growthAnalyticsService.calculateOrganicGrowth(rawSiteData);
        return ResponseEntity.ok(processedData);
    }
}