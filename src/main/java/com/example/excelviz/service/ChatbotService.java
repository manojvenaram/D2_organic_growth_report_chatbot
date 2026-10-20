package com.example.excelviz.service;

import com.example.excelviz.model.ChatRequest;
import com.example.excelviz.model.ChatResponse;
import com.example.excelviz.model.ExcelRowData;
import com.example.excelviz.model.ExecutiveSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent Analytics Chatbot Service for Documentum D2 Life Sciences eDMS.
 * Operates with an enterprise domain knowledge engine over in-memory cached Excel data.
 */
@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Autowired
    private ExcelCacheService cacheService;

    @Autowired
    private GrowthAnalyticsService growthAnalyticsService;

    @Autowired
    private MetadataService metadataService;

    public ChatResponse processQuery(ChatRequest request) {
        String query = request.getMessage() != null ? request.getMessage().trim() : "";
        if (query.isEmpty()) {
            return new ChatResponse(
                "👋 Hello! I am your **D2 Analytics Assistant**. Ask me anything about your Documentum D2 document volumes, site comparisons, YoY growth, or organic adoption rates.",
                getDefaultSuggestions()
            );
        }

        String lower = query.toLowerCase();

        // 1. Check if data is loaded
        if (!cacheService.isLoaded() || cacheService.getExcelData().isEmpty()) {
            ChatResponse res = new ChatResponse();
            res.setReply(
                "📁 **No Excel file has been uploaded yet.**\n\n" +
                "Please choose and upload your Excel report (`.xlsx`) using the sidebar upload button first. " +
                "Once uploaded, I can analyze:\n" +
                "- 📈 Multi-year organic document growth (2021–2026)\n" +
                "- 🏭 Plant site volumes and 5-year CAGR\n" +
                "- 🌱 Organic Created vs Migrated document ratios\n" +
                "- 📋 Top GMP groups, SOPs, and Forms"
            );
            res.setQuickSuggestions(List.of(
                "How do I upload an Excel file?",
                "What format is supported?",
                "What metrics can you calculate?"
            ));
            return res;
        }

        List<String> sites = new ArrayList<>(cacheService.getOwningSites());
        List<Integer> years = cacheService.getAvailableYears().stream().sorted().toList();

        // 2. Intent Routing

        // A. Help / Capabilities / Greetings
        if (isMatch(lower, "help", "who are you", "what can you do", "commands", "features", "capabilities", "hello", "hi", "hey")) {
            return handleHelp();
        }

        // B. Executive Growth Story / Leadership
        if (isMatch(lower, "growth", "growth story", "executive", "leadership", "cagr", "growth summary", "overall growth", "slide", "trend", "trends", "acceleration")) {
            return handleExecutiveGrowth();
        }

        // C. Created vs Migrated / Organic adoption
        if (isMatch(lower, "created vs migrated", "migrated", "organic", "legacy", "import", "cutover")) {
            return handleCreatedVsMigrated(years);
        }

        // D. Divested or inactive sites
        if (isMatch(lower, "divested", "inactive", "retired", "closed site")) {
            return handleDivestedSites();
        }

        // E. Comparison between sites
        if (isMatch(lower, "compare", "versus", " vs ", "difference between sites")) {
            return handleSiteComparison(lower, sites, years);
        }

        // F. Specific Site Inquiry
        for (String s : sites) {
            if (lower.contains(s.toLowerCase()) || matchesSiteAlias(lower, s)) {
                return handleSiteDeepDive(s, years);
            }
        }

        // G. Top GMP Groups / SOPs / Forms
        if (isMatch(lower, "gmp", "group", "sop", "form", "artifact", "top document", "protocol", "work instruction")) {
            return handleGmpBreakdown();
        }

        // H. Year-Specific Inquiry
        for (Integer y : years) {
            if (lower.contains(String.valueOf(y))) {
                return handleYearDeepDive(y);
            }
        }

        // I. Overview & Cache Stats
        if (isMatch(lower, "status", "overview", "summary", "how many sites", "total documents", "stats", "all sites")) {
            return handlePortfolioOverview(sites, years);
        }

        // J. Smart Fallback with keyword matching
        return handleSmartFallback(query, lower, sites, years);
    }

    public List<String> getDefaultSuggestions() {
        if (!cacheService.isLoaded()) {
            return List.of(
                "How do I upload an Excel file?",
                "What format is supported?",
                "What can you do?"
            );
        }
        return List.of(
            "📊 Executive Growth Story",
            "🌱 Created vs Migrated Ratio",
            "🏭 Compare All Sites",
            "📋 Top GMP Groups",
            "💡 What is the overall portfolio CAGR?"
        );
    }

    private boolean isMatch(String text, String... keywords) {
        for (String kw : keywords) {
            if (kw.length() <= 3) {
                // Short keywords like "hi", "sop", "vs" must match on word boundaries
                if (text.matches("(?i).*\\b" + java.util.regex.Pattern.quote(kw) + "\\b.*")) {
                    return true;
                }
            } else {
                if (text.contains(kw)) return true;
            }
        }
        return false;
    }

    private boolean matchesSiteAlias(String lower, String siteName) {
        String sLower = siteName.toLowerCase();
        if (sLower.contains("nashik") && lower.contains("nashik")) return true;
        if ((sLower.contains("chhatrapati") || sLower.contains("sambhajinagar") || sLower.contains("fdf"))
            && (lower.contains("sambhajinagar") || lower.contains("chhatrapati") || lower.contains("fdf"))) return true;
        return false;
    }

    // ── Intent Handlers ────────────────────────────────────────────────────────

    private ChatResponse handleHelp() {
        String reply =
            "### 🤖 D2 Analytics Assistant Capabilities\n\n" +
            "I provide real-time domain intelligence over your OpenText Documentum D2 Life Sciences eDMS data:\n\n" +
            "| Feature | Example Question |\n" +
            "|---|---|\n" +
            "| **📈 Growth Story** | *\"Summarize the executive growth story\"* or *\"What is the portfolio CAGR?\"* |\n" +
            "| **🏭 Site Analysis** | *\"Tell me about Nashik\"* or *\"What is Chh. Sambhajinagar volume?\"* |\n" +
            "| **⚖️ Site Comparison** | *\"Compare Nashik vs Chh. Sambhajinagar\"* |\n" +
            "| **🌱 Organic Adoption** | *\"What is the Created vs Migrated ratio?\"* |\n" +
            "| **📑 Document Types** | *\"What are the top GMP groups and SOP counts?\"* |\n" +
            "| **📅 Yearly Trends** | *\"What was the document volume in 2024?\"* |\n\n" +
            "💬 *Click any suggestion chip below or type your custom question!*";

        return new ChatResponse(reply, List.of(
            "📊 Executive Growth Story",
            "🏭 Tell me about Nashik",
            "⚖️ Compare All Sites",
            "🌱 Created vs Migrated Ratio"
        ));
    }

    private ChatResponse handleExecutiveGrowth() {
        ExecutiveSummaryResponse summary = growthAnalyticsService.getExecutiveSummary("CREATED");

        StringBuilder sb = new StringBuilder();
        sb.append("### 📈 Executive Growth Summary (Organic Created)\n\n");
        sb.append("Here is the leadership growth intelligence based on active operating sites:\n\n");
        sb.append(String.format("- **Starting Baseline (%d):** `%s` documents\n",
                summary.getBaselineYear(), formatNum(summary.getBaselineVolume())));
        sb.append(String.format("- **Current Volume (%d):** `%s` documents\n",
                summary.getCurrentYear(), formatNum(summary.getCurrentVolume())));
        sb.append(String.format("- **Overall Portfolio Expansion:** `+%.1f%%`\n", summary.getOverallGrowthPct()));
        sb.append(String.format("- **Latest YoY Growth Rate:** `%s%.1f%%`\n\n",
                summary.getLatestYoyGrowthPct() >= 0 ? "+" : "", summary.getLatestYoyGrowthPct()));

        sb.append("#### 💡 Strategic Insights:\n");
        if (summary.getBulletInsights() != null) {
            for (String bullet : summary.getBulletInsights()) {
                sb.append("- ").append(bullet).append("\n");
            }
        }

        ChatResponse res = new ChatResponse(sb.toString(), List.of(
            "🌱 How many were Migrated vs Created?",
            "🏭 Show Active Site matrix",
            "⚖️ Compare sites"
        ));
        res.setActionType("SWITCH_TAB");
        res.setActionPayload("growth");
        return res;
    }

    private ChatResponse handleCreatedVsMigrated(List<Integer> years) {
        long totalCreated = 0;
        long totalMigrated = 0;

        Map<Integer, Long> yCreated = new TreeMap<>();
        Map<Integer, Long> yMigrated = new TreeMap<>();

        for (ExcelRowData row : cacheService.getExcelData()) {
            if (row.getYearTypeCounts() == null) continue;
            for (Integer y : years) {
                Map<String, Integer> counts = row.getYearTypeCounts().get(y);
                if (counts == null) continue;
                int c = counts.getOrDefault(ExcelParserService.CREATED, 0);
                int m = counts.getOrDefault(ExcelParserService.MIGRATED, 0);
                totalCreated += c;
                totalMigrated += m;
                yCreated.merge(y, (long) c, Long::sum);
                yMigrated.merge(y, (long) m, Long::sum);
            }
        }

        long total = totalCreated + totalMigrated;
        double createdPct = total > 0 ? (totalCreated * 100.0 / total) : 0;
        double migratedPct = total > 0 ? (totalMigrated * 100.0 / total) : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("### 🌱 Created vs. Migrated Document Distribution\n\n");
        sb.append(String.format("Across the entire portfolio, a total of **%s** documents are logged:\n\n", formatNum(total)));
        sb.append(String.format("- 🌿 **Organic Created / Import:** `%s` (**%.1f%%**)\n", formatNum(totalCreated), createdPct));
        sb.append(String.format("- 📦 **Legacy Migrated:** `%s` (**%.1f%%**)\n\n", formatNum(totalMigrated), migratedPct));

        sb.append("| Year | Organic Created | Legacy Migrated | Total | Organic % |\n");
        sb.append("|---|---|---|---|---|\n");
        for (Integer y : years) {
            long c = yCreated.getOrDefault(y, 0L);
            long m = yMigrated.getOrDefault(y, 0L);
            long yrTotal = c + m;
            double pct = yrTotal > 0 ? (c * 100.0 / yrTotal) : 0;
            sb.append(String.format("| **%d** | %s | %s | %s | `%.1f%%` |\n",
                    y, formatNum(c), formatNum(m), formatNum(yrTotal), pct));
        }

        sb.append("\n> **Key Takeaway:** Post-cutover years show rapid transition where daily GMP operations run almost exclusively on organic document creation.");

        return new ChatResponse(sb.toString(), List.of(
            "📊 View Growth Analytics",
            "🏭 Tell me about Nashik",
            "📋 Top GMP Groups"
        ));
    }

    private ChatResponse handleDivestedSites() {
        ExecutiveSummaryResponse summary = growthAnalyticsService.getExecutiveSummary("CREATED");
        List<String> allSites = new ArrayList<>(cacheService.getOwningSites());
        Set<String> activeSites = summary.getSiteRows().stream()
                .map(ExecutiveSummaryResponse.SiteOverviewRow::getSiteName)
                .collect(Collectors.toSet());

        List<String> divested = allSites.stream()
                .filter(s -> !activeSites.contains(s))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("### 🏭 Portfolio Site Activity Status\n\n");
        if (divested.isEmpty()) {
            sb.append("✅ **All ").append(allSites.size()).append(" sites in the dataset are currently active** with document volumes recorded in recent operating years.\n\n");
            sb.append("Active operating sites included in analytics:\n");
            for (String s : allSites) {
                sb.append("- 🏢 **").append(s).append("**\n");
            }
        } else {
            sb.append("⚠️ **Divested / Inactive Sites Detected:**\n");
            for (String s : divested) {
                sb.append("- 🛑 **").append(s).append("** (Zero document volume recorded in the last 2 years — excluded from growth benchmarks)\n");
            }
            sb.append("\n✅ **Active Operating Sites:**\n");
            for (String s : activeSites) {
                sb.append("- 🏢 **").append(s).append("**\n");
            }
        }

        return new ChatResponse(sb.toString(), List.of(
            "📊 Executive Growth Story",
            "⚖️ Compare Active Sites"
        ));
    }

    private ChatResponse handleSiteComparison(String lower, List<String> sites, List<Integer> years) {
        StringBuilder sb = new StringBuilder();
        sb.append("### ⚖️ Multi-Site Comparison Overview\n\n");
        sb.append("| Site Name | Total Documents | Organic Created | Migrated | Status |\n");
        sb.append("|---|---|---|---|---|\n");

        for (String site : sites) {
            long created = 0, migrated = 0;
            for (ExcelRowData row : cacheService.getExcelData()) {
                if (!site.equalsIgnoreCase(row.getOwningSite())) continue;
                if (row.getYearTypeCounts() == null) continue;
                for (Integer y : years) {
                    Map<String, Integer> counts = row.getYearTypeCounts().get(y);
                    if (counts != null) {
                        created += counts.getOrDefault(ExcelParserService.CREATED, 0);
                        migrated += counts.getOrDefault(ExcelParserService.MIGRATED, 0);
                    }
                }
            }
            long total = created + migrated;
            sb.append(String.format("| **%s** | **%s** | %s | %s | %s |\n",
                    site, formatNum(total), formatNum(created), formatNum(migrated),
                    total > 0 ? "🟢 Active" : "⚪ Inactive"));
        }

        sb.append("\n💡 *To see visual side-by-side bar and area charts for these sites, switch to the **Compare Sites** tab.*");

        ChatResponse res = new ChatResponse(sb.toString(), List.of(
            "🏭 Tell me more about Nashik",
            "📈 Show Growth Analytics",
            "🌱 Created vs Migrated"
        ));
        res.setActionType("SWITCH_TAB");
        res.setActionPayload("compare");
        return res;
    }

    private ChatResponse handleSiteDeepDive(String siteName, List<Integer> years) {
        long createdTotal = 0, migratedTotal = 0;
        Map<Integer, Long> yearlyCreated = new TreeMap<>();
        Map<String, Long> profileCounts = new HashMap<>();
        Map<String, Long> gmpCounts = new HashMap<>();

        for (ExcelRowData row : cacheService.getExcelData()) {
            if (!siteName.equalsIgnoreCase(row.getOwningSite())) continue;

            String profile = row.getCreationProfile();
            String gmp = row.getGmpGroup();

            if (row.getYearTypeCounts() == null) continue;
            for (Integer y : years) {
                Map<String, Integer> counts = row.getYearTypeCounts().get(y);
                if (counts != null) {
                    int c = counts.getOrDefault(ExcelParserService.CREATED, 0);
                    int m = counts.getOrDefault(ExcelParserService.MIGRATED, 0);
                    createdTotal += c;
                    migratedTotal += m;
                    yearlyCreated.merge(y, (long) c, Long::sum);

                    if (profile != null && !profile.isBlank()) {
                        profileCounts.merge(profile, (long) (c + m), Long::sum);
                    }
                    if (gmp != null && !gmp.isBlank()) {
                        gmpCounts.merge(gmp, (long) (c + m), Long::sum);
                    }
                }
            }
        }

        long total = createdTotal + migratedTotal;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### 🏢 Site Deep Dive: **%s**\n\n", siteName));
        sb.append(String.format("- **Total Document Volume:** `%s`\n", formatNum(total)));
        sb.append(String.format("- **Organic Created:** `%s` (%.1f%%)\n",
                formatNum(createdTotal), total > 0 ? (createdTotal * 100.0 / total) : 0));
        sb.append(String.format("- **Legacy Migrated:** `%s` (%.1f%%)\n\n",
                formatNum(migratedTotal), total > 0 ? (migratedTotal * 100.0 / total) : 0));

        sb.append("#### 📅 Year-over-Year Organic Volumes:\n");
        sb.append("| Year | Created Documents | Change |\n|---|---|---|\n");
        Long prev = null;
        for (Integer y : years) {
            long c = yearlyCreated.getOrDefault(y, 0L);
            String chg = "—";
            if (prev != null && prev > 0) {
                double pct = ((c - prev) * 100.0) / prev;
                chg = String.format("%s%.1f%%", pct >= 0 ? "+" : "", pct);
            }
            sb.append(String.format("| **%d** | %s | `%s` |\n", y, formatNum(c), chg));
            prev = c;
        }

        if (!gmpCounts.isEmpty()) {
            sb.append("\n#### 📑 Top GMP Groups at this Site:\n");
            gmpCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(4)
                    .forEach(e -> sb.append(String.format("- **%s:** %s docs\n", e.getKey(), formatNum(e.getValue()))));
        }

        ChatResponse res = new ChatResponse(sb.toString(), List.of(
            "📊 Filter Chart to " + siteName,
            "⚖️ Compare with other sites",
            "🌱 Overall Created vs Migrated"
        ));
        res.setActionType("SELECT_SITE");
        res.setActionPayload(siteName);
        return res;
    }

    private ChatResponse handleGmpBreakdown() {
        Map<String, Long> gmpTotal = new HashMap<>();
        Map<String, Long> gmpCreated = new HashMap<>();

        for (ExcelRowData row : cacheService.getExcelData()) {
            String g = row.getGmpGroup();
            if (g == null || g.isBlank()) continue;

            if (row.getYearTypeCounts() == null) continue;
            for (Map<String, Integer> counts : row.getYearTypeCounts().values()) {
                int c = counts.getOrDefault(ExcelParserService.CREATED, 0);
                int m = counts.getOrDefault(ExcelParserService.MIGRATED, 0);
                gmpTotal.merge(g, (long) (c + m), Long::sum);
                gmpCreated.merge(g, (long) c, Long::sum);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 📑 Top GMP Groups & Artifact Distribution\n\n");
        sb.append("The highest volume regulated document groups across all manufacturing sites:\n\n");
        sb.append("| Rank | GMP Group | Total Volume | Organic Created | Share |\n");
        sb.append("|---|---|---|---|---|\n");

        long grandTotal = gmpTotal.values().stream().mapToLong(Long::longValue).sum();

        List<Map.Entry<String, Long>> sorted = gmpTotal.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .toList();

        int rank = 1;
        for (Map.Entry<String, Long> entry : sorted) {
            long total = entry.getValue();
            long cr = gmpCreated.getOrDefault(entry.getKey(), 0L);
            double share = grandTotal > 0 ? (total * 100.0 / grandTotal) : 0;
            sb.append(String.format("| #%d | **%s** | %s | %s | `%.1f%%` |\n",
                    rank++, entry.getKey(), formatNum(total), formatNum(cr), share));
        }

        sb.append("\n> **Regulatory Insight:** Standard Operating Procedures (SOPs) and Forms constitute the bulk of controlled plant documentation.");

        return new ChatResponse(sb.toString(), List.of(
            "📊 View Chart View",
            "📈 Show Growth Story",
            "🏭 Compare sites"
        ));
    }

    private ChatResponse handleYearDeepDive(int targetYear) {
        long created = 0, migrated = 0;
        Map<String, Long> siteVol = new HashMap<>();

        for (ExcelRowData row : cacheService.getExcelData()) {
            if (row.getYearTypeCounts() == null) continue;
            Map<String, Integer> counts = row.getYearTypeCounts().get(targetYear);
            if (counts == null) continue;

            int c = counts.getOrDefault(ExcelParserService.CREATED, 0);
            int m = counts.getOrDefault(ExcelParserService.MIGRATED, 0);
            created += c;
            migrated += m;

            String s = row.getOwningSite();
            if (s != null) {
                siteVol.merge(s, (long) (c + m), Long::sum);
            }
        }

        long total = created + migrated;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### 📅 Document Performance for Year **%d**\n\n", targetYear));
        sb.append(String.format("- **Total Document Volume:** `%s`\n", formatNum(total)));
        sb.append(String.format("- **Organic Created:** `%s` (%.1f%%)\n",
                formatNum(created), total > 0 ? (created * 100.0 / total) : 0));
        sb.append(String.format("- **Legacy Migrated:** `%s` (%.1f%%)\n\n",
                formatNum(migrated), total > 0 ? (migrated * 100.0 / total) : 0));

        sb.append("#### Site Volume Contribution in ").append(targetYear).append(":\n");
        for (Map.Entry<String, Long> e : siteVol.entrySet()) {
            sb.append(String.format("- **%s:** %s docs\n", e.getKey(), formatNum(e.getValue())));
        }

        return new ChatResponse(sb.toString(), List.of(
            "📈 Show full YoY Growth curve",
            "🌱 Created vs Migrated"
        ));
    }

    private ChatResponse handlePortfolioOverview(List<String> sites, List<Integer> years) {
        long totalRows = cacheService.getExcelData().size();
        long grandTotal = 0;

        for (ExcelRowData row : cacheService.getExcelData()) {
            if (row.getYearTypeCounts() == null) continue;
            for (Map<String, Integer> counts : row.getYearTypeCounts().values()) {
                grandTotal += counts.getOrDefault(ExcelParserService.CREATED, 0);
                grandTotal += counts.getOrDefault(ExcelParserService.MIGRATED, 0);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 📊 Documentum D2 Portfolio Overview\n\n");
        sb.append(String.format("- **Active Plant Sites:** `%d` (%s)\n", sites.size(), String.join(", ", sites)));
        sb.append(String.format("- **Time Horizon:** `%d – %d` (%d operational years)\n",
                years.get(0), years.get(years.size() - 1), years.size()));
        sb.append(String.format("- **Total Documents Logged:** `%s` across `%d` granular artifact rows\n",
                formatNum(grandTotal), totalRows));
        sb.append("- **System Status:** 🟢 Cache synchronized & ready for interactive query\n\n");
        sb.append("Ask me for a deep dive on any specific site, document type, or year!");

        return new ChatResponse(sb.toString(), List.of(
            "📈 Executive Growth Story",
            "🏭 Tell me about Nashik",
            "⚖️ Compare All Sites"
        ));
    }

    private ChatResponse handleSmartFallback(String original, String lower, List<String> sites, List<Integer> years) {
        // Search rows for matching keyword
        long matchCount = 0;
        Set<String> matchedSites = new HashSet<>();
        for (ExcelRowData row : cacheService.getExcelData()) {
            String combined = (row.getOwningSite() + " " + row.getCreationProfile() + " " +
                               row.getGmpGroup() + " " + row.getGmpSubgroup() + " " +
                               row.getGmpArtifact()).toLowerCase();
            if (combined.contains(lower)) {
                matchCount++;
                if (row.getOwningSite() != null) matchedSites.add(row.getOwningSite());
            }
        }

        if (matchCount > 0) {
            String reply = String.format(
                "🔎 Found **%d matching records** for `\"%s\"` across sites: **%s**.\n\n" +
                "Would you like me to analyze growth, volume breakdown, or specific years for this term?",
                matchCount, original, String.join(", ", matchedSites)
            );
            return new ChatResponse(reply, List.of(
                "📊 Executive Growth Story",
                "🌱 Created vs Migrated Ratio",
                "🏭 Compare All Sites"
            ));
        }

        String fallback =
            "💡 I analyzed the dataset, but couldn't find a direct match for `\"" + original + "\"`. \n\n" +
            "Here are some popular questions you can ask me:\n" +
            "- **\"What is the executive growth story?\"**\n" +
            "- **\"Tell me about Nashik\"**\n" +
            "- **\"What is the Created vs Migrated ratio?\"**\n" +
            "- **\"Compare all operating sites\"**\n" +
            "- **\"What are the top GMP groups?\"**";

        return new ChatResponse(fallback, getDefaultSuggestions());
    }

    private String formatNum(long num) {
        return String.format("%,d", num);
    }
}
