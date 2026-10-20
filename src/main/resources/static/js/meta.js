function loadOwningSites() {
    fetch("/api/meta/owning-sites")
        .then(res => res.json())
        .then(data => {
            const dropdown = document.getElementById("owningSite");
            dropdown.innerHTML = "<option value=''>— Select site —</option>";
            Array.from(data).sort().forEach(site => {
                dropdown.innerHTML += `<option value="${site}">${site}</option>`;
            });
            dropdown.disabled = false;
        });
}

function loadCreationProfiles() {
    const site = document.getElementById("owningSite").value;
    if (!site) return;

    fetch(`/api/meta/creation-profiles?owningSite=${encodeURIComponent(site)}`)
        .then(res => res.json())
        .then(data => {
            const sorted = Array.from(data).sort();
            buildChkList("profile", sorted, false);
            const trigger = document.getElementById("profileTrigger");
            if (trigger) trigger.classList.remove("disabled");
            const lbl = document.getElementById("profileTriggerLabel");
            if (lbl) lbl.textContent = "— Select profiles —";
            resetGmpDropdown();
            resetSubgroupDropdown();
            resetArtifactDropdown();
        });
}

function resetGmpDropdown() {
    const trigger = document.getElementById("gmpTrigger");
    if (trigger) trigger.classList.add("disabled");
    const lbl = document.getElementById("gmpTriggerLabel");
    if (lbl) lbl.textContent = "— Select profiles first —";
    buildChkList("gmp", [], false);
    resetSubgroupDropdown();
    resetArtifactDropdown();
}

function resetSubgroupDropdown() {
    const trigger = document.getElementById("subgroupTrigger");
    if (trigger) trigger.classList.add("disabled");
    const lbl = document.getElementById("subgroupTriggerLabel");
    if (lbl) lbl.textContent = "— Select group first —";
    buildChkList("subgroup", [], false);
    resetArtifactDropdown();
}

function resetArtifactDropdown() {
    const trigger = document.getElementById("artifactTrigger");
    if (trigger) trigger.classList.add("disabled");
    const lbl = document.getElementById("artifactTriggerLabel");
    if (lbl) lbl.textContent = "— Select sub group first —";
    buildChkList("artifact", [], false);
}

function loadGmpGroups() {
    const site     = document.getElementById("owningSite").value;
    const profiles = getSelectedProfiles();

    if (!site || profiles.length === 0) {
        resetGmpDropdown();
        return;
    }

    const promises = profiles.map(profile =>
        fetch(`/api/meta/gmp-groups?owningSite=${encodeURIComponent(site)}&creationProfile=${encodeURIComponent(profile)}`)
            .then(res => res.json())
            .then(groups => ({ profile, groups: Array.from(groups) }))
    );

    Promise.all(promises).then(results => {
        const enable = () => {
            const trigger = document.getElementById("gmpTrigger");
            if (trigger) trigger.classList.remove("disabled");
            const lbl = document.getElementById("gmpTriggerLabel");
            if (lbl) lbl.textContent = "— All groups (no filter) —";
        };

        if (profiles.length === 1) {
            const sorted = Array.from(new Set(results[0].groups)).sort();
            if (sorted.length === 0) { resetGmpDropdown(); return; }
            buildChkList("gmp", sorted, false);
            enable();
        } else {
            const grouped = results.map(r => ({
                group: r.profile,
                items: Array.from(new Set(r.groups)).sort()
            })).filter(grp => grp.items.length > 0);

            if (grouped.length === 0) { resetGmpDropdown(); return; }
            buildChkList("gmp", grouped, true);
            enable();
        }
        loadGmpSubgroups();
    }).catch(() => resetGmpDropdown());
}

function loadGmpSubgroups() {
    const site      = document.getElementById("owningSite").value;
    const profiles  = getSelectedProfiles();
    const gmpGroups = getSelectedGmps();

    if (!site || profiles.length === 0) {
        resetSubgroupDropdown();
        return;
    }

    let url = `/api/meta/gmp-subgroups?owningSite=${encodeURIComponent(site)}`;
    profiles.forEach(p => { url += `&creationProfiles=${encodeURIComponent(p)}`; });
    gmpGroups.forEach(g => { url += `&gmpGroups=${encodeURIComponent(g)}`; });

    fetch(url)
        .then(res => res.json())
        .then(data => {
            const trigger = document.getElementById("subgroupTrigger");
            const lbl     = document.getElementById("subgroupTriggerLabel");

            const grouped = Object.keys(data).sort().map(grp => ({
                group: grp,
                items: Array.from(data[grp]).sort()
            })).filter(g => g.items.length > 0);

            if (grouped.length === 0) {
                resetSubgroupDropdown();
                if (lbl) lbl.textContent = "— No sub groups found —";
                return;
            }

            buildChkList("subgroup", grouped, true);
            if (trigger) trigger.classList.remove("disabled");
            if (lbl) lbl.textContent = "— All sub groups (no filter) —";

            loadGmpArtifacts();
        })
        .catch(() => resetSubgroupDropdown());
}

function loadGmpArtifacts() {
    const site         = document.getElementById("owningSite").value;
    const profiles     = getSelectedProfiles();
    const gmpGroups    = getSelectedGmps();
    const gmpSubgroups = getSelectedSubgroups();

    if (!site || profiles.length === 0) {
        resetArtifactDropdown();
        return;
    }

    let url = `/api/meta/gmp-artifacts-grouped?owningSite=${encodeURIComponent(site)}`;
    profiles.forEach(p => { url += `&creationProfiles=${encodeURIComponent(p)}`; });
    gmpGroups.forEach(g => { url += `&gmpGroups=${encodeURIComponent(g)}`; });
    gmpSubgroups.forEach(s => { url += `&gmpSubgroups=${encodeURIComponent(s)}`; });

    fetch(url)
        .then(res => res.json())
        .then(data => {
            const trigger = document.getElementById("artifactTrigger");
            const lbl     = document.getElementById("artifactTriggerLabel");

            const grouped = Object.keys(data).sort().map(grp => ({
                group: grp,
                items: Array.from(data[grp]).sort()
            })).filter(g => g.items.length > 0);

            if (grouped.length === 0) {
                resetArtifactDropdown();
                if (lbl) lbl.textContent = "— No artifacts found —";
                return;
            }

            buildChkList("artifact", grouped, true);
            if (trigger) trigger.classList.remove("disabled");
            if (lbl) lbl.textContent = "— All artifacts (no filter) —";
        })
        .catch(() => resetArtifactDropdown());
}

function loadYears() {
    fetch("/api/meta/years")
        .then(res => res.json())
        .then(years => { populateYearDropdown(years); });
}
