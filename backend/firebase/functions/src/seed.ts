/**
 * Uploads ../seed/foods.json to Firestore and bumps meta/catalog.version so every installed app
 * picks up the new catalogue on its next sync (WorkManager, daily + on launch).
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=service-account.json npm run seed
 */
import { readFileSync } from "fs";
import { join } from "path";
import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";

type SeedFood = { id: string; [key: string]: unknown };

async function main() {
  initializeApp();
  const db = getFirestore();
  const foods = JSON.parse(readFileSync(join(__dirname, "..", "..", "seed", "foods.json"), "utf8")) as SeedFood[];
  const ids = new Set(foods.map((f) => f.id));
  if (ids.size !== foods.length) throw new Error("Duplicate food ids in foods.json");

  const writer = db.bulkWriter();
  for (const { id, ...food } of foods) writer.set(db.doc(`foods/${id}`), food);
  // Foods removed from the file are deactivated, not deleted, so old meal plans still resolve.
  const existing = await db.collection("foods").listDocuments();
  for (const doc of existing) if (!ids.has(doc.id)) writer.set(doc, { active: false }, { merge: true });
  await writer.close();

  await db.doc("meta/catalog").set({ version: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
  console.log(`Seeded ${foods.length} foods and bumped the catalogue version.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
