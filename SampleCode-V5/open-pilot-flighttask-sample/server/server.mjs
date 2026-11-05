import { createHash, randomBytes, randomUUID } from "node:crypto";
import { createReadStream, createWriteStream } from "node:fs";
import { mkdir, open, readdir, rename, rm, stat } from "node:fs/promises";
import { createServer } from "node:http";
import path from "node:path";
import { Transform } from "node:stream";
import { pipeline } from "node:stream/promises";
import { fileURLToPath } from "node:url";
import { createStore } from "./store.mjs";

const directory = path.dirname(fileURLToPath(import.meta.url));
const MAX_JSON_BYTES = 1024 * 1024;
const MAX_FILE_BYTES = 1024 * 1024 * 1024;
const CHUNK_SIZE_BYTES = 8 * 1024 * 1024;
const validStatuses = new Set([
  "DISPATCHED", "ACCEPTED", "IN_PROGRESS", "FINALIZING", "COMPLETED", "CANCELLED",
]);
const allowedTransitions = {
  DISPATCHED: new Set(["DISPATCHED", "ACCEPTED", "CANCELLED"]),
  ACCEPTED: new Set(["ACCEPTED", "IN_PROGRESS", "CANCELLED"]),
  IN_PROGRESS: new Set(["IN_PROGRESS", "FINALIZING", "CANCELLED"]),
  FINALIZING: new Set(["FINALIZING", "COMPLETED", "CANCELLED"]),
  COMPLETED: new Set(["COMPLETED"]),
  CANCELLED: new Set(["CANCELLED"]),
};

export async function createApp(options = {}) {
  const dataDir = path.resolve(options.dataDir || process.env.DATA_DIR || path.join(directory, "runtime"));
  const store = await createStore(
    dataDir,
    options.seedRouteFile || path.join(directory, "data/FLIGHT-TASK-DEMO-001.kmz"),
  );
  const webRoot = path.resolve(options.webRoot || path.join(directory, "web"));
  await cleanupExpiredSessions(store);

  return createServer(async (request, response) => {
    setCors(response);
    if (request.method === "OPTIONS") {
      response.writeHead(204).end();
      return;
    }

    try {
      const url = new URL(request.url || "/", `http://${request.headers.host || "localhost"}`);
      console.log(`${new Date().toISOString()} ${request.method} ${url.pathname}`);

      let rcSn = null;
      if (url.pathname.startsWith("/api/v1/")) {
        rcSn = headerValue(request, "x-rc-sn").trim();
        if (!rcSn) throw new HttpError(400, "X-RC-SN is required");
        if (rcSn === "*") throw new HttpError(400, "X-RC-SN must identify one remote controller");
        if (rcSn.length > 200) throw new HttpError(400, "X-RC-SN is too long");
        await store.update((state) => {
          const device = state.devices.find((item) => item.rcSn === rcSn);
          const lastSeenAt = new Date().toISOString();
          if (device) device.lastSeenAt = lastSeenAt;
          else state.devices.push({ rcSn, lastSeenAt });
        });
      }

      if ((request.method === "GET" || request.method === "HEAD") && url.pathname === "/health") {
        if (request.method === "HEAD") return response.writeHead(200).end();
        return sendJson(response, 200, { status: "ok" });
      }

      if (request.method === "GET" && url.pathname === "/api/v1/jobs") {
        const jobs = store.state.jobs.filter((job) => job.status !== "DRAFT" && ownsJob(job, rcSn));
        return sendJson(response, 200, { jobs });
      }

      const appJobMatch = url.pathname.match(/^\/api\/v1\/jobs\/([^/]+)$/);
      if (request.method === "GET" && appJobMatch) {
        const job = findJob(store.state, decodeSegment(appJobMatch[1]));
        requireAppJob(job, rcSn);
        return sendJson(response, 200, { job });
      }

      const statusMatch = url.pathname.match(/^\/api\/v1\/jobs\/([^/]+)\/status$/);
      if (request.method === "PUT" && statusMatch) {
        const jobId = decodeSegment(statusMatch[1]);
        const body = await readJson(request);
        if (!validStatuses.has(body.status)) throw new HttpError(400, `Unsupported status: ${body.status}`);
        const job = await store.update((state) => {
          const job = findJob(state, jobId);
          requireAppJob(job, rcSn);
          if (!allowedTransitions[job.status]?.has(body.status)) {
            throw new HttpError(409, `Invalid task transition: ${job.status} -> ${body.status}`);
          }
          if (job.status === body.status) return job;
          if (body.status === "IN_PROGRESS" && state.jobs.some((item) =>
            item.jobId !== job.jobId && item.targetRcSn === rcSn && ["IN_PROGRESS", "FINALIZING"].includes(item.status),
          )) {
            throw new HttpError(409, "Remote controller already has an active task");
          }
          job.status = body.status;
          job.timeline.push({ timestamp: Date.now(), description: `任务状态更新为 ${body.status}` });
          return job;
        });
        return sendJson(response, 200, { job });
      }

      const appRouteMatch = url.pathname.match(/^\/api\/v1\/routes\/([^/]+)\.kmz$/);
      if (request.method === "GET" && appRouteMatch) {
        const job = findJob(store.state, decodeSegment(appRouteMatch[1]));
        requireAppJob(job, rcSn);
        return sendFile(response, routePath(store, job.jobId), "application/vnd.google-earth.kmz");
      }

      if (request.method === "GET" && url.pathname === "/api/v1/asset-types") {
        return sendJson(response, 200, { assetTypes: assetTypes() });
      }

      if (request.method === "POST" && url.pathname === "/api/v1/upload-sessions") {
        const asset = await readJson(request);
        validateUploadAsset(asset);
        if (asset.typeId === "route") throw new HttpError(400, "Routes are published by the admin service");
        const job = findJob(store.state, asset.jobId);
        requireUploadJob(job, rcSn);
        const staleSessions = store.state.uploadSessions.filter((item) =>
          item.ownerRcSn === rcSn && item.asset.jobId === asset.jobId && item.asset.assetId === asset.assetId,
        );
        for (const stale of staleSessions) {
          await rm(sessionPath(store, stale.sessionId), { recursive: true, force: true });
        }
        const sessionId = randomUUID();
        const partCount = Math.max(1, Math.ceil(asset.sizeBytes / CHUNK_SIZE_BYTES));
        const session = {
          sessionId,
          ownerRcSn: rcSn,
          asset: {
            assetId: String(asset.assetId),
            jobId: String(asset.jobId),
            typeId: String(asset.typeId),
            fileName: safeFileName(asset.fileName || asset.assetId),
            sizeBytes: asset.sizeBytes,
            md5: asset.md5 ? String(asset.md5).toLowerCase() : null,
          },
          chunkSizeBytes: CHUNK_SIZE_BYTES,
          partCount,
          uploadedParts: {},
          createdAt: new Date().toISOString(),
        };
        await mkdir(sessionPath(store, sessionId), { recursive: true });
        await store.update((state) => {
          requireUploadJob(findJob(state, asset.jobId), rcSn);
          state.uploadSessions = state.uploadSessions.filter((item) =>
            !(item.ownerRcSn === rcSn && item.asset.jobId === asset.jobId && item.asset.assetId === asset.assetId),
          );
          state.uploadSessions.push(session);
        });
        return sendJson(response, 200, sessionResponse(session));
      }

      const partMatch = url.pathname.match(/^\/api\/v1\/upload-sessions\/([^/]+)\/parts\/(\d+)$/);
      if (request.method === "PUT" && partMatch) {
        const session = findSession(store.state, decodeSegment(partMatch[1]));
        requireSessionOwner(session, rcSn);
        requireUploadJob(findJob(store.state, session.asset.jobId), rcSn);
        const index = Number(partMatch[2]);
        if (!Number.isSafeInteger(index) || index < 0 || index >= session.partCount) {
          throw new HttpError(400, "Invalid part index");
        }
        const result = await streamRequestToFile(
          request,
          path.join(sessionPath(store, session.sessionId), `${index}.part`),
          MAX_FILE_BYTES,
        );
        const expectedSize = expectedPartSize(session, index);
        if (result.size !== expectedSize) {
          await rm(path.join(sessionPath(store, session.sessionId), `${index}.part`), { force: true });
          throw new HttpError(400, `Part size mismatch: expected ${expectedSize}, actual ${result.size}`);
        }
        await store.update((state) => {
          const currentSession = findSession(state, session.sessionId);
          requireSessionOwner(currentSession, rcSn);
          requireUploadJob(findJob(state, currentSession.asset.jobId), rcSn);
          currentSession.uploadedParts[index] = { sizeBytes: result.size, md5: result.md5 };
        });
        return sendJson(response, 200, { index, success: true, message: result.md5 });
      }

      const completeMatch = url.pathname.match(/^\/api\/v1\/upload-sessions\/([^/]+)\/complete$/);
      if (request.method === "POST" && completeMatch) {
        const body = await readJson(request);
        const session = findSession(store.state, decodeSegment(completeMatch[1]));
        requireSessionOwner(session, rcSn);
        requireUploadJob(findJob(store.state, session.asset.jobId), rcSn);
        const asset = await completeUpload(store, session, rcSn, body);
        return sendJson(response, 200, asset);
      }

      const appAssetsMatch = url.pathname.match(/^\/api\/v1\/jobs\/([^/]+)\/assets$/);
      if (request.method === "GET" && appAssetsMatch) {
        const jobId = decodeSegment(appAssetsMatch[1]);
        requireAppJob(findJob(store.state, jobId), rcSn);
        return sendJson(response, 200, { assets: store.state.assets.filter((asset) => asset.jobId === jobId && asset.typeId !== "route") });
      }

      if (request.method === "GET" && url.pathname === "/api/admin/devices") {
        const now = Date.now();
        const devices = store.state.devices.map((device) => ({
          ...device,
          online: now - Date.parse(device.lastSeenAt) < 30_000,
        }));
        return sendJson(response, 200, { devices });
      }

      if (request.method === "GET" && url.pathname === "/api/admin/jobs") {
        const search = (url.searchParams.get("search") || "").trim().toLowerCase();
        const status = (url.searchParams.get("status") || "").trim().toUpperCase();
        const jobs = store.state.jobs.filter((job) => {
          if (status && job.status !== status) return false;
          if (!search) return true;
          return [job.jobId, job.title, job.targetRcSn].some((value) => String(value || "").toLowerCase().includes(search));
        });
        return sendJson(response, 200, { jobs });
      }

      if (request.method === "POST" && url.pathname === "/api/admin/jobs") {
        const body = await readJson(request);
        if (!body.title || typeof body.title !== "string") throw new HttpError(400, "title is required");
        const job = {
          jobId: generateJobId(store.state.jobs),
          title: body.title.trim(),
          status: "DRAFT",
          targetRcSn: parseTargetRcSn(body.targetRcSn),
          routeStatus: "NOT_REQUIRED",
          routeFileUrl: null,
          timeline: [{ timestamp: Date.now(), description: "任务已创建" }],
          extras: isPlainObject(body.extras) ? body.extras : {},
        };
        await store.update((state) => state.jobs.push(job));
        return sendJson(response, 201, { job });
      }

      const adminJobMatch = url.pathname.match(/^\/api\/admin\/jobs\/([^/]+)$/);
      if (request.method === "PUT" && adminJobMatch) {
        const jobId = decodeSegment(adminJobMatch[1]);
        const body = await readJson(request);
        const job = await store.update((state) => {
          const job = findJob(state, jobId);
          if (job.status !== "DRAFT") throw new HttpError(409, "Only draft tasks can be edited");
          if (typeof body.title === "string" && body.title.trim()) job.title = body.title.trim();
          if (isPlainObject(body.extras)) job.extras = body.extras;
          if (Object.hasOwn(body, "targetRcSn")) job.targetRcSn = parseTargetRcSn(body.targetRcSn);
          job.timeline.push({ timestamp: Date.now(), description: "任务信息已更新" });
          return job;
        });
        return sendJson(response, 200, { job });
      }

      if (request.method === "DELETE" && adminJobMatch) {
        const jobId = decodeSegment(adminJobMatch[1]);
        const sessionIds = await store.update((state) => {
          const job = findJob(state, jobId);
          if (["IN_PROGRESS", "FINALIZING"].includes(job.status)) {
            throw new HttpError(409, "Active tasks cannot be deleted; cancel or complete the task first");
          }
          const ids = state.uploadSessions
            .filter((item) => item.asset.jobId === jobId)
            .map((item) => item.sessionId);
          state.jobs = state.jobs.filter((item) => item.jobId !== jobId);
          state.assets = state.assets.filter((item) => item.jobId !== jobId);
          state.uploadSessions = state.uploadSessions.filter((item) => item.asset.jobId !== jobId);
          return ids;
        });
        await Promise.all(sessionIds.map((sessionId) =>
          rm(sessionPath(store, sessionId), { recursive: true, force: true }),
        ));
        await Promise.all([
          rm(routePath(store, jobId), { force: true }),
          rm(path.join(store.paths.uploads, safePathPart(jobId)), { recursive: true, force: true }),
        ]);
        return response.writeHead(204).end();
      }

      const adminRouteMatch = url.pathname.match(/^\/api\/admin\/jobs\/([^/]+)\/route$/);
      if (request.method === "PUT" && adminRouteMatch) {
        const jobId = decodeSegment(adminRouteMatch[1]);
        const originalName = headerValue(request, "x-file-name");
        const fileName = originalName ? safeFileName(originalName) : `${jobId}.kmz`;
        if (!fileName.toLowerCase().endsWith(".kmz")) throw new HttpError(400, "Route file must be a KMZ");
        const job = await store.update(async (state) => {
          const job = findJob(state, jobId);
          if (job.status !== "DRAFT") throw new HttpError(409, "Routes can only be uploaded for draft tasks");
          await streamRequestToFile(request, routePath(store, job.jobId), MAX_FILE_BYTES);
          job.routeStatus = "CLOUD_PROVIDED";
          job.routeFileUrl = `/api/v1/routes/${job.jobId}.kmz`;
          job.routeFileName = fileName;
          job.timeline.push({ timestamp: Date.now(), description: `航线文件已上传：${fileName}` });
          return job;
        });
        return sendJson(response, 200, { job });
      }

      const dispatchMatch = url.pathname.match(/^\/api\/admin\/jobs\/([^/]+)\/dispatch$/);
      if (request.method === "POST" && dispatchMatch) {
        const jobId = decodeSegment(dispatchMatch[1]);
        const job = await store.update(async (state) => {
          const job = findJob(state, jobId);
          if (job.status !== "DRAFT") throw new HttpError(409, "Only draft tasks can be dispatched");
          if (!job.targetRcSn) throw new HttpError(400, "targetRcSn is required");
          if (!state.devices.some((device) => device.rcSn === job.targetRcSn)) {
            throw new HttpError(400, "Target remote controller is not registered");
          }
          if (job.routeStatus !== "CLOUD_PROVIDED") throw new HttpError(400, "Upload a KMZ route before dispatch");
          await stat(routePath(store, job.jobId)).catch(() => {
            throw new HttpError(400, "Route file does not exist");
          });
          job.status = "DISPATCHED";
          job.timeline.push({ timestamp: Date.now(), description: `任务已下发至 ${job.targetRcSn}` });
          return job;
        });
        return sendJson(response, 200, { job });
      }

      const withdrawMatch = url.pathname.match(/^\/api\/admin\/jobs\/([^/]+)\/withdraw$/);
      if (request.method === "POST" && withdrawMatch) {
        const jobId = decodeSegment(withdrawMatch[1]);
        const job = await store.update((state) => {
          const job = findJob(state, jobId);
          if (job.status !== "DISPATCHED") throw new HttpError(409, "Only dispatched tasks can be withdrawn");
          job.status = "DRAFT";
          job.timeline.push({ timestamp: Date.now(), description: `任务已从 ${job.targetRcSn} 撤回` });
          return job;
        });
        return sendJson(response, 200, { job });
      }

      const adminJobAssetsMatch = url.pathname.match(/^\/api\/admin\/jobs\/([^/]+)\/assets$/);
      if (request.method === "GET" && adminJobAssetsMatch) {
        const jobId = decodeSegment(adminJobAssetsMatch[1]);
        findJob(store.state, jobId);
        return sendJson(response, 200, { assets: store.state.assets.filter((asset) => asset.jobId === jobId && asset.typeId !== "route") });
      }

      if (request.method === "GET" && url.pathname === "/api/admin/assets") {
        return sendJson(response, 200, { assets: store.state.assets.filter((asset) => asset.typeId !== "route") });
      }

      const assetContentMatch = url.pathname.match(/^\/api\/admin\/assets\/([^/]+)\/content$/);
      if (request.method === "GET" && assetContentMatch) {
        const assetId = decodeSegment(assetContentMatch[1]);
        const jobId = url.searchParams.get("jobId");
        const matches = store.state.assets.filter((item) => item.assetId === assetId && (!jobId || item.jobId === jobId));
        if (matches.length > 1 && !jobId) throw new HttpError(409, "jobId is required for duplicate asset IDs");
        const asset = matches[0];
        if (!asset) throw new HttpError(404, "Asset not found");
        return sendFile(
          response,
          path.join(store.paths.uploads, safePathPart(asset.jobId), safeFileName(asset.meta.storageFileName || asset.meta.fileName)),
          "application/octet-stream",
          asset.meta.fileName,
        );
      }

      const routeContentMatch = url.pathname.match(/^\/api\/admin\/routes\/([^/]+)\/content$/);
      if (request.method === "GET" && routeContentMatch) {
        const job = findJob(store.state, decodeSegment(routeContentMatch[1]));
        return sendFile(response, routePath(store, job.jobId), "application/vnd.google-earth.kmz", job.routeFileName || `${job.jobId}.kmz`);
      }

      const staticFiles = {
        "/": ["index.html", "text/html; charset=utf-8"],
        "/app.js": ["app.js", "text/javascript; charset=utf-8"],
        "/styles.css": ["styles.css", "text/css; charset=utf-8"],
      };
      if (request.method === "GET" && staticFiles[url.pathname]) {
        const [name, type] = staticFiles[url.pathname];
        return sendFile(response, path.join(webRoot, name), type);
      }

      throw new HttpError(404, "Not found");
    } catch (error) {
      if (response.headersSent) {
        response.destroy(error);
        return;
      }
      const statusCode = error instanceof HttpError ? error.statusCode : 500;
      if (statusCode === 500) console.error(error);
      sendJson(response, statusCode, { error: error.message || "Internal server error" });
    }
  });
}

async function completeUpload(store, session, rcSn, body) {
  const suppliedParts = Array.isArray(body.parts) ? body.parts : [];
  if (suppliedParts.length && suppliedParts.length !== session.partCount) {
    throw new HttpError(400, `Part count mismatch: expected ${session.partCount}, actual ${suppliedParts.length}`);
  }
  const names = (await readdir(sessionPath(store, session.sessionId))).filter((name) => /^\d+\.part$/.test(name));
  if (names.length !== session.partCount) {
    throw new HttpError(400, `Part count mismatch: expected ${session.partCount}, actual ${names.length}`);
  }
  for (let index = 0; index < session.partCount; index++) {
    const info = await stat(path.join(sessionPath(store, session.sessionId), `${index}.part`)).catch(() => null);
    if (!info || info.size !== expectedPartSize(session, index)) {
      throw new HttpError(400, `Part ${index} size mismatch`);
    }
  }

  const uploadDirectory = path.join(store.paths.uploads, safePathPart(session.asset.jobId));
  await mkdir(uploadDirectory, { recursive: true });
  const storageFileName = safeFileName(`${session.asset.assetId}-${session.asset.fileName}`);
  const destination = path.join(uploadDirectory, storageFileName);
  const temporary = `${destination}.${randomUUID()}.tmp`;
  const output = await open(temporary, "wx");
  const hash = createHash("md5");
  let size = 0;
  try {
    for (let index = 0; index < session.partCount; index++) {
      for await (const chunk of createReadStream(path.join(sessionPath(store, session.sessionId), `${index}.part`))) {
        size += chunk.length;
        hash.update(chunk);
        await output.write(chunk);
      }
    }
    await output.close();
  } catch (error) {
    await output.close().catch(() => {});
    await rm(temporary, { force: true });
    throw error;
  }
  const md5 = hash.digest("hex");
  if (size !== session.asset.sizeBytes || (session.asset.md5 && session.asset.md5 !== md5)) {
    await rm(temporary, { force: true });
    if (size !== session.asset.sizeBytes) throw new HttpError(400, `Size mismatch: expected ${session.asset.sizeBytes}, actual ${size}`);
    throw new HttpError(400, `MD5 mismatch: expected ${session.asset.md5}, actual ${md5}`);
  }
  await rename(temporary, destination);
  const asset = {
    assetId: session.asset.assetId,
    jobId: session.asset.jobId,
    typeId: session.asset.typeId,
    localUri: null,
    sizeBytes: size,
    md5,
    state: "UPLOADED",
    meta: { fileName: session.asset.fileName, storageFileName },
  };
  await store.update((state) => {
    const currentSession = findSession(state, session.sessionId);
    requireSessionOwner(currentSession, rcSn);
    requireUploadJob(findJob(state, currentSession.asset.jobId), rcSn);
    const existing = state.assets.findIndex((item) => item.jobId === asset.jobId && item.assetId === asset.assetId);
    if (existing >= 0) state.assets[existing] = asset;
    else state.assets.push(asset);
    const job = state.jobs.find((item) => item.jobId === asset.jobId);
    job?.timeline.push({ timestamp: Date.now(), description: `资源已归档：${asset.meta.fileName}` });
    state.uploadSessions = state.uploadSessions.filter((item) => item.sessionId !== session.sessionId);
  });
  await rm(sessionPath(store, session.sessionId), { recursive: true, force: true });
  return asset;
}

async function streamRequestToFile(request, destination, limit) {
  const declaredLength = Number(headerValue(request, "content-length"));
  if (Number.isFinite(declaredLength) && declaredLength > limit) throw new HttpError(413, "Request body is too large");
  await mkdir(path.dirname(destination), { recursive: true });
  const temporary = `${destination}.${randomUUID()}.tmp`;
  const hash = createHash("md5");
  let size = 0;
  const meter = new Transform({
    transform(chunk, _encoding, callback) {
      size += chunk.length;
      if (size > limit) callback(new HttpError(413, "Request body is too large"));
      else {
        hash.update(chunk);
        callback(null, chunk);
      }
    },
  });
  try {
    await pipeline(request, meter, createWriteStream(temporary, { flags: "wx" }));
    await rename(temporary, destination);
    return { size, md5: hash.digest("hex") };
  } catch (error) {
    await rm(temporary, { force: true });
    throw error;
  }
}

async function readJson(request) {
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > MAX_JSON_BYTES) throw new HttpError(413, "JSON body is too large");
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

function validateUploadAsset(asset) {
  for (const field of ["assetId", "jobId", "typeId"]) {
    if (typeof asset[field] !== "string" || !asset[field].trim()) throw new HttpError(400, `${field} is required`);
  }
  if (!Number.isSafeInteger(asset.sizeBytes) || asset.sizeBytes <= 0 || asset.sizeBytes > MAX_FILE_BYTES) {
    throw new HttpError(400, "Invalid sizeBytes");
  }
  if (asset.md5 != null && !/^[a-fA-F0-9]{32}$/.test(asset.md5)) throw new HttpError(400, "Invalid md5");
}

function sessionResponse(session) {
  return {
    sessionId: session.sessionId,
    chunkSizeBytes: session.chunkSizeBytes,
    partUrls: Array.from({ length: session.partCount }, (_, index) => `/api/v1/upload-sessions/${session.sessionId}/parts/${index}`),
    uploadedChunks: Object.keys(session.uploadedParts).map(Number).sort((a, b) => a - b),
  };
}

function expectedPartSize(session, index) {
  if (index < session.partCount - 1) return session.chunkSizeBytes;
  return session.asset.sizeBytes - session.chunkSizeBytes * (session.partCount - 1);
}

function findJob(state, jobId) {
  const job = state.jobs.find((item) => item.jobId === jobId);
  if (!job) throw new HttpError(404, "Task not found");
  return job;
}

function findSession(state, sessionId) {
  const session = state.uploadSessions.find((item) => item.sessionId === sessionId);
  if (!session) throw new HttpError(404, "Upload session not found");
  return session;
}

function ownsJob(job, rcSn) {
  return job.targetRcSn === rcSn;
}

function requireAppJob(job, rcSn) {
  if (job.status === "DRAFT" || !ownsJob(job, rcSn)) throw new HttpError(404, "Task not found");
}

function requireUploadJob(job, rcSn) {
  requireAppJob(job, rcSn);
  if (["COMPLETED", "CANCELLED"].includes(job.status)) {
    throw new HttpError(409, "Task no longer accepts uploads");
  }
}

function requireSessionOwner(session, rcSn) {
  if (session.ownerRcSn !== rcSn) throw new HttpError(404, "Upload session not found");
}

function parseTargetRcSn(value) {
  if (value == null || value === "") return null;
  if (typeof value !== "string") throw new HttpError(400, "targetRcSn must be a string or null");
  const rcSn = value.trim();
  if (!rcSn) return null;
  if (rcSn === "*") throw new HttpError(400, "targetRcSn must identify one remote controller");
  if (rcSn.length > 200) throw new HttpError(400, "targetRcSn is too long");
  return rcSn;
}

function generateJobId(jobs) {
  const date = new Date().toISOString().slice(0, 10).replaceAll("-", "");
  for (;;) {
    const suffix = randomBytes(3).toString("hex").toUpperCase();
    const jobId = `FT-${date}-${suffix}`;
    if (!jobs.some((job) => job.jobId === jobId)) return jobId;
  }
}

function routePath(store, jobId) {
  return path.join(store.paths.routes, `${safePathPart(jobId)}.kmz`);
}

function sessionPath(store, sessionId) {
  return path.join(store.paths.sessions, safePathPart(sessionId));
}

function safePathPart(value) {
  const text = String(value);
  if (!/^[A-Za-z0-9_-]+$/.test(text)) throw new HttpError(400, "Invalid identifier");
  return text;
}

function safeFileName(value) {
  const name = path.basename(String(value)).replace(/[^A-Za-z0-9._-]/g, "_").replace(/^\.+/, "");
  if (!name || name === "." || name === "..") throw new HttpError(400, "Invalid file name");
  return name.slice(0, 255);
}

function decodeSegment(value) {
  try {
    return decodeURIComponent(value);
  } catch {
    throw new HttpError(400, "Invalid URL encoding");
  }
}

function isPlainObject(value) {
  return value != null && typeof value === "object" && !Array.isArray(value);
}

function headerValue(request, name) {
  const value = request.headers[name];
  return Array.isArray(value) ? value[0] || "" : String(value || "");
}

function assetTypes() {
  return [
    { typeId: "photo", extensions: ["jpg", "jpeg", "dng"], maxSizeBytes: MAX_FILE_BYTES, chunkSizeBytes: CHUNK_SIZE_BYTES, realtimeEligible: true },
  ];
}

async function cleanupExpiredSessions(store) {
  const cutoff = Date.now() - 24 * 60 * 60 * 1000;
  const expired = store.state.uploadSessions.filter((session) => Date.parse(session.createdAt) < cutoff);
  if (!expired.length) return;
  for (const session of expired) {
    await rm(sessionPath(store, session.sessionId), { recursive: true, force: true });
  }
  const expiredIds = new Set(expired.map((session) => session.sessionId));
  await store.update((state) => {
    state.uploadSessions = state.uploadSessions.filter((session) => !expiredIds.has(session.sessionId));
  });
}

function setCors(response) {
  response.setHeader("Access-Control-Allow-Origin", "*");
  response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-RC-SN, X-File-Name");
  response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
}

function sendJson(response, statusCode, value) {
  const body = JSON.stringify(value);
  response.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
  });
  response.end(body);
}

async function sendFile(response, file, contentType, downloadName) {
  let info;
  try {
    info = await stat(file);
    if (!info.isFile()) throw new Error("Not a file");
  } catch (error) {
    if (error.code === "ENOENT" || error.message === "Not a file") throw new HttpError(404, "File not found");
    throw error;
  }
  const headers = { "Content-Type": contentType, "Content-Length": info.size };
  if (downloadName) headers["Content-Disposition"] = `attachment; filename="${safeFileName(downloadName)}"`;
  response.writeHead(200, headers);
  await pipeline(createReadStream(file), response);
}

class HttpError extends Error {
  constructor(statusCode, message) {
    super(message);
    this.statusCode = statusCode;
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const port = Number(process.env.PORT || 8080);
  const host = process.env.HOST || "127.0.0.1";
  const server = await createApp();
  server.listen(port, host, () => {
    const address = server.address();
    console.log(`Flight Task Demo server: http://${host}:${address.port}`);
  });
}
