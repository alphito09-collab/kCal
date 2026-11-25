# kCal – what's needed to publish (Google Play + AdMob)

Status legend: ✅ done in code / Firebase  ·  🟡 you need to do it in a console  ·  ⏳ later (optional)

---

## 1. What is already done

| Area | Status | Notes |
|---|---|---|
| App id | ✅ | `com.alphito.kcal.mealplanner.padometer` (cannot change after the first upload) |
| Release build | ✅ | R8 full mode + resource shrinking, edge-to-edge, 12 languages (`res/xml/locales_config.xml`) |
| Firebase | ✅ | Project `kcal-bf1c2`: Anonymous Auth, Firestore (`asia-south1`) + rules, Remote Config, FCM. `app/google-services.json` is git-ignored |
| Remote flags | ✅ | `premium_enabled=false`, `ads_enabled=true`, `interstitial_interval_minutes=10`, `server_verification_enabled=false` |
| Food catalogue | ✅ | Bundled + Firestore (`foods`, `ingredients`, `meta/catalog`); re-seed with `node backend/firebase/seed/seed.mjs` |
| Billing code | ✅ | Play Billing 9, product `kcal_premium`, base plans `weekly` / `monthly`; acknowledged on device while on the free Spark plan |
| Ads code | ✅ | GMA Next-Gen SDK + UMP consent, splash interstitial (test IDs until real ones are set) |
| Notifications | ✅ | Reminder channels, `POST_NOTIFICATIONS` asked only when the user turns a reminder on |

---

## 2. Google Play Console 🟡

### 2.1 Account & app
1. Create or open the developer account at play.google.com/console.
   - A **personal** account created after Nov 2023 must run a **closed test with at least 12 testers for 14 days** before it can publish to production.
   - An organisation account does not have this rule.
2. **Create app** with these settings:
   - name **kCal**
   - default language English (US)
   - App (not game)
   - Free (subscriptions are in-app)

### 2.2 Signing & first upload
1. Create the upload keystore once and keep it **outside** the repo (`*.jks` is git-ignored):
   `keytool -genkey -v -keystore ~/keys/kcal-upload.jks -alias kcal -keyalg RSA -keysize 2048 -validity 10000`
2. Build the bundle with `./gradlew :app:bundleRelease`, signed with that key (Android Studio → Generate Signed Bundle).
3. Enrol in **Play App Signing** when you upload the first AAB to **Internal testing**.
4. Copy the **app signing key SHA-1** and the **upload key SHA-1** from Play Console → Setup → App integrity.
   - Add both to Firebase → Project settings → Android app → SHA certificate fingerprints.
   - Then download `google-services.json` again into `app/`.

### 2.3 App content (Policy → App content) – all required before review
| Form | What to answer for kCal |
|---|---|
| Privacy policy | A public URL (the app links `https://example.com/privacy` – replace it in `feature/premium` + `feature/settings` `LegalLinks`). |
| Ads | **Yes, contains ads** |
| App access | All features available without login |
| Content rating | IARC questionnaire → category *Reference, News or Educational / Health*; no violence etc. → expected **Everyone / PEGI 3** |
| Target audience | **18+** (or 13+ – do *not* include children: ads + health data) |
| Data safety | Collected: *App activity* (in-app actions – Firebase), *Device or other IDs* (Firebase install id, FCM token, Advertising ID via AdMob), *Health info* (weight, sugar readings – **stored on device only**, not shared). Encrypted in transit: yes. Users can request deletion: yes (Settings → Delete data). Ads SDK shares advertising ID with Google for ads. |
| Health apps declaration | Category **Nutrition & weight management**, **not** a medical device. Keep the in-app disclaimer ("Plans are algorithm-generated, not reviewed by a certified nutritionist"). |
| Advertising ID | **Yes** – used for advertising (AdMob). The Next-Gen SDK merges `com.google.android.gms.permission.AD_ID`. |
| Government app / Financial features | No |

### 2.4 Store listing
- Short description (≤80 chars), full description (≤4000) – translate for the 11 app languages (Store presence → Translations).
- App icon 512×512 PNG, feature graphic 1024×500, ≥2 phone screenshots (use the emulator shots), 7" and 10" tablet screenshots (the app supports tablets).
- Category **Health & Fitness**, contact e-mail, website (optional).

### 2.5 Subscriptions (only when you turn premium on)
1. Monetize → Subscriptions → create **`kcal_premium`**:
   - base plan **`weekly`** (auto-renewing, P1W)
   - base plan **`monthly`** (auto-renewing, P1M)
   - set prices for every country; Play converts them automatically
   - optional: a free-trial offer
2. Setup → License testing → add tester Gmail accounts, so their purchases are free test purchases.
3. Remote Config → `premium_enabled = true` (no app update needed).
4. ⏳ When you move to the Blaze plan:
   - deploy the Cloud Functions;
   - link the service account in Setup → API access (*View financial data*, *Manage orders and subscriptions*);
   - create the Pub/Sub topic `play-rtdn` and set it under Monetization setup → Real-time developer notifications;
   - set `server_verification_enabled = true`.

### 2.6 Release path
Internal testing (yourself) → Closed testing (12+ testers, 14 days for personal accounts) → Production (staged rollout 10% → 50% → 100%). Watch Android vitals (ANR < 0.47%, crash < 1.09%).

---

## 3. AdMob 🟡

1. Go to apps.admob.com → **Apps → Add app** → Android → "Is the app listed on a supported app store?" Answer **No** for now. Link it to Play once the app is published.
2. Copy the **App ID** (`ca-app-pub-XXXX~YYYY`).
3. **Ad units → Interstitial**, named e.g. `kcal_splash_interstitial`. Copy its ID (`ca-app-pub-XXXX/ZZZZ`).
4. Put both IDs in `~/.gradle/gradle.properties` or your CI secrets. Never put them in the repo:
   ```
   kcal.admob.appId=ca-app-pub-XXXX~YYYY
   kcal.admob.splashUnit=ca-app-pub-XXXX/ZZZZ
   ```
   Debug and release builds without these use Google's **test** IDs, so you never click your own live ads.
5. **Privacy & messaging**:
   - create a **European regulations (GDPR)** message;
   - create a **US state regulations** message;
   - publish both.
   The app already shows them through UMP and offers *Settings → Privacy & Security → Ad privacy choices*.
6. **app-ads.txt**: AdMob → Apps → app-ads.txt gives one line. Host it at `https://<your developer website>/app-ads.txt`, using the same website you set in the Play listing.
7. Payments: add your payment profile and tax info (AdMob → Payments). Ads only serve fully after the account review (usually 1–3 days, sometimes longer for new accounts).
8. Test on your own device: AdMob → Settings → Test devices → add your device's advertising ID.
9. Optional: Remote Config `interstitial_interval_minutes` tunes how often the splash ad can appear. `ads_enabled = false` turns it off instantly.

---

## 4. Before every release – checklist
- [ ] `versionCode` +1 and `versionName` in `app/build.gradle.kts`
- [ ] `./gradlew testDebugUnitTest stabilityCheck :app:bundleRelease` all green
- [ ] Real AdMob IDs present for the release build (check `BuildConfig.ADMOB_APP_ID` isn't the test id)
- [ ] `google-services.json` is the production project
- [ ] Re-seed the catalogue if `backend/firebase/seed/catalog/*` changed (`node backend/firebase/seed/seed.mjs`)
- [ ] Store listing "What's new" in all languages
