package com.example.excelviz.service;

import com.example.excelviz.model.ReportRequest;
import com.example.excelviz.model.ExcelRowData;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Generates a professional light-theme PDF report.
 * Reorganized Flow: Cover -> Global Summary -> Profile Deep Dives (Year-Wise Tables) -> Benchmark
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    @org.springframework.beans.factory.annotation.Autowired
    private ExcelCacheService cache;

    // ── Brand accent colours ───────────────────────────────────────
    private static final Color BRAND_BLUE   = new Color(59,  115, 232);
    private static final Color BRAND_INDIGO = new Color(124, 58,  237);
    private static final Color ACCENT_GREEN = new Color(5,   150, 105);
    private static final Color ACCENT_AMBER = new Color(217, 119, 6  );

    // ── Light theme palette ────────────────────────────────────────
    private static final Color C_BG         = new Color(255, 255, 255);
    private static final Color C_SURFACE    = new Color(248, 249, 252);
    private static final Color C_BORDER     = new Color(221, 225, 236);
    private static final Color C_TEXT       = new Color(15,  23,  42 );
    private static final Color C_MUTED      = new Color(100, 116, 139);
    private static final Color C_CELL_TEXT  = new Color(40,  55,  80 );
    private static final Color C_OBS_BG     = new Color(250, 251, 255);
    private static final Color C_DARK_HDR   = new Color(17,  52,  104); // Dark Blue for table headers

    // ── Fonts ──────────────────────────────────────────────────────
    private Font fSectionTitle, fSubHeading, fTableHdr, fTableCell, fObsBody;

    // ══════════════════════════════════════════════════════════════
    // PUBLIC ENTRY POINT & FLOW ORCHESTRATION
    // ══════════════════════════════════════════════════════════════
    public byte[] generateReport(ReportRequest req) throws Exception {
        initFonts();
        log.info("Generating PDF — site:{}, profiles:{}", req.getOwningSite(), req.getCreationProfiles());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 42, 42, 54, 46);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new HeaderFooterEvent(req));
        
        doc.open();
        writer.setViewerPreferences(PdfWriter.PageModeUseOutlines);
        PdfOutline rootOutline = writer.getRootOutline();

        PdfChartRenderer charts = new PdfChartRenderer();

        // 1. Cover Page
        addCoverPage(doc, writer, req);

        // 2. Global Executive Summary
        doc.newPage();
        new PdfOutline(rootOutline, new PdfDestination(PdfDestination.FIT), "Executive Summary");
        addGlobalSummary(doc, req);

        // 3. Profile Deep Dives
        if (req.getProfiles() != null && !req.getProfiles().isEmpty()) {
            PdfOutline profilesOutline = new PdfOutline(rootOutline, new PdfDestination(PdfDestination.FIT), "Creation Profiles Deep Dive");
            
            for (int i = 0; i < req.getProfiles().size(); i++) {
                doc.newPage();
                ReportRequest.ProfileSummary profile = req.getProfiles().get(i);
                new PdfOutline(profilesOutline, new PdfDestination(PdfDestination.FIT), profile.getProfileName());
                addProfileDeepDive(doc, profile, i + 1, req, writer, charts);
            }
        }

        // 4. Site Comparison
        if (req.getCompareSummary() != null) {
            doc.newPage();
            new PdfOutline(rootOutline, new PdfDestination(PdfDestination.FIT), "Site Comparison Benchmark");
            addCompareSection(doc, req.getCompareSummary());
        }

        doc.close();
        log.info("PDF done — {} bytes", out.size());
        return out.toByteArray();
    }

    // ══════════════════════════════════════════════════════════════
    // 1. COVER PAGE
    // ══════════════════════════════════════════════════════════════
    private void addCoverPage(Document doc, PdfWriter writer, ReportRequest req) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        float W = doc.getPageSize().getWidth();
        float H = doc.getPageSize().getHeight();

        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
        BaseFont bfReg  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);

        cb.setColorFill(Color.WHITE);
        cb.rectangle(0, 0, W, H);
        cb.fill();

        float hero = 300f;
        PdfShading shading = PdfShading.simpleAxial(writer, 0, H, W, H - hero, BRAND_BLUE, BRAND_INDIGO);
        PdfShadingPattern sp = new PdfShadingPattern(shading);
        cb.setShadingFill(sp);
        cb.rectangle(0, H - hero, W, hero);
        cb.fill();

        cb.setColorFill(new Color(255, 255, 255, 26));
        cb.circle(W - 26, H - 32, 92); cb.fill();
        cb.circle(W - 92, H - hero + 46, 44); cb.fill();

        cb.setColorFill(Color.WHITE);
        roundRect(cb, 50, H - 96, 36, 36, 9); cb.fill();
        coverText(cb, "D", 68, H - 84, Element.ALIGN_CENTER, bfBold, 22, BRAND_INDIGO);

        coverText(cb, "D2 Organic Growth Report", 50, H - 174, Element.ALIGN_LEFT, bfBold, 28, Color.WHITE);
        if (req.getOwningSite() != null && !req.getOwningSite().isEmpty())
            coverText(cb, req.getOwningSite(), 52, H - 200, Element.ALIGN_LEFT, bfReg, 13, new Color(255, 255, 255, 230));

        String docLabel = switch (req.getDocType() != null ? req.getDocType() : "BOTH") {
            case "CREATED"  -> "Created / Imported";
            case "MIGRATED" -> "Migrated";
            default         -> "Created/Imported + Migrated";
        };
        
        int nProf = req.getCreationProfiles() != null ? req.getCreationProfiles().size() : 0;
        String profilesVal = nProf == 0 ? "—" : nProf == 1 ? req.getCreationProfiles().get(0) : nProf + " profiles";

        float tx = 50, tw = (W - 100 - 15) / 2f, th = 66f, gap = 15f;
        float ty1 = H - hero - 46, ty2 = ty1 - th - gap;
        coverTile(cb, tx,            ty1, tw, th, "Profiles",     profilesVal,                                       bfReg, bfBold, BRAND_BLUE);
        coverTile(cb, tx + tw + gap, ty1, tw, th, "Year Range",   req.getYearFrom() + " – " + req.getYearTo(),  bfReg, bfBold, ACCENT_GREEN);
        coverTile(cb, tx,            ty2, tw, th, "Document Type", docLabel,                                          bfReg, bfBold, BRAND_INDIGO);
        coverTile(cb, tx + tw + gap, ty2, tw, th, "Generated",    LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), bfReg, bfBold, ACCENT_AMBER);

        cb.setColorStroke(C_BORDER); cb.setLineWidth(0.8f);
        cb.moveTo(50, 70); cb.lineTo(W - 50, 70); cb.stroke();
    }

    // ══════════════════════════════════════════════════════════════
    // 2. GLOBAL EXECUTIVE SUMMARY
    // ══════════════════════════════════════════════════════════════
    private void addGlobalSummary(Document doc, ReportRequest req) throws Exception {
        modernSectionHeader(doc, "Overview", "Executive Summary");

        if (req.getOwningSite() != null && !req.getOwningSite().isEmpty()) {
            Paragraph siteLine = new Paragraph("Site name: " + req.getOwningSite(), 
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, C_TEXT));
            siteLine.setSpacingAfter(4); doc.add(siteLine);
        }

        int totalDocs = 0, totalCreated = 0, totalMigrated = 0;
        String topProfile = "—"; int topProfileDocs = 0;
        String topGroup   = "—"; int topGroupDocs   = 0;
        java.util.Set<String> groupNames = new java.util.LinkedHashSet<>();

        if (req.getProfiles() != null) {
            for (var p : req.getProfiles()) {
                totalDocs += p.getTotalDocs();
                if (p.getTotalDocs() > topProfileDocs) { topProfileDocs = p.getTotalDocs(); topProfile = p.getProfileName(); }
                if (p.getYearData() != null)
                    for (var y : p.getYearData()) { totalCreated += y.getCreated(); totalMigrated += y.getMigrated(); }
                if (p.getGmpRows() != null)
                    for (var g : p.getGmpRows()) {
                        groupNames.add(g.getGmpGroup());
                        if (g.getTotal() > topGroupDocs) { topGroupDocs = g.getTotal(); topGroup = g.getGmpGroup(); }
                    }
            }
        }

        List<ReportRequest.YearData> agg = aggregateYears(req);
        int busiestYear = 0, busiestDocs = 0;
        for (var y : agg) if (y.getTotal() > busiestDocs) { busiestDocs = y.getTotal(); busiestYear = y.getYear(); }

        int splitBase = totalCreated + totalMigrated;
        int cPct = splitBase > 0 ? (int) Math.round(totalCreated * 100.0 / splitBase) : 0;
        int mPct = 100 - cPct;

        PdfPTable cards = new PdfPTable(4);
        cards.setWidthPercentage(100); cards.setSpacingBefore(14); cards.setSpacingAfter(18);
        addKpiCard(cards, "Total Documents",       num(totalDocs),                "in scope",           BRAND_BLUE);
        addKpiCard(cards, "Creation Profiles",      String.valueOf(req.getProfiles() != null ? req.getProfiles().size() : 0), "analysed", ACCENT_GREEN);
        addKpiCard(cards, "GMP Groups",             String.valueOf(groupNames.size()), "distinct groups", BRAND_INDIGO);
        
        // Blank space (" ") used to remove "selected period" subtitle
        addKpiCard(cards, "Year Range",             req.getYearFrom() + " – " + req.getYearTo(), " ", ACCENT_AMBER);
        
        addKpiCard(cards, "Created/Imported vs Migrated", (splitBase > 0 ? cPct + "% / " + mPct + "%" : "—"), "created/imported / migrated", BRAND_BLUE);
        
        addKpiCard(cards, "Top Creation Profile",   topProfile,     num(topProfileDocs) + " docs", ACCENT_GREEN);
        addKpiCard(cards, "Top GMP Group",          topGroup,       num(topGroupDocs) + " docs",   BRAND_INDIGO);
        addKpiCard(cards, "Busiest Year",           busiestYear > 0 ? String.valueOf(busiestYear) : "—", num(busiestDocs) + " docs", ACCENT_AMBER);
        doc.add(cards);

        addSubHeading(doc, "Key Observations");
        List<String> obs = new ArrayList<>();
        obs.add("A total of " + num(totalDocs) + " documents are in scope across " + (req.getProfiles() != null ? req.getProfiles().size() : 0) + " creation profiles for the selected period.");
        if (!"—".equals(topProfile)) obs.add("\"" + topProfile + "\" is the largest creation profile with " + num(topProfileDocs) + " documents.");
        if (!"—".equals(topGroup)) obs.add("The most active GMP group is \"" + topGroup + "\" with " + num(topGroupDocs) + " documents.");
        if (busiestYear > 0) obs.add("Document activity peaked in " + busiestYear + " with " + num(busiestDocs) + " documents.");
        renderBullets(doc, obs);
    }

    // ══════════════════════════════════════════════════════════════
    // 3. PROFILE-BY-PROFILE DEEP DIVE
    // ══════════════════════════════════════════════════════════════
    private void addProfileDeepDive(Document doc, ReportRequest.ProfileSummary profile, int sectionNum, ReportRequest req, PdfWriter writer, PdfChartRenderer charts) throws Exception {
        modernSectionHeader(doc, "Profile 0" + sectionNum, profile.getProfileName());

        String dt = req.getDocType() == null ? "BOTH" : req.getDocType();
        boolean showCreated  = !"MIGRATED".equalsIgnoreCase(dt);
        boolean showMigrated = !"CREATED".equalsIgnoreCase(dt);

        if (profile.getGmpRows() != null && !profile.getGmpRows().isEmpty()) {
            if (showCreated) {
                addYearWiseTable(doc, profile.getGmpRows(), req.getYearFrom(), req.getYearTo(), false, "1. Created / Imported – by GMP Group, Artifact & Year");
            }
            if (showMigrated) {
                addYearWiseTable(doc, profile.getGmpRows(), req.getYearFrom(), req.getYearTo(), true, "2. Migrated – by GMP Group, Artifact & Year");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // YEAR-WISE TABLE GENERATOR
    // ══════════════════════════════════════════════════════════════
    private void addYearWiseTable(Document doc, List<ReportRequest.GmpRow> rows, int yearFrom, int yearTo, boolean isMigrated, String title) throws Exception {
        Paragraph p = new Paragraph(title, fSubHeading);
        p.setSpacingBefore(12); p.setSpacingAfter(8);
        doc.add(p);

        int numYears = Math.max(1, yearTo - yearFrom + 1);
        int numCols = 3 + numYears; // Group, Subgroup, Artifact, [Years...]

        float[] widths = new float[numCols];
        widths[0] = 3.2f; // GMP Group 
        widths[1] = 2.7f; // GMP Subgroup 
        widths[2] = 3.3f; // GMP Artifact
        for (int i = 0; i < numYears; i++) {
            widths[3 + i] = 1.35f; // Wide enough to prevent 1000s wrapping
        }

        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(24);
        table.setHeaderRows(1);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Font.BOLD, Color.WHITE);

        // -- Render Table Headers --
        PdfPCell cG = new PdfPCell(new Phrase("GMP GROUP", headerFont));
        cG.setBackgroundColor(C_DARK_HDR); cG.setPadding(8); cG.setBorderColor(Color.WHITE);
        table.addCell(cG);
        
        PdfPCell cS = new PdfPCell(new Phrase("GMP SUBGROUP", headerFont));
        cS.setBackgroundColor(C_DARK_HDR); cS.setPadding(8); cS.setBorderColor(Color.WHITE);
        table.addCell(cS);

        PdfPCell cA = new PdfPCell(new Phrase("GMP ARTIFACT", headerFont));
        cA.setBackgroundColor(C_DARK_HDR); cA.setPadding(8); cA.setBorderColor(Color.WHITE);
        table.addCell(cA);

        for (int y = yearFrom; y <= yearTo; y++) {
            PdfPCell cY = new PdfPCell(new Phrase(String.valueOf(y), headerFont));
            cY.setBackgroundColor(C_DARK_HDR); 
            cY.setPaddingTop(8); cY.setPaddingBottom(8); cY.setPaddingLeft(2); cY.setPaddingRight(2);
            cY.setBorderColor(Color.WHITE); 
            cY.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cY);
        }

        // -- Check if there is actual data for this table type --
        boolean hasData = false;
        for (var row : rows) {
            if (row.getArtifacts() == null) continue;
            for (var art : row.getArtifacts()) {
                int artTotal = isMigrated ? art.getMigrated() : art.getCreated();
                if (artTotal > 0) {
                    hasData = true;
                    break;
                }
            }
            if (hasData) break;
        }

        // -- Empty "Zero State" Rendering --
        if (!hasData) {
            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setColspan(numCols);
            emptyCell.setBackgroundColor(Color.WHITE);
            emptyCell.setBorderColor(C_BORDER);
            emptyCell.setPaddingTop(40f);
            emptyCell.setPaddingBottom(40f);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            // Icon (Using text symbol since we lack image assets)
            Paragraph pIcon = new Paragraph("📦", FontFactory.getFont(FontFactory.HELVETICA, 24, Font.NORMAL, C_MUTED));
            pIcon.setAlignment(Element.ALIGN_CENTER);
            pIcon.setSpacingAfter(8f);
            emptyCell.addElement(pIcon);

            Paragraph pTitle = new Paragraph("No Data Available", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, C_DARK_HDR));
            pTitle.setAlignment(Element.ALIGN_CENTER);
            pTitle.setSpacingAfter(4f);
            emptyCell.addElement(pTitle);

            String msg = isMigrated ? "No migrated documents found for the selected filters and years."
                                    : "No created/imported documents found for the selected filters and years.";
            Paragraph pSub = new Paragraph(msg, FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Font.NORMAL, C_MUTED));
            pSub.setAlignment(Element.ALIGN_CENTER);
            emptyCell.addElement(pSub);

            table.addCell(emptyCell);
            doc.add(table);
            return; // Exit early, do not process rows
        }

        // -- Render Rows --
        for (var row : rows) {
            if (row.getArtifacts() == null || row.getArtifacts().isEmpty()) continue;

            // Group valid artifacts by Subgroup
            Map<String, List<ReportRequest.ArtifactRow>> subMap = new LinkedHashMap<>();
            for (var art : row.getArtifacts()) {
                int artTotal = isMigrated ? art.getMigrated() : art.getCreated();
                if (artTotal == 0) continue; 
                String subName = (art.getSubgroup() != null && !art.getSubgroup().isEmpty()) ? art.getSubgroup() : "General";
                subMap.computeIfAbsent(subName, k -> new ArrayList<>()).add(art);
            }

            int totalValidArtifacts = subMap.values().stream().mapToInt(List::size).sum();
            if (totalValidArtifacts == 0) continue;

            boolean firstInGroup = true;

            for (Map.Entry<String, List<ReportRequest.ArtifactRow>> entry : subMap.entrySet()) {
                String subName = entry.getKey();
                List<ReportRequest.ArtifactRow> subArtifacts = entry.getValue();
                boolean firstInSubgroup = true;

                for (var art : subArtifacts) {

                    // The spanning GMP GROUP column
                    if (firstInGroup) {
                        PdfPCell groupCell = new PdfPCell(new Phrase(row.getGmpGroup(), fTableHdr));
                        groupCell.setRowspan(totalValidArtifacts); 
                        groupCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        groupCell.setBackgroundColor(Color.WHITE);
                        groupCell.setBorderColor(C_BORDER);
                        groupCell.setPadding(8);
                        table.addCell(groupCell);
                        firstInGroup = false;
                    }
                    
                    // The spanning GMP SUBGROUP column
                    if (firstInSubgroup) {
                        PdfPCell subCell = new PdfPCell(new Phrase(subName, fTableCell));
                        subCell.setRowspan(subArtifacts.size());
                        subCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        subCell.setBackgroundColor(Color.WHITE);
                        subCell.setBorderColor(C_BORDER);
                        subCell.setPadding(8);
                        table.addCell(subCell);
                        firstInSubgroup = false;
                    }

                    // Artifact Name column
                    PdfPCell artCell = new PdfPCell(new Phrase(art.getArtifactName(), fTableCell));
                    artCell.setBackgroundColor(Color.WHITE); artCell.setBorderColor(C_BORDER); artCell.setPadding(6);
                    table.addCell(artCell);

                    // Yearly Data Columns - Pulls actual numbers from the frontend map
                    for (int i = 0; i < numYears; i++) {
                        int currentYear = yearFrom + i;
                        int yearVal = 0;
                        
                        if (isMigrated) {
                            yearVal = art.getYearlyMigrated() != null 
                                    ? art.getYearlyMigrated().getOrDefault(currentYear, 0) : 0;
                        } else {
                            yearVal = art.getYearlyCreated() != null 
                                    ? art.getYearlyCreated().getOrDefault(currentYear, 0) : 0;
                        }

                        PdfPCell yCell = new PdfPCell(new Phrase(num(yearVal), fTableCell));
                        yCell.setHorizontalAlignment(Element.ALIGN_CENTER); 
                        yCell.setBackgroundColor(Color.WHITE); 
                        yCell.setBorderColor(C_BORDER); 
                        yCell.setPadding(6);
                        table.addCell(yCell);
                    }
                }
            }
        }

        doc.add(table);
    }

    // ══════════════════════════════════════════════════════════════
    // 4. COMPARE SECTION (Appendix)
    // ══════════════════════════════════════════════════════════════
    private void addCompareSection(Document doc, ReportRequest.CompareSummary compare) throws Exception {
        modernSectionHeader(doc, "Benchmark", "Site Comparison Appendix");

        PdfPTable cards = new PdfPTable(3);
        cards.setWidthPercentage(100); cards.setSpacingBefore(14); cards.setSpacingAfter(18);
        addKpiCard(cards, "Total Documents", String.valueOf(compare.getOverallTotal()), "across all sites", BRAND_BLUE);
        addKpiCard(cards, "Top Site",         compare.getTopSite(), compare.getTopSiteDocs() + " docs", ACCENT_GREEN);
        addKpiCard(cards, "Year Range",        compare.getYearRange(),              "selected period",     ACCENT_AMBER);
        doc.add(cards);
    }

    // ══════════════════════════════════════════════════════════════
    // SHARED UTILITIES & HELPERS
    // ══════════════════════════════════════════════════════════════
    private List<ReportRequest.YearData> aggregateYears(ReportRequest req) {
        int from = req.getYearFrom(), to = req.getYearTo();
        if (to < from) { int t = from; from = to; to = t; }
        int n = Math.max(1, to - from + 1);
        int[] c = new int[n], m = new int[n], tot = new int[n];
        if (req.getProfiles() != null)
            for (var p : req.getProfiles())
                if (p.getYearData() != null)
                    for (var y : p.getYearData()) {
                        int idx = y.getYear() - from;
                        if (idx >= 0 && idx < n) { c[idx] += y.getCreated(); m[idx] += y.getMigrated(); tot[idx] += y.getTotal(); }
                    }
        List<ReportRequest.YearData> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ReportRequest.YearData y = new ReportRequest.YearData();
            y.setYear(from + i); y.setCreated(c[i]); y.setMigrated(m[i]); y.setTotal(tot[i]);
            out.add(y);
        }
        return out;
    }

    private void modernSectionHeader(Document doc, String eyebrow, String title) throws Exception {
        Paragraph eb = new Paragraph(eyebrow.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, BRAND_INDIGO));
        eb.setSpacingBefore(2); eb.setSpacingAfter(1); doc.add(eb);
        Paragraph tt = new Paragraph(title, fSectionTitle); tt.setSpacingAfter(5); doc.add(tt);
        doc.add(new LineSeparator(2f, 100, BRAND_INDIGO, Element.ALIGN_LEFT, -2));
        doc.add(new Paragraph(" "));
    }

    private void addSubHeading(Document doc, String text) throws Exception {
        Paragraph p = new Paragraph(text, fSubHeading); p.setSpacingBefore(12); p.setSpacingAfter(4); doc.add(p);
    }

    private void renderBullets(Document doc, List<String> bullets) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{0.35f, 9.65f});
        t.setWidthPercentage(100); t.setSpacingBefore(8); t.setSpacingAfter(10);
        for (String bullet : bullets) {
            PdfPCell iconCell = new PdfPCell(new Phrase("●", FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, BRAND_BLUE)));
            iconCell.setBorder(Rectangle.LEFT); iconCell.setBorderColor(BRAND_BLUE); iconCell.setBorderWidth(2.5f);
            iconCell.setBackgroundColor(C_OBS_BG); iconCell.setPadding(7); iconCell.setPaddingLeft(8); iconCell.setVerticalAlignment(Element.ALIGN_TOP);
            PdfPCell textCell = new PdfPCell(new Phrase(bullet, fObsBody));
            textCell.setBorder(Rectangle.NO_BORDER); textCell.setBackgroundColor(C_OBS_BG); textCell.setPadding(7); textCell.setPaddingLeft(6);
            t.addCell(iconCell); t.addCell(textCell);
        }
        doc.add(t);
    }

    private void addKpiCard(PdfPTable table, String label, String value, String sub, Color accent) {
        PdfPTable inner = new PdfPTable(1); inner.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        PdfPCell stripe = new PdfPCell(new Phrase(" ")); stripe.setFixedHeight(3f); stripe.setBorder(Rectangle.NO_BORDER); stripe.setBackgroundColor(accent); inner.addCell(stripe);
        PdfPCell lc = noBorderCell(label.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, C_MUTED), Element.ALIGN_LEFT); lc.setBackgroundColor(C_SURFACE); lc.setPadding(6); lc.setPaddingBottom(2); inner.addCell(lc);
        PdfPCell vc = noBorderCell(value, FontFactory.getFont(FontFactory.HELVETICA, value.length() < 10 ? 14f : 10f, Font.BOLD, accent), Element.ALIGN_LEFT); vc.setBackgroundColor(C_SURFACE); vc.setPadding(6); inner.addCell(vc);
        PdfPCell sc = noBorderCell(sub, FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, C_MUTED), Element.ALIGN_LEFT); sc.setBackgroundColor(C_SURFACE); sc.setPadding(6); inner.addCell(sc);
        PdfPCell wrapper = new PdfPCell(inner); wrapper.setBorderColor(C_BORDER); wrapper.setBorderWidth(0.5f); wrapper.setPadding(0); wrapper.setBackgroundColor(C_SURFACE);
        table.addCell(wrapper);
    }

    private PdfPCell noBorderCell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font)); c.setBorder(Rectangle.NO_BORDER); c.setHorizontalAlignment(align); return c;
    }

    private void roundRect(PdfContentByte cb, float x, float y, float w, float h, float r) {
        cb.moveTo(x + r, y); cb.lineTo(x + w - r, y); cb.curveTo(x + w, y, x + w, y, x + w, y + r); cb.lineTo(x + w, y + h - r); cb.curveTo(x + w, y + h, x + w, y + h, x + w - r, y + h); cb.lineTo(x + r, y + h); cb.curveTo(x, y + h, x, y + h, x, y + h - r); cb.lineTo(x, y + r); cb.curveTo(x, y, x, y, x + r, y); cb.closePath();
    }

    private void coverText(PdfContentByte cb, String text, float x, float y, int alignment, BaseFont font, float size, Color color) {
        cb.beginText();
        cb.setColorFill(color);
        cb.setFontAndSize(font, size);
        cb.showTextAligned(alignment, text, x, y, 0);
        cb.endText();
    }

    private void coverTile(PdfContentByte cb, float x, float y, float w, float h, String label, String value, BaseFont fontReg, BaseFont fontBold, Color accentColor) {
        cb.setColorFill(Color.WHITE); roundRect(cb, x, y, w, h, 6); cb.fill();
        cb.setColorStroke(C_BORDER); cb.setLineWidth(0.5f); roundRect(cb, x, y, w, h, 6); cb.stroke();
        cb.setColorFill(accentColor); roundRect(cb, x, y + h - 4, w, 4, 2); cb.fill();
        coverText(cb, label.toUpperCase(), x + 12, y + h - 22, Element.ALIGN_LEFT, fontReg, 8, C_MUTED);
        coverText(cb, value, x + 12, y + 16, Element.ALIGN_LEFT, fontBold, 14, C_TEXT);
    }

    private String num(int v) { return String.format("%,d", v); }

    private void initFonts() {
        fSectionTitle = FontFactory.getFont(FontFactory.HELVETICA,   14,   Font.BOLD,   C_TEXT);
        fSubHeading   = FontFactory.getFont(FontFactory.HELVETICA,   11,   Font.BOLD,   C_TEXT);
        fTableHdr     = FontFactory.getFont(FontFactory.HELVETICA,    9,   Font.BOLD,   C_TEXT);
        fTableCell    = FontFactory.getFont(FontFactory.HELVETICA,    9,   Font.NORMAL, C_CELL_TEXT);
        fObsBody      = FontFactory.getFont(FontFactory.HELVETICA,  9.5f,  Font.NORMAL, C_CELL_TEXT);
    }

    private static class HeaderFooterEvent extends PdfPageEventHelper {
        private final ReportRequest req;
        HeaderFooterEvent(ReportRequest req) { this.req = req; }
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (writer.getPageNumber() == 1) return;
            PdfContentByte cb = writer.getDirectContent();
            float W = document.getPageSize().getWidth(); float H = document.getPageSize().getHeight();
            try {
                BaseFont bf  = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                BaseFont bfb = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
                cb.setColorFill(new Color(248, 249, 252)); cb.rectangle(0, H - 42, W, 42); cb.fill();
                cb.setColorStroke(new Color(59, 115, 232, 100)); cb.setLineWidth(0.8f); cb.moveTo(40, H - 40); cb.lineTo(W - 40, H - 40); cb.stroke();
                
                cb.setColorFill(new Color(59, 115, 232)); cb.setFontAndSize(bfb, 8); cb.beginText(); cb.showTextAligned(Element.ALIGN_LEFT, "D2 Organic Growth Report", 42, H - 21, 0); cb.endText();
                
                cb.setColorFill(C_MUTED); cb.setFontAndSize(bf, 7.5f); cb.beginText(); cb.showTextAligned(Element.ALIGN_LEFT, req.getYearFrom() + "–" + req.getYearTo(), 42, H - 33, 0); cb.endText();
                String siteVal = (req.getOwningSite() != null && !req.getOwningSite().isEmpty()) ? req.getOwningSite() : "—";
                cb.setColorFill(C_MUTED); cb.setFontAndSize(bfb, 6.5f); cb.setCharacterSpacing(0.8f); cb.beginText(); cb.showTextAligned(Element.ALIGN_RIGHT, "SITE NAME", W - 42, H - 21, 0); cb.endText();
                cb.setCharacterSpacing(0f); cb.setColorFill(C_TEXT); cb.setFontAndSize(bfb, 9); cb.beginText(); cb.showTextAligned(Element.ALIGN_RIGHT, siteVal, W - 42, H - 33, 0); cb.endText();
                cb.setColorStroke(C_BORDER); cb.setLineWidth(0.5f); cb.moveTo(40, 34); cb.lineTo(W - 40, 34); cb.stroke();
                cb.setColorFill(C_MUTED); cb.setFontAndSize(bf, 8); cb.beginText(); cb.showTextAligned(Element.ALIGN_CENTER, "Page " + writer.getPageNumber(), W / 2, 24, 0); cb.endText();
            } catch (Exception ignored) {}
        }
    }
}