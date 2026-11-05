import { mkdir, readFile, rename, rm, writeFile } from "node:fs/promises";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { createSeedState } from "./seed.mjs";

/**
 * 任务状态存储。
 *
 * 设计意图：Demo 服务不引入数据库，用单个 `state.json` 保存全部数据；写入串行化并采用
 * 临时文件 + rename 的原子替换，避免进程中断留下半截文件。更新失败时回滚内存状态，
 * 保证内存与磁盘一致。
 */
export async function createStore(dataDir) {
  const root = path.resolve(dataDir);
  const paths = { root, state: path.join(root, "state.json") };
  await mkdir(root, { recursive: true });

  let state;
  try {
    state = normalizeState(JSON.parse(await readFile(paths.state, "utf8")));
  } catch (error) {
    if (error.code !== "ENOENT") throw error;
    state = normalizeState(createSeedState());
    await atomicWriteJson(paths.state, state);
  }

  let pending = Promise.resolve();
  return {
    paths,
    get state() {
      return state;
    },
    update(mutator) {
      const operation = pending.then(async () => {
        const previous = structuredClone(state);
        try {
          const result = await mutator(state);
          await atomicWriteJson(paths.state, state);
          return result;
        } catch (error) {
          state = previous;
          throw error;
        }
      });
      pending = operation.catch(() => {});
      return operation;
    },
  };
}

function normalizeState(value) {
  if (!value || typeof value !== "object") throw new Error("Invalid state.json");
  for (const key of ["jobs", "devices", "inspections"]) {
    if (!Array.isArray(value[key])) value[key] = [];
  }
  return value;
}

async function atomicWriteJson(file, value) {
  const temporary = `${file}.${process.pid}.${randomUUID()}.tmp`;
  try {
    await writeFile(temporary, `${JSON.stringify(value, null, 2)}\n`, "utf8");
    await rename(temporary, file);
  } catch (error) {
    await rm(temporary, { force: true }).catch(() => {});
    throw error;
  }
}
