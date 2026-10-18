const API_BASE = "http://localhost:8080";

const state = {
  vendors: [],
  orders: [],
  performance: [],
  alerts: [],
  compliance: [],
  performanceChart: null,
  vendorScoreChart: null
};

const $ = (id) => document.getElementById(id);

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function money(value) {
  const n = Number(value);
  return Number.isFinite(n) ? "â‚¹" + n.toLocaleString("en-IN") : "--";
}

function badge(value) {
  const text = String(value ?? "UNKNOWN");
  const upper = text.toUpperCase();
  let cls = "neutral";
  if (["DELIVERED", "ACTIVE", "OPEN"].includes(upper)) cls = "success";
  if (["PENDING", "MEDIUM", "IN_PROGRESS"].includes(upper)) cls = "warning";
  if (["HIGH", "FAILED", "CLOSED"].includes(upper)) cls = "danger";
  return `<span class="badge ${cls}">${escapeHtml(text)}</span>`;
}

async function api(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, options);

  if (!response.ok) {
    const message = await response.text();
    throw new Error(`${response.status}: ${message || response.statusText}`);
  }

  if (response.status === 204) return null;
  return response.json();
}

function showToast(message) {
  const toast = $("toast");
  toast.textContent = message;
  toast.classList.add("show");
  setTimeout(() => toast.classList.remove("show"), 3000);
}

function showPage(pageId) {
  document.querySelectorAll(".page").forEach(page => page.classList.remove("active-page"));
  document.querySelectorAll(".nav-item").forEach(item => item.classList.remove("active"));

  const page = $(pageId);
  if (page) page.classList.add("active-page");

  const nav = document.querySelector(`.nav-item[data-page="${pageId}"]`);
  if (nav) nav.classList.add("active");

  window.scrollTo({ top: 0, behavior: "smooth" });

  if (pageId === "vendors") renderVendors();
  if (pageId === "orders") renderOrders();
  if (pageId === "performance") renderPerformance();
  if (pageId === "alerts") renderAlerts();
  if (pageId === "compliance") renderCompliance();
}

document.querySelectorAll(".nav-item").forEach(button => {
  button.addEventListener("click", () => showPage(button.dataset.page));
});

document.querySelectorAll("[data-page-target]").forEach(button => {
  button.addEventListener("click", () => showPage(button.dataset.pageTarget));
});

async function loadVendors() {
  try {
    const data = await api("/api/vendors");
    state.vendors = Array.isArray(data) ? data : [];
    $("totalVendors").textContent = state.vendors.length;
    renderVendors();
    renderCompliance();
  } catch (error) {
    console.error("Vendor API:", error);
    $("totalVendors").textContent = "--";
  }
}

async function loadOrders() {
  try {
    const data = await api("/api/purchase-orders");
    state.orders = Array.isArray(data) ? data : [];
    $("totalPOs").textContent = state.orders.length;
    renderOrders();
    renderRecentOrders();
    calculateDeliveryKpis();
  } catch (error) {
    console.error("Purchase order API:", error);
    $("totalPOs").textContent = "--";
  }
}

async function loadPerformance() {
  try {
    const data = await api("/api/vendor-performance");
    state.performance = Array.isArray(data) ? data : [];
    renderPerformance();
  } catch (error) {
    console.warn("Performance endpoint unavailable:", error);
    renderPerformanceFallback();
  }
}

async function loadAlerts() {
  try {
    const data = await api("/api/alerts/open");
    state.alerts = Array.isArray(data) ? data : [];
    renderAlerts();
  } catch (error) {
    console.warn("Alert endpoint unavailable:", error);
    $("alertsTable").innerHTML =
      `<tr><td colspan="5" class="empty">Alert data could not be loaded.</td></tr>`;
  }
}

async function loadCompliance() {
  try {
    const data = await api("/api/compliance");
    state.compliance = Array.isArray(data) ? data : [];
    renderCompliance();
  } catch (error) {
    console.error("Compliance API:", error);
    const tbody = $("complianceTable");
    if (tbody) {
      tbody.innerHTML =
        `<tr><td colspan="5" class="empty">Compliance documents could not be loaded.</td></tr>`;
    }
  }
}
function renderCompliance() {
  const vendorSelect = $("complianceVendor");
  const tbody = $("complianceTable");
  if (vendorSelect) {
    const selectedVendor = vendorSelect.value;
    vendorSelect.innerHTML =
      '<option value="">Select vendor</option>' +
      state.vendors.map(v =>
        `<option value="${escapeHtml(v.id)}">${escapeHtml(v.name || v.vendorName || ("Vendor " + v.id))}</option>`
      ).join("");
    if (state.vendors.some(v => String(v.id) === selectedVendor)) {
      vendorSelect.value = selectedVendor;
    }
  }
  if (!tbody) return;
  if (!state.compliance.length) {
    tbody.innerHTML =
      '<tr><td colspan="5" class="empty">No compliance documents registered yet.</td></tr>';
    return;
  }
  tbody.innerHTML = [...state.compliance].reverse().map(doc => `
    <tr>
      <td>${escapeHtml(doc.documentName || "--")}</td>
      <td>${escapeHtml(doc.vendor?.name || doc.vendor?.vendorName || ("Vendor " + (doc.vendor?.id ?? "--")))}</td>
      <td>${escapeHtml(doc.referenceNumber || "--")}</td>
      <td>${escapeHtml(doc.expiryDate || "--")}</td>
      <td>${badge(doc.complianceStatus || "UNKNOWN")}</td>
    </tr>
  `).join("");
}
$("complianceForm").addEventListener("submit", async event => {
  event.preventDefault();
  const vendorId = $("complianceVendor").value;
  if (!vendorId) {
    showToast("Please select a vendor.");
    return;
  }
  const button = $("saveComplianceButton");
  button.disabled = true;
  button.textContent = "Saving...";
  const payload = {
    documentName: $("complianceDocumentName").value.trim(),
    referenceNumber: $("complianceReferenceNumber").value.trim(),
    issueDate: $("complianceIssueDate").value || null,
    expiryDate: $("complianceExpiryDate").value,
    notes: $("complianceNotes").value.trim()
  };
  try {
    await api(`/api/compliance/vendors/${encodeURIComponent(vendorId)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    event.target.reset();
    await loadCompliance();
    showToast("Compliance document saved successfully.");
  } catch (error) {
    console.error("Saving compliance document failed:", error);
    showToast("Could not save document: " + error.message);
  } finally {
    button.disabled = false;
    button.textContent = "Save Document";
  }
});
$("generateComplianceAlerts").addEventListener("click", async () => {
  const button = $("generateComplianceAlerts");
  button.disabled = true;
  button.textContent = "Checking Documents...";
  try {
    await api("/api/alerts/generate-compliance", { method: "POST" });
    await loadAlerts();
    showToast("Compliance alerts generated successfully.");
  } catch (error) {
    console.error("Generating compliance alerts failed:", error);
    showToast("Could not generate alerts: " + error.message);
  } finally {
    button.disabled = false;
    button.textContent = "Generate Compliance Alerts";
  }
});function calculateDeliveryKpis() {
  if (!state.orders.length) return;

  const delivered = state.orders.filter(po =>
    po.actualDeliveryDate || String(po.status).toUpperCase() === "DELIVERED"
  );

  let onTime = 0;

  delivered.forEach(po => {
    if (po.expectedDeliveryDate && po.actualDeliveryDate) {
      const expected = new Date(po.expectedDeliveryDate);
      const actual = new Date(po.actualDeliveryDate);
      if (actual <= expected) onTime++;
    }
  });

  if (delivered.length) {
    $("onTimeRate").textContent =
      `${((onTime / delivered.length) * 100).toFixed(1)}%`;
  }

  const high = state.orders.filter(po => {
    if (!po.expectedDeliveryDate || !po.actualDeliveryDate) return false;
    const delay = (new Date(po.actualDeliveryDate) - new Date(po.expectedDeliveryDate)) / 86400000;
    const ordered = Number(po.quantity || 0);
    const deliveredQty = Number(po.actualDeliveredQuantity ?? ordered);
    return delay >= 3 || (ordered > 0 && deliveredQty / ordered < 0.9);
  }).length;

  const low = state.orders.filter(po => {
    if (!po.expectedDeliveryDate || !po.actualDeliveryDate) return false;
    const delay = (new Date(po.actualDeliveryDate) - new Date(po.expectedDeliveryDate)) / 86400000;
    const ordered = Number(po.quantity || 0);
    const deliveredQty = Number(po.actualDeliveredQuantity ?? ordered);
    return delay < 1 && (ordered === 0 || deliveredQty / ordered >= 0.9);
  }).length;

  const medium = Math.max(0, state.orders.length - high - low);

  $("riskTotal").textContent = state.orders.length;
  $("lowRisk").textContent = low;
  $("mediumRisk").textContent = medium;
  $("highRisk").textContent = high;
  $("highRiskCount").textContent = high;

  updateRiskRing(low, medium, high);
}

function updateRiskRing(low, medium, high) {
  const total = low + medium + high || 1;
  const lowEnd = low / total * 100;
  const medEnd = (low + medium) / total * 100;

  const ring = document.querySelector(".risk-ring");
  if (ring) {
    ring.style.background =
      `conic-gradient(#16a34a 0 ${lowEnd}%, #f59e0b ${lowEnd}% ${medEnd}%, #dc2626 ${medEnd}% 100%)`;
  }
}

function renderRecentOrders() {
  const tbody = $("recentOrders");

  if (!state.orders.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty">No purchase orders found.</td></tr>`;
    return;
  }

  tbody.innerHTML = state.orders.slice(-6).reverse().map(po => `
    <tr>
      <td><strong>${escapeHtml(po.poNumber || po.id)}</strong></td>
      <td>${escapeHtml(po.vendor?.vendorName || po.vendor?.name || po.vendorId || "--")}</td>
      <td>${escapeHtml(po.orderDate || "--")}</td>
      <td>${escapeHtml(po.expectedDeliveryDate || "--")}</td>
      <td>${badge(po.status)}</td>
      <td>${money(po.totalAmount)}</td>
    </tr>
  `).join("");
}

function renderVendors(list = state.vendors) {
  const tbody = $("vendorsTable");
  if (!tbody) return;
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty">No vendors found.</td></tr>`;
    return;
  }
  tbody.innerHTML = list.map(v => `
    <tr>
      <td>${escapeHtml(v.id)}</td>
      <td><strong>${escapeHtml(v.vendorName || v.name || "--")}</strong></td>
      <td>${escapeHtml(v.category || "--")}</td>
      <td>${escapeHtml(v.contactPerson || v.email || "--")}</td>
      <td>${badge(v.status)}</td>
      <td><button type="button" class="secondary-button vendor-edit-button" data-edit-vendor="${escapeHtml(v.id)}">Edit</button></td>
    </tr>
  `).join("");
}

function renderOrders() {
  const tbody = $("ordersTable");
  if (!tbody) return;

  if (!state.orders.length) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty">No purchase orders found.</td></tr>`;
    return;
  }

  tbody.innerHTML = [...state.orders].reverse().map(po => `
    <tr>
      <td><strong>${escapeHtml(po.poNumber || po.id)}</strong></td>
      <td>${escapeHtml(po.vendor?.vendorName || po.vendor?.name || po.vendorId || "--")}</td>
      <td>${escapeHtml(po.orderDate || "--")}</td>
      <td>${escapeHtml(po.expectedDeliveryDate || "--")}</td>
      <td>${escapeHtml(po.actualDeliveryDate || "--")}</td>
      <td>${escapeHtml(po.actualDeliveredQuantity ?? po.quantity ?? "--")} / ${escapeHtml(po.quantity ?? "--")}</td>
      <td>${badge(po.status)}</td>
      <td>${money(po.totalAmount)}</td>
    </tr>
  `).join("");
}

function renderPerformance() {
  const data = state.performance;

  if (!data.length) {
    renderPerformanceFallback();
    return;
  }

  const scores = data.map(x => Number(x.performanceScore ?? x.score)).filter(Number.isFinite);
  const fulfillment = data.map(x => Number(x.fulfillmentRate)).filter(Number.isFinite);
  const delays = data.map(x => Number(x.averageDeliveryDelay ?? x.averageDelay)).filter(Number.isFinite);
  const quality = data.map(x => Number(x.qualityIssueRate)).filter(Number.isFinite);

  $("avgScore").textContent = scores.length ? average(scores).toFixed(1) : "--";
  $("avgFulfillment").textContent = fulfillment.length ? `${average(fulfillment).toFixed(1)}%` : "--";
  $("avgDelay").textContent = delays.length ? `${average(delays).toFixed(1)} days` : "--";
  $("avgQuality").textContent = quality.length ? `${average(quality).toFixed(1)}%` : "--";

  drawVendorScoreChart(data);
}

function renderPerformanceFallback() {
  if (!state.orders.length) return;

  const delivered = state.orders.filter(po => po.actualDeliveryDate);
  const fulfillment = delivered.map(po => {
    const qty = Number(po.quantity || 0);
    const deliveredQty = Number(po.actualDeliveredQuantity ?? qty);
    return qty ? deliveredQty / qty * 100 : 0;
  });

  $("avgFulfillment").textContent =
    fulfillment.length ? `${average(fulfillment).toFixed(1)}%` : "--";

  const delays = delivered.map(po => {
    if (!po.expectedDeliveryDate || !po.actualDeliveryDate) return 0;
    return (new Date(po.actualDeliveryDate) - new Date(po.expectedDeliveryDate)) / 86400000;
  });

  $("avgDelay").textContent =
    delays.length ? `${average(delays).toFixed(1)} days` : "--";

  $("avgScore").textContent = "--";
  $("avgQuality").textContent = "--";
}

function drawPerformanceChart() {
  const ctx = $("performanceChart");
  if (!ctx) return;

  if (state.performanceChart) state.performanceChart.destroy();

  const monthly = {};
  state.orders.forEach(po => {
    const date = po.orderDate;
    if (!date) return;
    const month = date.slice(0, 7);
    if (!monthly[month]) monthly[month] = { total: 0, onTime: 0 };

    monthly[month].total++;

    if (po.expectedDeliveryDate && po.actualDeliveryDate &&
        new Date(po.actualDeliveryDate) <= new Date(po.expectedDeliveryDate)) {
      monthly[month].onTime++;
    }
  });

  const labels = Object.keys(monthly).sort();
  const values = labels.map(label =>
    monthly[label].total ? monthly[label].onTime / monthly[label].total * 100 : 0
  );

  state.performanceChart = new Chart(ctx, {
    type: "line",
    data: {
      labels,
      datasets: [{
        label: "On-Time Delivery %",
        data: values,
        borderColor: "#1d72b8",
        backgroundColor: "rgba(29,114,184,.12)",
        fill: true,
        tension: .35,
        pointRadius: 3
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: { y: { beginAtZero: true, max: 100 } },
      plugins: { legend: { display: false } }
    }
  });
}

function drawVendorScoreChart(data) {
  const ctx = $("vendorScoreChart");
  if (!ctx) return;

  if (state.vendorScoreChart) state.vendorScoreChart.destroy();

  const labels = data.map(x => x.vendor?.vendorName || x.vendorName || x.vendorId || "Vendor");
  const values = data.map(x => Number(x.performanceScore ?? x.score ?? 0));

  state.vendorScoreChart = new Chart(ctx, {
    type: "bar",
    data: {
      labels,
      datasets: [{
        label: "Performance Score",
        data: values,
        backgroundColor: "rgba(29,114,184,.65)",
        borderRadius: 7
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } }
    }
  });
}

function renderAlerts() {
  const tbody = $("alertsTable");
  if (!tbody) return;

  if (!state.alerts.length) {
    tbody.innerHTML = `<tr><td colspan="5" class="empty">No alerts found.</td></tr>`;
    return;
  }

  tbody.innerHTML = [...state.alerts].reverse().map(a => `
    <tr>
      <td>${escapeHtml(a.alertType || "--")}</td>
      <td>${escapeHtml(a.message || "--")}</td>
      <td>${badge(a.severity)}</td>
      <td>${badge(a.status)}</td>
      <td>${escapeHtml(a.createdAt || "--")}</td>
    </tr>
  `).join("");
}

function average(values) {
  return values.length ? values.reduce((a,b) => a+b, 0) / values.length : 0;
}

$("vendorSearch").addEventListener("input", event => {
  const q = event.target.value.toLowerCase();
  const filtered = state.vendors.filter(v =>
    `${v.vendorName || ""} ${v.category || ""} ${v.contactPerson || ""} ${v.email || ""}`
      .toLowerCase().includes(q)
  );
  renderVendors(filtered);
});


function openVendorForm(v = null) {
  const f = $("vendorForm");
  f.reset();
  $("vendorFormError").hidden = true;
  $("vendorFormError").textContent = "";
  f.elements.vendorId.value = v ? v.id : "";
  $("vendorModalTitle").textContent = v ? "Edit Vendor" : "Add Vendor";
  $("saveVendorButton").textContent = v ? "Save Changes" : "Save Vendor";
  if (v) {
    ["vendorName","category","contactPerson","email","phone","address","status"].forEach(k => {
      f.elements[k].value = v[k] ?? (k === "status" ? "ACTIVE" : "");
    });
  }
  $("vendorModal").hidden = false;
}
function closeVendorForm() {
  $("vendorModal").hidden = true;
}
$("addVendorButton").onclick = function () {
  openVendorForm();
};
$("closeVendorModal").addEventListener("click", closeVendorForm);
$("cancelVendorForm").addEventListener("click", closeVendorForm);
$("vendorsTable").addEventListener("click", e => {
  const b = e.target.closest("[data-edit-vendor]");
  if (!b) return;
  const v = state.vendors.find(x => String(x.id) === b.dataset.editVendor);
  if (v) openVendorForm(v);
});
$("vendorForm").addEventListener("submit", async e => {
  e.preventDefault();
  const f = e.currentTarget;
  const d = Object.fromEntries(new FormData(f).entries());
  const id = d.vendorId;
  delete d.vendorId;
  ["vendorName","category","contactPerson","email","phone","address"].forEach(k => {
    d[k] = (d[k] || "").trim();
  });
  if (!d.email) d.email = null;
  const err = $("vendorFormError");
  const btn = $("saveVendorButton");
  err.hidden = true;
  btn.disabled = true;
  btn.textContent = "Saving...";
  try {
    await api(id ? `/api/vendors/${encodeURIComponent(id)}` : "/api/vendors", {
      method: id ? "PUT" : "POST",
      headers: {"Content-Type":"application/json"},
      body: JSON.stringify(d)
    });
    closeVendorForm();
    $("vendorSearch").value = "";
    await loadVendors();
    showToast(id ? "Vendor updated successfully." : "Vendor added successfully.");
  } catch (ex) {
    err.textContent = "Could not save vendor: " + ex.message;
    err.hidden = false;
  } finally {
    btn.disabled = false;
    btn.textContent = id ? "Save Changes" : "Save Vendor";
  }
});
$("riskForm").addEventListener("submit", async event => {
  event.preventDefault();

  const form = new FormData(event.target);
  const body = Object.fromEntries(form.entries());

  [
    "quantity", "unit_price", "total_amount", "planned_delivery_days",
    "previous_po_count", "previous_on_time_rate", "previous_average_delay",
    "previous_fulfillment_rate", "previous_quality_issue_rate",
    "recent_on_time_rate", "recent_average_delay", "recent_fulfillment_rate"
  ].forEach(key => body[key] = Number(body[key]));

  const resultBox = $("predictionResult");
  resultBox.innerHTML = `<div class="result-placeholder"><div class="status-symbol">...</div><h2>Predicting...</h2><p>Sending request through Spring Boot to the prediction service.</p></div>`;

  try {
    const result = await api("/api/ml/predict", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });

    const risk = result.delivery_risk || "Unknown";
    const riskClass = risk.toLowerCase();

    const probabilities = result.probabilities || {};

    resultBox.innerHTML = `
      <div class="prediction-main">
        <div class="status-symbol" style="margin:auto">âœ“</div>
        <p class="eyebrow" style="margin-top:15px">MODEL PREDICTION</p>
        <div class="prediction-risk ${riskClass}-text">${escapeHtml(risk)}</div>
        <p style="font-size:11px;color:#64748b">
          Selected-class model probability:
          <strong>${(Number(result.risk_probability || 0) * 100).toFixed(1)}%</strong>
        </p>

        <div style="margin-top:30px;text-align:left">
          ${Object.entries(probabilities).map(([name, value]) => `
            <div class="prob-row">
              <span>${escapeHtml(name)}</span>
              <div class="bar"><span style="width:${Math.min(100, Number(value)*100)}%"></span></div>
              <strong>${(Number(value)*100).toFixed(1)}%</strong>
            </div>
          `).join("")}
        </div>

        <p style="margin-top:25px;font-size:10px;color:#94a3b8;line-height:1.6">
          These values are the model's predicted class probabilities; they have not been calibrated as real-world probabilities.
        </p>
      </div>
    `;

    showToast("Delivery risk prediction completed.");
  } catch (error) {
    console.error(error);
    resultBox.innerHTML = `
      <div class="result-placeholder">
        <div class="status-symbol">!</div>
        <h2>Prediction unavailable</h2>
        <p>${escapeHtml(error.message)}</p>
      </div>
    `;
    showToast("Could not reach the prediction API.");
  }
});

$("feedbackForm").addEventListener("submit", async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form).entries());
  const vendorId = String(data.vendorId || "").trim();
  const purchaseOrderId = String(data.purchaseOrderId || "").trim();
  if (!vendorId || !purchaseOrderId) {
    showToast("Enter both Vendor ID and Purchase Order ID.");
    return;
  }
  const submitButton = form.querySelector('button[type="submit"]');
  submitButton.disabled = true;
  submitButton.textContent = "Submitting...";
  try {
    const params = new URLSearchParams({ vendorId, purchaseOrderId });
    const payload = {
      rating: Number(data.rating),
      feedbackType: data.feedbackType,
      feedbackText: String(data.feedbackText || "").trim()
    };
    await api(`/api/feedback?${params.toString()}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    form.reset();
    showToast("Feedback saved successfully.");
  } catch (error) {
    console.error("Feedback submission failed:", error);
    showToast("Feedback could not be saved: " + error.message);
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "Submit Feedback";
  }
});

async function initialise() {
  drawPerformanceChart();

  await Promise.allSettled([
    loadVendors(),
    loadOrders(),
    loadPerformance(),
    loadAlerts(),
    loadCompliance()
  ]);

  drawPerformanceChart();
}

initialise();


