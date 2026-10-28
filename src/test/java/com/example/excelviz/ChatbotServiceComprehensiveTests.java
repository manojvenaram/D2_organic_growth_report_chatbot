package com.example.excelviz;

import com.example.excelviz.model.ChatRequest;
import com.example.excelviz.model.ChatResponse;
import com.example.excelviz.service.ChatbotService;
import com.example.excelviz.service.ExcelCacheService;
import com.example.excelviz.service.ExcelParserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive verification of the D2 Life Sciences eDMS AI Assistant.
 * Validates natural language query handling across ALL dimensions:
 * - Sites (Nashik, Chh. Sambhajinagar, comparisons, leaders)
 * - Creation Profiles (Manufacturing, Governance, Validation, Facilities)
 * - GMP Groups (SOP, Forms, Specifications, Methods, MBRs)
 * - Granular Artifacts & Subgroups
 * - Multi-Filter Intersection (Site + Group + Year + Profile)
 * - Operational Years (2021–2026, comparisons, peak/min extremes)
 * - Statistical Averages & Leaderboard Rankings
 * - Organic Created vs Migrated Ratios
 * - Executive Growth Analytics & 5-Year CAGR
 * - PDF Report Generation & UI Navigation Actions
 * - D2 Life Sciences Domain Glossary & 21 CFR Part 11
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatbotServiceComprehensiveTests {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private ExcelCacheService excelCacheService;

    @BeforeAll
    void loadSampleExcelData() throws Exception {
        File file = new File("site_primarygroup_yearwise_Nashik_Artifact 1.xlsx");
        assertThat(file).exists();
        try (FileInputStream fis = new FileInputStream(file)) {
            excelParserService.parseExcel(fis, file.getName());
        }
        assertThat(excelCacheService.isLoaded()).isTrue();
        assertThat(excelCacheService.getOwningSites()).isNotEmpty();
    }

    // ── 1. Manufacturing Sites ────────────────────────────────────────────────

    @Test
    @DisplayName("Site Deep Dive — Nashik details & volume metrics")
    void testSiteDeepDiveNashik() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Tell me about Nashik"));
        assertThat(res.getReply())
                .contains("Nashik, IN - FDF 1")
                .contains("110,754")   // Total documents
                .contains("65,475")    // Organic created
                .contains("45,279");   // Migrated
        assertThat(res.getActionType()).isEqualTo("SELECT_SITE");
        assertThat(res.getActionPayload()).contains("Nashik");
    }

    @Test
    @DisplayName("Site Deep Dive — Chhatrapati Sambhajinagar details")
    void testSiteDeepDiveSambhajinagar() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Tell me about Sambhajinagar"));
        assertThat(res.getReply())
                .contains("Sambhajinagar")
                .contains("55,335")    // Total documents
                .contains("37,241");   // Organic created
        assertThat(res.getActionType()).isEqualTo("SELECT_SITE");
    }

    @Test
    @DisplayName("Cross-Plant Comparison — Nashik vs Sambhajinagar side-by-side")
    void testSiteComparison() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Compare Nashik vs Sambhajinagar"));
        assertThat(res.getReply())
                .contains("Cross-Plant Manufacturing Site Comparison")
                .contains("Nashik")
                .contains("Sambhajinagar");
        assertThat(res.getActionType()).isEqualTo("SWITCH_TAB");
        assertThat(res.getActionPayload()).isEqualTo("compare");
    }

    @Test
    @DisplayName("Site Extremes — Highest and lowest volume sites")
    void testSiteExtremes() {
        ChatResponse leader = chatbotService.processQuery(new ChatRequest("Which site has the highest volume?"));
        assertThat(leader.getReply())
                .contains("Leading (Highest Volume)")
                .contains("Nashik");

        ChatResponse lowest = chatbotService.processQuery(new ChatRequest("Which site has the lowest volume?"));
        assertThat(lowest.getReply())
                .contains("Lowest Volume")
                .contains("Sambhajinagar");
    }

    // ── 2. Creation Profiles ──────────────────────────────────────────────────

    @Test
    @DisplayName("Creation Profiles — Full portfolio breakdown")
    void testCreationProfilesBreakdown() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Show breakdown by creation profile"));
        assertThat(res.getReply())
                .contains("Creation Profiles Distribution")
                .contains("Manufacturing")
                .contains("Governance and Procedures")
                .contains("Validation")
                .contains("Facility and Organization");
    }

    @Test
    @DisplayName("Creation Profile Deep Dive — Validation profile")
    void testValidationProfile() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Tell me about Validation profile"));
        assertThat(res.getReply())
                .contains("Validation")
                .contains("18,849")   // Total volume
                .contains("1,874")    // Organic created
                .contains("16,975");  // Migrated
    }

    // ── 3. GMP Groups ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GMP Groups — Top regulated groups leaderboard")
    void testTopGmpGroups() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("What are the top GMP groups?"));
        assertThat(res.getReply())
                .contains("Top GMP Document Groups")
                .contains("Master Manufacturing")
                .contains("Specification")
                .contains("Form")
                .contains("Executed Validation")
                .contains("Standard Operating Procedure");
    }

    @Test
    @DisplayName("GMP Group Specific — Standard Operating Procedures (SOPs)")
    void testSopVolume() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("How many SOPs do we have?"));
        assertThat(res.getReply())
                .contains("Standard Operating Procedure")
                .contains("10,438")   // Exact volume
                .contains("100.0%");  // 100% organic created
    }

    @Test
    @DisplayName("GMP Group Specific — Forms volume")
    void testFormsVolume() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("How many Forms are logged?"));
        assertThat(res.getReply())
                .contains("Form")
                .contains("24,207");  // Exact volume
    }

    // ── 4. Operational Years & Historical Trends ──────────────────────────────

    @Test
    @DisplayName("Year Deep Dive — 2024 performance")
    void testYear2024() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("What happened in 2024?"));
        assertThat(res.getReply())
                .contains("Document Performance for Year **2024**")
                .contains("13,753")   // Total
                .contains("10,557");  // Created
    }

    @Test
    @DisplayName("Year Extremes — Peak and lowest output years")
    void testYearExtremes() {
        ChatResponse peak = chatbotService.processQuery(new ChatRequest("Which year had the highest volume?"));
        assertThat(peak.getReply())
                .contains("Peak Volume")
                .contains("2025")
                .contains("81,380");

        ChatResponse lowest = chatbotService.processQuery(new ChatRequest("Which year had the lowest volume?"));
        assertThat(lowest.getReply())
                .contains("Lowest Output")
                .contains("2021")
                .contains("4,491");
    }

    @Test
    @DisplayName("Year-over-Year Comparison — 2024 vs 2025")
    void testYearComparison() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Compare 2024 and 2025"));
        assertThat(res.getReply())
                .contains("Multi-Year Comparison: **2024 vs. 2025**")
                .contains("13,753")
                .contains("81,380")
                .contains("+67,627");
    }

    // ── 5. Multi-Filter Intersections ─────────────────────────────────────────

    @Test
    @DisplayName("Multi-Filter Intersection — Nashik SOP in 2024")
    void testMultiFilterIntersection() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Nashik SOP in 2024"));
        assertThat(res.getReply())
                .contains("Granular Filter Analysis")
                .contains("Nashik")
                .contains("Standard Operating Procedure")
                .contains("2024");
    }

    // ── 6. Statistical Averages & Leaderboard ─────────────────────────────────

    @Test
    @DisplayName("Statistical Averages — Arithmetic mean per year and site")
    void testAverages() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("What is the average document volume per year?"));
        assertThat(res.getReply())
                .contains("Portfolio Statistical Averages & Output Metrics")
                .contains("Average Output Per Year")
                .contains("Average Volume Per Manufacturing Site");
    }

    @Test
    @DisplayName("Rankings — Top artifacts leaderboard")
    void testArtifactRankings() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Rank top artifacts"));
        assertThat(res.getReply())
                .contains("Top 10 Granular Artifacts Leaderboard");
    }

    // ── 7. Organic vs Migrated Adoption ───────────────────────────────────────

    @Test
    @DisplayName("Organic vs Migrated — Portfolio cutover ratios")
    void testOrganicVsMigrated() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("What is the Created vs Migrated ratio?"));
        assertThat(res.getReply())
                .contains("Created vs. Migrated Document Distribution")
                .contains("102,716")  // Total organic created
                .contains("63,373")   // Total migrated
                .contains("61.8%")    // Organic %
                .contains("38.2%");   // Migrated %
    }

    // ── 8. Executive Growth & CAGR ────────────────────────────────────────────

    @Test
    @DisplayName("Executive Growth — CAGR and expansion narrative")
    void testExecutiveGrowth() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("What is the executive growth story?"));
        assertThat(res.getReply())
                .contains("Executive Growth Summary")
                .contains("4,491")     // Baseline
                .contains("32,138")    // Current
                .contains("+615.6%")   // Expansion
                .contains("+48.2%");   // CAGR
        assertThat(res.getActionType()).isEqualTo("SWITCH_TAB");
        assertThat(res.getActionPayload()).isEqualTo("growth");
    }

    // ── 9. Actions & Report Export ────────────────────────────────────────────

    @Test
    @DisplayName("Data Integrity Audit — GxP compliance & anomaly detection")
    void testDataIntegrityAudit() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Run data integrity audit"));
        assertThat(res.getReply())
                .contains("Data Integrity & Anomaly Audit")
                .contains("Overall Portfolio Health Score")
                .contains("Organic Adoption Integrity")
                .contains("Compliance Verdict");
        assertThat(res.getActionType()).isEqualTo("EXPORT_REPORT");
    }

    @Test
    @DisplayName("Executive PDF Report — Download action trigger")
    void testReportExport() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Download PDF report"));
        assertThat(res.getReply()).contains("Automated Executive PDF Report");
        assertThat(res.getActionType()).isEqualTo("EXPORT_REPORT");
    }

    @Test
    @DisplayName("Tab Navigation — Switch to Chart View")
    void testTabNavigation() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Open Chart View"));
        assertThat(res.getActionType()).isEqualTo("SWITCH_TAB");
        assertThat(res.getActionPayload()).isEqualTo("chart");
    }

    // ── 10. Domain Knowledge & Glossary ───────────────────────────────────────

    @Test
    @DisplayName("Domain Glossary — Documentum D2 concept")
    void testGlossaryD2() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("What is Documentum D2?"));
        assertThat(res.getReply())
                .contains("OpenText Documentum D2")
                .contains("21 CFR Part 11");
    }

    @Test
    @DisplayName("Domain Glossary — What is an Artifact?")
    void testGlossaryArtifact() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("What is an Artifact in D2?"));
        assertThat(res.getReply())
                .contains("Creation Profile")
                .contains("GMP Group")
                .contains("GMP Subgroup")
                .contains("GMP Artifact");
    }

    @Test
    @DisplayName("Domain Glossary — FDA 21 CFR Part 11")
    void testGlossaryPart11() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Tell me about 21 CFR Part 11"));
        assertThat(res.getReply())
                .contains("Title 21 CFR Part 11")
                .contains("electronic signatures")
                .contains("audit trails");
    }

    // ── 11. Portfolio Overview ────────────────────────────────────────────────

    @Test
    @DisplayName("Portfolio Overview — High-level summary of active database")
    void testPortfolioOverview() {
        ChatResponse res = chatbotService.processQuery(new ChatRequest("Show portfolio overview"));
        assertThat(res.getReply())
                .contains("Enterprise Portfolio Overview")
                .contains("166,089")  // Grand total documents
                .contains("2021 – 2026");
    }
}
