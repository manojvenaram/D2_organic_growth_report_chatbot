package com.example.excelviz.model;

import java.util.Collections;
import java.util.Map;

/**
 * Represents one data row parsed from the Excel sheet.
 * yearTypeCounts: year → { "CREATED" | "MIGRATED" | "TOTAL" → count }
 */
public class ExcelRowData {

    private String owningSite;
    private String creationProfile;
    private String gmpGroup;
    private String gmpSubgroup;
    private String gmpArtifact;
    private Map<Integer, Map<String, Integer>> yearTypeCounts;

    // ── Getters / Setters ─────────────────────────────────────────

    public String getOwningSite()                                       { return owningSite; }
    public void   setOwningSite(String v)                               { this.owningSite = v; }

    public String getCreationProfile()                                  { return creationProfile; }
    public void   setCreationProfile(String v)                          { this.creationProfile = v; }

    public String getGmpGroup()                                         { return gmpGroup; }
    public void   setGmpGroup(String v)                                 { this.gmpGroup = v; }

    public String getGmpSubgroup()                                      { return gmpSubgroup; }
    public void   setGmpSubgroup(String v)                              { this.gmpSubgroup = v; }

    public String getGmpArtifact()                                      { return gmpArtifact; }
    public void   setGmpArtifact(String v)                              { this.gmpArtifact = v; }

    public Map<Integer, Map<String, Integer>> getYearTypeCounts()       { return yearTypeCounts; }
    public void setYearTypeCounts(Map<Integer, Map<String, Integer>> v) { this.yearTypeCounts = v; }

    /**
     * Convenience method — get count for a specific year + docType key.
     * Returns 0 if not found.
     */
    public int getCount(int year, String docType) {
        if (yearTypeCounts == null) return 0;
        Map<String, Integer> typeMap = yearTypeCounts.get(year);
        if (typeMap == null) return 0;
        return typeMap.getOrDefault(docType, 0);
    }
}
