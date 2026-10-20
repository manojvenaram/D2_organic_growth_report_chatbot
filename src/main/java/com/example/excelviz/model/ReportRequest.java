package com.example.excelviz.model;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Payload for POST /api/report/generate
 * Frontend sends chart images (base64) + current filter state + theme
 */
public class ReportRequest {

    private String       owningSite;
    private List<String> creationProfiles;
    private String       docType;     // CREATED | MIGRATED | BOTH
    private int          yearFrom;
    private int          yearTo;
    private String       chartType;   // BAR | LINE | STACKED
    private String       theme;       // "DARK" | "LIGHT"

    private List<ChartImage>     chartImages;
    private List<ProfileSummary> profiles;
    private CompareSummary       compareSummary;

    // ── Getters / Setters ──────────────────────────────────────────
    public String            getOwningSite()                        { return owningSite; }
    public void              setOwningSite(String v)                { this.owningSite = v; }
    public List<String>      getCreationProfiles()                  { return creationProfiles; }
    public void              setCreationProfiles(List<String> v)    { this.creationProfiles = v; }
    public String            getDocType()                           { return docType; }
    public void              setDocType(String v)                   { this.docType = v; }
    public int               getYearFrom()                          { return yearFrom; }
    public void              setYearFrom(int v)                     { this.yearFrom = v; }
    public int               getYearTo()                            { return yearTo; }
    public void              setYearTo(int v)                       { this.yearTo = v; }
    public String            getChartType()                         { return chartType; }
    public void              setChartType(String v)                 { this.chartType = v; }
    public String            getTheme()                             { return theme; }
    public void              setTheme(String v)                     { this.theme = v; }
    public List<ChartImage>  getChartImages()                       { return chartImages; }
    public void              setChartImages(List<ChartImage> v)     { this.chartImages = v; }
    public List<ProfileSummary> getProfiles()                       { return profiles; }
    public void              setProfiles(List<ProfileSummary> v)    { this.profiles = v; }
    public CompareSummary    getCompareSummary()                    { return compareSummary; }
    public void              setCompareSummary(CompareSummary v)    { this.compareSummary = v; }

    // ── Inner classes ──────────────────────────────────────────────
    public static class ChartImage {
        private String profileName;
        private String base64Image;
        public String getProfileName()         { return profileName; }
        public void   setProfileName(String v) { this.profileName = v; }
        public String getBase64Image()         { return base64Image; }
        public void   setBase64Image(String v) { this.base64Image = v; }
    }

    public static class ProfileSummary {
        private String       profileName;
        private int          totalDocs;
        private List<YearData> yearData;   // yearly totals for trend analysis
        private List<GmpRow> gmpRows;

        public String         getProfileName()            { return profileName; }
        public void           setProfileName(String v)    { this.profileName = v; }
        public int            getTotalDocs()              { return totalDocs; }
        public void           setTotalDocs(int v)         { this.totalDocs = v; }
        public List<YearData> getYearData()               { return yearData; }
        public void           setYearData(List<YearData> v){ this.yearData = v; }
        public List<GmpRow>   getGmpRows()                { return gmpRows; }
        public void           setGmpRows(List<GmpRow> v)  { this.gmpRows = v; }
    }

    // Per-year totals for observation generation
    public static class YearData {
        private int    year;
        private int    total;
        private int    created;
        private int    migrated;

        public int  getYear()           { return year; }
        public void setYear(int v)      { this.year = v; }
        public int  getTotal()          { return total; }
        public void setTotal(int v)     { this.total = v; }
        public int  getCreated()        { return created; }
        public void setCreated(int v)   { this.created = v; }
        public int  getMigrated()       { return migrated; }
        public void setMigrated(int v)  { this.migrated = v; }
    }

    public static class GmpRow {
        private String gmpGroup;
        private int    total;
        private int    created;
        private int    migrated;
        private List<ArtifactRow> artifacts; // <--- HIERARCHY FIELD

        public String getGmpGroup()         { return gmpGroup; }
        public void   setGmpGroup(String v) { this.gmpGroup = v; }
        public int    getTotal()            { return total; }
        public void   setTotal(int v)       { this.total = v; }
        public int    getCreated()          { return created; }
        public void   setCreated(int v)     { this.created = v; }
        public int    getMigrated()         { return migrated; }
        public void   setMigrated(int v)    { this.migrated = v; }
        public List<ArtifactRow> getArtifacts() { return artifacts; }
        public void   setArtifacts(List<ArtifactRow> v) { this.artifacts = v; }
    }

    public static class ArtifactRow {
        private String subgroup;
        private String artifactName;
        private int    total;
        private int    created;
        private int    migrated;

        // Maps to hold exact counts per year
        private Map<Integer, Integer> yearlyCreated = new HashMap<>();
        private Map<Integer, Integer> yearlyMigrated = new HashMap<>();

        public String getSubgroup()         { return subgroup; }
        public void   setSubgroup(String v) { this.subgroup = v; }
        public String getArtifactName()     { return artifactName; }
        public void   setArtifactName(String v){ this.artifactName = v; }
        public int    getTotal()            { return total; }
        public void   setTotal(int v)       { this.total = v; }
        public int    getCreated()          { return created; }
        public void   setCreated(int v)     { this.created = v; }
        public int    getMigrated()         { return migrated; }
        public void   setMigrated(int v)    { this.migrated = v; }

        public Map<Integer, Integer> getYearlyCreated() { return yearlyCreated; }
        public void setYearlyCreated(Map<Integer, Integer> v) { this.yearlyCreated = v; }

        public Map<Integer, Integer> getYearlyMigrated() { return yearlyMigrated; }
        public void setYearlyMigrated(Map<Integer, Integer> v) { this.yearlyMigrated = v; }
    }

    public static class CompareSummary {
        private List<String> sites;
        private int          overallTotal;
        private String       topSite;
        private int          topSiteDocs;
        private String       yearRange;
        private String       compareImageBase64;

        public List<String> getSites()                     { return sites; }
        public void         setSites(List<String> v)       { this.sites = v; }
        public int          getOverallTotal()              { return overallTotal; }
        public void         setOverallTotal(int v)         { this.overallTotal = v; }
        public String       getTopSite()                   { return topSite; }
        public void         setTopSite(String v)           { this.topSite = v; }
        public int          getTopSiteDocs()               { return topSiteDocs; }
        public void         setTopSiteDocs(int v)          { this.topSiteDocs = v; }
        public String       getYearRange()                 { return yearRange; }
        public void         setYearRange(String v)         { this.yearRange = v; }
        public String       getCompareImageBase64()        { return compareImageBase64; }
        public void         setCompareImageBase64(String v){ this.compareImageBase64 = v; }
    }
}