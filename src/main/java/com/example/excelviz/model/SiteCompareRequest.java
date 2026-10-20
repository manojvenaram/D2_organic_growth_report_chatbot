package com.example.excelviz.model;

import java.util.List;

/**
 * Request payload for POST /api/compare/sites
 */
public class SiteCompareRequest {

    private List<String>  sites;
    private List<Integer> years;
    private String        docType;   // CREATED | MIGRATED | BOTH (default BOTH)

    public List<String>  getSites()              { return sites; }
    public void          setSites(List<String> v) { this.sites = v; }

    public List<Integer> getYears()              { return years; }
    public void          setYears(List<Integer> v){ this.years = v; }

    public String        getDocType()             { return docType; }
    public void          setDocType(String v)     { this.docType = v; }
}