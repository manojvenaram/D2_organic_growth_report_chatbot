package com.example.excelviz.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Response payload for POST /api/charts/data.
 * Always renderMode = "perProfile" — one ProfileChart per creation profile.
 */
public class ChartResponse {

    private String             renderMode = "perProfile";
    private List<ProfileChart> charts;

    public String             getRenderMode()               { return renderMode; }
    public void               setRenderMode(String v)       { this.renderMode = v; }

    public List<ProfileChart> getCharts()                   { return charts; }
    public void               setCharts(List<ProfileChart> v){ this.charts = v; }

    // ── One chart card per creation profile ──────────────────────
    public static class ProfileChart {

        private String               profileName;
        @JsonProperty("xAxis")
        private List<String>         xAxis;        // year labels ["2021","2022"…]
        private List<Series>         series;
        private int                  totalDocs;
        private String               docType;      // CREATED | MIGRATED | BOTH
        private List<GroupBreakdown> groupBreakdowns;

        public String               getProfileName()                { return profileName; }
        public void                 setProfileName(String v)        { this.profileName = v; }

        public List<String>         getXAxis()                      { return xAxis; }
        public void                 setXAxis(List<String> v)        { this.xAxis = v; }

        public List<Series>         getSeries()                     { return series; }
        public void                 setSeries(List<Series> v)       { this.series = v; }

        public int                  getTotalDocs()                  { return totalDocs; }
        public void                 setTotalDocs(int v)             { this.totalDocs = v; }

        public String               getDocType()                    { return docType; }
        public void                 setDocType(String v)            { this.docType = v; }

        public List<GroupBreakdown> getGroupBreakdowns()            { return groupBreakdowns; }
        public void                 setGroupBreakdowns(List<GroupBreakdown> v) { this.groupBreakdowns = v; }
    }

    // ── Group Breakdown with Child Artifacts for Accordion ───────
    public static class GroupBreakdown {
        private String               gmpGroup;
        private int                  created;
        private int                  migrated;
        private int                  total;
        private List<ArtifactDetail> artifacts;

        public GroupBreakdown() {}

        public GroupBreakdown(String gmpGroup, int created, int migrated, int total, List<ArtifactDetail> artifacts) {
            this.gmpGroup  = gmpGroup;
            this.created   = created;
            this.migrated  = migrated;
            this.total     = total;
            this.artifacts = artifacts;
        }

        public String               getGmpGroup()             { return gmpGroup; }
        public void                 setGmpGroup(String v)     { this.gmpGroup = v; }

        public int                  getCreated()              { return created; }
        public void                 setCreated(int v)         { this.created = v; }

        public int                  getMigrated()             { return migrated; }
        public void                 setMigrated(int v)        { this.migrated = v; }

        public int                  getTotal()                { return total; }
        public void                 setTotal(int v)           { this.total = v; }

        public List<ArtifactDetail> getArtifacts()            { return artifacts; }
        public void                 setArtifacts(List<ArtifactDetail> v) { this.artifacts = v; }
    }

    // ── Artifact Details per Parent Group ────────────────────────
    public static class ArtifactDetail {
        private String gmpSubgroup;
        private String artifactName;
        private int    created;
        private int    migrated;
        private int    total;

        public ArtifactDetail() {}

        public ArtifactDetail(String gmpSubgroup, String artifactName, int created, int migrated, int total) {
            this.gmpSubgroup  = gmpSubgroup;
            this.artifactName = artifactName;
            this.created      = created;
            this.migrated     = migrated;
            this.total        = total;
        }

        public String getGmpSubgroup()         { return gmpSubgroup; }
        public void   setGmpSubgroup(String v) { this.gmpSubgroup = v; }

        public String getArtifactName()        { return artifactName; }
        public void   setArtifactName(String v){ this.artifactName = v; }

        public int    getCreated()             { return created; }
        public void   setCreated(int v)        { this.created = v; }

        public int    getMigrated()            { return migrated; }
        public void   setMigrated(int v)       { this.migrated = v; }

        public int    getTotal()               { return total; }
        public void   setTotal(int v)          { this.total = v; }
    }

    // ── One series = one GMP group (or one docType sub-series) ───
    public static class Series {

        private String                             name;
        private List<Integer>                      data;
        private String                             seriesType;  // CREATED | MIGRATED | TOTAL
        private String                             gmpGroup;    // base GMP group name
        private Map<String, List<ArtifactDetail>> yearlyArtifacts; // "2021" -> list of artifacts for hover tooltip

        public Series() {}

        public Series(String name, List<Integer> data, String seriesType, String gmpGroup, Map<String, List<ArtifactDetail>> yearlyArtifacts) {
            this.name            = name;
            this.data            = data;
            this.seriesType      = seriesType;
            this.gmpGroup        = gmpGroup;
            this.yearlyArtifacts = yearlyArtifacts;
        }

        public String                             getName()            { return name; }
        public List<Integer>                      getData()            { return data; }
        public String                             getSeriesType()      { return seriesType; }
        public String                             getGmpGroup()        { return gmpGroup; }
        public Map<String, List<ArtifactDetail>>  getYearlyArtifacts() { return yearlyArtifacts; }
        public void setYearlyArtifacts(Map<String, List<ArtifactDetail>> v) { this.yearlyArtifacts = v; }
    }
}