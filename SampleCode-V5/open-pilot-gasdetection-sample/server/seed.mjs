/**
 * 全新数据目录的种子状态。
 *
 * 只创建一个草稿任务，方便管理台打开就有可操作对象；通道与单位沿用气体检测真实案例，
 * 阈值是演示值，不代表任何法规限值。
 */
export function createSeedState(now = Date.now()) {
  return {
    jobs: [createSeedJob(now)],
    devices: [],
  };
}

function createSeedJob(now) {
  const createdAt = new Date(now).toISOString();
  return {
    jobId: "GAS-DEMO-001",
    title: "船舶尾气检测-001",
    status: "DRAFT",
    targetRcSn: null,
    gas: {
      target: { latitude: 22.5, longitude: 113.9, altitudeM: 60 },
      channels: [
        {
          channelId: "so2",
          displayName: "SO2",
          unit: "µg/m³",
          threshold: 500,
        },
        {
          channelId: "co2",
          displayName: "CO2",
          unit: "ppm",
          threshold: 1000,
        },
      ],
      safetyBoundaryM: 200,
      requirements: "靠近船尾采样；阈值为演示值，不代表法规限值",
      deadline: new Date(now + 365 * 24 * 60 * 60 * 1000).toISOString(),
    },
    timeline: [{ timestamp: now, description: "任务已由 Demo 服务端创建（演示阈值）" }],
    createdAt,
    updatedAt: createdAt,
  };
}
