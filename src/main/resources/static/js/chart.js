/* =====================================================================
 * chart.js — Main on-screen charting for D2 Organic Growth Report.
 * Builds Chart.js instances (bar / line / stacked), GMP Group filter panel,
 * GMP Artifact indicator tags, and PDF report generation payload.
 * ===================================================================== */

var chartInstances = window.chartInstances || [];

var CREATED_COLORS = window.CREATED_COLORS || [
    "rgba(79,142,247,0.85)","rgba(34,197,120,0.85)","rgba(249,155,64,0.85)",
    "rgba(239,100,132,0.85)","rgba(167,139,250,0.85)","rgba(251,211,86,0.85)",
    "rgba(100,210,220,0.85)","rgba(248,113,113,0.85)","rgba(52,211,153,0.85)","rgba(196,181,253,0.85)"
];
var MIGRATED_COLORS = window.MIGRATED_COLORS || [
    "rgba(79,142,247,0.4)","rgba(34,197,120,0.4)","rgba(249,155,64,0.4)",
    "rgba(239,100,132,0.4)","rgba(167,139,250,0.4)","rgba(251,211,86,0.4)",
    "rgba(100,210,220,0.4)","rgba(248,113,113,0.4)","rgba(52,211,153,0.4)","rgba(196,181,253,0.4)"
];
var SOLID_COLORS = window.SOLID_COLORS || [
    "rgba(79,142,247,1)","rgba(34,197,120,1)","rgba(249,155,64,1)",
    "rgba(239,100,132,1)","rgba(167,139,250,1)","rgba(251,211,86,1)",
    "rgba(100,210,220,1)","rgba(248,113,113,1)","rgba(52,211,153,1)","rgba(196,181,253,1)"
];

var SOLID_COLORS_RGB = window.SOLID_COLORS_RGB || [
    [79,142,247],[34,197,120],[249,155,64],[239,100,132],[167,139,250],
    [251,211,86],[100,210,220],[248,113,113],[52,211,153],[196,181,253]
];

var _hatchCache = window._hatchCache || {};
function createHatchPattern(ci) {
    const key = ci % SOLID_COLORS_RGB.length;
    if (_hatchCache[key]) return _hatchCache[key];

    const [r, g, b] = SOLID_COLORS_RGB[key];
    const size = 10;
    const offscreen = document.createElement("canvas");
    offscreen.width  = size;
    offscreen.height = size;
    const ctx = offscreen.getContext("2d");

    ctx.fillStyle = `rgba(${r},${g},${b},0.55)`;
    ctx.fillRect(0, 0, size, size);

    ctx.strokeStyle = `rgba(255,255,255,0.55)`;
    ctx.lineWidth   = 2.5;
    ctx.beginPath();
    ctx.moveTo(0, 0); ctx.lineTo(size, size);
    ctx.moveTo(-size/2, size/2); ctx.lineTo(size/2, size * 1.5);
    ctx.moveTo(size/2, -size/2); ctx.lineTo(size * 1.5, size/2);
    ctx.stroke();

    _hatchCache[key] = offscreen;
    return offscreen;
}

var _patternCache = window._patternCache || {};
function resolveHatchPattern(ci, chartCtx) {
    const key = ci % SOLID_COLORS_RGB.length;
    if (_patternCache[key]) return _patternCache[key];
    const offscreen = createHatchPattern(key);
    const pattern   = chartCtx.createPattern(offscreen, "repeat");
    _patternCache[key] = pattern;
    return pattern;
}

function resetZoom() {
    chartInstances.forEach(c => { if (c) c.resetZoom(); });
}

function generateChart() {
    const years = getYearRange("yearFrom", "yearTo");
    if (years.length === 0) { alert("Please select a valid year range."); return; }

    const profiles = getSelectedProfiles();
    if (profiles.length === 0) { alert("Please select at least one Creation Profile"); return; }

    const chartType    = document.getElementById("chartType")?.value || "BAR";
    const site         = document.getElementById("owningSite")?.value || "";
    const docType      = document.getElementById("docType")?.value || "BOTH";
    const stackMode    = document.getElementById("stackMode")?.value || "LIFECYCLE";
    const gmpGroups    = getSelectedGmps();
    const gmpSubgroups = getSelectedSubgroups();
    const gmpArtifacts = getSelectedArtifacts();

    const typeLabel = { BAR:"Bar Chart", LINE:"Line Chart", STACKED:"Stacked Bar Chart" }[chartType] || chartType;
    const parts = [];
    if (site)            parts.push("Site: " + site);
    if (profiles.length) parts.push(profiles.length + " profile" + (profiles.length > 1 ? "s" : ""));
    const docLabel = { BOTH:"Created + Migrated", CREATED:"Created/Import only", MIGRATED:"Migrated only" }[docType] || docType;
    parts.push("Type: " + docLabel);
    parts.push("Color: " + (stackMode === "LIFECYCLE" ? "Lifecycle" : "Categories"));

    const sub = document.getElementById("chartSub");
    if (sub) sub.textContent = typeLabel + (parts.length ? " — " + parts.join(" | ") : "");

    document.getElementById("spinner").classList.add("on");

    fetch("/api/charts/data", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ owningSite:site, creationProfiles:profiles, gmpGroups, gmpSubgroups, gmpArtifacts, years, chartType, docType, stackMode })
    })
    .then(res => {
        if (!res.ok) return res.json().then(err => Promise.reject(err.error || JSON.stringify(err)));
        return res.json();
    })
    .then(data => {
        drawCharts(data, chartType);
        if (typeof explainChart === "function") explainChart(data, chartType, site, profiles, gmpGroups, years);
        window._lastChartData   = data;
        window._lastChartParams = { site, profiles, gmpGroups, gmpSubgroups, gmpArtifacts, years, chartType, docType, stackMode };
        const dlBtn = document.getElementById("downloadReportBtn");
        if (dlBtn) dlBtn.style.display = "inline-flex";
    })
    .catch(err => { console.error(err); alert("Error: " + err); })
    .finally(() => { document.getElementById("spinner").classList.remove("on"); });
}

/* ══════════════════════════════════════════════════
   DRAW ALL CHART CARDS
══════════════════════════════════════════════════ */
function drawCharts(data, chartType) {
    chartInstances.forEach(c => { if (c) c.destroy(); });
    chartInstances.length = 0;

    const body = document.getElementById("chartBody");
    document.getElementById("emptyState").style.display = "none";
    body.querySelectorAll(".profile-chart-wrap").forEach(el => el.remove());
    document.getElementById("chartCanvas").style.display = "none";

    body.style.flexDirection  = "column";
    body.style.alignItems     = "stretch";
    body.style.justifyContent = "flex-start";
    body.style.overflowY      = "auto";
    body.style.overflowX      = "hidden";
    body.style.gap            = "14px";
    body.style.padding        = "6px 2px";

    data.charts.forEach((chartData, idx) => {

        const wrap = document.createElement("div");
        wrap.className = "profile-chart-wrap";
        wrap.style.cssText = [
            "background:var(--card)", "border:1px solid var(--border)",
            "border-radius:10px", "padding:14px 16px",
            "flex-shrink:0", "display:flex", "flex-direction:column",
            "gap:10px", "min-height:340px"
        ].join(";");

        const header = document.createElement("div");
        header.style.cssText = "display:flex;align-items:center;justify-content:space-between;flex-shrink:0;";

        const titleLeft = document.createElement("div");
        titleLeft.style.cssText = "display:flex;align-items:center;gap:8px;min-width:0;";

        const title = document.createElement("div");
        title.style.cssText = "font-family:'Space Grotesk',sans-serif;font-size:13px;font-weight:700;color:var(--text);";
        title.textContent = chartData.profileName;

        const badge = document.createElement("span");
        badge.style.cssText = "font-size:10px;color:var(--muted);background:var(--surface);border:1px solid var(--border);padding:2px 8px;border-radius:10px;white-space:nowrap;flex-shrink:0;";
        badge.textContent = chartData.totalDocs.toLocaleString() + " docs";

        titleLeft.appendChild(title);
        titleLeft.appendChild(badge);
        header.appendChild(titleLeft);

        const viewToggle = document.createElement("div");
        viewToggle.style.cssText = "display:flex;align-items:center;gap:2px;background:var(--surface);border:1px solid var(--border);border-radius:20px;padding:2px;flex-shrink:0;";

        const btnTime = document.createElement("button");
        btnTime.style.cssText = "font-size:10px;font-weight:600;padding:3px 10px;border-radius:16px;cursor:pointer;border:none;font-family:'Inter',sans-serif;transition:all .2s;background:var(--accent);color:#fff;";
        btnTime.textContent = "📈 Time";
        btnTime.title = "Time-series view — trends over years";

        const btnRank = document.createElement("button");
        btnRank.style.cssText = "font-size:10px;font-weight:600;padding:3px 10px;border-radius:16px;cursor:pointer;border:none;font-family:'Inter',sans-serif;transition:all .2s;background:transparent;color:var(--muted);";
        btnRank.textContent = "📊 Rank";
        btnRank.title = "Rank view — all groups sorted by total, directly labeled";

        viewToggle.appendChild(btnTime);
        viewToggle.appendChild(btnRank);
        header.appendChild(viewToggle);
        wrap.appendChild(header);

        const divider = document.createElement("div");
        divider.style.cssText = "height:1px;background:var(--border);flex-shrink:0;";
        wrap.appendChild(divider);

        const viewHint = document.createElement("div");
        viewHint.style.cssText = "font-size:10px;color:var(--muted);flex-shrink:0;font-style:italic;";
        viewHint.textContent = "Time view — top groups labeled directly. Switch to Rank to see all.";
        wrap.appendChild(viewHint);

        const canvasWrap = document.createElement("div");
        canvasWrap.style.cssText = "position:relative;flex:1;min-height:240px;";

        const canvas = document.createElement("canvas");
        canvas.style.cssText = "position:absolute;top:0;left:0;width:100%;height:100%;";
        canvasWrap.appendChild(canvas);

        const rankCanvas = document.createElement("canvas");
        rankCanvas.style.cssText = "position:absolute;top:0;left:0;width:100%;height:100%;display:none;";
        canvasWrap.appendChild(rankCanvas);

        wrap.appendChild(canvasWrap);
        body.appendChild(wrap);

        const sharedColorMap = {};
        let   sharedColorIdx = 0;
        chartData.series.forEach(s => {
            const grp = s.gmpGroup || s.name.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
            if (!(grp in sharedColorMap)) sharedColorMap[grp] = sharedColorIdx++;
        });

        const instance = buildChartInstance(canvas, chartData, chartType, sharedColorMap);
        chartInstances.push(instance);

        const rowMap = buildGmpFilterPanel(wrap, instance, chartData, idx, sharedColorMap);
        wireChartToPanel(canvas, instance, chartData, rowMap);

        buildTotalCountBar(wrap, chartData);
        buildGroupDetailTable(wrap, instance, chartData, rowMap, sharedColorMap);

        let rankInstance = null;
        let currentView  = "time";

        function switchToTime() {
            if (currentView === "time") return;
            currentView = "time";
            canvas.style.display     = "block";
            rankCanvas.style.display = "none";
            btnTime.style.background = "var(--accent)";
            btnTime.style.color      = "#fff";
            btnRank.style.background = "transparent";
            btnRank.style.color      = "var(--muted)";
            viewHint.textContent = "Time view — top groups labeled directly. Switch to Rank to see all.";
            if (rankInstance) { rankInstance.destroy(); rankInstance = null; }
        }

        function switchToRank() {
            if (currentView === "rank") return;
            currentView = "rank";
            canvas.style.display     = "none";
            rankCanvas.style.display = "block";
            btnRank.style.background = "var(--accent)";
            btnRank.style.color      = "#fff";
            btnTime.style.background = "transparent";
            btnTime.style.color      = "var(--muted)";
            viewHint.textContent = "Rank view — all GMP groups sorted by total. Each group labeled directly.";
            if (rankInstance) rankInstance.destroy();
            rankInstance = buildRankChart(rankCanvas, chartData);
        }

        btnTime.addEventListener("click", switchToTime);
        btnRank.addEventListener("click", switchToRank);
    });
}

/* ══════════════════════════════════════════════════
   RANK VIEW
══════════════════════════════════════════════════ */
function buildRankChart(canvas, chartData) {

    const gmpTotals = new Map();
    chartData.series.forEach(s => {
        const grp = s.gmpGroup || s.name.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
        const tot = s.data.reduce((a, b) => a + b, 0);
        gmpTotals.set(grp, (gmpTotals.get(grp) || 0) + tot);
    });

    const sorted = Array.from(gmpTotals.entries())
        .filter(([, v]) => v > 0)
        .sort((a, b) => b[1] - a[1]);

    const labels  = sorted.map(([name]) => name);
    const values  = sorted.map(([, val]) => val);
    const maxVal  = values[0] || 1;
    const colors  = sorted.map((_, i) => SOLID_COLORS[i % SOLID_COLORS.length]);
    const bgColors= sorted.map((_, i) => CREATED_COLORS[i % CREATED_COLORS.length]);

    Chart.defaults.color       = "#7a8299";
    Chart.defaults.borderColor = "rgba(255,255,255,0.04)";

    const barHeight = 28;
    const minH      = Math.max(240, sorted.length * barHeight + 60);
    canvas.parentElement.style.minHeight = minH + "px";

    return new Chart(canvas.getContext("2d"), {
        type: "bar",
        data: {
            labels,
            datasets: [{
                data           : values,
                backgroundColor: bgColors,
                borderColor    : colors,
                borderWidth    : 1,
                borderRadius   : 4,
                borderSkipped  : false,
                barThickness   : 20
            }]
        },
        plugins: [
            {
                id: 'rankLabelsPlugin',
                afterDatasetsDraw(chart) {
                    const ctx     = chart.ctx;
                    const meta    = chart.getDatasetMeta(0);
                    const area    = chart.chartArea;

                    ctx.save();
                    ctx.font         = "700 11px Inter";
                    ctx.textBaseline = "middle";
                    ctx.textAlign    = "left";

                    meta.data.forEach((bar, i) => {
                        const val     = values[i];
                        if (!val) return;
                        const pct     = Math.round((val / maxVal) * 100);
                        const label   = val.toLocaleString() + "  (" + pct + "%)";
                        const x       = bar.x + 7;
                        const y       = bar.y;

                        ctx.shadowColor   = "rgba(0,0,0,0.6)";
                        ctx.shadowBlur    = 3;
                        ctx.fillStyle     = "#e2e8f0";
                        ctx.fillText(label, x, y);
                        ctx.shadowBlur    = 0;
                    });

                    ctx.font      = "700 9px Inter";
                    ctx.textAlign = "right";
                    ctx.fillStyle = "#4a5270";
                    meta.data.forEach((bar, i) => {
                        ctx.fillText("#" + (i+1), area.left - 4, bar.y);
                    });

                    ctx.restore();
                }
            }
        ],
        options: {
            indexAxis  : "y",
            responsive : true,
            maintainAspectRatio: false,
            layout: { padding: { top:8, bottom:8, left:8, right:90 } },
            plugins: {
                legend : { display: false },
                title  : { display: false },
                tooltip: {
                    backgroundColor: "rgba(20, 24, 35, 0.96)",
                    borderColor: "rgba(91, 141, 238, 0.4)",
                    borderWidth: 1,
                    padding: 12,
                    cornerRadius: 8,
                    titleFont: { family: "'Space Grotesk', sans-serif", size: 13, weight: "700" },
                    bodyFont: { family: "Inter", size: 11.5 },
                    callbacks: {
                        title: ctx => {
                            const year = ctx[0]?.label || "";
                            return "📅 Year " + year;
                        },
                        label: ctx => {
                            const count = ctx.parsed.y;
                            if (count === 0) return null;
                            const s       = chartData.series[ctx.datasetIndex];
                            const grp     = s?.gmpGroup || ctx.dataset.label.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
                            const typeTag = s?.seriesType === "MIGRATED" ? " ↑ Migrated" : " ✦ Created/Import";
                            return "📦 " + grp + typeTag + ": " + count.toLocaleString() + " docs";
                        },
                        afterBody: ctx => {
                            if (!ctx || ctx.length === 0) return [];
                            const lines = [];
                            const year = ctx[0]?.label;

                            ctx.forEach(item => {
                                const count = item.parsed.y;
                                if (!count) return;

                                const s = chartData.series[item.datasetIndex];
                                if (!s || !s.yearlyArtifacts) return;

                                const artifacts = s.yearlyArtifacts[year] || [];
                                if (artifacts.length > 0) {
                                    lines.push("  ──────────────────────────────");
                                    lines.push("  🔍 Artifact Breakdown (" + year + "):");
                                    
                                    const groupTotal = count;
                                    artifacts.forEach((art, ai) => {
                                        const isLast = ai === artifacts.length - 1;
                                        const prefix = isLast ? "  └─ " : "  ├─ ";
                                        const pct    = groupTotal > 0 ? Math.round((art.total / groupTotal) * 100) : 0;
                                        lines.push(`${prefix}[${art.gmpSubgroup}] ${art.artifactName}: ${art.total.toLocaleString()} docs (${pct}%)`);
                                    });
                                }
                            });

                            return lines;
                        },
                        filter: item => item.parsed.y > 0
                    }
                },
                zoom: {
                    pan:  { enabled: true, mode: "y" },
                    zoom: { wheel: { enabled: false }, pinch: { enabled: false } }
                }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    grid : { color:"rgba(255,255,255,0.04)" },
                    ticks: {
                        precision:0,
                        font    : { family:"Inter", size:10 },
                        color   : "#64748b"
                    }
                },
                y: {
                    grid: { display: false },
                    ticks: {
                        font    : { family:"Inter", size:11, weight:"600" },
                        color   : "#94a3b8",
                        autoSkip: false,
                        callback: function(val, i) {
                            const lbl = labels[i] || "";
                            return lbl.length > 28 ? lbl.substring(0,26)+"…" : lbl;
                        }
                    }
                }
            },
            animation: {
                duration: 400
            }
        }
    });
}

/* ══════════════════════════════════════════════════
   DIRECT BAR LABELS
══════════════════════════════════════════════════ */
function drawDirectLabels(chart, chartData, effectiveStacked) {
    const ctx    = chart.ctx;
    const series = chartData.series;
    const xAxis  = chartData.xAxis;
    if (!series || series.length === 0) return;

    ctx.save();

    if (effectiveStacked) {
        const yearTotals = xAxis.map((_, yi) =>
            series.reduce((sum, s, si) =>
                chart.getDatasetMeta(si).hidden ? sum : sum + (s.data[yi] || 0), 0)
        );
        ctx.font      = "700 11px Inter";
        ctx.fillStyle = "#c8d0e8";
        ctx.textAlign = "center";
        ctx.textBaseline = "bottom";

        xAxis.forEach((yr, yi) => {
            const total = yearTotals[yi];
            if (!total) return;
            let topY = Infinity, barX = null;
            series.forEach((s, si) => {
                const meta = chart.getDatasetMeta(si);
                if (meta.hidden) return;
                const bar = meta.data[yi];
                if (bar && bar.y < topY) { topY = bar.y; barX = bar.x; }
            });
            if (barX === null) return;

            const label = total.toLocaleString();
            const tw    = ctx.measureText(label).width;
            const pad   = 5;

            ctx.fillStyle = "rgba(13,15,20,0.88)";
            const rx = barX - tw/2 - pad;
            const ry = topY - 22;
            const rw = tw + pad*2;
            const rh = 16;
            roundRect(ctx, rx, ry, rw, rh, 4);
            ctx.fill();
            ctx.strokeStyle = "rgba(91,141,238,0.35)";
            ctx.lineWidth   = 0.8;
            ctx.stroke();

            ctx.fillStyle = "#93c5fd";
            ctx.fillText(label, barX, topY - 7);
        });

    } else {
        ctx.font         = "600 9.5px Inter";
        ctx.textAlign    = "center";
        ctx.textBaseline = "bottom";

        xAxis.forEach((yr, yi) => {
            const bars = [];
            series.forEach((s, si) => {
                const meta = chart.getDatasetMeta(si);
                if (meta.hidden) return;
                const val = s.data[yi];
                if (!val || val === 0) return;
                const grp = s.gmpGroup || s.name.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
                bars.push({ val, grp, bar: meta.data[yi], si });
            });

            bars.sort((a, b) => b.val - a.val);
            const topBars = bars.slice(0, 3);

            topBars.forEach((item, rank) => {
                const { val, grp, bar } = item;
                const label = val.toLocaleString();

                const tw  = ctx.measureText(label).width;
                const pad = 4;
                const bx  = bar.x;
                const by  = bar.y;

                if (bar.height < 14) return;

                const alpha = rank === 0 ? 0.92 : 0.75;
                ctx.fillStyle = `rgba(13,15,20,${alpha})`;
                const rx = bx - tw/2 - pad;
                const ry = by - 20;
                const rw = tw + pad*2;
                const rh = 14;
                roundRect(ctx, rx, ry, rw, rh, 3);
                ctx.fill();

                ctx.fillStyle = rank === 0 ? "#e2e8f0" : "#94a3b8";
                ctx.fillText(label, bx, by - 7);
            });
        });
    }

    ctx.restore();
}

function roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x+r, y);
    ctx.lineTo(x+w-r, y);
    ctx.quadraticCurveTo(x+w, y, x+w, y+r);
    ctx.lineTo(x+w, y+h-r);
    ctx.quadraticCurveTo(x+w, y+h, x+w-r, y+h);
    ctx.lineTo(x+r, y+h);
    ctx.quadraticCurveTo(x, y+h, x, y+h-r);
    ctx.lineTo(x, y+r);
    ctx.quadraticCurveTo(x, y, x, y, x+r, y);
    ctx.closePath();
}

/* ══════════════════════════════════════════════════
   GMP FILTER PANEL
══════════════════════════════════════════════════ */
function buildGmpFilterPanel(parentWrap, chartInstance, chartData, chartIdx, sharedColorMap) {
    const datasets  = chartInstance.data.datasets;
    if (datasets.length === 0) return new Map();

    const gmpMap = new Map();

    chartData.series.forEach((s, i) => {
        const grp = s.gmpGroup || s.name.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
        const ci  = (sharedColorMap[grp] ?? i) % SOLID_COLORS.length;
        if (!gmpMap.has(grp))  gmpMap.set(grp, { colorIdx:ci, indices:[], total:0 });
        const entry = gmpMap.get(grp);
        entry.indices.push(i);
        entry.total += s.data.reduce((a, b) => a + b, 0);
    });

    const gmpGroups = Array.from(gmpMap.entries())
        .map(([name, info]) => ({ name, colorIdx:info.colorIdx, indices:info.indices, total:info.total }))
        .sort((a, b) => b.total - a.total);

    const maxTotal   = gmpGroups[0]?.total || 1;
    const totalCount = gmpGroups.length;
    const hiddenGroups = new Set();

    const panel = document.createElement("div");
    panel.className = "gmp-filter-panel";
    panel.id = "gmpPanel-" + chartIdx;

    const panelHeader = document.createElement("div");
    panelHeader.className = "gmp-filter-header";

    const headerLeft = document.createElement("div");
    headerLeft.className = "gmp-filter-title";
    headerLeft.textContent = "🎯 GMP Group Filter";

    const countBadge = document.createElement("span");
    countBadge.className = "gmp-filter-count";
    countBadge.id = "gmpCount-" + chartIdx;
    countBadge.textContent = totalCount + " of " + totalCount + " shown";
    headerLeft.appendChild(countBadge);

    const toggleIcon = document.createElement("span");
    toggleIcon.className = "gmp-filter-toggle-icon";
    toggleIcon.textContent = "▼";
    panelHeader.appendChild(headerLeft);
    panelHeader.appendChild(toggleIcon);
    panelHeader.addEventListener("click", () => panel.classList.toggle("collapsed"));
    panel.appendChild(panelHeader);

    const panelBody = document.createElement("div");
    panelBody.className = "gmp-filter-body";

    const searchRow = document.createElement("div");
    searchRow.className = "gmp-search-row";
    searchRow.innerHTML = `<span class="gmp-search-icon">🔍</span>`;
    const searchInput = document.createElement("input");
    searchInput.className   = "gmp-search-input";
    searchInput.placeholder = "Search GMP groups… (" + totalCount + " total)";
    searchInput.type = "text";
    searchRow.appendChild(searchInput);
    panelBody.appendChild(searchRow);

    const actRow = document.createElement("div");
    actRow.className = "gmp-actions-row";

    const btnAll = document.createElement("button");
    btnAll.className = "gmp-act-btn gmp-act-all";
    btnAll.textContent = "✔ Show all";
    btnAll.addEventListener("click", () => { hiddenGroups.clear(); applyFilter(); renderList(""); });

    const btnNone = document.createElement("button");
    btnNone.className = "gmp-act-btn gmp-act-none";
    btnNone.textContent = "✖ Hide all";
    btnNone.addEventListener("click", () => { gmpGroups.forEach(g=>hiddenGroups.add(g.name)); applyFilter(); renderList(searchInput.value); });

    const selCount = document.createElement("span");
    selCount.className = "gmp-sel-count";
    selCount.id = "gmpSelCount-" + chartIdx;

    actRow.appendChild(btnAll);
    actRow.appendChild(btnNone);
    actRow.appendChild(selCount);
    panelBody.appendChild(actRow);

    const listEl = document.createElement("div");
    listEl.className = "gmp-list";
    panelBody.appendChild(listEl);

    panel.appendChild(panelBody);
    parentWrap.appendChild(panel);

    function restoreColors() {
        datasets.forEach(ds => {
            if (ds._orig) {
                ds.backgroundColor = ds._orig.bg;
                ds.borderColor     = ds._orig.br;
            }
        });
    }

    function applyFilter() {
        restoreColors();
        gmpGroups.forEach(g => {
            const hidden = hiddenGroups.has(g.name);
            g.indices.forEach(di => { chartInstance.getDatasetMeta(di).hidden = hidden; });
        });
        chartInstance.update();
        updateBadge();
    }

    function updateBadge() {
        const shown = gmpGroups.filter(g => !hiddenGroups.has(g.name)).length;
        const badge = document.getElementById("gmpCount-" + chartIdx);
        const cnt   = document.getElementById("gmpSelCount-" + chartIdx);
        if (badge) badge.textContent = shown + " of " + totalCount + " shown";
        if (cnt)   cnt.textContent   = hiddenGroups.size > 0 ? hiddenGroups.size + " hidden" : "all visible";
    }

    const rowMap = new Map();

    function renderList(query) {
        const q        = query.toLowerCase().trim();
        const filtered = q ? gmpGroups.filter(g => g.name.toLowerCase().includes(q)) : gmpGroups;
        listEl.innerHTML = "";
        rowMap.clear();

        if (!filtered.length) {
            listEl.innerHTML = `<div style="padding:12px;text-align:center;font-size:12px;color:var(--muted)">No GMP groups match</div>`;
            return;
        }

        filtered.forEach(g => {
            const isHidden = hiddenGroups.has(g.name);
            const ci       = g.colorIdx % SOLID_COLORS.length;
            const barPct   = Math.round((g.total / maxTotal) * 100);

            const row = document.createElement("div");
            row.className    = "gmp-row" + (isHidden ? " hidden-series" : "");
            row.dataset.gmp  = g.name;
            rowMap.set(g.name, row);

            const cb = document.createElement("input");
            cb.type = "checkbox"; cb.className = "gmp-row-check"; cb.checked = !isHidden;
            cb.addEventListener("change", () => {
                if (cb.checked) hiddenGroups.delete(g.name); else hiddenGroups.add(g.name);
                applyFilter();
                row.classList.toggle("hidden-series", !cb.checked);
                bar.style.opacity = cb.checked ? "0.8" : "0.3";
            });

            const dot = document.createElement("span");
            dot.className = "gmp-row-dot";
            dot.style.background = SOLID_COLORS[ci];
            dot.style.boxShadow  = `0 0 5px ${SOLID_COLORS[ci]}80`;

            const name = document.createElement("span");
            name.className  = "gmp-row-name";
            name.textContent = g.name; name.title = g.name;

            const barWrap = document.createElement("div");
            barWrap.className = "gmp-row-bar-wrap";
            const bar = document.createElement("div");
            bar.className = "gmp-row-bar";
            bar.style.width      = barPct + "%";
            bar.style.background = SOLID_COLORS[ci];
            bar.style.opacity    = isHidden ? "0.3" : "0.8";
            barWrap.appendChild(bar);

            const cnt = document.createElement("span");
            cnt.className   = "gmp-row-count";
            cnt.textContent = g.total.toLocaleString();

            row.appendChild(cb); row.appendChild(dot);
            row.appendChild(name); row.appendChild(barWrap); row.appendChild(cnt);

            row.addEventListener("mouseenter", () => {
                if (hiddenGroups.has(g.name)) return;
                datasets.forEach((ds, di) => {
                    const s    = chartData.series[di];
                    const grp  = s?.gmpGroup ||
                                 (s?.name || "").replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
                    if (!ds._orig) ds._orig = { bg: ds.backgroundColor, br: ds.borderColor };
                    if (grp !== g.name) {
                        ds.backgroundColor = "rgba(120,130,150,0.07)";
                        ds.borderColor     = "rgba(120,130,150,0.1)";
                    }
                });
                chartInstance.update("none");
            });
            row.addEventListener("mouseleave", () => {
                datasets.forEach(ds => {
                    if (ds._orig) { ds.backgroundColor = ds._orig.bg; ds.borderColor = ds._orig.br; }
                });
                chartInstance.update("none");
            });

            row.addEventListener("click", e => {
                if (e.target === cb) return;
                cb.checked = !cb.checked;
                if (cb.checked) hiddenGroups.delete(g.name); else hiddenGroups.add(g.name);
                applyFilter();
                row.classList.toggle("hidden-series", !cb.checked);
                bar.style.opacity = cb.checked ? "0.8" : "0.3";
            });

            listEl.appendChild(row);
        });

        updateBadge();
    }

    searchInput.addEventListener("input", () => renderList(searchInput.value));
    renderList("");
    return rowMap;
}

/* ══════════════════════════════════════════════════
   CHART → PANEL HOVER SYNC
══════════════════════════════════════════════════ */
function wireChartToPanel(canvas, chartInstance, chartData, rowMap) {
    if (!rowMap || rowMap.size === 0) return;
    let lastHighlighted = null;

    function resolveGmp(di) {
        const ser = chartData.series[di];
        if (!ser) return null;
        if (ser.gmpGroup && rowMap.has(ser.gmpGroup)) return ser.gmpGroup;
        const stripped = (ser.name || "")
            .replace(/\s+—\s+(Created\/Import|Migrated)$/i, "")
            .trim();
        if (rowMap.has(stripped)) return stripped;
        for (const key of rowMap.keys()) {
            if (key === stripped || stripped.startsWith(key) || key.startsWith(stripped)) return key;
        }
        return null;
    }

    canvas.addEventListener("mousemove", e => {
        let points = [];
        try {
            points = chartInstance.getElementsAtEventForMode(e, "nearest", { intersect: true }, false);
        } catch(_) {}

        if (!points.length) {
            if (lastHighlighted) { lastHighlighted.classList.remove("chart-hover"); lastHighlighted = null; }
            return;
        }

        const gmpName = resolveGmp(points[0].datasetIndex);
        if (!gmpName) return;

        const row = rowMap.get(gmpName);
        if (!row || lastHighlighted === row) return;

        if (lastHighlighted) lastHighlighted.classList.remove("chart-hover");
        row.classList.add("chart-hover");
        row.scrollIntoView({ block: "nearest", behavior: "smooth" });
        lastHighlighted = row;
    });

    canvas.addEventListener("mouseleave", () => {
        if (lastHighlighted) { lastHighlighted.classList.remove("chart-hover"); lastHighlighted = null; }
    });
}

/* ══════════════════════════════════════════════════
   BUILD TIME-SERIES CHART INSTANCE
══════════════════════════════════════════════════ */
function buildChartInstance(canvas, chartData, chartType, sharedColorMap) {

    const isLine    = chartType === "LINE";
    const isStacked = chartType === "STACKED";

    Chart.defaults.color       = "#7a8299";
    Chart.defaults.borderColor = "rgba(255,255,255,0.06)";

    const isBothStacked    = chartData.docType === "BOTH" && !isLine;
    const effectiveStacked = isStacked || isBothStacked;

    const datasets = chartData.series.map((s, i) => {
        const grp        = s.gmpGroup || s.name.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
        const ci         = (sharedColorMap[grp] ?? i) % SOLID_COLORS.length;
        const isMigrated = s.seriesType === "MIGRATED";

        const bgColor    = isMigrated ? MIGRATED_COLORS[ci] : CREATED_COLORS[ci];
        const borderCol  = SOLID_COLORS[ci];

        const base = {
            label          : s.name,
            data           : s.data,
            backgroundColor: bgColor,
            borderColor    : borderCol,
            borderWidth    : 0,
            borderRadius   : effectiveStacked ? 0 : 4,
            borderSkipped  : false
        };
        if (isLine) {
            base.fill             = false;
            base.tension          = 0.4;
            base.borderWidth      = 2.5;
            base.pointRadius      = 4;
            base.pointHoverRadius = 7;
            base.pointBackgroundColor = borderCol;
            base.pointBorderColor = "#fff";
            base.pointBorderWidth = 1.5;
            base.borderDash       = isMigrated ? [5,3] : [];
            base.spanGaps         = true;
        }
        return base;
    });

    return new Chart(canvas.getContext("2d"), {
        type: isLine ? "line" : "bar",
        data: { labels: chartData.xAxis, datasets },
        plugins: [
            {
                id: 'directLabelsPlugin',
                afterDatasetsDraw(chart) {
                    drawDirectLabels(chart, chartData, effectiveStacked);
                }
            }
        ],
        options: {
            responsive: true,
            maintainAspectRatio: false,
            layout: { padding: { top:40, bottom:10, left:8, right:8 } },
            plugins: {
                title  : { display:false },
                legend : { display:false },
                tooltip: {
                    backgroundColor: "rgba(24,28,39,0.97)",
                    borderColor: "rgba(79,142,247,0.3)", borderWidth:1, padding:12,
                    titleFont: { family:"Inter", size:13, weight:"600" },
                    bodyFont : { family:"Inter", size:12 },
                    callbacks: {
                        title: ctx => {
                            return ctx[0]?.label || "";
                        },
                        label: ctx => {
                            const ds    = ctx.dataset;
                            const count = ctx.parsed.y;
                            if (count === 0) return null;
                            const s       = chartData.series[ctx.datasetIndex];
                            const grp     = s?.gmpGroup || ds.label.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
                            const typeTag = s?.seriesType === "MIGRATED" ? " ↑ Migrated" : " ✦ Created/Import";
                            return "  " + grp + typeTag + ": " + count.toLocaleString() + " docs";
                        },
                        afterBody: ctx => {
                            if (ctx.length === 0) return;
                            const year  = ctx[0]?.label;
                            const yi    = chartData.xAxis?.indexOf(year);
                            if (yi === undefined || yi < 0) return;
                            const grpSet = new Set(ctx.map(c => {
                                const s = chartData.series[c.datasetIndex];
                                return s?.gmpGroup || c.dataset.label.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
                            }));
                            const lines = [];
                            grpSet.forEach(grp => {
                                let cr = 0, mg = 0;
                                chartData.series.forEach((s, di) => {
                                    const g = s?.gmpGroup || s.name.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
                                    if (g !== grp) return;
                                    if (s.seriesType === "MIGRATED") mg += s.data[yi] || 0;
                                    else cr += s.data[yi] || 0;
                                });
                                if (cr > 0 || mg > 0) {
                                    lines.push("  ────────────────────────");
                                    lines.push("  " + grp);
                                    if (cr > 0) lines.push("    ✦ Created/Import: " + cr.toLocaleString());
                                    if (mg > 0) lines.push("    ↑ Migrated:       " + mg.toLocaleString());
                                    lines.push("    Total: " + (cr+mg).toLocaleString());
                                }
                            });
                            return lines;
                        },
                        filter: item => item.parsed.y > 0
                    }
                },
                zoom: {
                    pan:  { enabled:true, mode:"x" },
                    zoom: { wheel:{ enabled:true }, pinch:{ enabled:true }, mode:"x" }
                }
            },
            scales: {
                x: {
                    stacked: effectiveStacked,
                    grid : { color:"rgba(255,255,255,0.04)" },
                    ticks: {
                        font:{ family:"Inter", size:11 }, color:"#64748b",
                        autoSkip:false, maxRotation:0, minRotation:0
                    },
                    title: {
                        display:true, text:"Year",
                        font:{ size:11, weight:"600", family:"Inter" },
                        color:"#64748b", padding:{ top:6 }
                    }
                },
                y: {
                    stacked: effectiveStacked,
                    beginAtZero:true, grace:"20%",
                    grid : { color:"rgba(255,255,255,0.04)" },
                    ticks: { precision:0, font:{ family:"Inter", size:11 }, color:"#64748b" },
                    title: {
                        display:true, text:"Documents",
                        font:{ size:11, weight:"600", family:"Inter" },
                        color:"#64748b", padding:{ bottom:6 }
                    }
                }
            },
            animation: {
                duration: 400
            }
        }
    });
}

/* ══════════════════════════════════════════════════
   DOWNLOAD REPORT — PDF Generation
══════════════════════════════════════════════════ */
async function downloadReport() {
    const btn = document.getElementById("downloadReportBtn");
    if (btn) { btn.textContent = "⏳ Generating…"; btn.disabled = true; }

    try {
        const params  = window._lastChartParams || {};
        const data    = window._lastChartData;

        const chartImages = [];
        const wraps = document.querySelectorAll(".profile-chart-wrap");
        wraps.forEach((wrap, i) => {
            const canvases = wrap.querySelectorAll("canvas");
            let activeCanvas = null;
            canvases.forEach(c => {
                if (c.style.display !== "none" && c.width > 0) activeCanvas = c;
            });
            if (activeCanvas) {
                chartImages.push({
                    profileName: data?.charts?.[i]?.profileName || ("Profile " + (i+1)),
                    base64Image: activeCanvas.toDataURL("image/png", 0.92)
                });
            }
        });

        const profiles = [];
        if (data?.charts) {
            data.charts.forEach(chart => {
                
                // 1. Build a lookup for artifact yearly data from the series
                const artifactYearlyData = {}; // key: "Subgroup|||Artifact", value: { year: { created, migrated } }
                
                if (chart && chart.series) {
                    chart.series.forEach(s => {
                        const gmpGroup = s.gmpGroup || s.name.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
                        if (s.yearlyArtifacts) {
                            Object.keys(s.yearlyArtifacts).forEach(year => {
                                s.yearlyArtifacts[year].forEach(art => {
                                    const sub = art.gmpSubgroup || "(No Subgroup)";
                                    const key = gmpGroup + "|||" + sub + "|||" + art.artifactName;
                                    
                                    if (!artifactYearlyData[key]) artifactYearlyData[key] = {};
                                    if (!artifactYearlyData[key][year]) artifactYearlyData[key][year] = { created: 0, migrated: 0 };
                                    
                                    artifactYearlyData[key][year].created += (art.created || 0);
                                    artifactYearlyData[key][year].migrated += (art.migrated || 0);
                                });
                            });
                        }
                    });
                }

                // 2. Map GroupBreakdowns and inject the correct yearly breakdown
                const mappedGmpRows = (chart.groupBreakdowns || []).map(gb => ({
                    gmpGroup: gb.gmpGroup,
                    total: gb.total,
                    created: gb.created,
                    migrated: gb.migrated,
                    artifacts: (gb.artifacts || []).map(a => {
                        const sub = a.gmpSubgroup || a.subgroup || a.subGroupName || "(No Subgroup)";
                        const key = gb.gmpGroup + "|||" + sub + "|||" + a.artifactName;
                        const yearData = artifactYearlyData[key] || {};
                        
                        const yearlyCreated = {};
                        const yearlyMigrated = {};
                        
                        Object.keys(yearData).forEach(y => {
                            yearlyCreated[parseInt(y)] = yearData[y].created;
                            yearlyMigrated[parseInt(y)] = yearData[y].migrated;
                        });

                        return {
                            subgroup: sub,
                            artifactName: a.artifactName,
                            total: a.total,
                            created: a.created,
                            migrated: a.migrated,
                            yearlyCreated: yearlyCreated,
                            yearlyMigrated: yearlyMigrated
                        };
                    })
                }));

                profiles.push({
                    profileName: chart.profileName,
                    totalDocs  : chart.totalDocs,
                    gmpRows    : mappedGmpRows
                });
            });
        }

        let compareSummary = null;
        const cmpResult = document.getElementById("compareResult");
        if (cmpResult && cmpResult.style.display !== "none") {
            const cmpCanvas = document.getElementById("compareChart");
            compareSummary = {
                sites          : getSelectedCompareSites(),
                overallTotal   : parseInt(document.getElementById("cTotalDocs")?.textContent?.replace(/,/g,"")) || 0,
                topSite        : document.getElementById("cTopSite")?.textContent || "",
                topSiteDocs    : parseInt(document.getElementById("cTopDocs")?.textContent?.replace(/[^0-9]/g,"")) || 0,
                yearRange      : document.getElementById("cYearRange")?.textContent || "",
                compareImageBase64: cmpCanvas ? cmpCanvas.toDataURL("image/png", 0.92) : null
            };
        }

        profiles.forEach((prof, i) => {
            const chart = data?.charts?.[i];
            if (chart && chart.xAxis) {
                const yearMap = {};
                chart.series.forEach(s => {
                    s.data.forEach((v, yi) => {
                        const yr = parseInt(chart.xAxis[yi]);
                        if (!yearMap[yr]) yearMap[yr] = { year:yr, total:0, created:0, migrated:0 };
                        yearMap[yr].total += v;
                        if (s.seriesType === "CREATED")  yearMap[yr].created  += v;
                        if (s.seriesType === "MIGRATED") yearMap[yr].migrated += v;
                    });
                });
                Object.values(yearMap).forEach(y => {
                    if (y.created === 0 && y.migrated === 0) y.created = y.total;
                });
                prof.yearData = Object.values(yearMap).sort((a,b) => a.year - b.year);
            }
        });

        const payload = {
            owningSite       : params.site || "",
            creationProfiles : params.profiles || [],
            docType          : params.docType || "BOTH",
            yearFrom         : params.years?.[0] || 0,
            yearTo           : params.years?.[params.years.length - 1] || 0,
            chartType        : params.chartType || "BAR",
            theme: "LIGHT",
            chartImages,
            profiles,
            compareSummary
        };

        const response = await fetch("/api/report/generate", {
            method : "POST",
            headers: { "Content-Type": "application/json" },
            body   : JSON.stringify(payload)
        });

        if (!response.ok) {
            const err = await response.json().catch(() => ({ error: "Unknown error" }));
            throw new Error(err.error || "Report generation failed");
        }

        const blob     = await response.blob();
        const url      = URL.createObjectURL(blob);
        const a        = document.createElement("a");
        const filename = response.headers.get("Content-Disposition")
            ?.split("filename=")[1]?.replace(/"/g,"")
            || "D2_Organic_Growth_Report.pdf";
        a.href     = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);

    } catch (err) {
        console.error("Report error:", err);
        alert("Failed to generate report: " + err.message);
    } finally {
        if (btn) { btn.textContent = "⬇ Download Report"; btn.disabled = false; }
    }
}

/* ══════════════════════════════════════════════════
   TOTAL COUNT SUMMARY BAR
══════════════════════════════════════════════════ */
function buildTotalCountBar(parentWrap, chartData) {
    let createdTotal  = 0;
    let migratedTotal = 0;
    chartData.series.forEach(s => {
        const tot = s.data.reduce((a, b) => a + b, 0);
        if (s.seriesType === "MIGRATED") migratedTotal += tot;
        else createdTotal += tot;
    });
    const grandTotal = createdTotal + migratedTotal || chartData.totalDocs || 0;

    const bar = document.createElement("div");
    bar.className = "chart-total-bar";

    const mainDiv = document.createElement("div");
    mainDiv.innerHTML = `
        <div class="chart-total-main">${grandTotal.toLocaleString()}</div>
        <div class="chart-total-label">Total Documents</div>`;
    bar.appendChild(mainDiv);

    const sep = document.createElement("div");
    sep.className = "chart-total-sep";
    bar.appendChild(sep);

    if (createdTotal > 0) {
        const pct = grandTotal > 0 ? Math.round((createdTotal/grandTotal)*100) : 0;
        const pill = document.createElement("div");
        pill.className = "chart-total-pill created";
        pill.innerHTML = `<span class="chart-total-pill-dot" style="background:#4f8ef7;"></span>
            <span>Created/Import</span>
            <strong>${createdTotal.toLocaleString()}</strong>
            <span style="opacity:.6;">(${pct}%)</span>`;
        bar.appendChild(pill);
    }

    if (migratedTotal > 0) {
        const pct = grandTotal > 0 ? Math.round((migratedTotal/grandTotal)*100) : 0;
        const pill = document.createElement("div");
        pill.className = "chart-total-pill migrated";
        pill.innerHTML = `<span class="chart-total-pill-dot" style="background:#a78bfa;"></span>
            <span>Migrated</span>
            <strong>${migratedTotal.toLocaleString()}</strong>
            <span style="opacity:.6;">(${pct}%)</span>`;
        bar.appendChild(pill);
    }

    if (chartData.xAxis && chartData.xAxis.length > 0) {
        const sep2 = document.createElement("div");
        sep2.className = "chart-total-sep";
        bar.appendChild(sep2);
        const rangeDiv = document.createElement("div");
        rangeDiv.innerHTML = `<div class="chart-total-label">Year Range</div>
            <div style="font-size:12px;font-weight:700;color:var(--sub);">
                ${chartData.xAxis[0]}${chartData.xAxis.length > 1 ? " → " + chartData.xAxis[chartData.xAxis.length-1] : ""}
            </div>`;
        bar.appendChild(rangeDiv);
    }

    parentWrap.appendChild(bar);
}

/* ══════════════════════════════════════════════════
   GROUP DETAIL TABLE (3-Level Hierarchy)
══════════════════════════════════════════════════ */
function buildGroupDetailTable(parentWrap, chartInstance, chartData, rowMap, sharedColorMap) {
    const datasets = chartInstance.data.datasets;
    const breakdowns = chartData.groupBreakdowns || [];
    if (breakdowns.length === 0) return;

    const maxTotal = breakdowns[0]?.total || 1;
    const showSplitCols = chartData.docType === "BOTH" || breakdowns.some(g => g.created > 0 || g.migrated > 0);

    const tableWrap = document.createElement("div");
    tableWrap.className = "group-detail-table";

    const header = document.createElement("div");
    header.className = "group-detail-header";
    header.innerHTML = `
        <div class="group-detail-title">
            <span>📋</span>
            <span>Group, Subgroup & Artifact Details</span>
            <span style="font-size:10px;font-weight:600;padding:1px 7px;border-radius:100px;background:rgba(91,141,238,.12);color:var(--accent);">${breakdowns.length} groups</span>
            <span style="font-size:10px;color:var(--muted);font-weight:400;margin-left:2px;">— click group rows to expand hierarchy</span>
        </div>
        <span class="group-detail-toggle">▼</span>`;
    header.addEventListener("click", () => tableWrap.classList.toggle("collapsed"));
    tableWrap.appendChild(header);

    const body = document.createElement("div");
    body.className = "group-detail-body";

    const table = document.createElement("table");
    table.className = "gd-table";

    let thHtml = `<tr>
        <th style="width:24px;"></th>
        <th style="width:32px;">#</th>
        <th>GMP Group / Subgroup / Artifact</th>
        <th class="num">Total</th>`;
        
    if (showSplitCols) {
        thHtml += `<th style="text-align:center; color:#4f8ef7;">CREATED/IMPORT</th>
                   <th style="text-align:center; color:#a78bfa;">MIGRATED</th>`;
    }
    thHtml += `<th>Distribution</th></tr>`;
    
    const thead = document.createElement("thead");
    thead.innerHTML = thHtml;
    table.appendChild(thead);

    const tbody = document.createElement("tbody");
    const selectedGroups = new Set();

    breakdowns.forEach((g, idx) => {
        const grpName = g.gmpGroup;
        const ci = (sharedColorMap[grpName] ?? idx) % SOLID_COLORS.length;
        const color = SOLID_COLORS[ci];
        const barPct = Math.round((g.total / maxTotal) * 100);
        const childArtifacts = g.artifacts || [];

        // 1. PARENT ROW (GMP Group)
        const tr = document.createElement("tr");
        tr.className = "gd-row gd-parent-row";
        tr.dataset.gmp = grpName;

        let tdHtml = `
            <td style="text-align:center;cursor:pointer;user-select:none;color:var(--accent);font-size:10px;" class="accordion-toggle-btn">
                ${childArtifacts.length > 0 ? '▶' : ''}
            </td>
            <td style="color:var(--muted);font-size:10px;font-weight:700;">#${idx+1}</td>
            <td>
                <div class="gd-name" style="display:flex;align-items:center;gap:6px;">
                    <span class="gd-dot" style="background:${color};box-shadow:0 0 5px ${color}70;"></span>
                    <span style="font-size:12px;font-weight:700;color:var(--text);" title="${grpName}">${grpName}</span>
                    ${childArtifacts.length > 0 ? `<span style="font-size:9.5px;padding:1px 6px;border-radius:10px;background:rgba(255,255,255,0.06);color:var(--muted);">${childArtifacts.length} artifacts</span>` : ''}
                </div>
            </td>
            <td class="num" style="font-weight:700;color:var(--text);">${g.total.toLocaleString()}</td>`;

        if (showSplitCols) {
            tdHtml += `
            <td style="text-align:center; vertical-align:middle;">
                ${g.created > 0 ? `<span class="badge-created">${g.created.toLocaleString()}</span>` : '<span style="color:var(--muted)">—</span>'}
            </td>
            <td style="text-align:center; vertical-align:middle;">
                ${g.migrated > 0 ? `<span class="badge-migrated">${g.migrated.toLocaleString()}</span>` : '<span style="color:var(--muted)">—</span>'}
            </td>`;
        }

        tdHtml += `
            <td class="gd-bar-cell">
                <div class="gd-bar-wrap">
                    <div class="gd-bar-fill" style="width:${barPct}%;background:${color};"></div>
                </div>
                <div style="font-size:9px;color:var(--muted);margin-top:2px;">${barPct}%</div>
            </td>`;
        tr.innerHTML = tdHtml;

        // 2. SUB-ROWS (Grouped by Subgroup)
        const subRows = [];
        
        // Map artifacts to their Subgroups natively inside JS
        const subgroupMap = new Map();
        childArtifacts.forEach(art => {
            const sgName = art.gmpSubgroup || "(No Subgroup)";
            if (!subgroupMap.has(sgName)) {
                subgroupMap.set(sgName, { total: 0, created: 0, migrated: 0, artifacts: [] });
            }
            const sg = subgroupMap.get(sgName);
            sg.total += (art.total || 0);
            sg.created += (art.created || 0);
            sg.migrated += (art.migrated || 0);
            sg.artifacts.push(art);
        });

        // Create HTML for Subgroups and their Artifacts
        subgroupMap.forEach((sgData, sgName) => {
            const sgPct = g.total > 0 ? Math.round((sgData.total / g.total) * 100) : 0;

            // -- Subgroup Header Row --
            const sgTr = document.createElement("tr");
            sgTr.className = "gd-row gd-sub-row";
            sgTr.style.display = "none";
            sgTr.style.background = "rgba(91, 141, 238, 0.04)"; // Faint blue background for subgroups

            let sgHtml = `
                <td></td>
                <td></td>
                <td>
                    <div style="display:flex;align-items:center;gap:6px;padding-left:16px;">
                        <span style="font-size:11px;color:var(--accent);font-weight:700;">📁 ${sgName}</span>
                    </div>
                </td>
                <td class="num" style="font-size:11px;font-weight:600;color:var(--text);">${sgData.total.toLocaleString()}</td>`;

            if (showSplitCols) {
                sgHtml += `
                <td style="text-align:center; font-size:11px; vertical-align:middle;">
                    ${sgData.created > 0 ? `<span style="color:#4f8ef7; font-weight:600;">${sgData.created.toLocaleString()}</span>` : '<span style="color:var(--muted)">—</span>'}
                </td>
                <td style="text-align:center; font-size:11px; vertical-align:middle;">
                    ${sgData.migrated > 0 ? `<span style="color:#a78bfa; font-weight:600;">${sgData.migrated.toLocaleString()}</span>` : '<span style="color:var(--muted)">—</span>'}
                </td>`;
            }

            sgHtml += `
                <td class="gd-bar-cell">
                    <div class="gd-bar-wrap" style="height:4px;background:rgba(255,255,255,0.05);">
                        <div class="gd-bar-fill" style="width:${sgPct}%;background:${color};opacity:0.9;"></div>
                    </div>
                    <div style="font-size:8.5px;color:var(--muted);margin-top:2px;">${sgPct}% of group</div>
                </td>`;
            sgTr.innerHTML = sgHtml;
            subRows.push(sgTr);

            // -- Artifact Rows inside this Subgroup --
            sgData.artifacts.forEach(art => {
                const artTr = document.createElement("tr");
                artTr.className = "gd-row gd-sub-row";
                artTr.style.display = "none";
                artTr.style.background = "rgba(255,255,255,0.015)";

                const artPct = g.total > 0 ? Math.round((art.total / g.total) * 100) : 0;

                let artHtml = `
                    <td></td>
                    <td></td>
                    <td>
                        <div style="display:flex;align-items:center;gap:6px;padding-left:36px;">
                            <span style="color:var(--muted);font-size:11px;opacity:0.6;">└─</span>
                            <span style="font-size:11px;color:var(--sub);font-weight:500;">${art.artifactName}</span>
                        </div>
                    </td>
                    <td class="num" style="font-size:11px;color:var(--text);">${art.total.toLocaleString()}</td>`;

                if (showSplitCols) {
                    artHtml += `
                    <td style="text-align:center; font-size:11px; vertical-align:middle;">
                        ${art.created > 0 ? `<span style="color:#4f8ef7;">${art.created.toLocaleString()}</span>` : '<span style="color:var(--muted)">—</span>'}
                    </td>
                    <td style="text-align:center; font-size:11px; vertical-align:middle;">
                        ${art.migrated > 0 ? `<span style="color:#a78bfa;">${art.migrated.toLocaleString()}</span>` : '<span style="color:var(--muted)">—</span>'}
                    </td>`;
                }

                artHtml += `
                    <td class="gd-bar-cell">
                        <div class="gd-bar-wrap" style="height:4px;background:rgba(255,255,255,0.05);">
                            <div class="gd-bar-fill" style="width:${artPct}%;background:${color};opacity:0.4;"></div>
                        </div>
                        <div style="font-size:8.5px;color:var(--muted);margin-top:2px;">${artPct}% of group</div>
                    </td>`;
                artTr.innerHTML = artHtml;
                subRows.push(artTr);
            });
        });

        // Toggle logic (expands everything for the group on one click)
        let isExpanded = false;
        tr.addEventListener("click", (e) => {
            isExpanded = !isExpanded;
            const toggleIcon = tr.querySelector(".accordion-toggle-btn");
            if (toggleIcon && childArtifacts.length > 0) {
                toggleIcon.textContent = isExpanded ? "▼" : "▶";
            }
            subRows.forEach(sr => sr.style.display = isExpanded ? "table-row" : "none");

            const isSelected = tr.classList.contains("selected");
            if (isSelected) {
                selectedGroups.delete(grpName);
                tr.classList.remove("selected");
            } else {
                selectedGroups.add(grpName);
                tr.classList.add("selected");
            }

            if (selectedGroups.size === 0) {
                datasets.forEach(ds => {
                    if (ds._orig) { ds.backgroundColor = ds._orig.bg; ds.borderColor = ds._orig.br; }
                });
            } else {
                datasets.forEach((ds, di) => {
                    const s = chartData.series[di];
                    const grp = s?.gmpGroup || (s?.name || "").replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
                    if (!ds._orig) ds._orig = { bg: ds.backgroundColor, br: ds.borderColor };
                    if (selectedGroups.has(grp)) {
                        ds.backgroundColor = ds._orig.bg;
                        ds.borderColor = ds._orig.br;
                    } else {
                        ds.backgroundColor = ds._orig.bg.replace(/[\d.]+\)$/, "0.06)");
                        ds.borderColor = ds._orig.br.replace(/[\d.]+\)$/, "0.08)");
                    }
                });
            }
            chartInstance.update("none");
        });

        tbody.appendChild(tr);
        subRows.forEach(sr => tbody.appendChild(sr));
    });

    table.appendChild(tbody);
    body.appendChild(table);
    tableWrap.appendChild(body);
    parentWrap.appendChild(tableWrap);
}

/* ══════════════════════════════════════════════════
   CHART LEGEND BAR
══════════════════════════════════════════════════ */
function buildChartLegendBar(parentWrap, chartData, sharedColorMap) {
    const hasBoth = chartData.series.some(s => s.seriesType === "CREATED") &&
                    chartData.series.some(s => s.seriesType === "MIGRATED");

    const seen = new Map();
    chartData.series.forEach((s, i) => {
        const grp = s.gmpGroup || s.name.replace(/\s+—\s+(Created\/Import|Migrated)$/i,"").trim();
        if (!seen.has(grp)) {
            const ci = (sharedColorMap[grp] ?? i) % SOLID_COLORS.length;
            seen.set(grp, ci);
        }
    });

    const wrap = document.createElement("div");
    wrap.style.cssText = "display:flex;flex-direction:column;gap:6px;padding:6px 4px 2px;flex-shrink:0;";

    seen.forEach((ci, grp) => {
        const [r,g,b] = SOLID_COLORS_RGB[ci];
        const solidHex = SOLID_COLORS[ci];

        const row = document.createElement("div");
        row.style.cssText = "display:flex;align-items:center;gap:10px;flex-wrap:wrap;";

        const lbl = document.createElement("span");
        lbl.style.cssText = "font-size:11px;font-weight:600;color:var(--text);min-width:90px;";
        lbl.textContent = grp.length > 22 ? grp.substring(0,20)+"…" : grp;
        row.appendChild(lbl);

        const crItem = document.createElement("div");
        crItem.style.cssText = "display:flex;align-items:center;gap:5px;font-size:10px;color:var(--muted);";
        const crBox = document.createElement("div");
        crBox.style.cssText = `width:20px;height:10px;border-radius:2px;background:rgba(${r},${g},${b},0.85);flex-shrink:0;`;
        crItem.appendChild(crBox);
        crItem.appendChild(document.createTextNode("Created/Import"));
        row.appendChild(crItem);

        if (hasBoth) {
            const mgItem = document.createElement("div");
            mgItem.style.cssText = "display:flex;align-items:center;gap:5px;font-size:10px;color:var(--muted);";
            const mgBox = document.createElement("div");
            mgBox.style.cssText = [
                "width:20px","height:10px","border-radius:2px","flex-shrink:0",
                `border:1.5px solid ${solidHex}`,
                "background:repeating-linear-gradient(" +
                    "45deg," +
                    `rgba(${r},${g},${b},0.55) 0px,` +
                    `rgba(${r},${g},${b},0.55) 4px,` +
                    "rgba(255,255,255,0.6) 4px," +
                    "rgba(255,255,255,0.6) 7px)"
            ].join(";");
            mgItem.appendChild(mgBox);
            mgItem.appendChild(document.createTextNode("Migrated"));
            row.appendChild(mgItem);
        }

        wrap.appendChild(row);
    });

    parentWrap.appendChild(wrap);
}