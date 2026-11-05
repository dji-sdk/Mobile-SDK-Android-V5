export function createSeedState(now = Date.now()) {
  return {
    jobs: [
      createTask(now),
    ],
    devices: [],
    assets: [],
    uploadSessions: [],
  };
}

function createTask(now) {
  return {
    jobId: "PATROL-DEMO-001",
    title: "天空之城",
    status: "DRAFT",
    targetRcSn: null,
    routeStatus: "CLOUD_PROVIDED",
    routeFileUrl: "/api/v1/routes/PATROL-DEMO-001.kmz",
    routeFileName: "generate_test.kmz",
    timeline: [{ timestamp: now, description: "任务已由 Demo 服务端创建" }],
    extras: {
      missionType: "INSPECTION",
      requirements: "航线模拟器验证",
      deadline: new Date(now + 365 * 24 * 60 * 60 * 1000).toISOString(),
    },
  };
}
