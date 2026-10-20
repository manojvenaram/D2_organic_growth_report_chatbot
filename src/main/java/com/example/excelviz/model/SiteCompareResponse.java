package com.example.excelviz.model;

import java.util.List;

/**
 * Response payload for POST /api/compare/sites
 */
public class SiteCompareResponse {

    private List<String>      sites;
    private List<Integer>     years;
    private List<ProfileRow>  profiles;
    private List<Integer>     grandTotals;
    private int               overallTotal;
    private String            docType;

    public List<String>     getSites()                  { return sites; }
    public void             setSites(List<String> v)    { this.sites = v; }

    public List<Integer>    getYears()                  { return years; }
    public void             setYears(List<Integer> v)   { this.years = v; }

    public List<ProfileRow> getProfiles()               { return profiles; }
    public void             setProfiles(List<ProfileRow> v){ this.profiles = v; }

    public List<Integer>    getGrandTotals()            { return grandTotals; }
    public void             setGrandTotals(List<Integer> v){ this.grandTotals = v; }

    public int              getOverallTotal()            { return overallTotal; }
    public void             setOverallTotal(int v)       { this.overallTotal = v; }

    public String           getDocType()                { return docType; }
    public void             setDocType(String v)        { this.docType = v; }

    // ── One row per Creation Profile ─────────────────────────────
    public static class ProfileRow {

        private String        profile;
        private List<Integer> siteTotals;
        private int           rowTotal;
        private List<GmpRow>  gmpRows;

        public String        getProfile()                  { return profile; }
        public void          setProfile(String v)          { this.profile = v; }

        public List<Integer> getSiteTotals()               { return siteTotals; }
        public void          setSiteTotals(List<Integer> v){ this.siteTotals = v; }

        public int           getRowTotal()                 { return rowTotal; }
        public void          setRowTotal(int v)            { this.rowTotal = v; }

        public List<GmpRow>  getGmpRows()                  { return gmpRows; }
        public void          setGmpRows(List<GmpRow> v)    { this.gmpRows = v; }
    }

    // ── One row per GMP Group inside a Profile ────────────────────
    public static class GmpRow {

        private String        gmpGroup;
        private List<Integer> siteTotals;
        private int           rowTotal;

        public String        getGmpGroup()                 { return gmpGroup; }
        public void          setGmpGroup(String v)         { this.gmpGroup = v; }

        public List<Integer> getSiteTotals()               { return siteTotals; }
        public void          setSiteTotals(List<Integer> v){ this.siteTotals = v; }

        public int           getRowTotal()                 { return rowTotal; }
        public void          setRowTotal(int v)            { this.rowTotal = v; }
    }
}