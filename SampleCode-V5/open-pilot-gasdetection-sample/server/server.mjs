import http from "node:http";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createStore } from "./store.mjs";

const directory = path.dirname(fileURLToPath(import.meta.url));
const DEFAULT_PORT = 18082;
const DEFAULT_HOST = "127.0.0.1";
const MAX_BODY_BYTES = 1024 * 1024;
const ONLINE_WINDOW_MS = 30_000;
const ACTIVE_STATUSES = new Set(["IN_PROGRESS", "FINALIZING"]);
const ALLOWED_TRANSITIONS = {
  DISPATCHED: new Set(["DISPATCHED", "ACCEPTED", "REJECTED", "CANCELLED"]),
  ACCEPTED: new Set(["ACCEPTED", "IN_PROGRESS", "CANCELLED"]),
  IN_PROGRESS: new Set(["IN_PROGRESS", "FINALIZING", "CANCELLED"]),
  FINALIZING: new Set(["FINALIZING", "COMPLETED", "CANCELLED"]),
  COMPLETED: new Set(["COMPLETED"]),
  CANCELLED: new Set(["CANCELLED"]),
  REJECTED: new Set(["REJECTED"]),
};

const STATIC_FILES = new Map([
  ["/", [path.join(directory, "web/index.html"), "text/html; charset=utf-8"]],
  ["/index.html", [path.join(directory, "web/index.html"), "text/html; charset=utf-8"]],
  ["/app.js", [path.join(directory, "web/app.js"), "text/javascript; charset=utf-8"]],
  ["/styles.css", [path.join(directory, "web/styles.css"), "text/css; charset=utf-8"]],
]);

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

/**
 * 创建气体检测演示服务。
 *
 * 不启动监听，调用方负责 `listen`，便于测试用随机端口。数据目录默认取 `DATA_DIR`，
 * 未设置时使用服务目录下的 `runtime/`。
 */
export async function createApp(options = {}) {
  const dataDir = path.resolve(options.dataDir || process.env.DATA_DIR || path.join(directory, "runtime"));
  const store = await createStore(dataDir);

  const server = http.createServer((request, response) => {
    handle(request, response).catch((error) => {
      const status = error instanceof HttpError ? error.status : 500;
      const message = error instanceof HttpError ? error.message : "Internal server error";
      if (status === 500) console.error(error);
      sendJson(response, status, { error: message });
    });
  });

  async function handle(request, response) {
    const url = new URL(request.url || "/", "http://localhost");

    if (request.method === "OPTIONS") {
      response.writeHead(204, {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "Content-Type, X-RC-SN",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
      });
      return response.end();
    }

    if ((request.method === "GET" || request.method === "HEAD") && url.pathname === "/health") {
      if (request.method === "HEAD") return response.writeHead(200).end();
      return sendJson(response, 200, { status: "ok" });
    }

    if (url.pathname.startsWith("/api/v1/")) {
      return handleAppApi(request, response, url);
    }
    if (url.pathname.startsWith("/api/admin/")) {
      return handleAdminApi(request, response, url);
    }

    const staticFile = STATIC_FILES.get(url.pathname);
    if (request.method === "GET" && staticFile) {
      const [file, type] = staticFile;
      const content = await readFile(file);
      response.writeHead(200, { "Content-Type": type });
      return response.end(content);
    }

    throw new HttpError(404, "Not found");
  }

  async function handleAppApi(request, response, url) {
    const rcSn = requireRcSn(request);
    await store.update((state) => registerDevice(state, rcSn));

    if (request.method === "GET" && url.pathname === "/api/v1/jobs") {
      const jobs = store.state.jobs
        .filter((job) => job.status !== "DRAFT" && job.targetRcSn === rcSn)
        .map((job) => job);
      return sendJson(response, 200, { jobs });
    }

    const jobMatch = url.pathname.match(/^\/api\/v1\/jobs\/([^/]+)$/);
    if (request.method === "GET" && jobMatch) {
      const job = requireAppJob(findJob(store.state, decodeSegment(jobMatch[1])), rcSn);
      return sendJson(response, 200, { job });
    }

    const statusMatch = url.pathname.match(/^\/api\/v1\/jobs\/([^/]+)\/status$/);
    if (request.method === "PUT" && statusMatch) {
      const jobId = decodeSegment(statusMatch[1]);
      const body = await readJson(request);
      const nextStatus = typeof body.status === "string" ? body.status : "";
      if (!Object.hasOwn(ALLOWED_TRANSITIONS, nextStatus)) {
        throw new HttpError(400, `Unsupported status: ${nextStatus}`);
      }

      const job = await store.update((state) => {
        const current = requireAppJob(findJob(state, jobId), rcSn);
        if (!ALLOWED_TRANSITIONS[current.status]?.has(nextStatus)) {
          throw new HttpError(409, `Invalid task transition: ${current.status} -> ${nextStatus}`);
        }
        if (current.status === nextStatus) return current;
        if (nextStatus === "IN_PROGRESS" && state.jobs.some((item) =>
          item.jobId !== current.jobId && item.targetRcSn === rcSn && ACTIVE_STATUSES.has(item.status)
        )) {
          throw new HttpError(409, "Remote controller already has an active task");
        }
        current.status = nextStatus;
        current.updatedAt = new Date().toISOString();
        current.timeline.push({ timestamp: Date.now(), description: `任务状态更新为 ${nextStatus}` });
        return current;
      });
      return sendJson(response, 200, { job });
    }

    // 检测记录：按 recordId 幂等写入，供插件离线补传。
    if (request.method === "POST" && url.pathname === "/api/v1/inspections") {
      const body = await readJson(request);
      const recordId = typeof body.recordId === "string" ? body.recordId.trim() : "";
      const jobId = typeof body.jobId === "string" ? body.jobId.trim() : "";
      if (!recordId || !jobId) throw new HttpError(400, "recordId and jobId are required");
      if (body.totalSamples !== undefined && typeof body.totalSamples !== "number") {
        throw new HttpError(400, "totalSamples must be a number");
      }
      requireAppJob(findJob(store.state, jobId), rcSn);
      const result = await store.update((state) => {
        const existing = state.inspections.find((item) => item.recordId === recordId);
        if (existing) return { record: existing, created: false };
        const record = {
          ...body,
          recordId,
          jobId,
          rcSn,
          receivedAt: new Date().toISOString(),
        };
        state.inspections.push(record);
        return { record, created: true };
      });
      return sendJson(response, result.created ? 201 : 200, { record: result.record });
    }

    if (request.method === "GET" && url.pathname === "/api/v1/inspections") {
      const jobId = url.searchParams.get("jobId");
      const records = store.state.inspections.filter((item) =>
        item.rcSn === rcSn && (!jobId || item.jobId === jobId)
      );
      return sendJson(response, 200, { records });
    }

    throw new HttpError(404, "Not found");
  }

  async function handleAdminApi(request, response, url) {
    if (request.method === "GET" && url.pathname === "/api/admin/devices") {
      const now = Date.now();
      const devices = store.state.devices.map((device) => ({
        rcSn: device.rcSn,
        lastSeenAt: device.lastSeenAt,
        online: now - Date.parse(device.lastSeenAt) <= ONLINE_WINDOW_MS,
      }));
      return sendJson(response, 200, { devices });
    }

    if (request.method === "GET" && url.pathname === "/api/admin/jobs") {
      return sendJson(response, 200, { jobs: store.state.jobs });
    }

    // 检测记录只读查询：管理台「检测记录」视图使用，不提供管理端写入口。
    if (request.method === "GET" && url.pathname === "/api/admin/inspections") {
      const jobId = url.searchParams.get("jobId");
      const records = store.state.inspections
        .filter((item) => !jobId || item.jobId === jobId)
        .sort((left, right) => receivedAtMillis(right) - receivedAtMillis(left));
      return sendJson(response, 200, { records });
    }

    if (request.method === "POST" && url.pathname === "/api/admin/jobs") {
      const body = await readJson(request);
      const title = requireText(body.title, "title");
      const gas = validateGas(body.gas);
      const targetRcSn = parseTargetRcSn(body.targetRcSn);
      const now = Date.now();
      const job = {
        jobId: nextJobId(store.state.jobs),
        title,
        status: "DRAFT",
        targetRcSn,
        gas,
        timeline: [{ timestamp: now, description: "任务已创建" }],
        createdAt: new Date(now).toISOString(),
        updatedAt: new Date(now).toISOString(),
      };
      await store.update((state) => state.jobs.push(job));
      return sendJson(response, 201, { job });
    }

    const jobMatch = url.pathname.match(/^\/api\/admin\/jobs\/([^/]+)$/);
    if (request.method === "PUT" && jobMatch) {
      const jobId = decodeSegment(jobMatch[1]);
      const body = await readJson(request);
      const job = await store.update((state) => {
        const current = findJob(state, jobId);
        if (current.status !== "DRAFT") throw new HttpError(409, "Only draft tasks can be edited");
        if (body.title !== undefined) current.title = requireText(body.title, "title");
        if (body.gas !== undefined) current.gas = validateGas(body.gas);
        if (Object.hasOwn(body, "targetRcSn")) current.targetRcSn = parseTargetRcSn(body.targetRcSn);
        current.updatedAt = new Date().toISOString();
        current.timeline.push({ timestamp: Date.now(), description: "任务信息已更新" });
        return current;
      });
      return sendJson(response, 200, { job });
    }

    if (request.method === "DELETE" && jobMatch) {
      const jobId = decodeSegment(jobMatch[1]);
      await store.update((state) => {
        const current = findJob(state, jobId);
        if (ACTIVE_STATUSES.has(current.status)) {
          throw new HttpError(409, "Active tasks cannot be deleted; cancel or complete the task first");
        }
        state.jobs = state.jobs.filter((item) => item.jobId !== current.jobId);
      });
      return sendJson(response, 200, { deleted: true, jobId });
    }

    const dispatchMatch = url.pathname.match(/^\/api\/admin\/jobs\/([^/]+)\/dispatch$/);
    if (request.method === "POST" && dispatchMatch) {
      const jobId = decodeSegment(dispatchMatch[1]);
      const job = await store.update((state) => {
        const current = findJob(state, jobId);
        if (current.status !== "DRAFT") throw new HttpError(409, "Only draft tasks can be dispatched");
        if (!current.targetRcSn) throw new HttpError(409, "targetRcSn is required before dispatch");
        current.status = "DISPATCHED";
        current.updatedAt = new Date().toISOString();
        current.timeline.push({ timestamp: Date.now(), description: `任务已下发给 ${current.targetRcSn}` });
        return current;
      });
      return sendJson(response, 200, { job });
    }

    const withdrawMatch = url.pathname.match(/^\/api\/admin\/jobs\/([^/]+)\/withdraw$/);
    if (request.method === "POST" && withdrawMatch) {
      const jobId = decodeSegment(withdrawMatch[1]);
      const job = await store.update((state) => {
        const current = findJob(state, jobId);
        if (current.status !== "DISPATCHED") throw new HttpError(409, "Only dispatched tasks can be withdrawn");
        current.status = "DRAFT";
        current.updatedAt = new Date().toISOString();
        current.timeline.push({ timestamp: Date.now(), description: "任务已撤回为草稿" });
        return current;
      });
      return sendJson(response, 200, { job });
    }

    throw new HttpError(404, "Not found");
  }

  return server;
}

function requireRcSn(request) {
  const header = request.headers["x-rc-sn"];
  const value = Array.isArray(header) ? header[0] : header;
  if (typeof value !== "string" || !value.trim()) throw new HttpError(400, "X-RC-SN header is required");
  return value.trim();
}

function registerDevice(state, rcSn) {
  const timestamp = new Date().toISOString();
  const device = state.devices.find((item) => item.rcSn === rcSn);
  if (device) {
    device.lastSeenAt = timestamp;
  } else {
    state.devices.push({ rcSn, lastSeenAt: timestamp });
  }
}

function findJob(state, jobId) {
  const job = state.jobs.find((item) => item.jobId === jobId);
  if (!job) throw new HttpError(404, "Task not found");
  return job;
}

/** 检测记录按接收时间倒序排序；缺失或非法时间视为最旧。 */
function receivedAtMillis(record) {
  const value = Date.parse(record.receivedAt);
  return Number.isNaN(value) ? 0 : value;
}

function requireAppJob(job, rcSn) {
  if (job.status === "DRAFT" || job.targetRcSn !== rcSn) throw new HttpError(404, "Task not found");
  return job;
}

function nextJobId(jobs) {
  const used = jobs
    .map((job) => Number(/^GAS-DEMO-(\d+)$/.exec(job.jobId)?.[1] ?? 0))
    .filter((value) => Number.isFinite(value));
  return `GAS-DEMO-${String(Math.max(0, ...used) + 1).padStart(3, "0")}`;
}

function parseTargetRcSn(value) {
  if (value === undefined || value === null || value === "") return null;
  if (typeof value !== "string") throw new HttpError(400, "targetRcSn must be a string");
  return value.trim() || null;
}

function validateGas(raw) {
  if (!isPlainObject(raw)) throw new HttpError(400, "gas is required");
  if (!isPlainObject(raw.target)) throw new HttpError(400, "gas.target is required");
  const target = {
    latitude: numberInRange(raw.target.latitude, "gas.target.latitude", -90, 90),
    longitude: numberInRange(raw.target.longitude, "gas.target.longitude", -180, 180),
    altitudeM: numberInRange(raw.target.altitudeM, "gas.target.altitudeM", 0, 500),
  };
  if (!Array.isArray(raw.channels) || raw.channels.length === 0) {
    throw new HttpError(400, "gas.channels must not be empty");
  }
  const seen = new Set();
  const channels = raw.channels.map((item, index) => {
    const prefix = `gas.channels[${index}]`;
    if (!isPlainObject(item)) throw new HttpError(400, `${prefix} must be an object`);
    const channelId = requireText(item.channelId, `${prefix}.channelId`);
    if (seen.has(channelId)) throw new HttpError(400, `${prefix}.channelId is duplicated`);
    seen.add(channelId);
    return {
      channelId,
      displayName: requireText(item.displayName, `${prefix}.displayName`),
      unit: requireText(item.unit, `${prefix}.unit`),
      threshold: requireNumber(item.threshold, `${prefix}.threshold`),
    };
  });
  const safetyBoundaryM = numberInRange(raw.safetyBoundaryM, "gas.safetyBoundaryM", 1, 5000);
  let requirements = null;
  if (raw.requirements !== undefined && raw.requirements !== null) {
    if (typeof raw.requirements !== "string") throw new HttpError(400, "gas.requirements must be a string");
    requirements = raw.requirements.trim() || null;
  }
  let deadline = null;
  if (raw.deadline !== undefined && raw.deadline !== null) {
    if (typeof raw.deadline !== "string" || Number.isNaN(Date.parse(raw.deadline))) {
      throw new HttpError(400, "gas.deadline must be an ISO timestamp");
    }
    deadline = raw.deadline;
  }
  return { target, channels, safetyBoundaryM, requirements, deadline };
}

function requireText(value, field, trim = true) {
  if (typeof value !== "string") throw new HttpError(400, `${field} is required`);
  const text = trim ? value.trim() : value;
  if (!text) throw new HttpError(400, `${field} is required`);
  return text;
}

function requireNumber(value, field) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new HttpError(400, `${field} must be a finite number`);
  }
  return value;
}

function numberInRange(value, field, min, max) {
  const number = requireNumber(value, field);
  if (number < min || number > max) {
    throw new HttpError(400, `${field} must be between ${min} and ${max}`);
  }
  return number;
}

function isPlainObject(value) {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function decodeSegment(value) {
  return decodeURIComponent(value);
}

async function readJson(request) {
  let size = 0;
  const chunks = [];
  for await (const chunk of request) {
    size += chunk.length;
    if (size > MAX_BODY_BYTES) throw new HttpError(413, "Request body too large");
    chunks.push(chunk);
  }
  const text = Buffer.concat(chunks).toString("utf8");
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch {
    throw new HttpError(400, "Invalid JSON body");
  }
}

function sendJson(response, status, value) {
  const body = `${JSON.stringify(value)}\n`;
  response.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
    "Access-Control-Allow-Origin": "*",
  });
  response.end(body);
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const port = Number(process.env.PORT || DEFAULT_PORT);
  const host = process.env.HOST || DEFAULT_HOST;
  const server = await createApp();
  server.listen(port, host, () => {
    console.log(`gas demo server listening on http://${host}:${port}`);
  });
}
