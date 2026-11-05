import assert from "node:assert/strict";
import { mkdtemp, rm } from "node:fs/promises";
import { after, before, test } from "node:test";
import os from "node:os";
import path from "node:path";
import { createApp } from "../server.mjs";

let server;
let baseUrl;
let dataDir;

before(async () => {
  dataDir = await mkdtemp(path.join(os.tmpdir(), "gas-demo-server-"));
  server = await createApp({ dataDir });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(async () => {
  await new Promise((resolve, reject) => server.close((error) => (error ? reject(error) : resolve())));
  await rm(dataDir, { recursive: true, force: true });
});

async function request(requestPath, options = {}) {
  const response = await fetch(`${baseUrl}${requestPath}`, options);
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : null };
}

function appRequest(requestPath, rcSn, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (rcSn) headers["X-RC-SN"] = rcSn;
  return request(requestPath, { ...options, headers });
}

function adminRequest(requestPath, options = {}) {
  return request(requestPath, options);
}

function validGas(overrides = {}) {
  return {
    target: { latitude: 22.5, longitude: 113.9, altitudeM: 60 },
    channels: [
      {
        channelId: "so2",
        displayName: "SO2",
        unit: "µg/m³",
        threshold: 500,
      },
    ],
    safetyBoundaryM: 200,
    requirements: "靠近船尾采样",
    deadline: new Date(Date.now() + 86_400_000).toISOString(),
    ...overrides,
  };
}

function createJob({ title = "测试任务", targetRcSn = null, gas = validGas() } = {}) {
  return adminRequest("/api/admin/jobs", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, targetRcSn, gas }),
  });
}

function setStatus(jobId, rcSn, status) {
  return appRequest(`/api/v1/jobs/${jobId}/status`, rcSn, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  });
}

async function createDispatchedJob(rcSn, title = "已下发任务") {
  const created = await createJob({ title, targetRcSn: rcSn });
  assert.equal(created.status, 201);
  const jobId = created.body.job.jobId;
  const dispatched = await adminRequest(`/api/admin/jobs/${jobId}/dispatch`, { method: "POST" });
  assert.equal(dispatched.status, 200);
  return jobId;
}

test("seeds exactly one draft gas task and serves health", async () => {
  const health = await request("/health");
  assert.equal(health.status, 200);
  assert.equal(health.body.status, "ok");

  const jobs = await adminRequest("/api/admin/jobs");
  assert.equal(jobs.status, 200);
  assert.equal(jobs.body.jobs.length, 1);

  const seed = jobs.body.jobs[0];
  assert.equal(seed.jobId, "GAS-DEMO-001");
  assert.equal(seed.status, "DRAFT");
  assert.ok(seed.gas.target.latitude >= -90 && seed.gas.target.latitude <= 90);
  assert.ok(seed.gas.channels.length >= 1);
  assert.ok(seed.gas.channels.every((channel) => channel.unit && channel.threshold));
  assert.ok(seed.gas.safetyBoundaryM > 0);
});

test("rejects a channel without unit and reports the field path", async () => {
  const gas = validGas();
  gas.channels[0].unit = "";
  const response = await createJob({ gas });

  assert.equal(response.status, 400);
  assert.match(response.body.error, /gas\.channels\[0\]\.unit/);
});

test("rejects an out-of-range target latitude", async () => {
  const gas = validGas({ target: { latitude: 120, longitude: 113.9, altitudeM: 60 } });
  const response = await createJob({ gas });

  assert.equal(response.status, 400);
  assert.match(response.body.error, /gas\.target\.latitude/);
});

test("rejects duplicated channel ids", async () => {
  const gas = validGas();
  gas.channels.push({ ...gas.channels[0], displayName: "SO2-2" });
  const response = await createJob({ gas });

  assert.equal(response.status, 400);
  assert.match(response.body.error, /duplicated/);
});

test("dispatch requires a target remote controller", async () => {
  const created = await createJob({ title: "无遥控器任务" });
  const jobId = created.body.job.jobId;

  const response = await adminRequest(`/api/admin/jobs/${jobId}/dispatch`, { method: "POST" });

  assert.equal(response.status, 409);
  assert.match(response.body.error, /targetRcSn/);
});

test("withdraw returns a dispatched task to draft and keeps the target", async () => {
  const jobId = await createDispatchedJob("RC-W");

  const withdrawn = await adminRequest(`/api/admin/jobs/${jobId}/withdraw`, { method: "POST" });

  assert.equal(withdrawn.status, 200);
  assert.equal(withdrawn.body.job.status, "DRAFT");
  assert.equal(withdrawn.body.job.targetRcSn, "RC-W");
});

test("enforces remote controller isolation", async () => {
  const jobId = await createDispatchedJob("RC-A", "隔离任务");

  const missingHeader = await appRequest("/api/v1/jobs", null);
  assert.equal(missingHeader.status, 400);

  const otherDetail = await appRequest(`/api/v1/jobs/${jobId}`, "RC-B");
  assert.equal(otherDetail.status, 404);

  const otherList = await appRequest("/api/v1/jobs", "RC-B");
  assert.ok(!otherList.body.jobs.some((job) => job.jobId === jobId));

  const ownerList = await appRequest("/api/v1/jobs", "RC-A");
  assert.ok(ownerList.body.jobs.some((job) => job.jobId === jobId));
  assert.ok(ownerList.body.jobs.every((job) => job.status !== "DRAFT"));
});

test("runs the full status flow and rejects illegal transitions", async () => {
  const jobId = await createDispatchedJob("RC-F", "状态流转任务");

  for (const status of ["ACCEPTED", "IN_PROGRESS", "FINALIZING", "COMPLETED"]) {
    const response = await setStatus(jobId, "RC-F", status);
    assert.equal(response.status, 200, `${status}: ${JSON.stringify(response.body)}`);
    assert.equal(response.body.job.status, status);
  }
  const detail = await appRequest(`/api/v1/jobs/${jobId}`, "RC-F");
  assert.ok(detail.body.job.timeline.length >= 5);

  const illegalJob = await createDispatchedJob("RC-F2", "非法迁移任务");
  const illegal = await setStatus(illegalJob, "RC-F2", "COMPLETED");
  assert.equal(illegal.status, 409);
});

test("allows rejecting a dispatched task", async () => {
  const jobId = await createDispatchedJob("RC-R", "拒绝任务");

  const response = await setStatus(jobId, "RC-R", "REJECTED");

  assert.equal(response.status, 200);
  assert.equal(response.body.job.status, "REJECTED");
});

test("blocks a second active task for the same remote controller", async () => {
  const first = await createDispatchedJob("RC-G", "任务一");
  const second = await createDispatchedJob("RC-G", "任务二");

  assert.equal((await setStatus(first, "RC-G", "ACCEPTED")).status, 200);
  assert.equal((await setStatus(first, "RC-G", "IN_PROGRESS")).status, 200);

  const blocked = await setStatus(second, "RC-G", "ACCEPTED");
  assert.equal(blocked.status, 200);
  const conflict = await setStatus(second, "RC-G", "IN_PROGRESS");
  assert.equal(conflict.status, 409);
  assert.match(conflict.body.error, /active task/);
});

test("only drafts can be edited", async () => {
  const jobId = await createDispatchedJob("RC-E", "只读任务");

  const response = await adminRequest(`/api/admin/jobs/${jobId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title: "改名" }),
  });

  assert.equal(response.status, 409);
});

test("active tasks cannot be deleted but drafts can", async () => {
  const activeJob = await createDispatchedJob("RC-D", "执行中任务");
  await setStatus(activeJob, "RC-D", "ACCEPTED");
  await setStatus(activeJob, "RC-D", "IN_PROGRESS");

  const blocked = await adminRequest(`/api/admin/jobs/${activeJob}`, { method: "DELETE" });
  assert.equal(blocked.status, 409);

  const draft = await createJob({ title: "可删除草稿" });
  const deleted = await adminRequest(`/api/admin/jobs/${draft.body.job.jobId}`, { method: "DELETE" });
  assert.equal(deleted.status, 200);
});

test("restores tasks after a restart", async () => {
  const created = await createJob({ title: "重启恢复任务" });
  const jobId = created.body.job.jobId;

  const second = await createApp({ dataDir });
  await new Promise((resolve) => second.listen(0, "127.0.0.1", resolve));
  try {
    const response = await fetch(`http://127.0.0.1:${second.address().port}/api/admin/jobs`);
    const body = await response.json();
    assert.ok(body.jobs.some((job) => job.jobId === jobId));
  } finally {
    await new Promise((resolve, reject) => second.close((error) => (error ? reject(error) : resolve())));
  }
});

function inspectionPayload(jobId, recordId, overrides = {}) {
  return {
    recordId,
    jobId,
    jobTitle: "检测任务",
    startedAtMs: 1_000,
    endedAtMs: 2_000,
    endReason: "PILOT_FINISH",
    totalSamples: 3,
    validSamples: 3,
    invalidSamples: 0,
    channels: [],
    alerts: [],
    alertIds: [],
    ...overrides,
  };
}

function createInspection(rcSn, jobId, recordId, overrides = {}) {
  return appRequest("/api/v1/inspections", rcSn, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(inspectionPayload(jobId, recordId, overrides)),
  });
}

test("inspection records are idempotent by recordId", async () => {
  const jobId = await createDispatchedJob("RC-REC-1", "记录任务");

  const first = await createInspection("RC-REC-1", jobId, "rec-1");
  assert.equal(first.status, 201);
  assert.equal(first.body.record.totalSamples, 3);

  const second = await createInspection("RC-REC-1", jobId, "rec-1", { totalSamples: 99 });
  assert.equal(second.status, 200);
  assert.equal(second.body.record.totalSamples, 3);
  assert.equal(second.body.record.receivedAt, first.body.record.receivedAt);
});

test("inspection records require recordId and jobId", async () => {
  const jobId = await createDispatchedJob("RC-REC-2", "记录校验任务");
  const missingRecord = await appRequest("/api/v1/inspections", "RC-REC-2", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ jobId }),
  });
  assert.equal(missingRecord.status, 400);

  const missingJob = await appRequest("/api/v1/inspections", "RC-REC-2", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ recordId: "rec-x" }),
  });
  assert.equal(missingJob.status, 400);
});

test("inspection records are scoped by remote controller and job", async () => {
  const ownJob = await createDispatchedJob("RC-REC-3", "本人任务");
  const otherJob = await createDispatchedJob("RC-REC-4", "他人任务");
  await createInspection("RC-REC-3", ownJob, "rec-own");

  const forbidden = await createInspection("RC-REC-4", ownJob, "rec-other");
  assert.equal(forbidden.status, 404);

  const list = await appRequest(`/api/v1/inspections?jobId=${ownJob}`, "RC-REC-3");
  assert.equal(list.status, 200);
  assert.equal(list.body.records.length, 1);
  assert.equal(list.body.records[0].recordId, "rec-own");

  const otherList = await appRequest(`/api/v1/inspections?jobId=${otherJob}`, "RC-REC-3");
  assert.equal(otherList.body.records.length, 0);
});

test("admin inspections endpoint is read-only, filterable and newest-first", async () => {
  const isolatedDir = await mkdtemp(path.join(os.tmpdir(), "gas-demo-admin-records-"));
  const isolated = await createApp({ dataDir: isolatedDir });
  await new Promise((resolve) => isolated.listen(0, "127.0.0.1", resolve));
  const isolatedUrl = `http://127.0.0.1:${isolated.address().port}`;

  const call = async (requestPath, options = {}) => {
    const response = await fetch(`${isolatedUrl}${requestPath}`, options);
    const text = await response.text();
    return { status: response.status, body: text ? JSON.parse(text) : null };
  };

  try {
    const empty = await call("/api/admin/inspections");
    assert.equal(empty.status, 200);
    assert.deepEqual(empty.body.records, []);

    const created = await call("/api/admin/jobs", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title: "记录查询任务", targetRcSn: "RC-ADMIN-1", gas: validGas() }),
    });
    assert.equal(created.status, 201);
    const jobId = created.body.job.jobId;

    const otherCreated = await call("/api/admin/jobs", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title: "另一个任务", targetRcSn: "RC-ADMIN-1", gas: validGas() }),
    });
    const otherJobId = otherCreated.body.job.jobId;

    for (const id of [jobId, otherJobId]) {
      const dispatched = await call(`/api/admin/jobs/${id}/dispatch`, { method: "POST" });
      assert.equal(dispatched.status, 200);
    }

    const upload = (recordId, targetJobId, totalSamples) => call("/api/v1/inspections", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-RC-SN": "RC-ADMIN-1" },
      body: JSON.stringify(inspectionPayload(targetJobId, recordId, { totalSamples })),
    });

    const firstRecord = await upload("rec-admin-1", jobId, 4);
    assert.equal(firstRecord.status, 201);
    await new Promise((resolve) => setTimeout(resolve, 5));
    const secondRecord = await upload("rec-admin-2", jobId, 6);
    assert.equal(secondRecord.status, 201);
    await upload("rec-admin-3", otherJobId, 2);

    const all = await call("/api/admin/inspections");
    assert.equal(all.status, 200);
    assert.equal(all.body.records.length, 3);
    assert.ok(Date.parse(all.body.records[0].receivedAt) >= Date.parse(all.body.records[1].receivedAt));

    const filtered = await call(`/api/admin/inspections?jobId=${jobId}`);
    assert.equal(filtered.status, 200);
    assert.equal(filtered.body.records.length, 2);
    assert.deepEqual(filtered.body.records.map((record) => record.recordId).sort(), ["rec-admin-1", "rec-admin-2"]);
    assert.equal(filtered.body.records[0].totalSamples, 6);
    assert.equal(filtered.body.records[0].rcSn, "RC-ADMIN-1");

    const missingJob = await call("/api/admin/inspections?jobId=GAS-DEMO-404");
    assert.equal(missingJob.status, 200);
    assert.deepEqual(missingJob.body.records, []);

    const rejectedWrite = await call("/api/admin/inspections", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(inspectionPayload(jobId, "rec-admin-4")),
    });
    assert.equal(rejectedWrite.status, 404);
  } finally {
    await new Promise((resolve, reject) => isolated.close((error) => (error ? reject(error) : resolve())));
    await rm(isolatedDir, { recursive: true, force: true });
  }
});
