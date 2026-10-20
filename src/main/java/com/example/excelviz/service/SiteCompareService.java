package com.example.excelviz.service;

import com.example.excelviz.model.ExcelRowData;
import com.example.excelviz.model.SiteCompareRequest;
import com.example.excelviz.model.SiteCompareResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SiteCompareService {

    @Autowired
    private ExcelCacheService cacheService;

    public SiteCompareResponse compare(SiteCompareRequest request) {

        List<Integer> years   = request.getYears().stream().sorted().toList();
        List<String>  sites   = request.getSites();
        String        docType = request.getDocType() != null ? request.getDocType() : "BOTH";

        // Determine which key to sum
        // BOTH = TOTAL column (sum of created + migrated)
        String typeKey = switch (docType) {
            case "CREATED"  -> ExcelParserService.CREATED;
            case "MIGRATED" -> ExcelParserService.MIGRATED;
            default         -> ExcelParserService.TOTAL;
        };

        Map<String, Map<String, Map<String, Integer>>> data = new LinkedHashMap<>();
        for (String site : sites) data.put(site, new LinkedHashMap<>());

        Map<String, Set<String>> profileToGmps = new LinkedHashMap<>();

        for (ExcelRowData row : cacheService.getExcelData()) {
            if (row.getOwningSite() == null || row.getCreationProfile() == null
                    || row.getGmpGroup() == null) continue;

            String matchedSite = sites.stream()
                .filter(s -> s.equalsIgnoreCase(row.getOwningSite().trim()))
                .findFirst().orElse(null);
            if (matchedSite == null) continue;

            String profile = row.getCreationProfile().trim();
            String gmp     = row.getGmpGroup().trim();

            // Sum across selected years using selected docType
            int total = years.stream().mapToInt(y -> {
                Map<String, Integer> typeMap = row.getYearTypeCounts() != null
                    ? row.getYearTypeCounts().getOrDefault(y, Collections.emptyMap())
                    : Collections.emptyMap();
                int c = typeMap.getOrDefault(ExcelParserService.CREATED, 0);
                int m = typeMap.getOrDefault(ExcelParserService.MIGRATED, 0);
                int t = typeMap.getOrDefault(ExcelParserService.TOTAL, c + m);
                return switch (typeKey) {
                    case "CREATED"  -> c;
                    case "MIGRATED" -> m;
                    default         -> t;
                };
            }).sum();

            if (total == 0) continue;

            data.get(matchedSite)
                .computeIfAbsent(profile, k -> new LinkedHashMap<>())
                .merge(gmp, total, Integer::sum);

            profileToGmps.computeIfAbsent(profile, k -> new LinkedHashSet<>()).add(gmp);
        }

        // Sort profiles by combined total descending
        List<String> sortedProfiles = profileToGmps.keySet().stream()
            .sorted((a, b) -> {
                int tA = sites.stream().mapToInt(s ->
                    data.get(s).getOrDefault(a, Collections.emptyMap())
                        .values().stream().mapToInt(Integer::intValue).sum()).sum();
                int tB = sites.stream().mapToInt(s ->
                    data.get(s).getOrDefault(b, Collections.emptyMap())
                        .values().stream().mapToInt(Integer::intValue).sum()).sum();
                return Integer.compare(tB, tA);
            }).collect(Collectors.toList());

        List<SiteCompareResponse.ProfileRow> profileRows = new ArrayList<>();

        for (String profile : sortedProfiles) {
            Set<String> gmps = profileToGmps.get(profile);

            List<String> sortedGmps = gmps.stream()
                .sorted((a, b) -> {
                    int tA = sites.stream().mapToInt(s ->
                        data.get(s).getOrDefault(profile, Collections.emptyMap()).getOrDefault(a, 0)).sum();
                    int tB = sites.stream().mapToInt(s ->
                        data.get(s).getOrDefault(profile, Collections.emptyMap()).getOrDefault(b, 0)).sum();
                    return Integer.compare(tB, tA);
                }).collect(Collectors.toList());

            List<Integer> profileSiteTotals = sites.stream()
                .map(s -> data.get(s).getOrDefault(profile, Collections.emptyMap())
                    .values().stream().mapToInt(Integer::intValue).sum())
                .collect(Collectors.toList());

            int profileRowTotal = profileSiteTotals.stream().mapToInt(Integer::intValue).sum();

            List<SiteCompareResponse.GmpRow> gmpRows = new ArrayList<>();
            for (String gmp : sortedGmps) {
                List<Integer> gmpSiteTotals = sites.stream()
                    .map(s -> data.get(s).getOrDefault(profile, Collections.emptyMap()).getOrDefault(gmp, 0))
                    .collect(Collectors.toList());
                SiteCompareResponse.GmpRow gmpRow = new SiteCompareResponse.GmpRow();
                gmpRow.setGmpGroup(gmp);
                gmpRow.setSiteTotals(gmpSiteTotals);
                gmpRow.setRowTotal(gmpSiteTotals.stream().mapToInt(Integer::intValue).sum());
                gmpRows.add(gmpRow);
            }

            SiteCompareResponse.ProfileRow profileRow = new SiteCompareResponse.ProfileRow();
            profileRow.setProfile(profile);
            profileRow.setSiteTotals(profileSiteTotals);
            profileRow.setRowTotal(profileRowTotal);
            profileRow.setGmpRows(gmpRows);
            profileRows.add(profileRow);
        }

        List<Integer> grandTotals = new ArrayList<>();
        for (int si = 0; si < sites.size(); si++) {
            final int idx = si;
            grandTotals.add(profileRows.stream().mapToInt(r -> r.getSiteTotals().get(idx)).sum());
        }

        SiteCompareResponse response = new SiteCompareResponse();
        response.setSites(sites);
        response.setYears(years);
        response.setProfiles(profileRows);
        response.setGrandTotals(grandTotals);
        response.setOverallTotal(grandTotals.stream().mapToInt(Integer::intValue).sum());
        response.setDocType(docType);
        return response;
    }
}