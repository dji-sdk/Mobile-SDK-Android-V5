import { copyFile, mkdir, readFile, rename, rm, writeFile } from "node:fs/promises";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { createSeedState } from "./seed.mjs";

export async function createStore(dataDir, seedRouteFile) {
  const root = path.resolve(dataDir);
  const paths = {
    root,
    state: path.join(root, "state.json"),
    routes: path.join(root, "routes"),
    uploads: path.join(root, "uploads"),
    sessions: path.join(root, "sessions"),
  };
  await Promise.all([
    mkdir(paths.routes, { recursive: true }),
    mkdir(paths.uploads, { recursive: true }),
    mkdir(paths.sessions, { recursive: true }),
  ]);

  let state;
  try {
    state = normalizeState(JSON.parse(await readFile(paths.state, "utf8")));
  } catch (error) {
    if (error.code !== "ENOENT") throw error;
    state = createSeedState();
    for (const job of state.jobs) {
      await copyFile(seedRouteFile, path.join(paths.routes, `${job.jobId}.kmz`));
    }
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
  for (const key of ["jobs", "devices", "assets", "uploadSessions"]) {
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
