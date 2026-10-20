package com.example.excelviz.controller;

import com.example.excelviz.model.SiteCompareRequest;
import com.example.excelviz.model.SiteCompareResponse;
import com.example.excelviz.service.SiteCompareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Handles multi-site comparison requests.
 * POST /api/compare/sites
 */
@RestController
@RequestMapping("/api/compare")
public class SiteCompareController {

    private static final Logger log = LoggerFactory.getLogger(SiteCompareController.class);

    @Autowired
    private SiteCompareService siteCompareService;

    @PostMapping("/sites")
    public SiteCompareResponse compareSites(@RequestBody SiteCompareRequest request) {

        if (request.getSites() == null || request.getSites().size() < 2) {
            throw new IllegalArgumentException("Select at least 2 sites to compare");
        }
        if (request.getYears() == null || request.getYears().isEmpty()) {
            throw new IllegalArgumentException("At least one year must be selected");
        }

        log.debug("Compare request — sites: {}, years: {}, docType: {}",
                request.getSites(), request.getYears(), request.getDocType());

        return siteCompareService.compare(request);
    }
}