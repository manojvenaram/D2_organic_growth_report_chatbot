package com.example.excelviz;

import com.example.excelviz.service.ExcelCacheService;
import com.example.excelviz.service.MetadataService;
import com.example.excelviz.service.AggregationService;
import com.example.excelviz.service.SiteCompareService;
import com.example.excelviz.service.ExcelParserService;
import com.example.excelviz.controller.ChartDataController;
import com.example.excelviz.controller.MetadataController;
import com.example.excelviz.controller.SiteCompareController;
import com.example.excelviz.controller.ExcelUploadController;
import com.example.excelviz.controller.GrowthController;
import com.example.excelviz.service.GrowthAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration smoke tests — verifies the full Spring context loads
 * and all beans are wired correctly.
 *
 * Run with: mvn test
 */
@SpringBootTest(properties = "excelviz.preload-sample=false")
class ExcelvizApplicationTests {

    // ── Controllers ───────────────────────────────────────────────
    @Autowired private ChartDataController    chartDataController;
    @Autowired private MetadataController     metadataController;
    @Autowired private SiteCompareController  siteCompareController;
    @Autowired private ExcelUploadController  excelUploadController;
    @Autowired private GrowthController       growthController;

    // ── Services ──────────────────────────────────────────────────
    @Autowired private ExcelParserService     excelParserService;
    @Autowired private ExcelCacheService      excelCacheService;
    @Autowired private MetadataService        metadataService;
    @Autowired private AggregationService     aggregationService;
    @Autowired private SiteCompareService     siteCompareService;
    @Autowired private GrowthAnalyticsService growthAnalyticsService;

    /**
     * Verifies the Spring application context loads without errors.
     * All @Autowired beans must be non-null — proves DI wiring is correct.
     */
    @Test
    void contextLoads() {
        // Controllers
        assertThat(chartDataController)     .isNotNull();
        assertThat(metadataController)      .isNotNull();
        assertThat(siteCompareController)   .isNotNull();
        assertThat(excelUploadController)   .isNotNull();
        assertThat(growthController)        .isNotNull();

        // Services
        assertThat(excelParserService)      .isNotNull();
        assertThat(excelCacheService)       .isNotNull();
        assertThat(metadataService)         .isNotNull();
        assertThat(aggregationService)      .isNotNull();
        assertThat(siteCompareService)      .isNotNull();
        assertThat(growthAnalyticsService)  .isNotNull();
    }

    /**
     * Verifies the cache starts empty before any Excel is uploaded.
     */
    @Test
    void cacheIsEmptyOnStartup() {
        assertThat(excelCacheService.isLoaded())         .isFalse();
        assertThat(excelCacheService.getExcelData())     .isEmpty();
        assertThat(excelCacheService.getOwningSites())   .isEmpty();
        assertThat(excelCacheService.getAvailableYears()).isEmpty();
    }

    /**
     * Verifies metadata endpoints return empty collections
     * when no data has been loaded yet — not null, not an error.
     */
    @Test
    void metadataReturnsEmptyBeforeUpload() {
        assertThat(metadataService.getOwningSites())   .isNotNull().isEmpty();
        assertThat(metadataService.getAvailableYears()).isNotNull().isEmpty();
        assertThat(metadataService.getCreationProfilesBySite("any-site"))
                .isNotNull().isEmpty();
        assertThat(metadataService.getGmpGroupsBySiteAndProfile("any-site", "any-profile"))
                .isNotNull().isEmpty();
    }

    // ── Chatbot Extension ─────────────────────────────────────────
    @Autowired private com.example.excelviz.controller.ChatController chatController;
    @Autowired private com.example.excelviz.service.ChatbotService    chatbotService;

    @Test
    void growthAnalyticsReturnsEmptySummaryBeforeUpload() {
        var summary = growthAnalyticsService.getExecutiveSummary("CREATED");
        assertThat(summary).isNotNull();
        assertThat(summary.getYearlyPoints()).isEmpty();
        assertThat(summary.getSiteRows()).isEmpty();
    }

    @Test
    void chatbotServiceReturnsHelpAndSuggestions() {
        assertThat(chatController).isNotNull();
        assertThat(chatbotService).isNotNull();
        
        var suggestions = chatbotService.getDefaultSuggestions();
        assertThat(suggestions).isNotEmpty();
        
        var res = chatbotService.processQuery(new com.example.excelviz.model.ChatRequest("help"));
        assertThat(res).isNotNull();
        assertThat(res.getReply()).contains("No Excel file has been uploaded yet");
    }
}