package com.example.excelviz.model;

public class SiteGrowthData {
    private String siteName;
    private double baselineVolume;     // e.g., 2021
    private double previousYearVolume; // e.g., 2025
    private double currentYearVolume;  // e.g., 2026
    private double yoyGrowth;
    private double cagr;

    public SiteGrowthData() {}

    public SiteGrowthData(String siteName, double baselineVolume, double previousYearVolume, double currentYearVolume) {
        this.siteName = siteName;
        this.baselineVolume = baselineVolume;
        this.previousYearVolume = previousYearVolume;
        this.currentYearVolume = currentYearVolume;
    }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public double getBaselineVolume() { return baselineVolume; }
    public void setBaselineVolume(double baselineVolume) { this.baselineVolume = baselineVolume; }

    public double getPreviousYearVolume() { return previousYearVolume; }
    public void setPreviousYearVolume(double previousYearVolume) { this.previousYearVolume = previousYearVolume; }

    public double getCurrentYearVolume() { return currentYearVolume; }
    public void setCurrentYearVolume(double currentYearVolume) { this.currentYearVolume = currentYearVolume; }

    public double getYoyGrowth() { return yoyGrowth; }
    public void setYoyGrowth(double yoyGrowth) { this.yoyGrowth = yoyGrowth; }

    public double getCagr() { return cagr; }
    public void setCagr(double cagr) { this.cagr = cagr; }
}