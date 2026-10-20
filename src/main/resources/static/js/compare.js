let compareChartInstance = null;
let expandedProfiles     = new Set();

const SITE_COLORS = ["#5b8dee","#10b981","#f59e0b","#f43f5e","#a78bfa","#06b6d4","#ec4899","#f97316"];
const SITE_FILLS  = [
  "rgba(91,141,238,.75)","rgba(16,185,129,.75)","rgba(245,158,11,.75)",
  "rgba(244,63,94,.75)","rgba(167,139,250,.75)","rgba(6,182,212,.75)",
  "rgba(236,72,153,.75)","rgba(249,115,22,.75)"
];

// ── Populate year dropdowns ───────────────────────────────────────
function populateCompareYears(years) {
  const sorted = Array.from(years).map(Number).sort((a,b) => a-b);
  ["compareYearFrom","compareYearTo"].forEach(id => {
    const s = document.getElementById(id); if (!s) return;
    s.innerHTML = sorted.map(y => `<option value="${y}">${y}</option>`).join("");
  });
  document.getElementById("compareYearFrom").value = sorted[0];
  document.getElementById("compareYearTo").value   = sorted[sorted.length-1];
}

// ── Run comparison ────────────────────────────────────────────────
function runComparison() {
  const sites = getSelectedCompareSites();
  if (sites.length < 2) { alert("Please select at least 2 sites to compare."); return; }

  const years = getYearRange("compareYearFrom","compareYearTo");
  if (years.length === 0) { alert("Please select a valid year range."); return; }

  const docType = document.getElementById("cmpDocType")?.value || "BOTH";

  document.getElementById("spinner").classList.add("on");

  fetch("/api/compare/sites", {
    method: "POST",
    headers: { "Content-Type":"application/json" },
    body: JSON.stringify({ sites, years, docType })
  })
  .then(res => {
    if (!res.ok) return res.json().then(e => Promise.reject(e.error || JSON.stringify(e)));
    return res.json();
  })
  .then(data => renderComparison(data))
  .catch(err => { console.error(err); alert("Error: " + err); })
  .finally(() => document.getElementById("spinner").classList.remove("on"));
}

// ── Render all comparison results ─────────────────────────────────
function renderComparison(data) {
  document.getElementById("compareEmptyState").style.display = "none";
  const res = document.getElementById("compareResult");
  res.style.display = "flex";
  res.style.flexDirection = "column";

  const sites   = data.sites;
  const topIdx  = data.grandTotals.indexOf(Math.max(...data.grandTotals));
  expandedProfiles.clear();

  // ── Summary cards ─────────────────────────────────────────────
  document.getElementById("cTotalDocs").textContent    = data.overallTotal.toLocaleString();
  document.getElementById("cTopSite").textContent      = shortName(sites[topIdx]);
  document.getElementById("cTopDocs").textContent      = data.grandTotals[topIdx].toLocaleString() + " docs";
  document.getElementById("cProfileCount").textContent = data.profiles.length;
  document.getElementById("cYearRange").textContent    = data.years[0] + " – " + data.years[data.years.length-1];

  const dtEl = document.getElementById("cDocType");
  if (dtEl) {
    const dtLabel = { BOTH:"Created + Migrated", CREATED:"Created/Import", MIGRATED:"Migrated" };
    dtEl.textContent = dtLabel[data.docType || "BOTH"] || "All";
  }

  // ── Table ─────────────────────────────────────────────────────
  renderTable(data);

  // ── Chart ─────────────────────────────────────────────────────
  renderChart(data);

  // ── Legend ────────────────────────────────────────────────────
  renderLegend(data.sites);
}

// ── Expandable profile → GMP table ───────────────────────────────
function renderTable(data) {
  const sites     = data.sites;
  const container = document.getElementById("compareTableContainer");
  if (!container) return;

  const siteHeaders = sites.map((s,i) =>
    `<th class="num" style="color:${SITE_COLORS[i % SITE_COLORS.length]}">${shortName(s)}</th>`
  ).join("");

  let html = `<table>
    <thead>
      <tr>
        <th style="min-width:200px">Creation Profile / GMP Group</th>
        ${siteHeaders}
        <th class="num">Total</th>
        <th>Leader</th>
      </tr>
    </thead>
    <tbody>`;

  data.profiles.forEach((profile, pi) => {
    const maxVal    = Math.max(...profile.siteTotals);
    const leaderIdx = profile.siteTotals.indexOf(maxVal);
    const leaderColor = SITE_COLORS[leaderIdx % SITE_COLORS.length];
    const allEqual  = profile.siteTotals.every(v => v === profile.siteTotals[0]);

    const profileCells = profile.siteTotals.map((val, i) => {
      const pct    = profile.rowTotal > 0 ? Math.round((val / profile.rowTotal) * 100) : 0;
      const isLead = val === maxVal && !allEqual;
      return `<td class="num">
        <span style="${isLead ? 'font-weight:700;color:' + SITE_COLORS[i % SITE_COLORS.length] : ''}">${val.toLocaleString()}</span>
        <span style="font-size:10px;color:var(--muted);margin-left:4px">${pct}%</span>
      </td>`;
    }).join("");

    html += `<tr class="profile-row" onclick="toggleProfile(${pi})">
      <td>
        <span style="display:inline-flex;align-items:center;gap:8px;">
          <span id="exp-icon-${pi}" style="font-size:10px;color:var(--muted);transition:transform .2s;display:inline-block">▶</span>
          <span style="font-weight:600">${profile.profile}</span>
          <span style="font-size:10px;color:var(--muted);background:var(--card);padding:1px 6px;border-radius:10px;">${profile.gmpRows ? profile.gmpRows.length : 0} groups</span>
        </span>
      </td>
      ${profileCells}
      <td class="num" style="font-weight:600">${profile.rowTotal.toLocaleString()}</td>
      <td>
        ${!allEqual && maxVal > 0
          ? `<span style="font-size:10px;padding:2px 7px;border-radius:10px;font-weight:600;background:rgba(${hexToRgb(leaderColor)},.12);color:${leaderColor}">${shortName(sites[leaderIdx])}</span>`
          : `<span style="font-size:10px;color:var(--muted)">Tied</span>`}
      </td>
    </tr>`;

    // GMP sub-rows (hidden by default)
    if (profile.gmpRows) {
      profile.gmpRows.forEach((gmpRow, gi) => {
        const gmpMax    = Math.max(...gmpRow.siteTotals);
        const gmpLeader = gmpRow.siteTotals.indexOf(gmpMax);
        const gmpColor  = SITE_COLORS[gmpLeader % SITE_COLORS.length];
        const gmpEqual  = gmpRow.siteTotals.every(v => v === gmpRow.siteTotals[0]);

        const gmpCells = gmpRow.siteTotals.map((val, i) => {
          const pct    = gmpRow.rowTotal > 0 ? Math.round((val / gmpRow.rowTotal) * 100) : 0;
          const isLead = val === gmpMax && !gmpEqual;
          const barW   = gmpRow.rowTotal > 0 ? Math.round((val / gmpRow.rowTotal) * 50) : 0;
          return `<td class="num">
            <div style="display:flex;align-items:center;justify-content:flex-end;gap:5px;">
              <div style="width:50px;height:4px;background:rgba(255,255,255,.06);border-radius:3px;overflow:hidden;">
                <div style="width:${barW}px;height:100%;background:${SITE_COLORS[i % SITE_COLORS.length]};border-radius:3px;"></div>
              </div>
              <span style="${isLead ? 'font-weight:700;color:' + SITE_COLORS[i % SITE_COLORS.length] : 'color:var(--sub)'}">${val}</span>
              <span style="font-size:10px;color:var(--muted);min-width:26px;">${pct}%</span>
            </div>
          </td>`;
        }).join("");

        html += `<tr id="gmp-of-${pi}-${gi}" style="display:none;">
          <td style="padding-left:28px;color:var(--sub);font-size:11.5px;">
            <span style="margin-right:6px;opacity:.4">└</span>${gmpRow.gmpGroup}
          </td>
          ${gmpCells}
          <td class="num" style="color:var(--muted);font-size:12px;">${gmpRow.rowTotal}</td>
          <td>
            ${!gmpEqual && gmpMax > 0
              ? `<span style="font-size:10px;color:${gmpColor}">${shortName(sites[gmpLeader])}</span>`
              : `<span style="font-size:10px;color:var(--muted)">—</span>`}
          </td>
        </tr>`;
      });
    }
  });

  // Grand total row
  const grandCells = data.grandTotals.map((t, i) =>
    `<td class="num" style="color:${SITE_COLORS[i % SITE_COLORS.length]};font-weight:700">${t.toLocaleString()}</td>`
  ).join("");

  html += `<tr class="grand-total">
    <td>Grand Total</td>
    ${grandCells}
    <td class="num" style="font-weight:700">${data.overallTotal.toLocaleString()}</td>
    <td></td>
  </tr>
  </tbody></table>`;

  container.innerHTML = html;
}

// ── Toggle profile expand/collapse ────────────────────────────────
function toggleProfile(pi) {
  if (expandedProfiles.has(pi)) {
    expandedProfiles.delete(pi);
  } else {
    expandedProfiles.add(pi);
  }
  const isExpanded = expandedProfiles.has(pi);
  const icon = document.getElementById("exp-icon-" + pi);
  if (icon) icon.style.transform = isExpanded ? "rotate(90deg)" : "";
  document.querySelectorAll(`[id^="gmp-of-${pi}-"]`).forEach(row => {
    row.style.display = isExpanded ? "table-row" : "none";
  });
}

// ── Grouped bar chart ─────────────────────────────────────────────
function renderChart(data) {
  if (compareChartInstance) compareChartInstance.destroy();

  const canvas = document.getElementById("compareChart");
  if (!canvas) return;

  const sites  = data.sites;
  const labels = data.profiles.map(p =>
    p.profile.length > 22 ? p.profile.substring(0, 22) + "…" : p.profile
  );

  compareChartInstance = new Chart(canvas.getContext("2d"), {
    type: "bar",
    data: {
      labels,
      datasets: sites.map((site, si) => ({
        label          : shortName(site),
        data           : data.profiles.map(p => p.siteTotals[si]),
        backgroundColor: SITE_FILLS[si % SITE_FILLS.length],
        borderColor    : SITE_COLORS[si % SITE_COLORS.length],
        borderWidth    : 1,
        borderRadius   : 4
      }))
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: "rgba(19,22,31,.97)",
          borderColor: "rgba(91,141,238,.3)", borderWidth: 1, padding: 12,
          callbacks: {
            label: ctx => "  " + ctx.dataset.label + ": " + ctx.parsed.y.toLocaleString() + " docs"
          }
        }
      },
      scales: {
        x: {
          grid : { color:"rgba(255,255,255,.04)" },
          ticks: { color:"#64748b", font:{ size:11, family:"Inter" }, maxRotation:35, autoSkip:false }
        },
        y: {
          beginAtZero: true,
          grid : { color:"rgba(255,255,255,.04)" },
          ticks: { precision:0, color:"#64748b", font:{ size:11, family:"Inter" } },
          title: { display:true, text:"Documents", color:"#64748b", font:{ size:11, weight:"600", family:"Inter" } }
        }
      }
    }
  });
}

// ── Legend ────────────────────────────────────────────────────────
function renderLegend(sites) {
  const el = document.getElementById("compareLegend");
  if (!el) return;
  el.innerHTML = sites.map((s, i) =>
    `<div class="leg-item">
      <span style="display:inline-block;width:12px;height:12px;background:${SITE_COLORS[i % SITE_COLORS.length]};border-radius:3px;"></span>
      ${s}
    </div>`
  ).join("");
}

// ── Helpers ───────────────────────────────────────────────────────
function shortName(site) { return site.split(" - ")[0].trim(); }

function hexToRgb(hex) {
  return `${parseInt(hex.slice(1,3),16)},${parseInt(hex.slice(3,5),16)},${parseInt(hex.slice(5,7),16)}`;
}