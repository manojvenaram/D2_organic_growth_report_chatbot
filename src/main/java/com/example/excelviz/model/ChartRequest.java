package com.example.excelviz.model;

import java.util.List;

/**
 * Request payload for POST /api/charts/data
 */
public class ChartRequest {

    private String        owningSite;
    private List<String>  creationProfiles;
    private List<String>  gmpGroups;        // empty = all groups
    private List<String>  gmpSubgroups;     // empty = all subgroups
    private List<String>  gmpArtifacts;     // empty = all artifacts
    private List<Integer> years;
    private String        chartType;        // BAR | LINE | STACKED
    private String        docType;          // CREATED | MIGRATED | BOTH
    private String        stackMode;        // LIFECYCLE | CATEGORY

    public String        getOwningSite()                   { return owningSite; }
    public void          setOwningSite(String v)            { this.owningSite = v; }

    public List<String>  getCreationProfiles()              { return creationProfiles; }
    public void          setCreationProfiles(List<String> v){ this.creationProfiles = v; }

    public List<String>  getGmpGroups()                     { return gmpGroups; }
    public void          setGmpGroups(List<String> v)       { this.gmpGroups = v; }

    public List<String>  getGmpSubgroups()                  { return gmpSubgroups; }
    public void          setGmpSubgroups(List<String> v)    { this.gmpSubgroups = v; }

    public List<String>  getGmpArtifacts()                  { return gmpArtifacts; }
    public void          setGmpArtifacts(List<String> v)    { this.gmpArtifacts = v; }

    public List<Integer> getYears()                         { return years; }
    public void          setYears(List<Integer> v)          { this.years = v; }

    public String        getChartType()                     { return chartType; }
    public void          setChartType(String v)             { this.chartType = v; }

    public String        getDocType()                       { return docType; }
    public void          setDocType(String v)               { this.docType = v; }

    public String        getStackMode()                     { return stackMode; }
    public void          setStackMode(String v)             { this.stackMode = v; }
}
