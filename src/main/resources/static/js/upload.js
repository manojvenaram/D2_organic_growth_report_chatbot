function uploadExcel() {

    const fileInput = document.getElementById("excelFile");

    if (!fileInput.files || fileInput.files.length === 0) {
        alert("Please select an Excel file first.");
        return;
    }

    setStatus("⏳ Uploading and processing…", "info");
    document.getElementById("spinner").classList.add("on");

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    fetch("/api/excel/upload", {
        method: "POST",
        body: formData
    })
    .then(res => {
        if (!res.ok) return res.json().then(err => Promise.reject(err.error || JSON.stringify(err)));
        return res.json();
    })
    .then(data => {
        setStatus("✅ " + (data.message || "Excel uploaded successfully"), "ok");
        loadOwningSites();
        loadYears();
        if (typeof loadGrowthAnalytics === "function") {
            loadGrowthAnalytics();
        }
    })
    .catch(err => {
        setStatus("❌ Upload failed: " + err, "err");
    })
    .finally(() => {
        document.getElementById("spinner").classList.remove("on");
    });
}

function loadSampleData() {
    setStatus("⏳ Loading sample Documentum D2 dataset…", "info");
    document.getElementById("spinner").classList.add("on");

    fetch("/api/excel/load-sample", {
        method: "POST"
    })
    .then(res => {
        if (!res.ok) return res.json().then(err => Promise.reject(err.error || JSON.stringify(err)));
        return res.json();
    })
    .then(data => {
        setStatus("✅ " + (data.message || "Sample data loaded successfully"), "ok");
        if (typeof showPostUploadSections === "function") showPostUploadSections();
        loadOwningSites();
        loadYears();
        if (typeof loadGrowthAnalytics === "function") {
            loadGrowthAnalytics();
        }
        if (typeof loadChatSuggestions === "function") {
            loadChatSuggestions();
        }
    })
    .catch(err => {
        setStatus("❌ Failed: " + err, "err");
    })
    .finally(() => {
        document.getElementById("spinner").classList.remove("on");
    });
}