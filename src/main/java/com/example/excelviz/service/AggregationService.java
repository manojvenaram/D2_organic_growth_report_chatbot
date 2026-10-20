package com.example.excelviz.service;

import com.example.excelviz.model.ChartRequest;
import com.example.excelviz.model.ChartResponse;
import com.example.excelviz.model.ExcelRowData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AggregationService {

    @Autowired
    private ExcelCacheService cacheService;

    public ChartResponse aggregate(ChartRequest request) {

        List<Integer> years    = request.getYears().stream().sorted().toList();
        List<String>  xAxis    = years.stream().map(String::valueOf).toList();
        List<String>  profiles = request.getCreationProfiles();
        String        chartType = request.getChartType();
        String        docType   = request.getDocType() != null ? request.getDocType() : "BOTH";

        // ── Map: profile → gmpGroup → year → {CREATED, MIGRATED, TOTAL} ─
        Map<String, Map<String, Map<Integer, Map<String, Integer>>>> dataMap = new LinkedHashMap<>();
        
        // ── Map: profile → gmpGroup → compKey(subgroup|||artifact) → {CREATED, MIGRATED, TOTAL} ─
        Map<String, Map<String, Map<String, Map<String, Integer>>>> artifactDataMap = new LinkedHashMap<>();

        // ── Map: profile → gmpGroup → year → seriesType(CREATED/MIGRATED) → compKey → count ─
        Map<String, Map<String, Map<Integer, Map<String, Map<String, Integer>>>>> yearlyArtifactMap = new LinkedHashMap<>();

        for (String p : profiles) {
            dataMap.put(p, new LinkedHashMap<>());
            artifactDataMap.put(p, new LinkedHashMap<>());
            yearlyArtifactMap.put(p, new LinkedHashMap<>());
        }

        for (ExcelRowData row : cacheService.getExcelData()) {

            if (row.getOwningSite() == null || row.getCreationProfile() == null) continue;

            if (request.getOwningSite() != null && !request.getOwningSite().isEmpty()) {
                if (!row.getOwningSite().trim().equalsIgnoreCase(request.getOwningSite().trim())) continue;
            }

            String matchedProfile = profiles.stream()
                .filter(p -> p.trim().equalsIgnoreCase(row.getCreationProfile().trim()))
                .findFirst().orElse(null);
            if (matchedProfile == null) continue;

            String gmp = row.getGmpGroup() == null ? "(No Group)" : row.getGmpGroup().trim();
            if (request.getGmpGroups() != null && !request.getGmpGroups().isEmpty()) {
                final String prof = matchedProfile;
                boolean multi = request.getCreationProfiles() != null
                        && request.getCreationProfiles().size() > 1;
                boolean matches = request.getGmpGroups().stream().anyMatch(sel -> {
                    if (sel == null) return false;
                    String s = sel.trim();
                    if (multi) {
                        int p = s.lastIndexOf(" (");
                        if (p > 0 && s.endsWith(")")) {
                            String selGroup   = s.substring(0, p).trim();
                            String selProfile = s.substring(p + 2, s.length() - 1).trim();
                            return selGroup.equalsIgnoreCase(gmp) && selProfile.equalsIgnoreCase(prof);
                        }
                    }
                    return s.equalsIgnoreCase(gmp);
                });
                if (!matches) continue;
            }

            String subgroup = row.getGmpSubgroup() == null ? "(No Subgroup)" : row.getGmpSubgroup().trim();
            if (request.getGmpSubgroups() != null && !request.getGmpSubgroups().isEmpty()) {
                boolean matchesSub = request.getGmpSubgroups().stream()
                    .anyMatch(s -> s != null && s.trim().equalsIgnoreCase(subgroup));
                if (!matchesSub) continue;
            }

            String artifact = row.getGmpArtifact() == null ? "(No Artifact)" : row.getGmpArtifact().trim();
            if (request.getGmpArtifacts() != null && !request.getGmpArtifacts().isEmpty()) {
                boolean matchesArt = request.getGmpArtifacts().stream()
                    .anyMatch(a -> a != null && a.trim().equalsIgnoreCase(artifact));
                if (!matchesArt) continue;
            }

            String compKey = subgroup + "|||" + artifact;

            dataMap.get(matchedProfile).computeIfAbsent(gmp, k -> new HashMap<>());
            artifactDataMap.get(matchedProfile)
                .computeIfAbsent(gmp, k -> new HashMap<>())
                .computeIfAbsent(compKey, k -> new HashMap<>());
            yearlyArtifactMap.get(matchedProfile)
                .computeIfAbsent(gmp, k -> new HashMap<>());

            for (Integer year : years) {
                Map<String, Integer> typeMap = row.getYearTypeCounts() != null
                    ? row.getYearTypeCounts().getOrDefault(year, Collections.emptyMap())
                    : Collections.emptyMap();

                int created  = typeMap.getOrDefault(ExcelParserService.CREATED, 0);
                int migrated = typeMap.getOrDefault(ExcelParserService.MIGRATED, 0);
                int total    = typeMap.getOrDefault(ExcelParserService.TOTAL, created + migrated);

                Map<String, Integer> dest = dataMap.get(matchedProfile).get(gmp)
                    .computeIfAbsent(year, k -> new HashMap<>());
                dest.merge(ExcelParserService.CREATED,  created,  Integer::sum);
                dest.merge(ExcelParserService.MIGRATED, migrated, Integer::sum);
                dest.merge(ExcelParserService.TOTAL,    total,    Integer::sum);

                Map<String, Integer> artDest = artifactDataMap.get(matchedProfile).get(gmp).get(compKey);
                artDest.merge(ExcelParserService.CREATED,  created,  Integer::sum);
                artDest.merge(ExcelParserService.MIGRATED, migrated, Integer::sum);
                artDest.merge(ExcelParserService.TOTAL,    total,    Integer::sum);

                if (created > 0 || migrated > 0) {
                    Map<String, Map<String, Integer>> yTypeMap = yearlyArtifactMap.get(matchedProfile).get(gmp)
                        .computeIfAbsent(year, k -> new HashMap<>());
                    if (created > 0) {
                        yTypeMap.computeIfAbsent("CREATED", k -> new HashMap<>())
                            .merge(compKey, created, Integer::sum);
                    }
                    if (migrated > 0) {
                        yTypeMap.computeIfAbsent("MIGRATED", k -> new HashMap<>())
                            .merge(compKey, migrated, Integer::sum);
                    }
                }
            }
        }

        // ── Build one ProfileChart per profile ────────────────────
        List<ChartResponse.ProfileChart> charts = new ArrayList<>();

        for (String profile : profiles) {
            Map<String, Map<Integer, Map<String, Integer>>> gmpMap = dataMap.get(profile);
            Map<String, Map<Integer, Map<String, Map<String, Integer>>>> profYearArtMap = yearlyArtifactMap.get(profile);

            List<Map.Entry<String, Map<Integer, Map<String, Integer>>>> sortedGmps =
                gmpMap.entrySet().stream()
                    .sorted((a, b) -> {
                        int tA = a.getValue().values().stream()
                            .mapToInt(m -> m.getOrDefault(ExcelParserService.TOTAL, 0)).sum();
                        int tB = b.getValue().values().stream()
                            .mapToInt(m -> m.getOrDefault(ExcelParserService.TOTAL, 0)).sum();
                        return Integer.compare(tB, tA);
                    })
                    .collect(Collectors.toList());

            List<ChartResponse.Series> seriesList = new ArrayList<>();
            int profileTotal = 0;

            if ("BOTH".equals(docType)) {
                for (var entry : sortedGmps) {
                    String gmpName = entry.getKey();
                    Map<Integer, Map<String, Integer>> yearData = entry.getValue();

                    List<Integer> createdVals = years.stream()
                        .map(y -> yearData.getOrDefault(y, Collections.emptyMap())
                            .getOrDefault(ExcelParserService.CREATED, 0)).toList();
                    List<Integer> migratedVals = years.stream()
                        .map(y -> yearData.getOrDefault(y, Collections.emptyMap())
                            .getOrDefault(ExcelParserService.MIGRATED, 0)).toList();

                    int gmpCreated  = createdVals.stream().mapToInt(Integer::intValue).sum();
                    int gmpMigrated = migratedVals.stream().mapToInt(Integer::intValue).sum();
                    int gmpTotal    = gmpCreated + gmpMigrated;
                    if (gmpTotal == 0) continue;

                    profileTotal += gmpTotal;

                    // Build yearly artifact details for Created
                    if (gmpCreated > 0) {
                        Map<String, List<ChartResponse.ArtifactDetail>> createdYearlyArtifacts = new LinkedHashMap<>();
                        for (Integer y : years) {
                            Map<String, Integer> artMap = profYearArtMap.getOrDefault(gmpName, Collections.emptyMap())
                                .getOrDefault(y, Collections.emptyMap())
                                .getOrDefault("CREATED", Collections.emptyMap());
                            
                            List<ChartResponse.ArtifactDetail> details = new ArrayList<>();
                            artMap.forEach((compKey, cnt) -> {
                                if (cnt > 0) {
                                    String[] parts = compKey.split("\\|\\|\\|");
                                    String subName = parts[0];
                                    String artName = parts.length > 1 ? parts[1] : "";
                                    details.add(new ChartResponse.ArtifactDetail(subName, artName, cnt, 0, cnt));
                                }
                            });
                            details.sort((a, b) -> Integer.compare(b.getTotal(), a.getTotal()));
                            createdYearlyArtifacts.put(String.valueOf(y), details);
                        }

                        seriesList.add(new ChartResponse.Series(
                            gmpName + " — Created/Import",
                            createdVals,
                            "CREATED",
                            gmpName,
                            createdYearlyArtifacts
                        ));
                    }

                    // Build yearly artifact details for Migrated
                    if (gmpMigrated > 0) {
                        Map<String, List<ChartResponse.ArtifactDetail>> migratedYearlyArtifacts = new LinkedHashMap<>();
                        for (Integer y : years) {
                            Map<String, Integer> artMap = profYearArtMap.getOrDefault(gmpName, Collections.emptyMap())
                                .getOrDefault(y, Collections.emptyMap())
                                .getOrDefault("MIGRATED", Collections.emptyMap());
                            
                            List<ChartResponse.ArtifactDetail> details = new ArrayList<>();
                            artMap.forEach((compKey, cnt) -> {
                                if (cnt > 0) {
                                    String[] parts = compKey.split("\\|\\|\\|");
                                    String subName = parts[0];
                                    String artName = parts.length > 1 ? parts[1] : "";
                                    details.add(new ChartResponse.ArtifactDetail(subName, artName, 0, cnt, cnt));
                                }
                            });
                            details.sort((a, b) -> Integer.compare(b.getTotal(), a.getTotal()));
                            migratedYearlyArtifacts.put(String.valueOf(y), details);
                        }

                        seriesList.add(new ChartResponse.Series(
                            gmpName + " — Migrated",
                            migratedVals,
                            "MIGRATED",
                            gmpName,
                            migratedYearlyArtifacts
                        ));
                    }
                }
            } else {
                String typeKey = "CREATED".equals(docType)
                    ? ExcelParserService.CREATED : ExcelParserService.MIGRATED;

                for (var entry : sortedGmps) {
                    String gmpName = entry.getKey();
                    Map<Integer, Map<String, Integer>> yearData = entry.getValue();

                    List<Integer> vals = years.stream()
                        .map(y -> yearData.getOrDefault(y, Collections.emptyMap())
                            .getOrDefault(typeKey, 0)).toList();

                    int gmpTotal = vals.stream().mapToInt(Integer::intValue).sum();
                    if (gmpTotal == 0) continue;

                    profileTotal += gmpTotal;

                    Map<String, List<ChartResponse.ArtifactDetail>> yearlyArtifacts = new LinkedHashMap<>();
                    for (Integer y : years) {
                        Map<String, Integer> artMap = profYearArtMap.getOrDefault(gmpName, Collections.emptyMap())
                            .getOrDefault(y, Collections.emptyMap())
                            .getOrDefault(typeKey, Collections.emptyMap());
                        
                        List<ChartResponse.ArtifactDetail> details = new ArrayList<>();
                        artMap.forEach((compKey, cnt) -> {
                            if (cnt > 0) {
                                String[] parts = compKey.split("\\|\\|\\|");
                                String subName = parts[0];
                                String artName = parts.length > 1 ? parts[1] : "";
                                int cr = "CREATED".equals(docType) ? cnt : 0;
                                int mg = "MIGRATED".equals(docType) ? cnt : 0;
                                details.add(new ChartResponse.ArtifactDetail(subName, artName, cr, mg, cnt));
                            }
                        });
                        details.sort((a, b) -> Integer.compare(b.getTotal(), a.getTotal()));
                        yearlyArtifacts.put(String.valueOf(y), details);
                    }

                    seriesList.add(new ChartResponse.Series(
                        gmpName,
                        vals,
                        docType,
                        gmpName,
                        yearlyArtifacts
                    ));
                }
            }

            // ── Build GroupBreakdown list with child ArtifactDetails ──
            List<ChartResponse.GroupBreakdown> breakdowns = new ArrayList<>();
            Map<String, Map<String, Map<String, Integer>>> profArtMap = artifactDataMap.get(profile);

            for (var entry : sortedGmps) {
                String gmpName = entry.getKey();
                Map<String, Map<String, Integer>> artMap = profArtMap.getOrDefault(gmpName, Collections.emptyMap());

                List<ChartResponse.ArtifactDetail> artifactDetails = new ArrayList<>();
                int groupCr = 0, groupMg = 0, groupTot = 0;

                for (var artEntry : artMap.entrySet()) {
                    String compKey = artEntry.getKey();
                    Map<String, Integer> counts = artEntry.getValue();

                    String[] parts = compKey.split("\\|\\|\\|");
                    String subName = parts[0];
                    String artName = parts.length > 1 ? parts[1] : "";

                    int cr  = counts.getOrDefault(ExcelParserService.CREATED, 0);
                    int mg  = counts.getOrDefault(ExcelParserService.MIGRATED, 0);
                    int tot = counts.getOrDefault(ExcelParserService.TOTAL, cr + mg);

                    if (tot > 0) {
                        artifactDetails.add(new ChartResponse.ArtifactDetail(subName, artName, cr, mg, tot));
                        groupCr  += cr;
                        groupMg  += mg;
                        groupTot += tot;
                    }
                }

                if (groupTot > 0) {
                    artifactDetails.sort((a, b) -> Integer.compare(b.getTotal(), a.getTotal()));
                    breakdowns.add(new ChartResponse.GroupBreakdown(gmpName, groupCr, groupMg, groupTot, artifactDetails));
                }
            }

            ChartResponse.ProfileChart chart = new ChartResponse.ProfileChart();
            chart.setProfileName(profile);
            chart.setXAxis(xAxis);
            chart.setSeries(seriesList);
            chart.setTotalDocs(profileTotal);
            chart.setDocType(docType);
            chart.setGroupBreakdowns(breakdowns);
            charts.add(chart);
        }

        ChartResponse response = new ChartResponse();
        response.setRenderMode("perProfile");
        response.setCharts(charts);
        return response;
    }
}