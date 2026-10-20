package com.example.excelviz.service;

import com.example.excelviz.model.ReportRequest.GmpRow;
import com.example.excelviz.model.ReportRequest.ProfileSummary;
import com.example.excelviz.model.ReportRequest.YearData;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws report charts as crisp vector graphics directly into the PDF.
 *
 * Nothing here is rasterised and nothing is captured from the browser — each
 * chart is built from the numeric data and returned as a vector {@link Image}
 * (a PDF form XObject) that flows in the document like a picture but stays
 * sharp at any zoom and matches the report's styling exactly.
 */
public class PdfChartRenderer {

    // ── Palette (kept in sync with ReportService) ──────────────────
    private static final Color CREATED  = new Color(59,  115, 232);   // blue
    private static final Color MIGRATED = new Color(139, 92,  246);   // violet
    private static final Color C_BORDER = new Color(221, 225, 236);
    private static final Color C_MUTED  = new Color(100, 116, 139);
    private static final Color C_TEXT   = new Color(15,  23,  42);
    private static final Color C_TRACK  = new Color(238, 241, 247);
    private static final Color C_AXIS   = new Color(176, 184, 199);
    private static final Color WHITE    = Color.WHITE;

    private static final Color[] PALETTE = {
            new Color(59, 115, 232), new Color(139, 92, 246), new Color(13, 148, 136),
            new Color(217, 119, 6),  new Color(225, 29,  72), new Color(5,  150, 105),
            new Color(100, 116, 139), new Color(8, 145, 178)
    };

    private static final float CHART_W = 515f;

    private final BaseFont bf;
    private final BaseFont bfb;

    public PdfChartRenderer() throws Exception {
        this.bf  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
        this.bfb = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
    }

    // ══════════════════════════════════════════════════════════════
    // 1. DOCUMENT ACTIVITY BY YEAR — vertical bars (stacked when BOTH)
    // ══════════════════════════════════════════════════════════════
    public Image yearActivity(PdfWriter writer, List<YearData> years, String docType) throws Exception {
        boolean stack = docType == null || "BOTH".equalsIgnoreCase(docType);
        float H = 232f;
        PdfTemplate t = writer.getDirectContent().createTemplate(CHART_W, H);

        float mL = 42, mR = 14, mT = stack ? 42 : 28, mB = 30;
        float px0 = mL, py0 = mB, px1 = CHART_W - mR, py1 = H - mT;
        float pw = px1 - px0, ph = py1 - py0;

        int max = 0;
        for (YearData y : years) max = Math.max(max, y.getTotal());
        int step     = niceCeil((int) Math.ceil(Math.max(1, max) / 4.0));
        int scaleMax = step * 4;

        // horizontal gridlines + Y labels
        t.setLineWidth(0.5f);
        for (int i = 0; i <= 4; i++) {
            float gy = py0 + ph * i / 4f;
            t.setColorStroke(C_BORDER);
            t.moveTo(px0, gy); t.lineTo(px1, gy); t.stroke();
            text(t, fmt(step * i), px0 - 5, gy - 2.5f, Element.ALIGN_RIGHT, bf, 7, C_MUTED);
        }

        // bars
        int n = years.size();
        float slot = pw / Math.max(1, n);
        float bw   = Math.min(48f, slot * 0.55f);
        for (int i = 0; i < n; i++) {
            YearData y = years.get(i);
            float cx = px0 + slot * i + slot / 2f;
            float bx = cx - bw / 2f;

            if (stack) {
                float ch = ph * y.getCreated()  / scaleMax;
                float mh = ph * y.getMigrated() / scaleMax;
                t.setColorFill(CREATED);  t.rectangle(bx, py0,      bw, ch); t.fill();
                t.setColorFill(MIGRATED); t.rectangle(bx, py0 + ch, bw, mh); t.fill();
            } else {
                float bh = ph * y.getTotal() / scaleMax;
                t.setColorFill(CREATED);  t.rectangle(bx, py0, bw, bh); t.fill();
            }

            if (y.getTotal() > 0) {
                float top = py0 + ph * y.getTotal() / scaleMax;
                text(t, fmt(y.getTotal()), cx, top + 4, Element.ALIGN_CENTER, bfb, 7, C_TEXT);
            }
            text(t, String.valueOf(y.getYear()), cx, py0 - 13, Element.ALIGN_CENTER, bf, 7.5f, C_MUTED);
        }

        // baseline
        t.setColorStroke(C_AXIS); t.setLineWidth(0.8f);
        t.moveTo(px0, py0); t.lineTo(px1, py0); t.stroke();

        if (stack) {
            float ly = H - 16;
            legendSwatch(t, px0, ly, CREATED, "Created / Import");
            float off = bf.getWidthPoint("Created / Import", 7.5f) + 13 + 22;
            legendSwatch(t, px0 + off, ly, MIGRATED, "Migrated");
        }

        return Image.getInstance(t);
    }

    // ══════════════════════════════════════════════════════════════
    // 2. GMP GROUP BREAKDOWN — horizontal bars, sorted, labelled
    // ══════════════════════════════════════════════════════════════
    public Image gmpBreakdown(PdfWriter writer, List<GmpRow> rowsIn) throws Exception {
        List<GmpRow> rows = new ArrayList<>(rowsIn);
        rows.sort(Comparator.comparingInt(GmpRow::getTotal).reversed());
        int show = Math.min(rows.size(), 8);

        float rowH = 25f, mT = 12f, mB = 8f;
        float H = mT + mB + show * rowH;
        PdfTemplate t = writer.getDirectContent().createTemplate(CHART_W, H);

        float labelW = 150f, valW = 88f;
        float bx0 = labelW, bx1 = CHART_W - valW, bw = bx1 - bx0;

        int max      = rows.stream().mapToInt(GmpRow::getTotal).max().orElse(1);
        int scaleMax = niceCeil(Math.max(1, max));
        int grand    = rows.stream().mapToInt(GmpRow::getTotal).sum();

        for (int i = 0; i < show; i++) {
            GmpRow r  = rows.get(i);
            float cy  = H - mT - rowH * i - rowH / 2f;     // vertical centre of the band
            float barY = cy - 7f, barH = 14f;

            String name = truncate(r.getGmpGroup(), 7.5f, labelW - 10);
            text(t, name, labelW - 10, cy - 2.5f, Element.ALIGN_RIGHT, bf, 7.5f, C_TEXT);

            // track + bar
            t.setColorFill(C_TRACK); t.rectangle(bx0, barY, bw, barH); t.fill();
            float w = Math.max(1.5f, bw * r.getTotal() / scaleMax);
            t.setColorFill(PALETTE[i % PALETTE.length]); t.rectangle(bx0, barY, w, barH); t.fill();

            double share = grand > 0 ? r.getTotal() * 100.0 / grand : 0;
            text(t, fmt(r.getTotal()) + "  (" + String.format("%.1f", share) + "%)",
                    bx1 + 6, cy - 2.5f, Element.ALIGN_LEFT, bfb, 7, C_TEXT);
        }
        return Image.getInstance(t);
    }

    // ══════════════════════════════════════════════════════════════
    // 8. GMP DISTRIBUTION — every GMP group (across all profiles),
    //    each bar's share of the overall document total.
    //    Same horizontal-bar style as gmpBreakdown but shows ALL groups.
    // ══════════════════════════════════════════════════════════════
    public Image gmpDistribution(PdfWriter writer, List<GmpRow> rowsIn) throws Exception {
        List<GmpRow> rows = new ArrayList<>(rowsIn);
        rows.sort(Comparator.comparingInt(GmpRow::getTotal).reversed());
        int show = Math.min(rows.size(), 16);          // include all groups (cap for page sanity)

        float rowH = 25f, mT = 12f, mB = 8f;
        float H = mT + mB + Math.max(1, show) * rowH;
        PdfTemplate t = writer.getDirectContent().createTemplate(CHART_W, H);

        float labelW = 170f, valW = 96f;
        float bx0 = labelW, bx1 = CHART_W - valW, bw = bx1 - bx0;

        int max      = rows.stream().mapToInt(GmpRow::getTotal).max().orElse(1);
        int scaleMax = niceCeil(Math.max(1, max));
        int grand    = rows.stream().mapToInt(GmpRow::getTotal).sum();

        for (int i = 0; i < show; i++) {
            GmpRow r  = rows.get(i);
            float cy  = H - mT - rowH * i - rowH / 2f;
            float barY = cy - 7f, barH = 14f;

            String name = truncate(r.getGmpGroup(), 7.5f, labelW - 10);
            text(t, name, labelW - 10, cy - 2.5f, Element.ALIGN_RIGHT, bf, 7.5f, C_TEXT);

            t.setColorFill(C_TRACK); t.rectangle(bx0, barY, bw, barH); t.fill();
            float w = Math.max(1.5f, bw * r.getTotal() / scaleMax);
            t.setColorFill(PALETTE[i % PALETTE.length]); t.rectangle(bx0, barY, w, barH); t.fill();

            double share = grand > 0 ? r.getTotal() * 100.0 / grand : 0;
            text(t, fmt(r.getTotal()) + "  (" + String.format("%.1f", share) + "%)",
                    bx1 + 6, cy - 2.5f, Element.ALIGN_LEFT, bfb, 7, C_TEXT);
        }
        return Image.getInstance(t);
    }

    // ══════════════════════════════════════════════════════════════
    // 3. CREATED vs MIGRATED — single 100% proportion bar
    // ══════════════════════════════════════════════════════════════
    public Image splitBar(PdfWriter writer, int created, int migrated) throws Exception {
        float H = 54f;
        PdfTemplate t = writer.getDirectContent().createTemplate(CHART_W, H);

        int total = created + migrated;
        if (total <= 0) total = 1;

        float mX = 2f, barY = 24f, barH = 20f;
        float bx0 = mX, bx1 = CHART_W - mX, bw = bx1 - bx0;
        float cw = bw * created / total;

        t.setColorFill(CREATED);  t.rectangle(bx0,      barY, cw,      barH); t.fill();
        t.setColorFill(MIGRATED); t.rectangle(bx0 + cw, barY, bw - cw, barH); t.fill();

        int cp = Math.round(created * 100f / total);
        int mp = 100 - cp;

        if (cw > 34)      text(t, cp + "%", bx0 + cw / 2f,        barY + 6.5f, Element.ALIGN_CENTER, bfb, 8, WHITE);
        if (bw - cw > 34) text(t, mp + "%", bx0 + cw + (bw - cw) / 2f, barY + 6.5f, Element.ALIGN_CENTER, bfb, 8, WHITE);

        text(t, "Created / Import: " + fmt(created) + " (" + cp + "%)",
                bx0, 8, Element.ALIGN_LEFT, bf, 8, CREATED);
        text(t, "Migrated: " + fmt(migrated) + " (" + mp + "%)",
                bx1, 8, Element.ALIGN_RIGHT, bf, 8, MIGRATED);

        return Image.getInstance(t);
    }

    // ══════════════════════════════════════════════════════════════
    // 4. SINGLE-METRIC TREND — one clean series (Created OR Migrated)
    //    A shared scaleMax lets the two charts be compared directly.
    // ══════════════════════════════════════════════════════════════
    public Image trendBars(PdfWriter writer, List<YearData> years,
                           boolean migrated, int scaleMaxIn) throws Exception {
        Color barColor = migrated ? MIGRATED : CREATED;
        float H = 188f;
        PdfTemplate t = writer.getDirectContent().createTemplate(CHART_W, H);

        float mL = 44, mR = 14, mT = 26, mB = 28;
        float px0 = mL, py0 = mB, px1 = CHART_W - mR, py1 = H - mT;
        float pw = px1 - px0, ph = py1 - py0;

        int max = scaleMaxIn > 0 ? scaleMaxIn
                : years.stream().mapToInt(y -> migrated ? y.getMigrated() : y.getCreated()).max().orElse(1);
        int step     = niceCeil((int) Math.ceil(Math.max(1, max) / 4.0));
        int scaleMax = step * 4;

        t.setLineWidth(0.5f);
        for (int i = 0; i <= 4; i++) {
            float gy = py0 + ph * i / 4f;
            t.setColorStroke(C_BORDER);
            t.moveTo(px0, gy); t.lineTo(px1, gy); t.stroke();
            text(t, fmt(step * i), px0 - 5, gy - 2.5f, Element.ALIGN_RIGHT, bf, 7, C_MUTED);
        }

        int n = years.size();
        float slot = pw / Math.max(1, n);
        float bw   = Math.min(46f, slot * 0.5f);
        for (int i = 0; i < n; i++) {
            YearData y = years.get(i);
            int v = migrated ? y.getMigrated() : y.getCreated();
            float cx = px0 + slot * i + slot / 2f;
            float bh = ph * v / scaleMax;
            t.setColorFill(barColor);
            t.rectangle(cx - bw / 2f, py0, bw, bh); t.fill();
            if (v > 0) text(t, fmt(v), cx, py0 + bh + 4, Element.ALIGN_CENTER, bfb, 7, C_TEXT);
            text(t, String.valueOf(y.getYear()), cx, py0 - 13, Element.ALIGN_CENTER, bf, 7.5f, C_MUTED);
        }

        t.setColorStroke(C_AXIS); t.setLineWidth(0.8f);
        t.moveTo(px0, py0); t.lineTo(px1, py0); t.stroke();
        return Image.getInstance(t);
    }

    // ══════════════════════════════════════════════════════════════
    // 5. CREATED vs MIGRATED — donut with side legend
    // ══════════════════════════════════════════════════════════════
    public Image donut(PdfWriter writer, int created, int migrated) throws Exception {
        float H = 150f;
        PdfTemplate t = writer.getDirectContent().createTemplate(CHART_W, H);

        int total = created + migrated;
        if (total <= 0) total = 1;

        float cx = 92, cy = H / 2f, ro = 56, ri = 33;
        double aCreated = 360.0 * created / total;

        fillRing(t, cx, cy, ri, ro, 90,            90 - aCreated, CREATED);
        fillRing(t, cx, cy, ri, ro, 90 - aCreated, 90 - 360,      MIGRATED);

        text(t, fmt(total),   cx, cy + 2,  Element.ALIGN_CENTER, bfb, 13, C_TEXT);
        text(t, "documents",  cx, cy - 11, Element.ALIGN_CENTER, bf,  7,  C_MUTED);

        int cp = Math.round(created * 100f / total);
        float lx = 196;
        legendRow(t, lx, cy + 18, CREATED,  "Created / Import", fmt(created)  + "  (" + cp + "%)");
        legendRow(t, lx, cy - 22, MIGRATED, "Migrated",         fmt(migrated) + "  (" + (100 - cp) + "%)");
        return Image.getInstance(t);
    }

    private void legendRow(PdfTemplate t, float x, float y, Color c, String title, String sub) {
        t.setColorFill(c);
        t.circle(x + 4, y + 6, 4.5f);
        t.fill();
        text(t, title, x + 16, y + 3, Element.ALIGN_LEFT, bfb, 9.5f, C_TEXT);
        text(t, sub,   x + 16, y - 10, Element.ALIGN_LEFT, bf, 8.5f, C_MUTED);
    }

    /** Fills an annular sector (ring slice) from a0° to a1° using a polygon path. */
    private void fillRing(PdfTemplate t, float cx, float cy, float ri, float ro,
                          double a0, double a1, Color c) {
        t.setColorFill(c);
        int steps = Math.max(2, (int) Math.ceil(Math.abs(a1 - a0) / 3.0));
        double da = (a1 - a0) / steps;
        double s0 = Math.toRadians(a0);
        t.moveTo(cx + (float)(ro * Math.cos(s0)), cy + (float)(ro * Math.sin(s0)));
        for (int i = 1; i <= steps; i++) {
            double a = Math.toRadians(a0 + da * i);
            t.lineTo(cx + (float)(ro * Math.cos(a)), cy + (float)(ro * Math.sin(a)));
        }
        for (int i = steps; i >= 0; i--) {
            double a = Math.toRadians(a0 + da * i);
            t.lineTo(cx + (float)(ri * Math.cos(a)), cy + (float)(ri * Math.sin(a)));
        }
        t.closePath();
        t.fill();
    }

    // ══════════════════════════════════════════════════════════════
    // 6. CREATION PROFILE SPLIT — grouped bars (Created vs Migrated)
    //    one pair of bars per Creation Profile
    // ══════════════════════════════════════════════════════════════
    public Image profileSplitBars(PdfWriter writer, List<ProfileSummary> profiles,
                                  boolean showCreated, boolean showMigrated) throws Exception {
        boolean both = showCreated && showMigrated;
        float H = 244f;
        PdfTemplate t = writer.getDirectContent().createTemplate(CHART_W, H);

        float mL = 44, mR = 14, mT = 42, mB = 40;
        float px0 = mL, py0 = mB, px1 = CHART_W - mR, py1 = H - mT;
        float pw = px1 - px0, ph = py1 - py0;

        int n = profiles.size();
        int[] created = new int[n], migrated = new int[n];
        int max = 1;
        for (int i = 0; i < n; i++) {
            int c = 0, m = 0;
            if (profiles.get(i).getYearData() != null) {
                for (YearData y : profiles.get(i).getYearData()) { c += y.getCreated(); m += y.getMigrated(); }
            }
            created[i] = c; migrated[i] = m;
            if (showCreated)  max = Math.max(max, c);
            if (showMigrated) max = Math.max(max, m);
        }
        int step     = niceCeil((int) Math.ceil(max / 4.0));
        int scaleMax = step * 4;

        // gridlines + Y labels
        t.setLineWidth(0.5f);
        for (int i = 0; i <= 4; i++) {
            float gy = py0 + ph * i / 4f;
            t.setColorStroke(C_BORDER);
            t.moveTo(px0, gy); t.lineTo(px1, gy); t.stroke();
            text(t, fmt(step * i), px0 - 5, gy - 2.5f, Element.ALIGN_RIGHT, bf, 7, C_MUTED);
        }

        float slot = pw / Math.max(1, n);
        float bw   = Math.min(34f, slot * (both ? 0.28f : 0.36f));
        float gap  = 4f;
        for (int i = 0; i < n; i++) {
            float cx = px0 + slot * i + slot / 2f;
            if (both) {
                float cBarX = cx - bw - gap / 2f, mBarX = cx + gap / 2f;
                float ch = ph * created[i] / scaleMax, mh = ph * migrated[i] / scaleMax;
                t.setColorFill(CREATED);  t.rectangle(cBarX, py0, bw, ch); t.fill();
                t.setColorFill(MIGRATED); t.rectangle(mBarX, py0, bw, mh); t.fill();
                if (created[i]  > 0) text(t, fmt(created[i]),  cBarX + bw / 2f, py0 + ch + 4, Element.ALIGN_CENTER, bfb, 6.5f, C_TEXT);
                if (migrated[i] > 0) text(t, fmt(migrated[i]), mBarX + bw / 2f, py0 + mh + 4, Element.ALIGN_CENTER, bfb, 6.5f, C_TEXT);
            } else {
                int   v  = showCreated ? created[i] : migrated[i];
                Color cc = showCreated ? CREATED   : MIGRATED;
                float bh = ph * v / scaleMax;
                t.setColorFill(cc); t.rectangle(cx - bw / 2f, py0, bw, bh); t.fill();
                if (v > 0) text(t, fmt(v), cx, py0 + bh + 4, Element.ALIGN_CENTER, bfb, 6.5f, C_TEXT);
            }
            String name = truncate(profiles.get(i).getProfileName(), 7, slot - 6);
            text(t, name, cx, py0 - 13, Element.ALIGN_CENTER, bf, 7, C_MUTED);
        }

        t.setColorStroke(C_AXIS); t.setLineWidth(0.8f);
        t.moveTo(px0, py0); t.lineTo(px1, py0); t.stroke();

        float ly = H - 16;
        if (both) {
            legendSwatch(t, px0, ly, CREATED, "Created / Import");
            float off = bf.getWidthPoint("Created / Import", 7.5f) + 13 + 22;
            legendSwatch(t, px0 + off, ly, MIGRATED, "Migrated");
        } else if (showCreated) {
            legendSwatch(t, px0, ly, CREATED, "Created / Import");
        } else {
            legendSwatch(t, px0, ly, MIGRATED, "Migrated");
        }

        return Image.getInstance(t);
    }

    // ══════════════════════════════════════════════════════════════
    // 7. GMP GROUP SPLIT — grouped bars (Created vs Migrated) per group
    //    one cluster of bars per GMP group, with document-count labels
    // ══════════════════════════════════════════════════════════════
    public Image gmpGroupedBars(PdfWriter writer, List<GmpRow> rows,
                                boolean showCreated, boolean showMigrated) throws Exception {
        boolean both = showCreated && showMigrated;
        float H = 272f;
        PdfTemplate t = writer.getDirectContent().createTemplate(CHART_W, H);

        float mL = 44, mR = 14, mT = 42, mB = 68;
        float px0 = mL, py0 = mB, px1 = CHART_W - mR, py1 = H - mT;
        float pw = px1 - px0, ph = py1 - py0;

        int n = rows.size();
        int[] created = new int[n], migrated = new int[n];
        int max = 1;
        for (int i = 0; i < n; i++) {
            created[i]  = rows.get(i).getCreated();
            migrated[i] = rows.get(i).getMigrated();
            if (showCreated)  max = Math.max(max, created[i]);
            if (showMigrated) max = Math.max(max, migrated[i]);
        }
        int step     = niceCeil((int) Math.ceil(max / 4.0));
        int scaleMax = step * 4;

        // gridlines + Y labels
        t.setLineWidth(0.5f);
        for (int i = 0; i <= 4; i++) {
            float gy = py0 + ph * i / 4f;
            t.setColorStroke(C_BORDER);
            t.moveTo(px0, gy); t.lineTo(px1, gy); t.stroke();
            text(t, fmt(step * i), px0 - 5, gy - 2.5f, Element.ALIGN_RIGHT, bf, 7, C_MUTED);
        }

        float slot = pw / Math.max(1, n);
        float bw   = Math.min(34f, slot * (both ? 0.28f : 0.36f));
        float gap  = 4f;
        for (int i = 0; i < n; i++) {
            float cx = px0 + slot * i + slot / 2f;
            if (both) {
                float cBarX = cx - bw - gap / 2f, mBarX = cx + gap / 2f;
                float ch = ph * created[i] / scaleMax, mh = ph * migrated[i] / scaleMax;
                t.setColorFill(CREATED);  t.rectangle(cBarX, py0, bw, ch); t.fill();
                t.setColorFill(MIGRATED); t.rectangle(mBarX, py0, bw, mh); t.fill();
                if (created[i]  > 0) text(t, fmt(created[i]),  cBarX + bw / 2f, py0 + ch + 4, Element.ALIGN_CENTER, bfb, 6.5f, C_TEXT);
                if (migrated[i] > 0) text(t, fmt(migrated[i]), mBarX + bw / 2f, py0 + mh + 4, Element.ALIGN_CENTER, bfb, 6.5f, C_TEXT);
            } else {
                int   v  = showCreated ? created[i] : migrated[i];
                Color cc = showCreated ? CREATED   : MIGRATED;
                float bh = ph * v / scaleMax;
                t.setColorFill(cc); t.rectangle(cx - bw / 2f, py0, bw, bh); t.fill();
                if (v > 0) text(t, fmt(v), cx, py0 + bh + 4, Element.ALIGN_CENTER, bfb, 6.5f, C_TEXT);
            }
            String name = truncate(rows.get(i).getGmpGroup(), 6.8f, 92f);
            text(t, name, cx + 2, py0 - 7, Element.ALIGN_RIGHT, bf, 6.8f, C_MUTED, 35);
        }

        t.setColorStroke(C_AXIS); t.setLineWidth(0.8f);
        t.moveTo(px0, py0); t.lineTo(px1, py0); t.stroke();

        float ly = H - 16;
        if (both) {
            legendSwatch(t, px0, ly, CREATED, "Created / Import");
            float off = bf.getWidthPoint("Created / Import", 7.5f) + 13 + 22;
            legendSwatch(t, px0 + off, ly, MIGRATED, "Migrated");
        } else if (showCreated) {
            legendSwatch(t, px0, ly, CREATED, "Created / Import");
        } else {
            legendSwatch(t, px0, ly, MIGRATED, "Migrated");
        }

        return Image.getInstance(t);
    }

    // ── Helpers ─────────────────────────────────────────────────────
    private void legendSwatch(PdfTemplate t, float x, float y, Color c, String label) {
        t.setColorFill(c);
        t.rectangle(x, y, 9, 9);
        t.fill();
        text(t, label, x + 13, y + 1.5f, Element.ALIGN_LEFT, bf, 7.5f, C_MUTED);
    }

    private void text(PdfTemplate t, String s, float x, float y, int align,
                      BaseFont font, float size, Color color) {
        t.beginText();
        t.setColorFill(color);
        t.setFontAndSize(font, size);
        t.showTextAligned(align, s, x, y, 0);
        t.endText();
    }

    private void text(PdfTemplate t, String s, float x, float y, int align,
                      BaseFont font, float size, Color color, float rotation) {
        t.beginText();
        t.setColorFill(color);
        t.setFontAndSize(font, size);
        t.showTextAligned(align, s, x, y, rotation);
        t.endText();
    }

    private String truncate(String s, float size, float maxW) {
        if (s == null) return "";
        if (bf.getWidthPoint(s, size) <= maxW) return s;
        String out = s;
        while (out.length() > 1 && bf.getWidthPoint(out + "\u2026", size) > maxW)
            out = out.substring(0, out.length() - 1);
        return out + "\u2026";
    }

    /** Smallest 1/2/5 × 10ⁿ value that is ≥ v, for clean axis scales. */
    private int niceCeil(int v) {
        if (v <= 1) return 1;
        int[] steps = {1, 2, 5};
        int pow = 1;
        while (true) {
            for (int s : steps) {
                int cand = s * pow;
                if (cand >= v) return cand;
            }
            pow *= 10;
        }
    }

    private String fmt(int v) {
        return String.format("%,d", v);
    }
}