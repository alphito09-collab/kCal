# kCal backend (Firebase)

The Android app works fully offline without any of this. Each piece switches on when configured.

| Piece | Where | What it does |
|---|---|---|
| Anonymous Auth | Firebase Auth | gives every install a `uid` (no sign-in UI) |
| Food catalogue | Firestore `foods/*`, `meta/catalog` | remote, localized foods; app syncs on launch + daily (WorkManager) |
| Remote Config | `remoteconfig.template.json` | `premium_enabled`, `ads_enabled`, `interstitial_interval_minutes` |
| Subscriptions | `functions/src/index.ts` | `verifySubscription` callable + `playRtdn` Pub/Sub handler (Blaze plan only) |
| Push | FCM | token stored in `users/{uid}.fcmTokens`, topic `all_users` |

## Current setup (project `kcal-bf1c2`, free Spark plan)
Done already:
- Anonymous Auth is on.
- Firestore is in `asia-south1`, with the rules and indexes from this folder.
- Remote Config is published.
- The catalog is seeded.

Cloud Functions need the Blaze plan, so `server_verification_enabled` stays **false**. Purchases are acknowledged on the device and restored from Google Play on every launch.

To move to server verification:
1. Upgrade to Blaze.
2. Deploy the functions with `firebase deploy --only functions`.
3. Do the Play setup in section 4.
4. Set `server_verification_enabled` to true in Remote Config. No app update is needed.

## 1. Firebase project
1. Create a project, add an Android app with package `com.alphito.kcal.mealplanner.padometer` (add your debug + release SHA-1).
2. Download `google-services.json` into `app/`. The Gradle build applies the google-services plugin only when that file exists. Don't commit it to a public repository.
3. Enable **Authentication → Anonymous**, **Firestore**, **Cloud Messaging** and **Remote Config**.
4. `cp .firebaserc.example .firebaserc` and set your project id.

## 2. Deploy
```bash
cd backend/firebase
npx firebase-tools deploy --only firestore,remoteconfig      # works on the free plan
cd functions && npm install && cd .. && npx firebase-tools deploy --only functions   # Blaze only
```
`remoteconfig.template.json` is the source of truth: deploying it replaces the console values.

## 3. Seed the food catalogue
Edit the tables in `seed/catalog/` (`ingredients.psv`, `foods_*.psv`), bump `seed/catalog/VERSION`, run `python3 seed/build_catalog.py`, then seed:
- `names` and `portions` are maps from a language code to text (`en`, `ar`, `de`, `es`, `fr`, `hi`, `id`, `pt`, `ru`, `tr`, `ur`, `zh`); `en` is required.
- `regions` is empty for every region.
- `imageUrl` is used when `imageKey` has no bundled image.

```bash
cd backend/firebase
node seed/seed.mjs        # uses your `npx firebase-tools login`; no service account needed
```
(Or, with a service-account key: `cd functions && GOOGLE_APPLICATION_CREDENTIALS=key.json npm run seed`.)
Foods removed from the file are deactivated, not deleted, so old meal plans still resolve. The `meta/catalog.version` bump makes apps re-download.

## 4. Google Play subscriptions
1. Play Console → Monetize → Subscriptions: create product **`kcal_premium`** with base plans **`weekly`** and **`monthly`** (auto-renewing).
2. Play Console → Setup → API access: link the Google Cloud project and grant the Cloud Functions service account (`<project-id>@appspot.gserviceaccount.com`) the *View financial data* and *Manage orders and subscriptions* permissions.
3. Real-time developer notifications: create Pub/Sub topic **`play-rtdn`**, give `google-play-developer-notifications@system.gserviceaccount.com` the *Publisher* role, and put the topic name in Play Console → Monetization setup.
4. Test with license testers on an internal-testing track build.

While `server_verification_enabled` is false, the app acknowledges purchases on the device. Google Play still enforces payment, and the app restores the subscription from Play on every launch. It doesn't defend against modified APKs, though. Turn on server verification once you're on Blaze.

## 5. AdMob (GMA Next-Gen SDK)
Debug builds use Google's **test** IDs. For release, add to `~/.gradle/gradle.properties` (or CI secrets):
```
kcal.admob.appId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
kcal.admob.splashUnit=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```
- Set up a GDPR / US-states consent message in AdMob → Privacy & messaging.
- The app shows it through UMP before any ad request.
- It adds an "Ad privacy choices" row in Settings → Privacy & Security when required.

## 6. Push campaigns
Send from Firebase Console → Messaging to topic `all_users`. Messages use the "Updates from kCal" channel.
