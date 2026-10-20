/* =====================================================================
 * growth.js — Executive Summary & Document Growth Intelligence View
 * Renders the exact 2021–2026 Executive Slide layout:
 * - 4 Top KPI Cards
 * - Year-over-Year Document Growth Curve with point labels & YoY% arrows
 * - Leadership Insight Callout Card
 * - Active Site Growth Overview Matrix (excludes divested sites)
 * ===================================================================== */

var execChartInstance = null;
var currentExecDocType = "CREATED";

function setExecDocType(type) {
    currentExecDocType = type;
    document.querySelectorAll(".exec-doc-pill").forEach(p => {
        p.classList.toggle("active", p.dataset.type === type);
    });
    loadExecutiveSummary();
}

function loadExecutiveSummary() {
    const spinner = document.getElementById("spinner");
    if (spinner) spinner.classList.add("on");

    fetch(`/api/growth/summary?docType=${encodeURIComponent(currentExecDocType)}`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to load growth summary");
            return res.json();
        })
        .then(data => {
            renderExecutiveSlide(data);
        })
        .catch(err => {
            console.error("Growth summary error:", err);
            renderExecutiveSlide(null);
        })
        .finally(() => {
            if (spinner) spinner.classList.remove("on");
        });
}

window.loadGrowthAnalytics = loadExecutiveSummary;

function renderExecutiveSlide(data) {
    const emptyEl = document.getElementById("execEmptyState");
    const contentEl = document.getElementById("execContentContainer");

    if (!data || !data.yearlyPoints || data.yearlyPoints.length === 0) {
        if (emptyEl) emptyEl.style.display = "block";
        if (contentEl) contentEl.style.display = "none";
        return;
    }

    if (emptyEl) emptyEl.style.display = "none";
    if (contentEl) contentEl.style.display = "flex";

    // 1. KPI Cards
    const kpiBase = document.getElementById("execKpiBase");
    const kpiCurr = document.getElementById("execKpiCurr");
    const kpiOverall = document.getElementById("execKpiOverall");
    const kpiYoy = document.getElementById("execKpiYoy");

    if (kpiBase) kpiBase.textContent = Number(data.baselineVolume).toLocaleString();
    if (kpiCurr) kpiCurr.textContent = Number(data.currentVolume).toLocaleString();

    if (kpiOverall) {
        const sign = data.overallGrowthPct >= 0 ? "+" : "";
        kpiOverall.textContent = `${sign}${data.overallGrowthPct.toFixed(1)}%`;
    }
    if (kpiYoy) {
        const sign = data.latestYoyGrowthPct >= 0 ? "+" : "";
        kpiYoy.textContent = `${sign}${data.latestYoyGrowthPct.toFixed(1)}%`;
    }

    const baseLbl = document.getElementById("execKpiBaseLabel");
    const currLbl = document.getElementById("execKpiCurrLabel");
    if (baseLbl) baseLbl.textContent = `${data.baselineYear} Baseline`;
    if (currLbl) currLbl.textContent = `${data.currentYear} Volume`;

    // 2. Leadership Insights
    const headlineEl = document.getElementById("execHeadline");
    const subheadlineEl = document.getElementById("execSubheadline");
    const bulletsEl = document.getElementById("execBullets");

    if (headlineEl) headlineEl.textContent = data.headline || "";
    if (subheadlineEl) subheadlineEl.textContent = data.subheadline || "";

    if (bulletsEl && data.bulletInsights) {
        bulletsEl.innerHTML = data.bulletInsights.map((b, i) => {
            const icons = ["📊", "🚀", "📈", "🛡️"];
            return `<li style="display:flex;align-items:flex-start;gap:8px;margin-bottom:8px;font-size:12px;color:var(--text);line-height:1.4;">
                <span style="font-size:14px;flex-shrink:0;">${icons[i % icons.length]}</span>
                <span>${b}</span>
            </li>`;
        }).join("");
    }

    // 3. Render YoY Chart
    renderExecChart(data.yearlyPoints);

    // 4. Render Active Sites Table
    renderExecTable(data);
}

function renderExecChart(points) {
    const canvas = document.getElementById("execChartCanvas");
    if (!canvas) return;

    if (execChartInstance) {
        execChartInstance.destroy();
        execChartInstance = null;
    }

    const ctx = canvas.getContext("2d");
    const isDark = !document.body.classList.contains("light");

    const labels = points.map(p => String(p.year));
    const dataVals = points.map(p => p.volume);

    // Custom plugin to draw volume labels and YoY badges above points
    const customLabelsPlugin = {
        id: "customLabelsPlugin",
        afterDatasetsDraw(chart) {
            const chartCtx = chart.ctx;
            const meta = chart.getDatasetMeta(0);

            chartCtx.save();
            chartCtx.font = "bold 11px Inter, sans-serif";
            chartCtx.textAlign = "center";
            chartCtx.textBaseline = "bottom";

            meta.data.forEach((element, index) => {
                const { x, y } = element;
                const pt = points[index];

                // Draw volume label
                chartCtx.fillStyle = isDark ? "#ffffff" : "#0f172a";
                chartCtx.fillText(pt.volume.toLocaleString(), x, y - 18);

                // Draw YoY arrow and % badge
                if (index > 0 && pt.yoyGrowthPct !== null) {
                    const prevElement = meta.data[index - 1];
                    const midX = (prevElement.x + x) / 2;
                    const midY = (prevElement.y + y) / 2 - 12;

                    const sign = pt.yoyGrowthPct >= 0 ? "+" : "";
                    const badgeText = `↗ ${sign}${pt.yoyGrowthPct.toFixed(1)}%`;

                    chartCtx.font = "bold 9.5px Inter, sans-serif";
                    const badgeW = chartCtx.measureText(badgeText).width + 8;
                    const badgeH = 16;

                    // Badge pill
                    chartCtx.fillStyle = pt.yoyGrowthPct >= 0
                        ? (isDark ? "rgba(16,185,129,0.25)" : "rgba(5,150,105,0.18)")
                        : (isDark ? "rgba(244,63,94,0.25)" : "rgba(225,29,72,0.18)");
                    chartCtx.strokeStyle = pt.yoyGrowthPct >= 0 ? "#10b981" : "#f43f5e";
                    chartCtx.lineWidth = 1;

                    roundRect(chartCtx, midX - badgeW / 2, midY - badgeH + 2, badgeW, badgeH, 4);
                    chartCtx.fill();
                    chartCtx.stroke();

                    // Text
                    chartCtx.fillStyle = pt.yoyGrowthPct >= 0 ? "#10b981" : "#f43f5e";
                    chartCtx.fillText(badgeText, midX, midY);
                }
            });
            chartCtx.restore();
        }
    };

    const gradient = ctx.createLinearGradient(0, 0, 0, canvas.height || 260);
    if (isDark) {
        gradient.addColorStop(0, "rgba(59, 130, 246, 0.45)");
        gradient.addColorStop(1, "rgba(59, 130, 246, 0.02)");
    } else {
        gradient.addColorStop(0, "rgba(37, 99, 235, 0.35)");
        gradient.addColorStop(1, "rgba(37, 99, 235, 0.02)");
    }

    execChartInstance = new Chart(ctx, {
        type: "line",
        data: {
            labels: labels,
            datasets: [{
                label: "Document Volume",
                data: dataVals,
                borderColor: "#0284c7",
                borderWidth: 3,
                backgroundColor: gradient,
                fill: true,
                tension: 0.25,
                pointBackgroundColor: "#0284c7",
                pointBorderColor: "#ffffff",
                pointBorderWidth: 2,
                pointRadius: 6,
                pointHoverRadius: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            layout: {
                padding: { top: 35, bottom: 10, left: 10, right: 15 }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: {
                        color: isDark ? "#94a3b8" : "#64748b",
                        font: { family: "Inter", size: 11, weight: "bold" }
                    }
                },
                y: {
                    beginAtZero: true,
                    grid: {
                        color: isDark ? "rgba(255,255,255,0.06)" : "rgba(0,0,0,0.06)"
                    },
                    ticks: {
                        color: isDark ? "#94a3b8" : "#64748b",
                        font: { family: "Inter", size: 10 },
                        callback: val => Number(val).toLocaleString()
                    }
                }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: isDark ? "#1e293b" : "#ffffff",
                    titleColor: isDark ? "#f8fafc" : "#0f172a",
                    bodyColor: isDark ? "#cbd5e1" : "#334155",
                    borderColor: isDark ? "#334155" : "#e2e8f0",
                    borderWidth: 1,
                    padding: 10,
                    displayColors: false,
                    callbacks: {
                        label: ctx => `Volume: ${ctx.parsed.y.toLocaleString()} documents`
                    }
                }
            }
        },
        plugins: [customLabelsPlugin]
    });
}

function roundRect(ctx, x, y, width, height, radius) {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
    ctx.lineTo(x + width, y + height - radius);
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    ctx.lineTo(x + radius, y + height);
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
}

function renderExecTable(data) {
    const tableHead = document.getElementById("execTableHead");
    const tableBody = document.getElementById("execTableBody");
    if (!tableHead || !tableBody) return;

    const years = data.yearlyPoints.map(p => p.year);

    // Header
    tableHead.innerHTML = `<tr>
        <th style="padding:10px 14px;text-align:left;font-weight:700;font-size:11px;letter-spacing:0.04em;">Site</th>
        ${years.map(y => `<th style="padding:10px 8px;text-align:right;font-weight:700;font-size:11px;">${y}</th>`).join("")}
        <th style="padding:10px 14px;text-align:right;font-weight:700;font-size:11px;">Overall Growth</th>
    </tr>`;

    let html = "";

    // Site rows
    data.siteRows.forEach((row, i) => {
        const sign = row.overallGrowthPct >= 0 ? "+" : "";
        const growthColor = row.overallGrowthPct >= 0 ? "#10b981" : "#f43f5e";
        const bgStyle = i % 2 === 0 ? "background:var(--hover);" : "";

        html += `<tr style="${bgStyle};transition:background .15s;">
            <td style="padding:9px 14px;font-weight:600;font-size:12px;color:var(--text);">${row.siteName}</td>
            ${row.yearlyVolumes.map(v => `<td style="padding:9px 8px;text-align:right;font-size:12px;color:var(--text);">${v.toLocaleString()}</td>`).join("")}
            <td style="padding:9px 14px;text-align:right;font-weight:700;font-size:12px;color:${growthColor};">${sign}${row.overallGrowthPct.toFixed(1)}%</td>
        </tr>`;
    });

    // Total row
    if (data.totalRow) {
        const tr = data.totalRow;
        const sign = tr.overallGrowthPct >= 0 ? "+" : "";
        const growthColor = tr.overallGrowthPct >= 0 ? "#10b981" : "#f43f5e";

        html += `<tr style="border-top:2px solid var(--border);background:var(--card);font-weight:700;">
            <td style="padding:11px 14px;font-size:12px;color:var(--text);font-weight:700;">${tr.siteName}</td>
            ${tr.yearlyVolumes.map(v => `<td style="padding:11px 8px;text-align:right;font-size:12px;color:var(--text);font-weight:700;">${v.toLocaleString()}</td>`).join("")}
            <td style="padding:11px 14px;text-align:right;font-weight:800;font-size:12px;color:${growthColor};">${sign}${tr.overallGrowthPct.toFixed(1)}%</td>
        </tr>`;
    }

    tableBody.innerHTML = html;
}