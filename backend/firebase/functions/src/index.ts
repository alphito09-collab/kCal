/**
 * kCal Cloud Functions
 *
 *  - verifySubscription  (callable)  client sends a Play purchase token after checkout; the token is
 *                                     verified with the Play Developer API, acknowledged, and the
 *                                     entitlement written to users/{uid}/entitlements/premium.
 *  - playRtdn            (Pub/Sub)    Real-time developer notifications: renewals, cancellations,
 *                                     expiries and revocations keep the entitlement in sync.
 */
import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { onMessagePublished } from "firebase-functions/v2/pubsub";
import { google } from "googleapis";

initializeApp();
const db = getFirestore();

const PACKAGE_NAME = "com.alphito.kcal.mealplanner.padometer";
const SUBSCRIPTION_ID = "kcal_premium";
const RTDN_TOPIC = "play-rtdn";

/** Play Developer API client; the function's service account must be linked in Play Console. */
const publisher = google.androidpublisher({
  version: "v3",
  auth: new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/androidpublisher"] }),
});

type Entitlement = {
  active: boolean;
  basePlanId: string | null;
  expiryTime: string | null;
  state: string;
};

/** Reads the subscription from Play and turns it into kCal's entitlement shape. */
async function readSubscription(purchaseToken: string): Promise<Entitlement & { acknowledged: boolean; linkedPurchaseToken?: string }> {
  const { data } = await publisher.purchases.subscriptionsv2.get({ packageName: PACKAGE_NAME, token: purchaseToken });
  const item = data.lineItems?.find((l) => l.productId === SUBSCRIPTION_ID) ?? data.lineItems?.[0];
  const state = data.subscriptionState ?? "SUBSCRIPTION_STATE_UNSPECIFIED";
  const expiry = item?.expiryTime ?? null;
  const notExpired = expiry != null && Date.parse(expiry) > Date.now();
  // Grace period and cancelled-but-paid-through keep access until expiry.
  const active =
    notExpired &&
    ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", "SUBSCRIPTION_STATE_CANCELED"].includes(state);
  return {
    active,
    basePlanId: item?.offerDetails?.basePlanId ?? null,
    expiryTime: expiry,
    state,
    acknowledged: data.acknowledgementState === "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
    linkedPurchaseToken: data.linkedPurchaseToken ?? undefined,
  };
}

async function writeEntitlement(uid: string, purchaseToken: string, e: Entitlement) {
  const batch = db.batch();
  batch.set(
    db.doc(`users/${uid}/entitlements/premium`),
    { ...e, purchaseToken, updatedAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
  batch.set(db.doc(`purchases/${purchaseToken}`), { uid, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
  await batch.commit();
}

export const verifySubscription = onCall({ region: "us-central1", enforceAppCheck: false }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign-in required");
  const { packageName, productId, purchaseToken } = (request.data ?? {}) as Record<string, unknown>;
  if (packageName !== PACKAGE_NAME || productId !== SUBSCRIPTION_ID || typeof purchaseToken !== "string" || !purchaseToken) {
    throw new HttpsError("invalid-argument", "Unknown product or missing purchase token");
  }

  // A token belongs to exactly one user: refuse to re-bind someone else's purchase.
  const owner = await db.doc(`purchases/${purchaseToken}`).get();
  if (owner.exists && owner.get("uid") !== uid) throw new HttpsError("permission-denied", "Purchase belongs to another account");

  let sub;
  try {
    sub = await readSubscription(purchaseToken);
  } catch (err) {
    logger.error("Play verification failed", err);
    throw new HttpsError("failed-precondition", "Could not verify the purchase with Google Play");
  }

  if (sub.active && !sub.acknowledged) {
    await publisher.purchases.subscriptions.acknowledge({
      packageName: PACKAGE_NAME,
      subscriptionId: SUBSCRIPTION_ID,
      token: purchaseToken,
      requestBody: {},
    });
  }
  // An upgrade/downgrade replaces the old token – retire it.
  if (sub.linkedPurchaseToken) await db.doc(`purchases/${sub.linkedPurchaseToken}`).delete().catch(() => undefined);

  const entitlement: Entitlement = { active: sub.active, basePlanId: sub.basePlanId, expiryTime: sub.expiryTime, state: sub.state };
  await writeEntitlement(uid, purchaseToken, entitlement);
  return entitlement;
});

/** Play → Pub/Sub topic configured in Play Console → this function. */
export const playRtdn = onMessagePublished({ topic: RTDN_TOPIC, region: "us-central1" }, async (event) => {
  const payload = event.data.message.json as {
    packageName?: string;
    subscriptionNotification?: { purchaseToken: string; notificationType: number };
    testNotification?: unknown;
  };
  if (payload.testNotification) {
    logger.info("RTDN test notification received");
    return;
  }
  const note = payload.subscriptionNotification;
  if (!note || payload.packageName !== PACKAGE_NAME) return;

  const owner = await db.doc(`purchases/${note.purchaseToken}`).get();
  const uid = owner.get("uid") as string | undefined;
  if (!uid) {
    // Purchase not verified by the app yet; the client will call verifySubscription on next launch.
    logger.info("RTDN for unknown token", { type: note.notificationType });
    return;
  }
  const sub = await readSubscription(note.purchaseToken);
  await writeEntitlement(uid, note.purchaseToken, {
    active: sub.active,
    basePlanId: sub.basePlanId,
    expiryTime: sub.expiryTime,
    state: sub.state,
  });
});
