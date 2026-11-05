"use strict";

const API = "/api/admin";
const state = {
  jobs: [], devices: [], assets: [], previewUrls: new Map(), expandedGroups: new Set(),
  assetsLoaded: false, assetsSignature: "", loadingJobs: false, loadingAssets: false,
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const statusNames = {
  DRAFT: "草稿", DISPATCHED: "待接收", ACCEPTED: "待执行", IN_PROGRESS: "执行中",
  FINALIZING: "同步中", COMPLETED: "已完成", CANCELLED: "已取消",
  UPLOADED: "已同步", UPLOADING: "同步中", FAILED: "失败", CONFIGURED: "已配置",
};

const icons = {
  tasks: '<svg viewBox="0 0 24 24"><path d="M5 4h14v16H5zM8 8h8M8 12h8M8 16h5"/></svg>',
  sent: '<svg viewBox="0 0 24 24"><path d="m3 11 18-8-7 18-3-7zM11 14l4-4"/></svg>',
  active: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4z"/></svg>',
  done: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></svg>',
  device: '<svg viewBox="0 0 24 24"><path d="M4 9h16v9H4zM8 9V6h8v3M8 18v2M16 18v2"/></svg>',
  route: '<svg viewBox="0 0 24 24"><path d="M6 3v18M18 3v18M6 7h8a4 4 0 0 1 4 4M18 17h-8a4 4 0 0 1-4-4"/><circle cx="6" cy="5" r="2"/><circle cx="18" cy="19" r="2"/></svg>',
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
  $("#assetSearch").addEventListener("input", renderAssets);
  $("#assetTypeFilter").addEventListener("change", renderAssets);
  $("#assetStatusFilter").addEventListener("change", renderAssets);
  $("#jobForm").addEventListener("submit", saveJob);
  $("#jobForm").elements.route.addEventListener("change", (event) => {
    $("#routeFileHint").textContent = event.target.files[0]?.name || "未选择新文件；管理台不编辑航点、高度或速度";
  });
  document.addEventListener("click", handleAction);
  $$(".overlay").forEach((overlay) => overlay.addEventListener("click", (event) => {
    if (event.target === overlay) closeOverlays();
  }));
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") closeOverlays();
    if ((event.key === "Enter" || event.key === " ") && event.target.matches('[data-action="preview"]')) {
      event.preventDefault();
      event.target.click();
    }
  });
  document.addEventListener("visibilitychange", refreshVisibleView);
  window.addEventListener("pagehide", (event) => { if (!event.persisted) clearPreviewUrls(); });
}

function refreshVisibleView() {
  if (document.hidden) return;
  if (location.hash === "#assets") refreshAssets();
  else refreshJobs();
}

function routeView() {
  const view = location.hash === "#assets" ? "assets" : "tasks";
  $$(".view").forEach((node) => node.classList.toggle("active", node.id === `${view}View`));
  $$('[data-view-link]').forEach((node) => node.classList.toggle("active", node.dataset.viewLink === view));
  $("#pageCrumb").textContent = view === "assets" ? "媒体文件" : "任务工单";
  $("#sidebar").classList.remove("open");
  if (view === "assets" && !state.assets.length) refreshAssets();
}

async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, options);
  if (!response.ok) {
    let detail = `${response.status} ${response.statusText}`;
    try { detail = (await response.json()).error || detail; } catch (_) { /* Non-JSON error. */ }
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

async function refreshAssets() {
  if (state.loadingAssets) return;
  state.loadingAssets = true;
  if (!state.assetsLoaded) setStateBox("assets", "loading", "正在加载媒体文件…");
  try {
    await ensureJobsLoaded();
    const assets = listFrom(await api("/assets"), "assets");
    const signature = assetListSignature(assets);
    const changed = signature !== state.assetsSignature;
    state.assets = assets;
    state.assetsSignature = signature;
    if (!state.assetsLoaded || changed) renderAssets();
    state.assetsLoaded = true;
    setHealth(true);
  } catch (error) {
    if (!state.assetsLoaded) {
      state.assets = [];
      $("#assetGrid").replaceChildren();
      setStateBox("assets", "error", "媒体加载失败", error.message);
    }
    setHealth(false);
  } finally {
    state.loadingAssets = false;
  }
}

function assetListSignature(assets) {
  return JSON.stringify(assets.map((asset) => ({
    jobId: String(asset.jobId || ""),
    assetId: String(asset.assetId || asset.id || ""),
    typeId: String(asset.typeId || ""),
    state: String(asset.state || ""),
    size: Number(asset.sizeBytes || asset.size || 0),
    md5: String(asset.md5 || ""),
    fileName: assetName(asset),
  })).sort((left, right) => `${left.jobId}/${left.assetId}`.localeCompare(`${right.jobId}/${right.assetId}`)));
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
    ["sent", "已下发", counts.sent, "#3278c4", "#eaf2fb"],
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
    const text = `${job.jobId || job.id || ""} ${job.title || ""}`.toLowerCase();
    return (!search || text.includes(search)) && (!status || job.status === status);
  });
  $("#jobsCount").textContent = `共 ${jobs.length} 条任务`;
  $("#jobsTable").innerHTML = jobs.map(jobRow).join("");
  if (!jobs.length) setStateBox("jobs", "empty", search || status ? "没有符合筛选条件的任务" : "还没有任务", search || status ? "调整搜索词或筛选条件后重试" : "点击“新增任务”创建第一条工单");
  else hideStateBox("jobs");
}

function jobRow(job) {
  const id = job.jobId || job.id || "";
  const extras = job.extras || {};
  const rcSn = job.targetRcSn || "";
  const device = state.devices.find((item) => item.rcSn === rcSn);
  const deviceState = device?.online === true ? "在线" : "离线";
  const routeName = job.routeFileName || job.route?.fileName || extras.routeFileName;
  const mayEdit = ["DRAFT", undefined, null].includes(job.status);
  const mayDispatch = job.status === "DRAFT";
  const mayDelete = !["IN_PROGRESS", "FINALIZING"].includes(job.status);
  return `<tr>
    <td><div class="job-name"><strong>${escapeHtml(job.title || "未命名任务")}</strong><small>${escapeHtml(id)}</small></div></td>
    <td><div class="device-cell"><span class="device-icon">${icons.device}</span><span><strong>${escapeHtml(rcSn || "未分配")}</strong><small>${rcSn ? deviceState : "等待分配"}</small></span></div></td>
    <td class="date-cell"><span>${escapeHtml(formatDate(extras.deadline || job.deadline, false))}</span><small>${escapeHtml(extras.missionType || job.missionType || "飞行任务")}</small></td>
    <td>${job.routeStatus === "CLOUD_PROVIDED" ? `<span class="route-chip">${icons.route}${escapeHtml(routeName || "已配置")}</span>` : '<span class="muted">未上传</span>'}</td>
    <td>${statusBadge(job.status || "DRAFT")}</td>
    <td><div class="row-actions"><button class="text-button" data-action="detail" data-id="${escapeAttr(id)}">详情</button>${mayEdit ? `<button class="text-button" data-action="edit" data-id="${escapeAttr(id)}">编辑</button>` : ""}${mayDispatch ? `<button class="text-button" data-action="dispatch" data-id="${escapeAttr(id)}">下发</button>` : ""}${job.status === "DISPATCHED" ? `<button class="text-button danger" data-action="withdraw" data-id="${escapeAttr(id)}">撤回</button>` : ""}${mayDelete ? `<button class="text-button danger" data-action="delete" data-id="${escapeAttr(id)}">删除</button>` : ""}</div></td>
  </tr>`;
}

function renderAssets() {
  reconcilePreviewUrls();
  const search = $("#assetSearch").value.trim().toLowerCase();
  const type = $("#assetTypeFilter").value;
  const status = $("#assetStatusFilter").value;
  const routes = state.jobs.filter((job) => job.routeStatus === "CLOUD_PROVIDED").map((job) => ({
    assetId: `route:${job.jobId || job.id}`,
    jobId: job.jobId || job.id,
    typeId: "route",
    fileName: job.routeFileName || `${job.jobId || job.id}.kmz`,
    state: "CONFIGURED",
    virtualRoute: true,
  }));
  const assets = [...routes, ...state.assets].filter((asset) => {
    const normalizedType = assetType(asset);
    const text = `${asset.assetId || asset.id || ""} ${asset.jobId || ""} ${assetName(asset)}`.toLowerCase();
    return (!search || text.includes(search)) && (!type || normalizedType === type) && (!status || asset.state === status);
  });
  const groups = groupAssetsByJob(assets);
  const forceExpand = Boolean(search || type || status);
  $("#assetsCount").textContent = `共 ${assets.length} 个文件 · ${groups.length} 个任务`;
  $("#assetGrid").innerHTML = groups.map((group) => assetGroup(group, forceExpand)).join("");
  loadJpegPreviews();
  if (!assets.length) setStateBox("assets", "empty", search || type || status ? "没有符合筛选条件的文件" : "暂无媒体文件", "任务执行并完成同步后，文件将显示在这里");
  else hideStateBox("assets");
}

function groupAssetsByJob(assets) {
  const groups = new Map();
  assets.forEach((asset) => {
    const jobId = String(asset.jobId || "");
    if (!groups.has(jobId)) groups.set(jobId, []);
    groups.get(jobId).push(asset);
  });
  return [...groups.entries()]
    .map(([jobId, items]) => ({ jobId, items }))
    .sort((left, right) => backgroundJobOrder(left.jobId) - backgroundJobOrder(right.jobId));
}

function backgroundJobOrder(jobId) {
  if (!jobId) return state.jobs.length + 1;
  const index = state.jobs.findIndex((job) => String(job.jobId || job.id) === jobId);
  return index < 0 ? state.jobs.length : index;
}

function assetGroup(group, forceExpand = false) {
  const { jobId, items } = group;
  const job = findJob(jobId);
  const title = job?.title || jobId || "未关联任务";
  const photos = items.filter((asset) => assetType(asset) === "photo").length;
  const bytes = items.reduce((sum, asset) => sum + Number(asset.sizeBytes || asset.size || 0), 0);
  const expanded = forceExpand || state.expandedGroups.has(jobId);
  return `<section class="asset-group${expanded ? " open" : ""}">
    <button class="asset-group-head" type="button" data-action="toggle-group" data-job-id="${escapeAttr(jobId)}" aria-expanded="${expanded}">
      <span class="asset-group-chevron"><svg viewBox="0 0 24 24"><path d="m9 6 6 6-6 6"/></svg></span>
      <span class="asset-group-title"><strong>${escapeHtml(title)}</strong><small>${escapeHtml(jobId || "未关联任务")}</small></span>
      <span class="asset-group-meta">${job ? statusBadge(job.status || "DRAFT") : ""}<span>${photos} 张照片</span><span>${formatBytes(bytes)}</span></span>
    </button>
    <div class="asset-group-body"${expanded ? "" : " hidden"}><div class="asset-grid">${items.map(assetCard).join("")}</div></div>
  </section>`;
}

function toggleGroup(jobId) {
  const key = String(jobId || "");
  if (state.expandedGroups.has(key)) state.expandedGroups.delete(key);
  else state.expandedGroups.add(key);
  renderAssets();
}

function assetCard(asset) {
  const id = asset.assetId || asset.id || "";
  const type = assetType(asset);
  const name = assetName(asset);
  const isJpeg = type === "photo" && /\.jpe?g$/i.test(name);
  const contentUrl = `${API}/assets/${encodeURIComponent(id)}/content?jobId=${encodeURIComponent(asset.jobId || "")}`;
  const previewKey = isJpeg ? jpegPreviewKey(asset) : "";
  const previewUrl = state.previewUrls.get(previewKey)?.url;
  const preview = isJpeg
    ? `<img data-preview-url="${contentUrl}" data-preview-key="${escapeAttr(previewKey)}"${previewUrl ? ` src="${escapeAttr(previewUrl)}"` : ""} alt="${escapeAttr(name)}" loading="lazy">`
    : icons.route;
  const previewAction = isJpeg
    ? ` data-action="preview" data-id="${escapeAttr(id)}" data-job-id="${escapeAttr(asset.jobId || "")}" role="button" tabindex="0" aria-label="查看 ${escapeAttr(name)}"`
    : "";
  return `<article class="asset-card">
    <div class="asset-preview ${type === "route" ? "route" : ""}${isJpeg ? " clickable" : ""}"${previewAction}>${preview}<span class="asset-type">${type === "route" ? "KMZ 航线" : "JPEG 照片"}</span></div>
    <div class="asset-copy"><div class="asset-title" title="${escapeAttr(name)}">${escapeHtml(name)}</div><div class="asset-meta"><span>${escapeHtml(asset.jobId || "未关联任务")}</span><span>${asset.virtualRoute ? "KMZ" : formatBytes(asset.sizeBytes || asset.size)}</span></div>
    <div class="asset-actions">${statusBadge(asset.state || "UPLOADED")}<a class="text-button" href="${type === "route" && asset.jobId ? `${API}/routes/${encodeURIComponent(asset.jobId)}/content` : contentUrl}" download="${escapeAttr(name)}">下载</a></div></div>
  </article>`;
}

async function loadJpegPreviews() {
  await Promise.all($$(".asset-group.open img[data-preview-url]").map(async (image) => {
    const key = image.dataset.previewKey;
    let entry = state.previewUrls.get(key);
    try {
      if (!entry) {
        entry = { url: "", promise: null };
        state.previewUrls.set(key, entry);
        entry.promise = fetch(image.dataset.previewUrl).then(async (response) => {
          if (!response.ok) throw new Error(String(response.status));
          const blob = await response.blob();
          if (state.previewUrls.get(key) !== entry) return "";
          entry.url = URL.createObjectURL(blob);
          return entry.url;
        }).catch((error) => {
          if (state.previewUrls.get(key) === entry) state.previewUrls.delete(key);
          throw error;
        }).finally(() => { entry.promise = null; });
      }
      const url = entry.url || await entry.promise;
      if (url && image.isConnected && image.dataset.previewKey === key) image.src = url;
    } catch (_) {
      if (image.isConnected && image.dataset.previewKey === key) image.hidden = true;
    }
  }));
}

function jpegPreviewKey(asset) {
  const id = asset.assetId || asset.id || "";
  const md5 = String(asset.md5 || asset.meta?.md5 || "").toLowerCase();
  const identity = md5 ? `md5:${md5}` : `meta:${asset.sizeBytes ?? asset.size ?? ""}:${assetName(asset)}`;
  return JSON.stringify([String(asset.jobId || ""), String(id), identity]);
}

function reconcilePreviewUrls() {
  const currentKeys = new Set(state.assets
    .filter((asset) => assetType(asset) === "photo" && /\.jpe?g$/i.test(assetName(asset)))
    .map(jpegPreviewKey));
  for (const [key, entry] of state.previewUrls) {
    if (currentKeys.has(key)) continue;
    state.previewUrls.delete(key);
    if (entry.url) URL.revokeObjectURL(entry.url);
  }
}

function clearPreviewUrls() {
  for (const entry of state.previewUrls.values()) {
    if (entry.url) URL.revokeObjectURL(entry.url);
  }
  state.previewUrls.clear();
}

function handleAction(event) {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const actions = {
    "new-job": () => openJobModal(),
    "close-modal": () => closeOverlay("jobModal"),
    "close-detail": () => closeOverlay("detailDrawer"),
    "refresh-jobs": refreshJobs,
    "refresh-assets": refreshAssets,
    edit: () => openJobModal(findJob(target.dataset.id)),
    detail: () => openDetail(findJob(target.dataset.id)),
    dispatch: () => dispatchJob(target.dataset.id, target),
    withdraw: () => withdrawJob(target.dataset.id, target),
    delete: () => deleteJob(target.dataset.id, target),
    preview: () => openLightbox(target.dataset.id, target.dataset.jobId),
    "toggle-group": () => toggleGroup(target.dataset.jobId),
    "close-lightbox": () => closeOverlay("assetLightbox"),
  };
  actions[target.dataset.action]?.();
}

function openLightbox(assetId, jobId) {
  const asset = state.assets.find((item) => {
    const id = item.assetId || item.id || "";
    return String(id) === String(assetId) && String(item.jobId || "") === String(jobId || "");
  });
  if (!asset) return;
  const name = assetName(asset);
  const contentUrl = `${API}/assets/${encodeURIComponent(asset.assetId || asset.id || "")}/content?jobId=${encodeURIComponent(asset.jobId || "")}`;
  const cached = state.previewUrls.get(jpegPreviewKey(asset))?.url;
  const image = $("#lightboxImage");
  image.src = cached || contentUrl;
  image.alt = name;
  $("#lightboxTitle").textContent = name;
  $("#lightboxMeta").textContent = `${asset.jobId || "未关联任务"} · ${formatBytes(asset.sizeBytes || asset.size)}`;
  const download = $("#lightboxDownload");
  download.href = contentUrl;
  download.download = name;
  $("#assetLightbox").hidden = false;
  document.body.style.overflow = "hidden";
}

function findJob(id) {
  return state.jobs.find((job) => String(job.jobId || job.id) === String(id));
}

function openJobModal(job = null) {
  const form = $("#jobForm");
  form.reset();
  renderDeviceOptions();
  $("#routeFileHint").textContent = job?.routeStatus === "CLOUD_PROVIDED" ? (job.routeFileName || `${job.jobId}.kmz（已配置）`) : "未选择新文件；管理台不编辑航点、高度或速度";
  $("#jobModalTitle").textContent = job ? "编辑任务" : "新增任务";
  if (job) {
    const extras = job.extras || {};
    form.elements.jobId.value = job.jobId || job.id || "";
    form.elements.title.value = job.title || "";
    form.elements.missionType.value = extras.missionType || job.missionType || "INSPECTION";
    form.elements.rcSn.value = job.targetRcSn || "";
    form.elements.deadline.value = toLocalInput(extras.deadline || job.deadline);
    form.elements.requirements.value = extras.requirements || job.requirements || "";
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

async function saveJob(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const submitter = event.submitter;
  const shouldDispatch = submitter?.value === "dispatch";
  const data = new FormData(form);
  const id = data.get("jobId");
  const targetRcSn = data.get("rcSn") || null;
  const payload = {
    title: String(data.get("title")).trim(),
    targetRcSn,
    extras: {
      missionType: data.get("missionType"),
      deadline: data.get("deadline") ? new Date(data.get("deadline")).toISOString() : null,
      requirements: String(data.get("requirements") || "").trim(),
    },
  };
  if (!payload.title) return;
  setFormBusy(true);
  try {
    const result = await api(id ? `/jobs/${encodeURIComponent(id)}` : "/jobs", {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const saved = result?.job || result;
    const savedId = saved?.jobId || saved?.id || id;
    if (!savedId) throw new Error("服务端未返回任务编号");
    form.elements.jobId.value = savedId;
    const route = data.get("route");
    if (route?.size) await api(`/jobs/${encodeURIComponent(savedId)}/route`, { method: "PUT", headers: { "Content-Type": "application/octet-stream", "X-File-Name": route.name }, body: route });
    if (shouldDispatch) {
      if (!targetRcSn) throw new Error("下发前必须选择遥控器 SN");
      await api(`/jobs/${encodeURIComponent(savedId)}/dispatch`, { method: "POST" });
    }
    toast(shouldDispatch ? "任务已保存并下发" : "任务草稿已保存");
    closeOverlay("jobModal");
    await refreshJobs();
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
  if (!id || !confirm("确认永久删除该任务？关联的航线、媒体文件和上传记录也会一并删除，且无法恢复。")) return;
  button.disabled = true;
  try {
    await api(`/jobs/${encodeURIComponent(id)}`, { method: "DELETE" });
    toast("任务及关联文件已删除");
    await refreshJobs();
    if (state.assetsLoaded) {
      state.assetsSignature = "";
      await refreshAssets();
    }
  } catch (error) {
    toast(`删除失败：${error.message}`, true);
  } finally {
    button.disabled = false;
  }
}

async function openDetail(job) {
  if (!job) return;
  $("#detailDrawer").hidden = false;
  document.body.style.overflow = "hidden";
  renderDetail(job, null, true);
  const id = job.jobId || job.id;
  try {
    const assets = listFrom(await api(`/jobs/${encodeURIComponent(id)}/assets`), "assets");
    renderDetail(job, assets, false);
  } catch (error) {
    renderDetail(job, [], false, error.message);
  }
}

function renderDetail(job, assets, loading, assetError = "") {
  const id = job.jobId || job.id || "";
  const extras = job.extras || {};
  const timeline = Array.isArray(job.timeline) ? [...job.timeline].reverse() : [];
  const uploaded = assets?.filter((asset) => asset.state === "UPLOADED").length || 0;
  const photos = assets?.filter((asset) => assetType(asset) === "photo").length || 0;
  const totalBytes = assets?.reduce((sum, asset) => sum + Number(asset.sizeBytes || asset.size || 0), 0) || 0;
  const routeConfigured = job.routeStatus === "CLOUD_PROVIDED";
  const routeName = routeConfigured ? (job.routeFileName || `${id}.kmz`) : "未配置";
  $("#detailTitle").textContent = "任务详情";
  $("#detailBody").innerHTML = `
    <section class="detail-hero">${statusBadge(job.status || "DRAFT")}<h3>${escapeHtml(job.title || "未命名任务")}</h3><p>${escapeHtml(id)}</p></section>
    <section class="detail-section"><h3 class="detail-section-title">任务信息</h3><div class="info-grid">
      ${infoItem("任务类型", extras.missionType || job.missionType || "INSPECTION")}${infoItem("遥控器 SN", job.targetRcSn || "未分配")}
      ${infoItem("截止时间", formatDate(extras.deadline || job.deadline))}${infoItem("航线编辑", "请在航线工具中修改后重新上传 KMZ")}
      ${infoItem("作业要求", extras.requirements || job.requirements || "未填写")}${infoItem("航线状态", routeConfigured ? "已配置" : "未上传")}
    </div></section>
    <section class="detail-section"><h3 class="detail-section-title">航线文件</h3><div class="download-card"><span class="device-icon">${icons.route}</span><div><strong>${escapeHtml(routeName)}</strong><small>KMZ 航线文件</small></div>${routeConfigured ? `<a class="text-button" href="${API}/routes/${encodeURIComponent(id)}/content" download="${escapeAttr(routeName)}">下载</a>` : ""}</div></section>
    <section class="detail-section"><h3 class="detail-section-title">文件同步统计 <span>${assetError ? escapeHtml(assetError) : loading ? "加载中…" : `${assets.length} 个文件`}</span></h3><div class="sync-row"><div class="sync-metric"><strong>${loading ? "–" : photos}</strong><span>任务照片</span></div><div class="sync-metric"><strong>${loading ? "–" : uploaded}</strong><span>已同步</span></div><div class="sync-metric"><strong>${loading ? "–" : formatBytes(totalBytes)}</strong><span>文件容量</span></div></div></section>
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

function assetType(asset) {
  const type = String(asset.typeId || asset.type || "").toLowerCase();
  if (type.includes("route") || /\.kmz$/i.test(assetName(asset))) return "route";
  return "photo";
}

function assetName(asset) { return asset.fileName || asset.meta?.fileName || asset.name || asset.assetId || asset.id || "未命名文件"; }

function formatBytes(bytes) {
  const value = Number(bytes || 0);
  if (!value) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1);
  return `${(value / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`;
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
