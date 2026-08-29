import { readFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getFirebaseDataConnect } from "../../../../../packages/config/src/index.mjs";
import {
  connectorConfig as socialConnectorConfig,
  listUserBlocksByTenant as listUserBlocksByTenantQuery
} from "../../../../../packages/dataconnect/social-admin-sdk/esm/index.esm.js";

const TENANT_SCAN_LIMIT = 5000;
const directoryName = path.dirname(fileURLToPath(import.meta.url));
const fallbackStorePath =
  process.env.VYB_SERVERLESS_RUNTIME === "vercel"
    ? path.join(os.tmpdir(), "vyb", "social-store.json")
    : path.resolve(directoryName, "../../data/social-store.json");

function getSocialDc() {
  return getFirebaseDataConnect(socialConnectorConfig);
}

function isFallbackEligibleError(error) {
  const message = error instanceof Error ? error.message.toLowerCase() : String(error).toLowerCase();
  return [
    "database is not available",
    "data connect",
    "service unavailable",
    "oauth2.googleapis.com/token",
    "google oauth2 access token",
    "default credentials",
    "metadata lookup",
    "metadata server",
    "all promises were rejected",
    "connect eacces",
    "econnrefused",
    "enotfound",
    "fetch failed",
    "unrecognized operation",
    "cannot query field",
    "unknown field"
  ].some((part) => message.includes(part));
}

async function readFallbackBlocks(tenantId) {
  try {
    const store = JSON.parse(await readFile(fallbackStorePath, "utf8"));
    return (Array.isArray(store?.userBlocks) ? store.userBlocks : []).filter(
      (item) => (item.tenantId ?? "tenant-demo") === tenantId && !item.deletedAt
    );
  } catch {
    return [];
  }
}

async function listActiveUserBlocksByTenant(tenantId) {
  try {
    const response = await listUserBlocksByTenantQuery(getSocialDc(), {
      tenantId,
      limit: TENANT_SCAN_LIMIT
    });
    return Array.isArray(response.data.userBlocks) ? response.data.userBlocks : [];
  } catch (error) {
    if (!isFallbackEligibleError(error)) throw error;
    return readFallbackBlocks(tenantId);
  }
}

export async function getRelationshipBlockedUserIds({ tenantId, userId }) {
  if (!tenantId || !userId) return new Set();
  const rows = await listActiveUserBlocksByTenant(tenantId);
  const blocked = new Set();
  for (const row of rows) {
    if (row.blockerUserId === userId) blocked.add(row.blockedUserId);
    if (row.blockedUserId === userId) blocked.add(row.blockerUserId);
  }
  return blocked;
}
