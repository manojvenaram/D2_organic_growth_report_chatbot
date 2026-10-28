package com.example.excelviz.model;

import java.util.List;

public class ExecutiveSummaryResponse {
    private int baselineYear;
    private int baselineVolume;
    private int currentYear;
    private int currentVolume;
    private double overallGrowthPct;
    private double latestYoyGrowthPct;
    private String docType;
    private List<YearlyGrowthPoint> yearlyPoints;
    private List<SiteOverviewRow> siteRows;
    private SiteOverviewRow totalRow;
    private String headline;
    private String subheadline;
    private List<String> bulletInsights;

    public int getBaselineYear() { return baselineYear; }
    public void setBaselineYear(int baselineYear) { this.baselineYear = baselineYear; }

    public int getBaselineVolume() { return baselineVolume; }
    public void setBaselineVolume(int baselineVolume) { this.baselineVolume = baselineVolume; }

    public int getCurrentYear() { return currentYear; }
    public void setCurrentYear(int currentYear) { this.currentYear = currentYear; }

    public int getCurrentVolume() { return currentVolume; }
    public void setCurrentVolume(int currentVolume) { this.currentVolume = currentVolume; }

    public double getOverallGrowthPct() { return overallGrowthPct; }
    public void setOverallGrowthPct(double overallGrowthPct) { this.overallGrowthPct = overallGrowthPct; }

    public double getLatestYoyGrowthPct() { return latestYoyGrowthPct; }
    public void setLatestYoyGrowthPct(double latestYoyGrowthPct) { this.latestYoyGrowthPct = latestYoyGrowthPct; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public List<YearlyGrowthPoint> getYearlyPoints() { return yearlyPoints; }
    public void setYearlyPoints(List<YearlyGrowthPoint> yearlyPoints) { this.yearlyPoints = yearlyPoints; }

    public List<SiteOverviewRow> getSiteRows() { return siteRows; }
    public void setSiteRows(List<SiteOverviewRow> siteRows) { this.siteRows = siteRows; }

    public SiteOverviewRow getTotalRow() { return totalRow; }
    public void setTotalRow(SiteOverviewRow totalRow) { this.totalRow = totalRow; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getSubheadline() { return subheadline; }
    public void setSubheadline(String subheadline) { this.subheadline = subheadline; }

    public List<String> getBulletInsights() { return bulletInsights; }
    public void setBulletInsights(List<String> bulletInsights) { this.bulletInsights = bulletInsights; }

    public double getCagrPct() { return totalRow != null ? totalRow.getCagrPct() : 0.0; }

    public static class YearlyGrowthPoint {
        private int year;
        private int volume;
        private Double yoyGrowthPct; // null for first year

        public YearlyGrowthPoint() {}
        public YearlyGrowthPoint(int year, int volume, Double yoyGrowthPct) {
            this.year = year;
            this.volume = volume;
            this.yoyGrowthPct = yoyGrowthPct;
        }

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }

        public int getVolume() { return volume; }
        public void setVolume(int volume) { this.volume = volume; }

        public Double getYoyGrowthPct() { return yoyGrowthPct; }
        public void setYoyGrowthPct(Double yoyGrowthPct) { this.yoyGrowthPct = yoyGrowthPct; }
    }

    public static class SiteOverviewRow {
        private String siteName;
        private List<Integer> yearlyVolumes;
        private double overallGrowthPct;
        private double cagrPct;

        public SiteOverviewRow() {}
        public SiteOverviewRow(String siteName, List<Integer> yearlyVolumes, double overallGrowthPct, double cagrPct) {
            this.siteName = siteName;
            this.yearlyVolumes = yearlyVolumes;
            this.overallGrowthPct = overallGrowthPct;
            this.cagrPct = cagrPct;
        }

        public String getSiteName() { return siteName; }
        public void setSiteName(String siteName) { this.siteName = siteName; }

        public List<Integer> getYearlyVolumes() { return yearlyVolumes; }
        public void setYearlyVolumes(List<Integer> yearlyVolumes) { this.yearlyVolumes = yearlyVolumes; }

        public double getOverallGrowthPct() { return overallGrowthPct; }
        public void setOverallGrowthPct(double overallGrowthPct) { this.overallGrowthPct = overallGrowthPct; }

        public double getCagrPct() { return cagrPct; }
        public void setCagrPct(double cagrPct) { this.cagrPct = cagrPct; }
    }
}
