#!/usr/bin/env node
/**
 * Uploads seed/catalog.json (built by build_catalog.py) to Firestore with your Firebase CLI login
 * (no service account needed), deactivates foods that were removed, and sets meta/catalog.version
 * to the catalogue version so every app newer than it re-downloads on its next sync.
 *
 *   python3 seed/build_catalog.py     # after editing seed/catalog/*.psv (bump catalog/VERSION)
 *   npx firebase-tools login          # once
 *   node seed/seed.mjs                # from backend/firebase
 *
 * Writes go through the Firestore REST API as the signed-in project member (IAM), so they are
 * not subject to the client security rules, which block catalogue writes from the app.
 */
import { execSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { homedir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const projectId = JSON.parse(readFileSync(join(here, "..", ".firebaserc"), "utf8")).projects.default;
const catalog = JSON.parse(readFileSync(join(here, "catalog.json"), "utf8"));
const foods = catalog.foods;

function accessToken() {
  // Any authenticated CLI call refreshes the stored OAuth token.
  execSync("npx -y firebase-tools projects:list", { stdio: "ignore" });
  const store = JSON.parse(readFileSync(join(homedir(), ".config", "configstore", "firebase-tools.json"), "utf8"));
  const token = store.tokens?.access_token;
  if (!token) throw new Error("Not logged in – run `npx firebase-tools login` first.");
  return token;
}

/** JS value → Firestore REST Value. */
function encode(v) {
  if (v === null || v === undefined) return { nullValue: null };
  if (typeof v === "boolean") return { booleanValue: v };
  if (typeof v === "number") return Number.isInteger(v) ? { integerValue: String(v) } : { doubleValue: v };
  if (typeof v === "string") return { stringValue: v };
  if (Array.isArray(v)) return { arrayValue: { values: v.map(encode) } };
  return { mapValue: { fields: Object.fromEntries(Object.entries(v).map(([k, x]) => [k, encode(x)])) } };
}

const ids = new Set();
for (const f of foods) {
  if (!f.id || !f.names?.en) throw new Error(`Food needs id and names.en: ${JSON.stringify(f).slice(0, 80)}`);
  if (ids.has(f.id)) throw new Error(`Duplicate food id ${f.id}`);
  ids.add(f.id);
}

const token = accessToken();
const base = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
const headers = { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };

const existing = [];
for (let pageToken = ""; ; ) {
  const res = await fetch(`${base}/foods?pageSize=300&mask.fieldPaths=active${pageToken ? `&pageToken=${pageToken}` : ""}`, { headers });
  if (!res.ok) throw new Error(`List foods failed: ${res.status} ${await res.text()}`);
  const body = await res.json();
  for (const d of body.documents ?? []) existing.push(d.name.split("/").pop());
  if (!body.nextPageToken) break;
  pageToken = body.nextPageToken;
}

const docName = (path) => `projects/${projectId}/databases/(default)/documents/${path}`;
const writes = [
  ...foods.map(({ id, ...food }) => ({
    update: { name: docName(`foods/${id}`), fields: Object.fromEntries(Object.entries(food).map(([k, v]) => [k, encode(v)])) },
  })),
  // Removed from the file → deactivated (old meal plans still resolve the food).
  ...existing.filter((id) => !ids.has(id)).map((id) => ({
    update: { name: docName(`foods/${id}`), fields: { active: { booleanValue: false } } },
    updateMask: { fieldPaths: ["active"] },
  })),
  // Same version as the bundled asset → apps that already ship this catalogue skip the download.
  { update: { name: docName("meta/catalog"), fields: { version: { integerValue: String(catalog.version) } } }, updateMask: { fieldPaths: ["version"] } },
  {
    transform: {
      document: docName("meta/catalog"),
      fieldTransforms: [{ fieldPath: "updatedAt", setToServerValue: "REQUEST_TIME" }],
    },
  },
];

// A commit takes at most 500 writes; the version bump goes in the last batch.
for (let i = 0; i < writes.length; i += 500) {
  const res = await fetch(`${base}:commit`, { method: "POST", headers, body: JSON.stringify({ writes: writes.slice(i, i + 500) }) });
  if (!res.ok) throw new Error(`Commit failed: ${res.status} ${await res.text()}`);
}

const meta = await (await fetch(`${base}/meta/catalog`, { headers })).json();
console.log(
  `Seeded ${foods.length} foods (${existing.filter((id) => !ids.has(id)).length} deactivated) into ${projectId}; ` +
    `catalogue version is now ${meta.fields?.version?.integerValue}.`,
);
