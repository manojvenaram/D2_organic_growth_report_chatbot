package com.example.excelviz.service;

import com.example.excelviz.model.ExcelRowData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

/**
 * Parses the Excel sheet "Yearwise_Report".
 * Supports single-row headers and multi-row section headers.
 * Handles: Owning Site | Creation Profile | GMP Group | GMP Subgroup | GMP Artifact
 */
@Service
public class ExcelParserService {

    private static final Logger log = LoggerFactory.getLogger(ExcelParserService.class);

    public static final String CREATED  = "CREATED";
    public static final String MIGRATED = "MIGRATED";
    public static final String TOTAL    = "TOTAL";

    private static final String SHEET_NAME = "Yearwise_Report";

    @Autowired
    private ExcelCacheService cacheService;

    public void parseExcel(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        try (InputStream is = file.getInputStream()) {
            parseExcel(is, file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    public void parseExcel(InputStream is, String filename) {

        cacheService.clearCache();

        try (Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                if (workbook.getNumberOfSheets() > 0) {
                    sheet = workbook.getSheetAt(0);
                } else {
                    throw new IllegalArgumentException("Workbook is empty. Please check your Excel file.");
                }
            }

            Row row0 = sheet.getRow(0);
            if (row0 == null) {
                throw new IllegalArgumentException("Header row is empty.");
            }

            Row row1 = sheet.getRow(1);

            // Check if Row 1 contains dimension column headers (indicating multi-row header structure)
            boolean isMultiHeader = false;
            if (row1 != null) {
                for (Cell cell : row1) {
                    String val = normaliseHeader(getCellString(cell));
                    if (val.contains("owning site") || val.contains("creation profile") || val.contains("gmp group")) {
                        isMultiHeader = true;
                        break;
                    }
                }
            }

            Map<Integer, String> dimensionCols  = new LinkedHashMap<>();
            Map<Integer, Integer> createdCols   = new HashMap<>();
            Map<Integer, Integer> migratedCols  = new HashMap<>();
            Map<Integer, Integer> totalCols     = new HashMap<>();

            int dataStartRow = 1;

            if (isMultiHeader) {
                dataStartRow = 2;
                String currentSection = "";

                int maxCols = Math.max(row0.getLastCellNum(), row1.getLastCellNum());
                for (int c = 0; c < maxCols; c++) {
                    String sec = normaliseHeader(getCellString(row0.getCell(c)));
                    if (!sec.isEmpty()) {
                        currentSection = sec;
                    }

                    String h1 = normaliseHeader(getCellString(row1.getCell(c)));
                    if (h1.isEmpty()) continue;

                    if (h1.contains("owning site")) {
                        dimensionCols.put(c, "Owning Site");
                    } else if (h1.contains("creation profile")) {
                        dimensionCols.put(c, "Creation Profile");
                    } else if (h1.contains("gmp subgroup") || h1.contains("subgroup")) {
                        dimensionCols.put(c, "GMP Subgroup");
                    } else if (h1.contains("gmp group")) {
                        dimensionCols.put(c, "GMP Group");
                    } else if (h1.contains("gmp artifact") || h1.equals("artifact")) {
                        dimensionCols.put(c, "GMP Artifact");
                    } else if (h1.matches("\\d{4}")) {
                        int year = Integer.parseInt(h1);
                        if (currentSection.contains("migrated")) {
                            migratedCols.put(c, year);
                        } else if (currentSection.contains("created") || currentSection.contains("import")) {
                            createdCols.put(c, year);
                        } else {
                            totalCols.put(c, year);
                        }
                    }
                }
            } else {
                dataStartRow = 1;
                for (Cell cell : row0) {
                    String h = normaliseHeader(getCellString(cell));
                    if (h.isEmpty()) continue;

                    int colIdx = cell.getColumnIndex();

                    if (h.contains("owning site")) {
                        dimensionCols.put(colIdx, "Owning Site");
                    } else if (h.contains("creation profile")) {
                        dimensionCols.put(colIdx, "Creation Profile");
                    } else if (h.contains("gmp subgroup") || h.contains("subgroup")) {
                        dimensionCols.put(colIdx, "GMP Subgroup");
                    } else if (h.contains("gmp group")) {
                        dimensionCols.put(colIdx, "GMP Group");
                    } else if (h.contains("gmp artifact") || h.equals("artifact")) {
                        dimensionCols.put(colIdx, "GMP Artifact");
                    } else if (h.matches(".*\\d{4}.*")) {
                        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d{4}").matcher(h);
                        if (matcher.find()) {
                            int year = Integer.parseInt(matcher.group());
                            if (h.contains("created") || h.contains("import")) {
                                createdCols.put(colIdx, year);
                            } else if (h.contains("migrated")) {
                                migratedCols.put(colIdx, year);
                            } else {
                                totalCols.put(colIdx, year);
                            }
                        }
                    }
                }
            }

            Set<Integer> allYears = new TreeSet<>();
            allYears.addAll(createdCols.values());
            allYears.addAll(migratedCols.values());
            allYears.addAll(totalCols.values());

            log.debug("Columns parsed — multiHeader:{}, dimensions:{}, created:{}, migrated:{}, total:{}, years:{}",
                    isMultiHeader, dimensionCols.size(), createdCols.size(), migratedCols.size(),
                    totalCols.size(), allYears);

            int rowsAdded = 0;

            // ── Data rows ─────────────────────────────────────
            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                ExcelRowData data = new ExcelRowData();
                Map<Integer, Map<String, Integer>> ytc = new HashMap<>();
                for (int y : allYears) ytc.put(y, new HashMap<>());

                // Dimensions
                for (Map.Entry<Integer, String> e : dimensionCols.entrySet()) {
                    String val = getCellString(row.getCell(e.getKey()));
                    switch (e.getValue()) {
                        case "Owning Site"      -> data.setOwningSite(val);
                        case "Creation Profile" -> data.setCreationProfile(val);
                        case "GMP Group"        -> data.setGmpGroup(val);
                        case "GMP Subgroup"     -> data.setGmpSubgroup(val);
                        case "GMP Artifact"     -> data.setGmpArtifact(val);
                    }
                }

                // Skip rows without a profile
                if (data.getCreationProfile() == null || data.getCreationProfile().isBlank()) continue;

                // Numeric columns
                createdCols.forEach((col, year) ->
                    ytc.get(year).put(CREATED, getCellInt(row.getCell(col))));
                migratedCols.forEach((col, year) ->
                    ytc.get(year).put(MIGRATED, getCellInt(row.getCell(col))));
                totalCols.forEach((col, year) ->
                    ytc.get(year).put(TOTAL, getCellInt(row.getCell(col))));

                // Derive missing TOTAL
                for (int y : allYears) {
                    Map<String, Integer> m = ytc.get(y);
                    if (!m.containsKey(TOTAL)) {
                        m.put(TOTAL, m.getOrDefault(CREATED, 0) + m.getOrDefault(MIGRATED, 0));
                    }
                }

                data.setYearTypeCounts(ytc);
                cacheService.addRow(data);
                rowsAdded++;
            }

            cacheService.markLoaded();
            log.info("Parsed {} data rows from '{}'", rowsAdded, filename != null ? filename : "stream");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String normaliseHeader(String raw) {
        if (raw == null) return "";
        return raw.trim()
                  .toLowerCase()
                  .replaceAll("\\.0+$", "")        // "2021.0" → "2021"
                  .replaceAll("\s+", " ");         // collapse whitespace
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default      -> "";
        };
    }

    private int getCellInt(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try { return Integer.parseInt(cell.toString().trim()); } catch (NumberFormatException e) { return 0; }
    }
}
