package com.example.excelviz.controller;

import com.example.excelviz.model.ChartRequest;
import com.example.excelviz.model.ChartResponse;
import com.example.excelviz.service.AggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Handles chart data requests.
 * POST /api/charts/data
 */
@RestController
@RequestMapping("/api/charts")
public class ChartDataController {

    private static final Logger log = LoggerFactory.getLogger(ChartDataController.class);

    @Autowired
    private AggregationService aggregationService;

    @PostMapping("/data")
    public ChartResponse getChartData(@RequestBody ChartRequest request) {

        if (request.getCreationProfiles() == null || request.getCreationProfiles().isEmpty()) {
            throw new IllegalArgumentException("At least one Creation Profile must be selected");
        }
        if (request.getYears() == null || request.getYears().isEmpty()) {
            throw new IllegalArgumentException("At least one year must be selected");
        }

        log.debug("Chart request — site: {}, profiles: {}, docType: {}, years: {}",
                request.getOwningSite(),
                request.getCreationProfiles(),
                request.getDocType(),
                request.getYears());

        return aggregationService.aggregate(request);
    }
}