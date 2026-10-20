package com.example.excelviz.service;

import com.example.excelviz.model.ExcelRowData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides metadata for populating the UI filter dropdowns.
 */
@Service
public class MetadataService {

    @Autowired
    private ExcelCacheService cacheService;

    public Set<String> getOwningSites() {
        return cacheService.getOwningSites();
    }

    public Set<String> getCreationProfilesBySite(String site) {
        return cacheService.getExcelData().stream()
                .filter(r -> r.getOwningSite() != null
                          && r.getOwningSite().equalsIgnoreCase(site.trim()))
                .map(ExcelRowData::getCreationProfile)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toCollection(() -> new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }

    public Set<String> getGmpGroupsBySiteAndProfile(String site, String profile) {
        return cacheService.getExcelData().stream()
                .filter(r -> r.getOwningSite()       != null && r.getOwningSite().equalsIgnoreCase(site.trim()))
                .filter(r -> r.getCreationProfile()  != null && r.getCreationProfile().equalsIgnoreCase(profile.trim()))
                .map(ExcelRowData::getGmpGroup)
                .filter(g -> g != null && !g.isBlank())
                .collect(Collectors.toCollection(() -> new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }

    public Map<String, Set<String>> getGmpSubgroupsGroupedByGmpGroup(String site, List<String> profiles, List<String> gmpGroups) {
        Map<String, Set<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (ExcelRowData r : cacheService.getExcelData()) {
            if (r.getOwningSite() == null || !r.getOwningSite().equalsIgnoreCase(site.trim())) continue;

            if (profiles != null && !profiles.isEmpty()) {
                if (r.getCreationProfile() == null || profiles.stream().noneMatch(p -> p.trim().equalsIgnoreCase(r.getCreationProfile().trim()))) {
                    continue;
                }
            }

            String gmp = r.getGmpGroup() != null ? r.getGmpGroup().trim() : "";
            if (gmp.isEmpty()) continue;

            if (gmpGroups != null && !gmpGroups.isEmpty()) {
                boolean matches = matchesFilterItem(gmp, r.getCreationProfile(), gmpGroups);
                if (!matches) continue;
            }

            String subgroup = r.getGmpSubgroup() != null ? r.getGmpSubgroup().trim() : "";
            if (!subgroup.isEmpty()) {
                map.computeIfAbsent(gmp, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(subgroup);
            }
        }

        return map;
    }

    public Map<String, Set<String>> getGmpArtifactsGrouped(String site, List<String> profiles, List<String> gmpGroups, List<String> gmpSubgroups) {
        Map<String, Set<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (ExcelRowData r : cacheService.getExcelData()) {
            if (r.getOwningSite() == null || !r.getOwningSite().equalsIgnoreCase(site.trim())) continue;

            if (profiles != null && !profiles.isEmpty()) {
                if (r.getCreationProfile() == null || profiles.stream().noneMatch(p -> p.trim().equalsIgnoreCase(r.getCreationProfile().trim()))) {
                    continue;
                }
            }

            String gmp = r.getGmpGroup() != null ? r.getGmpGroup().trim() : "";
            if (gmp.isEmpty()) continue;

            if (gmpGroups != null && !gmpGroups.isEmpty()) {
                boolean matches = matchesFilterItem(gmp, r.getCreationProfile(), gmpGroups);
                if (!matches) continue;
            }

            String sub = r.getGmpSubgroup() != null ? r.getGmpSubgroup().trim() : "";
            if (gmpSubgroups != null && !gmpSubgroups.isEmpty()) {
                boolean matchesSub = gmpSubgroups.stream().anyMatch(s -> s != null && s.trim().equalsIgnoreCase(sub));
                if (!matchesSub) continue;
            }

            String parentGroupKey = !sub.isEmpty() ? sub : gmp;
            String artifact = r.getGmpArtifact() != null ? r.getGmpArtifact().trim() : "";
            if (!artifact.isEmpty()) {
                map.computeIfAbsent(parentGroupKey, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(artifact);
            }
        }

        return map;
    }

    private boolean matchesFilterItem(String value, String creationProfile, List<String> selectedFilterItems) {
        if (selectedFilterItems == null || selectedFilterItems.isEmpty()) return true;
        String val = value != null ? value.trim() : "";
        String prof = creationProfile != null ? creationProfile.trim() : "";

        return selectedFilterItems.stream().anyMatch(sel -> {
            if (sel == null) return false;
            String s = sel.trim();
            int p = s.lastIndexOf(" (");
            if (p > 0 && s.endsWith(")")) {
                String selVal     = s.substring(0, p).trim();
                String selProfile = s.substring(p + 2, s.length() - 1).trim();
                return selVal.equalsIgnoreCase(val) && selProfile.equalsIgnoreCase(prof);
            }
            return s.equalsIgnoreCase(val);
        });
    }

    public Set<Integer> getAvailableYears() {
        return cacheService.getAvailableYears();
    }
}
