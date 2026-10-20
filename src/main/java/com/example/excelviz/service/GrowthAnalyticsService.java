package com.example.excelviz.service;

import com.example.excelviz.model.ExcelRowData;
import com.example.excelviz.model.ExecutiveSummaryResponse;
import com.example.excelviz.model.SiteGrowthData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GrowthAnalyticsService {

    @Autowired
    private ExcelCacheService cacheService;

    public ExecutiveSummaryResponse getExecutiveSummary(String docType) {
        String dt = docType != null ? docType.toUpperCase() : "CREATED";

        List<Integer> years = cacheService.getAvailableYears().stream().sorted().toList();
        if (years.isEmpty()) {
            ExecutiveSummaryResponse empty = new ExecutiveSummaryResponse();
            empty.setDocType(dt);
            empty.setYearlyPoints(Collections.emptyList());
            empty.setSiteRows(Collections.emptyList());
            empty.setBulletInsights(Collections.emptyList());
            return empty;
        }

        int baselineYear = years.get(0);
        int currentYear = years.get(years.size() - 1);

        // Map: Site -> Year -> Count
        Map<String, Map<Integer, Integer>> siteYearlyMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (ExcelRowData row : cacheService.getExcelData()) {
            String site = row.getOwningSite();
            if (site == null || site.isBlank()) continue;
            site = site.trim();

            Map<Integer, Integer> yMap = siteYearlyMap.computeIfAbsent(site, k -> new HashMap<>());

            for (Integer y : years) {
                Map<String, Integer> counts = row.getYearTypeCounts() != null ? row.getYearTypeCounts().get(y) : null;
                if (counts == null) continue;

                int c = counts.getOrDefault(ExcelParserService.CREATED, 0);
                int m = counts.getOrDefault(ExcelParserService.MIGRATED, 0);
                int val = "BOTH".equals(dt) ? (c + m) : ("MIGRATED".equals(dt) ? m : c);

                yMap.merge(y, val, Integer::sum);
            }
        }

        // Exclude divested / inactive sites: A site is active if (volume in last 2 years) > 0
        int lastYr = currentYear;
        int prevYr = years.size() >= 2 ? years.get(years.size() - 2) : currentYear;

        List<ExecutiveSummaryResponse.SiteOverviewRow> activeSiteRows = new ArrayList<>();
        int[] portfolioYearlyTotals = new int[years.size()];

        for (Map.Entry<String, Map<Integer, Integer>> entry : siteYearlyMap.entrySet()) {
            String siteName = entry.getKey();
            Map<Integer, Integer> yMap = entry.getValue();

            int recentVol = yMap.getOrDefault(lastYr, 0) + yMap.getOrDefault(prevYr, 0);
            if (recentVol <= 0) {
                // Divested or inactive site - excluded from summary
                continue;
            }

            List<Integer> vols = new ArrayList<>();
            for (int i = 0; i < years.size(); i++) {
                int v = yMap.getOrDefault(years.get(i), 0);
                vols.add(v);
                portfolioYearlyTotals[i] += v;
            }

            int baseV = vols.get(0);
            int currV = vols.get(vols.size() - 1);
            double growth = baseV > 0 ? Math.round(((double)(currV - baseV) / baseV * 100.0) * 10.0) / 10.0 : 0.0;
            double cagr = (baseV > 0 && currV > 0 && years.size() > 1)
                    ? Math.round((Math.pow((double) currV / baseV, 1.0 / (years.size() - 1)) - 1.0) * 1000.0) / 10.0
                    : 0.0;

            activeSiteRows.add(new ExecutiveSummaryResponse.SiteOverviewRow(siteName, vols, growth, cagr));
        }

        activeSiteRows.sort((a, b) -> Integer.compare(
                b.getYearlyVolumes().get(b.getYearlyVolumes().size() - 1),
                a.getYearlyVolumes().get(a.getYearlyVolumes().size() - 1)
        ));

        // Portfolio totals across active sites
        List<Integer> totalVols = new ArrayList<>();
        List<ExecutiveSummaryResponse.YearlyGrowthPoint> points = new ArrayList<>();

        for (int i = 0; i < years.size(); i++) {
            int y = years.get(i);
            int vol = portfolioYearlyTotals[i];
            totalVols.add(vol);

            Double yoyPct = null;
            if (i > 0) {
                int prev = portfolioYearlyTotals[i - 1];
                if (prev > 0) {
                    yoyPct = Math.round(((double)(vol - prev) / prev * 100.0) * 10.0) / 10.0;
                } else {
                    yoyPct = 0.0;
                }
            }
            points.add(new ExecutiveSummaryResponse.YearlyGrowthPoint(y, vol, yoyPct));
        }

        int portBase = totalVols.isEmpty() ? 0 : totalVols.get(0);
        int portCurr = totalVols.isEmpty() ? 0 : totalVols.get(totalVols.size() - 1);
        int portPrev = totalVols.size() >= 2 ? totalVols.get(totalVols.size() - 2) : portCurr;

        double overallGrowth = portBase > 0 ? Math.round(((double)(portCurr - portBase) / portBase * 100.0) * 10.0) / 10.0 : 0.0;
        double latestYoy = portPrev > 0 ? Math.round(((double)(portCurr - portPrev) / portPrev * 100.0) * 10.0) / 10.0 : 0.0;
        double overallCagr = (portBase > 0 && portCurr > 0 && years.size() > 1)
                ? Math.round((Math.pow((double) portCurr / portBase, 1.0 / (years.size() - 1)) - 1.0) * 1000.0) / 10.0
                : 0.0;

        ExecutiveSummaryResponse.SiteOverviewRow totalRow = new ExecutiveSummaryResponse.SiteOverviewRow(
                "Total (All Active Sites)", totalVols, overallGrowth, overallCagr
        );

        ExecutiveSummaryResponse resp = new ExecutiveSummaryResponse();
        resp.setBaselineYear(baselineYear);
        resp.setBaselineVolume(portBase);
        resp.setCurrentYear(currentYear);
        resp.setCurrentVolume(portCurr);
        resp.setOverallGrowthPct(overallGrowth);
        resp.setLatestYoyGrowthPct(latestYoy);
        resp.setDocType(dt);
        resp.setYearlyPoints(points);
        resp.setSiteRows(activeSiteRows);
        resp.setTotalRow(totalRow);

        String growthSign = overallGrowth >= 0 ? "+" : "";
        resp.setHeadline("Document volume increased " + growthSign + overallGrowth + "% from " + baselineYear + " to " + currentYear + ".");
        resp.setSubheadline("Growth reflects strong organic Documentum D2 adoption, driven by plant-level digital manufacturing lifecycles.");

        List<String> bullets = new ArrayList<>();
        bullets.add("Steady compound organic growth (" + (overallCagr >= 0 ? "+" : "") + overallCagr + "% 5-year CAGR) across active manufacturing facilities.");
        if (!points.isEmpty()) {
            ExecutiveSummaryResponse.YearlyGrowthPoint maxPeak = points.stream()
                    .filter(p -> p.getYoyGrowthPct() != null)
                    .max(Comparator.comparingDouble(ExecutiveSummaryResponse.YearlyGrowthPoint::getYoyGrowthPct))
                    .orElse(null);
            if (maxPeak != null && maxPeak.getYoyGrowthPct() != null) {
                bullets.add("Highest acceleration of +" + maxPeak.getYoyGrowthPct() + "% YoY in " + maxPeak.getYear() + ", marking major D2 manufacturing deployment.");
            }
        }
        bullets.add("Latest volume establishes a high-throughput operational steady state across packaging and specification lifecycles.");
        bullets.add("Excludes divested sites to ensure true operational trend integrity.");
        resp.setBulletInsights(bullets);

        return resp;
    }

    public List<SiteGrowthData> calculateOrganicGrowth(List<SiteGrowthData> rawSiteData) {
        return rawSiteData.stream()
                .filter(this::isActiveSite)
                .map(this::calculateMetrics)
                .collect(Collectors.toList());
    }

    private boolean isActiveSite(SiteGrowthData data) {
        return (data.getPreviousYearVolume() + data.getCurrentYearVolume()) > 0;
    }

    private SiteGrowthData calculateMetrics(SiteGrowthData data) {
        if (data.getPreviousYearVolume() > 0) {
            double yoy = ((data.getCurrentYearVolume() - data.getPreviousYearVolume()) / data.getPreviousYearVolume()) * 100;
            data.setYoyGrowth(Math.round(yoy * 10.0) / 10.0);
        } else {
            data.setYoyGrowth(0.0);
        }

        if (data.getBaselineVolume() > 0) {
            double cagr = (Math.pow((data.getCurrentYearVolume() / data.getBaselineVolume()), (1.0 / 5.0)) - 1.0) * 100;
            data.setCagr(Math.round(cagr * 10.0) / 10.0);
        } else {
            data.setCagr(0.0);
        }

        return data;
    }
}