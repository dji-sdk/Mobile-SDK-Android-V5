import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { mkdtemp, readFile, rm, stat } from "node:fs/promises";
import { after, before, test } from "node:test";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createApp } from "../server.mjs";

const directory = path.dirname(fileURLToPath(import.meta.url));
let baseUrl;
let dataDir;
let server;

before(async () => {
  dataDir = await mkdtemp(path.join(os.tmpdir(), "patrol-server-"));
  server = await createApp({ dataDir });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(async () => {
  await new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
  await rm(dataDir, { recursive: true, force: true });
});

test("seeds exactly one configured draft and registers RC serial numbers", async () => {
  const health = await fetch(`${baseUrl}/health`, { method: "HEAD" });
  assert.equal(health.status, 200);

  let response = await fetch(`${baseUrl}/api/v1/jobs`, { headers: { "X-Device-Id": "legacy-device" } });
  assert.equal(response.status, 400);
  response = await appRequest("/api/v1/jobs", "RC-A");
  assert.equal(response.status, 200);
  assert.deepEqual((await response.json()).jobs, []);
  await appRequest("/api/v1/jobs", "RC-B");

  const jobs = (await (await fetch(`${baseUrl}/api/admin/jobs`)).json()).jobs;
  assert.equal(jobs.length, 1);
  assert.equal(jobs[0].jobId, "PATROL-DEMO-001");
  assert.equal(jobs[0].title, "天空之城");
  assert.equal(jobs[0].routeFileName, "generate_test.kmz");
  assert.equal(jobs[0].routeStatus, "CLOUD_PROVIDED");
  assert.equal(jobs[0].targetRcSn, null);
  assert.equal(jobs[0].extras.requirements, "航线模拟器验证");
  assert.ok(Date.parse(jobs[0].extras.deadline) > Date.now());

  const state = JSON.parse(await readFile(path.join(dataDir, "state.json"), "utf8"));
  assert.deepEqual(state.devices.map((device) => device.rcSn), ["RC-A", "RC-B"]);
  assert.ok(!("deviceId" in state.devices[0]));
  assert.deepEqual(
    await readFile(path.join(dataDir, "routes", "PATROL-DEMO-001.kmz")),
    await readFile(path.resolve(directory, "../data/PATROL-DEMO-001.kmz")),
  );
});

test("dispatch uses canonical targetRcSn and enforces multi-RC isolation", async () => {
  const job = await createDraft("RC-A", "多遥控器隔离任务");
  await uploadRoute(job.jobId, Buffer.from("isolation-route"));

  const dispatchAttempts = await Promise.all([
    jsonRequest(`/api/admin/jobs/${job.jobId}/dispatch`, "POST", { targetRcSn: "RC-B" }),
    jsonRequest(`/api/admin/jobs/${job.jobId}/dispatch`, "POST", {}),
  ]);
  assert.deepEqual(dispatchAttempts.map((item) => item.status).sort(), [200, 409]);
  const dispatched = (await dispatchAttempts.find((item) => item.status === 200).json()).job;
  assert.equal(dispatched.targetRcSn, "RC-A");
  assert.equal(dispatched.status, "DISPATCHED");

  const ownJobs = (await (await appRequest("/api/v1/jobs", "RC-A")).json()).jobs;
  const otherJobs = (await (await appRequest("/api/v1/jobs", "RC-B")).json()).jobs;
  assert.ok(ownJobs.some((item) => item.jobId === job.jobId));
  assert.ok(!otherJobs.some((item) => item.jobId === job.jobId));
  assert.equal((await appRequest(`/api/v1/jobs/${job.jobId}`, "RC-B")).status, 404);
  assert.equal((await appRequest(`/api/v1/routes/${job.jobId}.kmz`, "RC-B")).status, 404);
  assert.equal((await appRequest(`/api/v1/jobs/${job.jobId}/assets`, "RC-B")).status, 404);
  assert.equal((await appStatus(job.jobId, "RC-B", "ACCEPTED")).status, 404);
  assert.deepEqual(
    Buffer.from(await (await appRequest(`/api/v1/routes/${job.jobId}.kmz`, "RC-A")).arrayBuffer()),
    Buffer.from("isolation-route"),
  );
  let response = await appJsonRequest("/api/v1/upload-sessions", "RC-B", "POST", uploadAsset(job.jobId, "foreign"));
  assert.equal(response.status, 404);
  response = await appJsonRequest("/api/v1/upload-sessions", "RC-A", "POST", {
    ...uploadAsset(job.jobId, "route-copy"), typeId: "route",
  });
  assert.equal(response.status, 400);

  response = await fetch(`${baseUrl}/api/admin/jobs/${job.jobId}/route`, {
    method: "PUT", headers: { "X-File-Name": "late.kmz" }, body: "late-route",
  });
  assert.equal(response.status, 409);
});

test("withdraws a dispatched task once, preserves assignment, and allows reassignment", async () => {
  const job = await createDraft("RC-A", "撤回任务");
  await uploadRoute(job.jobId, Buffer.from("withdraw-route"));
  assert.equal((await jsonRequest(`/api/admin/jobs/${job.jobId}/dispatch`, "POST", {})).status, 200);

  const attempts = await Promise.all([
    jsonRequest(`/api/admin/jobs/${job.jobId}/withdraw`, "POST", {}),
    jsonRequest(`/api/admin/jobs/${job.jobId}/withdraw`, "POST", {}),
  ]);
  assert.deepEqual(attempts.map((item) => item.status).sort(), [200, 409]);
  const withdrawn = await getAdminJob(job.jobId);
  assert.equal(withdrawn.status, "DRAFT");
  assert.equal(withdrawn.targetRcSn, "RC-A");

  let response = await jsonRequest(`/api/admin/jobs/${job.jobId}`, "PUT", { targetRcSn: "RC-B" });
  assert.equal(response.status, 200);
  response = await jsonRequest(`/api/admin/jobs/${job.jobId}/dispatch`, "POST", {});
  assert.equal((await response.json()).job.targetRcSn, "RC-B");
  assert.ok(!(await (await appRequest("/api/v1/jobs", "RC-A")).json()).jobs.some((item) => item.jobId === job.jobId));
  assert.ok((await (await appRequest("/api/v1/jobs", "RC-B")).json()).jobs.some((item) => item.jobId === job.jobId));

  response = await appStatus(job.jobId, "RC-B", "ACCEPTED");
  assert.equal(response.status, 200);
  assert.equal((await jsonRequest(`/api/admin/jobs/${job.jobId}`, "PUT", { targetRcSn: "RC-A" })).status, 409);
  assert.equal((await jsonRequest(`/api/admin/jobs/${job.jobId}/withdraw`, "POST", {})).status, 409);
});

test("same-state updates are idempotent and one RC has at most one active task", async () => {
  const first = await createDispatchedJob("RC-B", "活动任务一");
  const second = await createDispatchedJob("RC-B", "活动任务二");
  await appStatus(first.jobId, "RC-B", "ACCEPTED");
  await appStatus(second.jobId, "RC-B", "ACCEPTED");

  const before = (await getAdminJob(first.jobId)).timeline.length;
  let response = await appStatus(first.jobId, "RC-B", "ACCEPTED");
  assert.equal(response.status, 200);
  assert.equal((await getAdminJob(first.jobId)).timeline.length, before);

  const attempts = await Promise.all([
    appStatus(first.jobId, "RC-B", "IN_PROGRESS"),
    appStatus(second.jobId, "RC-B", "IN_PROGRESS"),
  ]);
  assert.deepEqual(attempts.map((item) => item.status).sort(), [200, 409]);
  const activeJob = (await getAdminJobs()).find((item) => [first.jobId, second.jobId].includes(item.jobId) && item.status === "IN_PROGRESS");
  const waitingJob = activeJob.jobId === first.jobId ? second : first;

  assert.equal((await appStatus(activeJob.jobId, "RC-B", "COMPLETED")).status, 409);
  assert.equal((await appStatus(activeJob.jobId, "RC-B", "FINALIZING")).status, 200);
  assert.equal((await appStatus(waitingJob.jobId, "RC-B", "IN_PROGRESS")).status, 409);
  assert.equal((await appStatus(activeJob.jobId, "RC-B", "COMPLETED")).status, 200);
  assert.equal((await appStatus(waitingJob.jobId, "RC-B", "IN_PROGRESS")).status, 200);
});

test("upload sessions use ownerRcSn and reject every other RC", async () => {
  await appRequest("/api/v1/jobs", "RC-UPLOAD");
  const job = await createDispatchedJob("RC-UPLOAD", "上传归属任务");
  const content = Buffer.from("archived-photo-data");
  const md5 = createHash("md5").update(content).digest("hex");
  let response = await appJsonRequest("/api/v1/upload-sessions", "RC-UPLOAD", "POST", {
    ...uploadAsset(job.jobId, "asset-1"), sizeBytes: content.length, md5,
  });
  assert.equal(response.status, 200);
  const session = await response.json();
  const state = JSON.parse(await readFile(path.join(dataDir, "state.json"), "utf8"));
  const stored = state.uploadSessions.find((item) => item.sessionId === session.sessionId);
  assert.equal(stored.ownerRcSn, "RC-UPLOAD");
  assert.ok(!("ownerDeviceId" in stored));

  assert.equal((await appRequest(session.partUrls[0], "RC-A", { method: "PUT", body: content })).status, 404);
  response = await appRequest(session.partUrls[0], "RC-UPLOAD", { method: "PUT", body: content });
  const part = await response.json();
  assert.equal(part.message, md5);
  assert.equal((await appJsonRequest(`/api/v1/upload-sessions/${session.sessionId}/complete`, "RC-A", "POST", { parts: [part] })).status, 404);
  response = await appJsonRequest(`/api/v1/upload-sessions/${session.sessionId}/complete`, "RC-UPLOAD", "POST", { parts: [part] });
  assert.equal(response.status, 200);
  assert.deepEqual(await readFile(path.join(dataDir, "uploads", job.jobId, "asset-1-photo.jpg")), content);
});

test("deletes inactive tasks with their routes, assets, and upload sessions", async () => {
  const rcSn = "RC-DELETE";
  await appRequest("/api/v1/jobs", rcSn);
  const job = await createDispatchedJob(rcSn, "完整删除任务");
  await appStatus(job.jobId, rcSn, "ACCEPTED");
  await appStatus(job.jobId, rcSn, "IN_PROGRESS");

  let response = await fetch(`${baseUrl}/api/admin/jobs/${job.jobId}`, { method: "DELETE" });
  assert.equal(response.status, 409);
  assert.match((await response.json()).error, /Active tasks/);

  await appStatus(job.jobId, rcSn, "FINALIZING");
  const content = Buffer.from("0123456789");
  const md5 = createHash("md5").update(content).digest("hex");
  response = await appJsonRequest("/api/v1/upload-sessions", rcSn, "POST", {
    ...uploadAsset(job.jobId, "delete-asset"), md5,
  });
  const completedSession = await response.json();
  response = await appRequest(completedSession.partUrls[0], rcSn, { method: "PUT", body: content });
  const part = await response.json();
  response = await appJsonRequest(`/api/v1/upload-sessions/${completedSession.sessionId}/complete`, rcSn, "POST", { parts: [part] });
  assert.equal(response.status, 200);

  response = await appJsonRequest("/api/v1/upload-sessions", rcSn, "POST", uploadAsset(job.jobId, "pending-asset"));
  const pendingSession = await response.json();
  await appStatus(job.jobId, rcSn, "COMPLETED");

  response = await fetch(`${baseUrl}/api/admin/jobs/${job.jobId}`, { method: "DELETE" });
  assert.equal(response.status, 204);
  assert.equal(await getAdminJob(job.jobId), undefined);
  assert.equal((await appRequest(`/api/v1/jobs/${job.jobId}`, rcSn)).status, 404);

  const state = JSON.parse(await readFile(path.join(dataDir, "state.json"), "utf8"));
  assert.ok(!state.assets.some((item) => item.jobId === job.jobId));
  assert.ok(!state.uploadSessions.some((item) => item.asset.jobId === job.jobId));
  await assert.rejects(stat(path.join(dataDir, "routes", `${job.jobId}.kmz`)), { code: "ENOENT" });
  await assert.rejects(stat(path.join(dataDir, "uploads", job.jobId)), { code: "ENOENT" });
  await assert.rejects(stat(path.join(dataDir, "sessions", pendingSession.sessionId)), { code: "ENOENT" });
});

test("rejects missing assignments, wildcard ownership, and unregistered targets", async () => {
  let response = await jsonRequest("/api/admin/jobs", "POST", { title: "无归属任务" });
  const unassigned = (await response.json()).job;
  await uploadRoute(unassigned.jobId, Buffer.from("unassigned-route"));
  assert.equal((await jsonRequest(`/api/admin/jobs/${unassigned.jobId}/dispatch`, "POST", {})).status, 400);

  response = await jsonRequest("/api/admin/jobs", "POST", { title: "通配任务", targetRcSn: "*" });
  assert.equal(response.status, 400);
  response = await jsonRequest("/api/admin/jobs", "POST", { title: "未登记任务", targetRcSn: "RC-MISSING" });
  const missing = (await response.json()).job;
  await uploadRoute(missing.jobId, Buffer.from("missing-route"));
  assert.equal((await jsonRequest(`/api/admin/jobs/${missing.jobId}/dispatch`, "POST", {})).status, 400);
});

async function createDraft(targetRcSn, title) {
  const response = await jsonRequest("/api/admin/jobs", "POST", {
    title,
    targetRcSn,
    extras: { requirements: "测试" },
  });
  assert.equal(response.status, 201);
  return (await response.json()).job;
}

async function createDispatchedJob(targetRcSn, title) {
  const job = await createDraft(targetRcSn, title);
  await uploadRoute(job.jobId, Buffer.from(`route-${job.jobId}`));
  const response = await jsonRequest(`/api/admin/jobs/${job.jobId}/dispatch`, "POST", {});
  assert.equal(response.status, 200);
  return (await response.json()).job;
}

async function uploadRoute(jobId, content) {
  const response = await fetch(`${baseUrl}/api/admin/jobs/${jobId}/route`, {
    method: "PUT",
    headers: { "X-File-Name": "route.kmz" },
    body: content,
  });
  assert.equal(response.status, 200);
}

function uploadAsset(jobId, assetId) {
  return { assetId, jobId, typeId: "photo", fileName: "photo.jpg", sizeBytes: 10 };
}

function appStatus(jobId, rcSn, status) {
  return appJsonRequest(`/api/v1/jobs/${jobId}/status`, rcSn, "PUT", { status });
}

function appRequest(url, rcSn, options = {}) {
  return fetch(`${baseUrl}${url}`, {
    ...options,
    headers: { ...options.headers, "X-RC-SN": rcSn },
  });
}

function appJsonRequest(url, rcSn, method, body) {
  return appRequest(url, rcSn, {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

function jsonRequest(url, method, body) {
  return fetch(`${baseUrl}${url}`, {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

async function getAdminJobs() {
  return (await (await fetch(`${baseUrl}/api/admin/jobs`)).json()).jobs;
}

async function getAdminJob(jobId) {
  return (await getAdminJobs()).find((job) => job.jobId === jobId);
}
