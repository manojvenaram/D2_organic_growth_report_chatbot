package com.example.excelviz.service;

import com.example.excelviz.model.ExcelRowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * In-memory cache for parsed Excel data.
 * Cleared and repopulated on every upload.
 * Thread-safety note: single-user dashboard — no concurrent-access concern.
 */
@Service
public class ExcelCacheService {

    private static final Logger log = LoggerFactory.getLogger(ExcelCacheService.class);

    private final List<ExcelRowData>         excelData     = new ArrayList<>();
    private final Set<String>                owningSites   = new LinkedHashSet<>();
    private final Set<Integer>               years         = new TreeSet<>();
    private final Map<String, Set<String>>   gmpByProfile  = new HashMap<>();
    private boolean loaded = false;

    public void clearCache() {
        excelData.clear();
        owningSites.clear();
        years.clear();
        gmpByProfile.clear();
        loaded = false;
        log.debug("Cache cleared");
    }

    public void addRow(ExcelRowData row) {
        excelData.add(row);

        if (row.getOwningSite() != null)
            owningSites.add(row.getOwningSite());

        if (row.getYearTypeCounts() != null)
            years.addAll(row.getYearTypeCounts().keySet());

        if (row.getCreationProfile() != null && row.getGmpGroup() != null)
            gmpByProfile
                .computeIfAbsent(row.getCreationProfile(), k -> new HashSet<>())
                .add(row.getGmpGroup());
    }

    public void markLoaded() {
        this.loaded = true;
        log.info("Cache ready — {} rows, {} sites, {} years",
                excelData.size(), owningSites.size(), years.size());
    }

    public List<ExcelRowData>        getExcelData()          { return Collections.unmodifiableList(excelData); }
    public Set<String>               getOwningSites()        { return Collections.unmodifiableSet(owningSites); }
    public Set<Integer>              getAvailableYears()     { return Collections.unmodifiableSet(years); }
    public Map<String, Set<String>>  getGmpByProfile()       { return Collections.unmodifiableMap(gmpByProfile); }
    public boolean                   isLoaded()              { return loaded; }
}