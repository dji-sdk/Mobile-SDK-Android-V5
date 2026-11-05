"use strict";

const API = "/api/admin";
const state = {
  jobs: [], devices: [], records: [],
  expandedGroups: new Set(),
  recordsLoaded: false, recordsSignature: "",
  loadingJobs: false, loadingRecords: false,
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

/** 任务状态的中文名；与巡逻管理台保持一致，额外增加「已拒绝」。 */
const statusNames = {
  DRAFT: "草稿", DISPATCHED: "待接收", ACCEPTED: "待执行", IN_PROGRESS: "执行中",
  FINALIZING: "同步中", COMPLETED: "已完成", CANCELLED: "已取消", REJECTED: "已拒绝",
};

const endReasonNames = {
  PILOT_FINISH: "飞手结束",
  FOLLOW_EXIT_THEN_FINISH: "跟随退出后结束",
};

const icons = {
  tasks: '<svg viewBox="0 0 24 24"><path d="M5 4h14v16H5zM8 8h8M8 12h8M8 16h5"/></svg>',
  sent: '<svg viewBox="0 0 24 24"><path d="m3 11 18-8-7 18-3-7zM11 14l4-4"/></svg>',
  active: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4z"/></svg>',
  done: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></svg>',
  device: '<svg viewBox="0 0 24 24"><path d="M4 9h16v9H4zM8 9V6h8v3M8 18v2M16 18v2"/></svg>',
  gas: '<svg viewBox="0 0 24 24"><path d="M9 3h6v4l3 5v9H6v-9l3-5z"/><path d="M9 15h6"/></svg>',
};

/** 新增通道时使用的演示默认值；阈值不代表任何法规限值。 */
const channelPreset = {
  channelId: "so2",
  displayName: "SO2",
  unit: "µg/m³",
  threshold: 500,
};

document.addEventListener("DOMContentLoaded", () => {
  $("#today").textContent = new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "long", day: "numeric" }).format(new Date());
  bindEvents();
  routeView();
  refreshVisibleView();
  setInterval(refreshVisibleView, 5000);
});

function bindEvents() {
  window.addEventListener("hashchange", routeView);
  $("#menuButton").addEventListener("click", () => $("#sidebar").classList.toggle("open"));
  $("#jobSearch").addEventListener("input", renderJobs);
  $("#jobStatusFilter").addEventListener("change", renderJobs);
  $("#recordSearch").addEventListener("input", renderRecords);
  $("#recordJobFilter").addEventListener("change", renderRecords);
  $("#jobForm").addEventListener("submit", saveJob);
  document.addEventListener("click", handleAction);
  $$(".overlay").forEach((overlay) => overlay.addEventListener("click", (event) => {
    if (event.target === overlay) closeOverlays();
  }));
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") closeOverlays();
  });
  document.addEventListener("visibilitychange", refreshVisibleView);
}

function refreshVisibleView() {
  if (document.hidden) return;
  if (location.hash === "#records") refreshRecords();
  else refreshJobs();
}

function routeView() {
  const view = location.hash === "#records" ? "records" : "tasks";
  $$(".view").forEach((node) => node.classList.toggle("active", node.id === `${view}View`));
  $$("[data-view-link]").forEach((node) => node.classList.toggle("active", node.dataset.viewLink === view));
  $("#pageCrumb").textContent = view === "records" ? "检测记录" : "任务工单";
  $("#sidebar").classList.remove("open");
  if (view === "records" && !state.recordsLoaded) refreshRecords();
}

async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, options);
  if (!response.ok) {
    let detail = `${response.status} ${response.statusText}`;
    try { detail = (await response.json()).error || detail; } catch (_) { /* 非 JSON 错误响应。 */ }
    throw new Error(detail);
  }
  if (response.status === 204) return null;
  const type = response.headers.get("content-type") || "";
  return type.includes("application/json") ? response.json() : response;
}

async function refreshJobs() {
  if (state.loadingJobs) return;
  state.loadingJobs = true;
  setStateBox("jobs", "loading", "正在加载任务数据…");
  try {
    const [jobsResult, devicesResult] = await Promise.allSettled([api("/jobs"), api("/devices")]);
    if (jobsResult.status === "rejected") throw jobsResult.reason;
    state.jobs = listFrom(jobsResult.value, "jobs");
    if (devicesResult.status === "fulfilled") state.devices = listFrom(devicesResult.value, "devices");
    renderJobs();
    renderDeviceOptions();
    renderRecordJobOptions();
    setHealth(true);
  } catch (error) {
    state.jobs = [];
    renderJobStats();
    $("#jobsTable").replaceChildren();
    setStateBox("jobs", "error", "任务加载失败", error.message);
    setHealth(false);
  } finally {
    state.loadingJobs = false;
  }
}

async function ensureJobsLoaded() {
  if (state.jobs.length || state.loadingJobs) return;
  await refreshJobs();
}

async function refreshRecords() {
  if (state.loadingRecords) return;
  state.loadingRecords = true;
  if (!state.recordsLoaded) setStateBox("records", "loading", "正在加载检测记录…");
  try {
    await ensureJobsLoaded();
    const records = listFrom(await api("/inspections"), "records");
    const signature = recordListSignature(records);
    const changed = signature !== state.recordsSignature;
    state.records = records;
    state.recordsSignature = signature;
    if (!state.recordsLoaded || changed) renderRecords();
    state.recordsLoaded = true;
    setHealth(true);
  } catch (error) {
    if (!state.recordsLoaded) {
      state.records = [];
      $("#recordGrid").replaceChildren();
      setStateBox("records", "error", "检测记录加载失败", error.message);
    }
    setHealth(false);
  } finally {
    state.loadingRecords = false;
  }
}

function recordListSignature(records) {
  return JSON.stringify(records.map((record) => ({
    jobId: String(record.jobId || ""),
    recordId: String(record.recordId || ""),
    receivedAt: String(record.receivedAt || ""),
    totalSamples: Number(record.totalSamples || 0),
    alertCount: Array.isArray(record.alerts) ? record.alerts.length : 0,
  })).sort((left, right) => `${left.jobId}/${left.recordId}`.localeCompare(`${right.jobId}/${right.recordId}`)));
}

function listFrom(value, key) {
  if (Array.isArray(value)) return value;
  return Array.isArray(value?.[key]) ? value[key] : [];
}

function renderJobStats() {
  const counts = {
    total: state.jobs.length,
    sent: state.jobs.filter((job) => ["DISPATCHED", "ACCEPTED"].includes(job.status)).length,
    active: state.jobs.filter((job) => ["IN_PROGRESS", "FINALIZING"].includes(job.status)).length,
    done: state.jobs.filter((job) => job.status === "COMPLETED").length,
  };
  const cards = [
    ["tasks", "任务总数", counts.total, "#168da7", "#e5f7fa"],
    ["sent", "待处理", counts.sent, "#3278c4", "#eaf2fb"],
    ["active", "执行中", counts.active, "#c88822", "#fff4df"],
    ["done", "已完成", counts.done, "#15936b", "#e8f7f1"],
  ];
  $("#jobStats").innerHTML = cards.map(([icon, label, value, tone, tint]) => `
    <article class="stat-card" style="--tone:${tone};--tint:${tint}"><span class="stat-icon">${icons[icon]}</span><strong>${value}</strong><p>${label}</p></article>`).join("");
}

function renderJobs() {
  renderJobStats();
  const search = $("#jobSearch").value.trim().toLowerCase();
  const status = $("#jobStatusFilter").value;
  const jobs = state.jobs.filter((job) => {
    const text = `${job.jobId || ""} ${job.title || ""}`.toLowerCase();
    return (!search || text.includes(search)) && (!status || job.status === status);
  });
  $("#jobsCount").textContent = `共 ${jobs.length} 条任务`;
  $("#jobsTable").innerHTML = jobs.map(jobRow).join("");
  if (!jobs.length) setStateBox("jobs", "empty", search || status ? "没有符合筛选条件的任务" : "还没有任务", search || status ? "调整搜索词或筛选条件后重试" : "点击“新增任务”创建第一条工单");
  else hideStateBox("jobs");
}

/**
 * 渲染一条任务行。
 *
 * 与巡逻管理台同构，差异仅在：字段取自 `job.gas`，新增「检测通道」列，状态包含「已拒绝」。
 */
function jobRow(job) {
  const id = job.jobId || "";
  const gas = job.gas || {};
  const rcSn = job.targetRcSn || "";
  const device = state.devices.find((item) => item.rcSn === rcSn);
  const deviceState = device?.online === true ? "在线" : "离线";
  const channels = Array.isArray(gas.channels) ? gas.channels : [];
  const channelText = channels.length
    ? channels.map((channel) => `<span class="channel-chip">${escapeHtml(channel.displayName || channel.channelId)} ≥ ${escapeHtml(channel.threshold)}${escapeHtml(channel.unit || "")}</span>`).join("")
    : '<span class="muted">未配置</span>';
  const mayEdit = job.status === "DRAFT";
  const mayDispatch = job.status === "DRAFT";
  const mayWithdraw = job.status === "DISPATCHED";
  const mayDelete = !["IN_PROGRESS", "FINALIZING"].includes(job.status);
  return `<tr>
    <td><div class="job-name"><strong>${escapeHtml(job.title || "未命名任务")}</strong><small>${escapeHtml(id)}</small></div></td>
    <td><div class="device-cell"><span class="device-icon">${icons.device}</span><span><strong>${escapeHtml(rcSn || "未分配")}</strong><small>${rcSn ? deviceState : "等待分配"}</small></span></div></td>
    <td class="date-cell"><span>${escapeHtml(formatDate(gas.deadline, false))}</span><small>安全边界 ${escapeHtml(gas.safetyBoundaryM ?? "—")} 米</small></td>
    <td>${channelText}</td>
    <td>${statusBadge(job.status || "DRAFT")}</td>
    <td><div class="row-actions"><button class="text-button" data-action="detail" data-id="${escapeAttr(id)}">详情</button>${mayEdit ? `<button class="text-button" data-action="edit" data-id="${escapeAttr(id)}">编辑</button>` : ""}${mayDispatch ? `<button class="text-button" data-action="dispatch" data-id="${escapeAttr(id)}">下发</button>` : ""}${mayWithdraw ? `<button class="text-button danger" data-action="withdraw" data-id="${escapeAttr(id)}">撤回</button>` : ""}${mayDelete ? `<button class="text-button danger" data-action="delete" data-id="${escapeAttr(id)}">删除</button>` : ""}</div></td>
  </tr>`;
}

function renderRecords() {
  renderRecordJobOptions();
  const search = $("#recordSearch").value.trim().toLowerCase();
  const jobId = $("#recordJobFilter").value;
  const records = state.records.filter((record) => {
    const text = `${record.recordId || ""} ${record.jobId || ""} ${record.jobTitle || ""} ${record.rcSn || ""}`.toLowerCase();
    return (!search || text.includes(search)) && (!jobId || record.jobId === jobId);
  });
  const groups = groupRecordsByJob(records);
  const forceExpand = Boolean(search || jobId);
  $("#recordsCount").textContent = `共 ${records.length} 条记录 · ${groups.length} 个任务`;
  $("#recordGrid").innerHTML = groups.map((group) => recordGroup(group, forceExpand)).join("");
  if (!records.length) {
    setStateBox("records", "empty", search || jobId ? "没有符合筛选条件的记录" : "暂无检测记录", search || jobId ? "调整搜索词或任务筛选后重试" : "插件完成检测并上传后，记录会显示在这里");
  } else {
    hideStateBox("records");
  }
}

function groupRecordsByJob(records) {
  const groups = new Map();
  records.forEach((record) => {
    const key = String(record.jobId || "");
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(record);
  });
  return [...groups.entries()]
    .map(([jobId, items]) => ({ jobId, items }))
    .sort((left, right) => jobOrder(left.jobId) - jobOrder(right.jobId));
}

function jobOrder(jobId) {
  if (!jobId) return state.jobs.length + 1;
  const index = state.jobs.findIndex((job) => job.jobId === jobId);
  return index < 0 ? state.jobs.length : index;
}

function recordGroup(group, forceExpand = false) {
  const { jobId, items } = group;
  const job = findJob(jobId);
  const title = job?.title || items[0]?.jobTitle || jobId || "未关联任务";
  const samples = items.reduce((sum, record) => sum + Number(record.totalSamples || 0), 0);
  const alerts = items.reduce((sum, record) => sum + alertCount(record), 0);
  const key = `record:${jobId}`;
  const expanded = forceExpand || state.expandedGroups.has(key);
  return `<section class="record-group${expanded ? " open" : ""}">
    <button class="record-group-head" type="button" data-action="toggle-group" data-group-key="${escapeAttr(key)}" aria-expanded="${expanded}">
      <span class="record-group-chevron"><svg viewBox="0 0 24 24"><path d="m9 6 6 6-6 6"/></svg></span>
      <span class="record-group-title"><strong>${escapeHtml(title)}</strong><small>${escapeHtml(jobId || "未关联任务")}</small></span>
      <span class="record-group-meta">${job ? statusBadge(job.status || "DRAFT") : ""}<span>${items.length} 条记录</span><span>${samples} 个样本</span><span>${alerts} 次告警</span></span>
    </button>
    <div class="record-list"${expanded ? "" : " hidden"}>${items.map(recordCard).join("")}</div>
  </section>`;
}

function recordCard(record) {
  const channels = Array.isArray(record.channels) ? record.channels : [];
  const channelChips = channels.length
    ? channels.map((channel) => `<span class="channel-chip">${escapeHtml(channel.displayName || channel.channelId)} 均值 ${escapeHtml(formatNumber(channel.average))}${escapeHtml(channel.unit || "")}</span>`).join("")
    : '<span class="channel-chip">无有效通道样本</span>';
  const alertChips = (Array.isArray(record.alerts) ? record.alerts : [])
    .map((alert) => `<span class="channel-chip alert">${escapeHtml(alert.type || "告警")} × ${escapeHtml(alert.count ?? 0)}</span>`).join("");
  return `<article class="record-card">
    <div class="record-card-head">
      <div><strong>${escapeHtml(record.recordId || "未命名记录")}</strong><small>${escapeHtml(record.rcSn || "未知遥控器")} · ${escapeHtml(endReasonNames[record.endReason] || record.endReason || "结束方式未知")}</small></div>
      <div><small>${escapeHtml(formatDate(record.receivedAt))}</small></div>
    </div>
    <div class="record-card-body">
      <div class="record-metrics">
        <div class="record-metric"><strong>${escapeHtml(record.totalSamples ?? 0)}</strong><span>采样总数</span></div>
        <div class="record-metric"><strong>${escapeHtml(record.validSamples ?? 0)}</strong><span>有效样本</span></div>
        <div class="record-metric"><strong>${escapeHtml(record.invalidSamples ?? 0)}</strong><span>无效样本</span></div>
        <div class="record-metric"><strong>${alertCount(record)}</strong><span>告警次数</span></div>
      </div>
      <div class="record-channels">${channelChips}${alertChips}</div>
      <div class="record-metrics" style="margin-top:12px;grid-template-columns:repeat(2,1fr)">
        <div class="record-metric"><strong style="font-size:12px">${escapeHtml(formatDate(record.startedAtMs))}</strong><span>开始时间</span></div>
        <div class="record-metric"><strong style="font-size:12px">${escapeHtml(formatDate(record.endedAtMs))}</strong><span>结束时间</span></div>
      </div>
    </div>
  </article>`;
}

function alertCount(record) {
  return (Array.isArray(record.alerts) ? record.alerts : []).reduce((sum, alert) => sum + Number(alert.count || 0), 0);
}

function toggleGroup(key) {
  if (state.expandedGroups.has(key)) state.expandedGroups.delete(key);
  else state.expandedGroups.add(key);
  renderRecords();
}

function handleAction(event) {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const actions = {
    "new-job": () => openJobModal(),
    "close-modal": () => closeOverlay("jobModal"),
    "close-detail": () => closeOverlay("detailDrawer"),
    "refresh-jobs": refreshJobs,
    "refresh-records": refreshRecords,
    "add-channel": () => addChannelRow(),
    "remove-channel": () => removeChannelRow(target),
    edit: () => openJobModal(findJob(target.dataset.id)),
    detail: () => openDetail(findJob(target.dataset.id)),
    dispatch: () => dispatchJob(target.dataset.id, target),
    withdraw: () => withdrawJob(target.dataset.id, target),
    delete: () => deleteJob(target.dataset.id, target),
    "toggle-group": () => toggleGroup(target.dataset.groupKey),
  };
  actions[target.dataset.action]?.();
}

function findJob(id) {
  return state.jobs.find((job) => String(job.jobId) === String(id));
}

function openJobModal(job = null) {
  const form = $("#jobForm");
  form.reset();
  renderDeviceOptions();
  $("#jobModalTitle").textContent = job ? "编辑任务" : "新增任务";
  $("#channelRows").replaceChildren();
  if (job) {
    const gas = job.gas || {};
    form.elements.jobId.value = job.jobId || "";
    form.elements.title.value = job.title || "";
    form.elements.rcSn.value = job.targetRcSn || "";
    form.elements.deadline.value = toLocalInput(gas.deadline);
    form.elements.latitude.value = gas.target?.latitude ?? "";
    form.elements.longitude.value = gas.target?.longitude ?? "";
    form.elements.altitudeM.value = gas.target?.altitudeM ?? "";
    form.elements.safetyBoundaryM.value = gas.safetyBoundaryM ?? "";
    form.elements.requirements.value = gas.requirements || "";
    const channels = Array.isArray(gas.channels) && gas.channels.length ? gas.channels : [channelPreset];
    channels.forEach((channel) => addChannelRow(channel));
  } else {
    form.elements.jobId.value = "";
    form.elements.latitude.value = "22.5";
    form.elements.longitude.value = "113.9";
    form.elements.altitudeM.value = "60";
    form.elements.safetyBoundaryM.value = "200";
    addChannelRow();
  }
  $("#jobModal").hidden = false;
  document.body.style.overflow = "hidden";
  setTimeout(() => form.elements.title.focus(), 0);
}

function renderDeviceOptions() {
  const select = $("#deviceSelect");
  const current = select.value;
  select.innerHTML = '<option value="">暂不分配</option>' + state.devices.map((device) => {
    const rcSn = device.rcSn || "";
    const status = device.online === true ? "在线" : "离线";
    return `<option value="${escapeAttr(rcSn)}">${escapeHtml(rcSn)}（${status}）</option>`;
  }).join("");
  select.value = current;
}

function renderRecordJobOptions() {
  const select = $("#recordJobFilter");
  const current = select.value;
  select.innerHTML = '<option value="">全部任务</option>' + state.jobs.map((job) => {
    const id = job.jobId || "";
    return `<option value="${escapeAttr(id)}">${escapeHtml(job.title || id)}（${escapeHtml(id)}）</option>`;
  }).join("");
  select.value = state.jobs.some((job) => job.jobId === current) ? current : "";
}

function addChannelRow(channel = channelPreset) {
  const row = document.createElement("tr");
  const fields = ["channelId", "displayName", "unit", "threshold"];
  for (const field of fields) {
    const cell = document.createElement("td");
    const input = document.createElement("input");
    input.dataset.field = field;
    input.required = true;
    input.value = channel[field] ?? "";
    if (field === "threshold") {
      input.type = "number";
      input.step = "any";
    }
    cell.append(input);
    row.append(cell);
  }
  const removeCell = document.createElement("td");
  const removeButton = document.createElement("button");
  removeButton.type = "button";
  removeButton.className = "text-button danger";
  removeButton.dataset.action = "remove-channel";
  removeButton.textContent = "移除";
  removeCell.append(removeButton);
  row.append(removeCell);
  $("#channelRows").append(row);
}

function removeChannelRow(button) {
  const row = button.closest("tr");
  if (!row) return;
  if ($("#channelRows").children.length <= 1) {
    toast("至少保留一个检测通道", true);
    return;
  }
  row.remove();
}

function collectForm() {
  const form = $("#jobForm");
  const channels = $$("#channelRows tr").map((row) => {
    const value = (name) => row.querySelector(`input[data-field="${name}"]`).value.trim();
    return {
      channelId: value("channelId"),
      displayName: value("displayName"),
      unit: value("unit"),
      threshold: Number(value("threshold")),
    };
  });
  const deadline = form.elements.deadline.value ? new Date(form.elements.deadline.value).toISOString() : null;
  const requirements = form.elements.requirements.value.trim();
  return {
    title: form.elements.title.value.trim(),
    targetRcSn: form.elements.rcSn.value || null,
    gas: {
      target: {
        latitude: Number(form.elements.latitude.value),
        longitude: Number(form.elements.longitude.value),
        altitudeM: Number(form.elements.altitudeM.value),
      },
      channels,
      safetyBoundaryM: Number(form.elements.safetyBoundaryM.value),
      requirements: requirements || null,
      deadline,
    },
  };
}

async function saveJob(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const shouldDispatch = event.submitter?.value === "dispatch";
  const payload = collectForm();
  if (!payload.title) return;
  if (shouldDispatch && !payload.targetRcSn) {
    toast("下发前必须选择遥控器 SN", true);
    return;
  }
  const id = form.elements.jobId.value;
  setFormBusy(true);
  try {
    const result = await api(id ? `/jobs/${encodeURIComponent(id)}` : "/jobs", {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const saved = result?.job || result;
    const savedId = saved?.jobId || id;
    if (!savedId) throw new Error("服务端未返回任务编号");
    if (shouldDispatch) {
      await api(`/jobs/${encodeURIComponent(savedId)}/dispatch`, { method: "POST" });
    }
    toast(shouldDispatch ? "任务已保存并下发" : "任务草稿已保存");
    closeOverlay("jobModal");
    await refreshJobs();
    if (state.recordsLoaded) await refreshRecords();
  } catch (error) {
    toast(`保存失败：${error.message}`, true);
  } finally {
    setFormBusy(false);
  }
}

async function dispatchJob(id, button) {
  if (!id || !confirm("确认将该任务下发到所选遥控器？")) return;
  const job = findJob(id);
  if (!job?.targetRcSn) {
    toast("请先编辑任务并选择遥控器 SN", true);
    return;
  }
  button.disabled = true;
  try {
    await api(`/jobs/${encodeURIComponent(id)}/dispatch`, { method: "POST" });
    toast("任务已下发");
    await refreshJobs();
  } catch (error) {
    toast(`下发失败：${error.message}`, true);
  } finally {
    button.disabled = false;
  }
}

async function withdrawJob(id, button) {
  if (!id || !confirm("确认撤回该任务并恢复为草稿？")) return;
  button.disabled = true;
  try {
    await api(`/jobs/${encodeURIComponent(id)}/withdraw`, { method: "POST" });
    toast("任务已撤回，可重新分配");
    await refreshJobs();
  } catch (error) {
    toast(`撤回失败：${error.message}`, true);
  } finally {
    button.disabled = false;
  }
}

async function deleteJob(id, button) {
  if (!id || !confirm("确认永久删除该任务？删除后无法恢复。")) return;
  button.disabled = true;
  try {
    await api(`/jobs/${encodeURIComponent(id)}`, { method: "DELETE" });
    toast("任务已删除");
    await refreshJobs();
    if (state.recordsLoaded) {
      state.recordsSignature = "";
      await refreshRecords();
    }
  } catch (error) {
    toast(`删除失败：${error.message}`, true);
  } finally {
    button.disabled = false;
  }
}

function openDetail(job) {
  if (!job) return;
  $("#detailDrawer").hidden = false;
  document.body.style.overflow = "hidden";
  renderDetail(job);
}

function renderDetail(job) {
  const id = job.jobId || "";
  const gas = job.gas || {};
  const timeline = Array.isArray(job.timeline) ? [...job.timeline].reverse() : [];
  const channels = Array.isArray(gas.channels) ? gas.channels : [];
  const target = gas.target || {};
  const channelRows = channels.length
    ? channels.map((channel) => `<tr><td>${escapeHtml(channel.displayName || channel.channelId)}</td><td>${escapeHtml(channel.channelId)}</td><td>${escapeHtml(channel.threshold)} ${escapeHtml(channel.unit || "")}</td></tr>`).join("")
    : '<tr><td colspan="3">未配置通道</td></tr>';
  $("#detailTitle").textContent = "任务详情";
  $("#detailBody").innerHTML = `
    <section class="detail-hero">${statusBadge(job.status || "DRAFT")}<h3>${escapeHtml(job.title || "未命名任务")}</h3><p>${escapeHtml(id)}</p></section>
    <section class="detail-section"><h3 class="detail-section-title">任务信息</h3><div class="info-grid">
      ${infoItem("遥控器 SN", job.targetRcSn || "未分配")}${infoItem("截止时间", formatDate(gas.deadline))}
      ${infoItem("安全边界", `${gas.safetyBoundaryM ?? "—"} 米`)}${infoItem("目标点", `${formatNumber(target.latitude)}, ${formatNumber(target.longitude)}`)}
      ${infoItem("相对高度", `${formatNumber(target.altitudeM)} 米`)}${infoItem("作业要求", gas.requirements || "未填写")}
    </div></section>
    <section class="detail-section"><h3 class="detail-section-title">气体检测配置 <span>${channels.length} 个通道</span></h3>
      <div class="channel-table-scroll"><table class="channel-table"><thead><tr><th>通道</th><th>通道 ID</th><th>阈值</th></tr></thead><tbody>${channelRows}</tbody></table></div>
      <p class="field-hint">阈值为演示值，不代表法规限值。</p>
    </section>
    <section class="detail-section"><h3 class="detail-section-title">任务时间线</h3>${timeline.length ? `<ol class="timeline">${timeline.map((item) => `<li>${escapeHtml(item.description || item.message || "状态更新")}<time>${escapeHtml(formatDate(item.timestamp || item.time))}</time></li>`).join("")}</ol>` : '<div class="state-box">暂无状态记录</div>'}</section>`;
}

function infoItem(label, value) {
  return `<div class="info-item"><span>${label}</span><strong>${escapeHtml(String(value))}</strong></div>`;
}

function closeOverlay(id) {
  $("#" + id).hidden = true;
  if ($$(".overlay:not([hidden])").length === 0) document.body.style.overflow = "";
}

function closeOverlays() {
  $$(".overlay").forEach((node) => { node.hidden = true; });
  document.body.style.overflow = "";
}

function setFormBusy(busy) {
  $$("button, input, select, textarea", $("#jobForm")).forEach((node) => { node.disabled = busy; });
}

function setStateBox(scope, type, title, detail = "") {
  const node = $(`#${scope}State`);
  node.hidden = false;
  node.className = `state-box ${type}`;
  node.innerHTML = `<strong>${escapeHtml(title)}</strong>${detail ? `<span>${escapeHtml(detail)}</span>` : ""}`;
}

function hideStateBox(scope) { $(`#${scope}State`).hidden = true; }

function setHealth(online) {
  $("#healthDot").className = `health-dot ${online ? "online" : "offline"}`;
  $("#healthText").textContent = online ? "服务已连接" : "服务不可用";
}

function toast(message, error = false) {
  const node = document.createElement("div");
  node.className = `toast${error ? " error" : ""}`;
  node.textContent = message;
  $("#toastStack").append(node);
  setTimeout(() => node.remove(), 3800);
}

function statusBadge(status) {
  const safeStatus = String(status || "DRAFT").toUpperCase();
  return `<span class="status status-${escapeAttr(safeStatus)}">${escapeHtml(statusNames[safeStatus] || safeStatus)}</span>`;
}

function formatNumber(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return "—";
  return Number.isInteger(number) ? String(number) : number.toFixed(2);
}

function formatDate(value, withTime = true) {
  if (!value) return "未设置";
  const date = typeof value === "number" || /^\d+$/.test(String(value)) ? new Date(Number(value)) : new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", ...(withTime ? { hour: "2-digit", minute: "2-digit", hour12: false } : {}) }).format(date).replaceAll("/", "-");
}

function toLocalInput(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}

function escapeAttr(value) { return escapeHtml(value); }
