# D2 Organic Growth Report — Summary of Work Done & Technical Rationale

**Project:** D2 Organic Growth Report (Documentum D2 Life Sciences eDMS Analytics)  
**Status:** Fully Runnable, Verified, Live on `http://localhost:8081`  

---

## 📌 Summary of Architecture & Implemented Work

| Area | Description & Actions Taken | Rationale & Benefit |
|---|---|---|
| **1. Project Structure & Portability** | Consolidated Maven build files (`pom.xml`, `mvnw`, `.mvn/`) and source code at the root. | The repository is directly recognised by VS Code, IntelliJ, Eclipse, and CI/CD pipelines without needing nested subdirectories. |
| **2. Spring Boot Configuration** | Configured `application.yml` with 50MB multipart file limits and structured logging. | Allows seamless processing of large multi-sheet Excel files up to 50MB without file size truncation errors. |
| **3. Clean Asset Exception Handling** | Added dedicated exception handler in `GlobalExceptionHandler.java` returning a 404 for missing static browser resources (e.g. `/favicon.ico`). | Prevents console logs from being spammed with unnecessary internal 500 error stack traces. |
| **4. User-Driven Data Ingestion** | The application starts in a clean state awaiting user upload. Upon selecting and uploading an Excel workbook, the backend categorizes Created vs Migrated counts, updates dropdown filters, and shows confirmed site loading counts. | Gives complete user control over data loading and session management. |
| **5. Growth Analytics Engine** | Built `GrowthAnalyticsService.java` and `/api/growth/summary` endpoint. Dynamically filters out divested sites, computes baseline and current volumes, overall portfolio expansion %, latest YoY rate, and CAGR. | Delivers executive-level growth intelligence beyond raw document counts. |
| **6. Dynamic Growth Analytics Dashboard** | Added the **`📈 Growth Analytics`** tab in `index.html` with 4 top KPI cards, YoY document growth curve with volume and YoY % badges, leadership narrative callout, active site matrix, and Created vs Migrated toggles. | Fully dynamic view without hardcoded dates; automatically adapts to the calendar years present in the uploaded workbook. |
| **7. Multi-Site Comparison & PDF Export** | Integrated multi-site comparison with comparative summary cards and automated PDF report generation (`POST /api/report/generate`). | Enables plant-level cross-site benchmarking and downloadable executive reports. |
| **8. Automated Testing** | End-to-end unit and integration smoke tests in `ExcelvizApplicationTests.java` covering controllers, services, cache lifecycle, and metadata safety. | 100% test pass rate ensuring zero regressions. |

---

## 🚀 How to Run the Application

```cmd
# Run using the Maven Wrapper:
.\mvnw.cmd spring-boot:run
```

**Browser URL:** `http://localhost:8081`
