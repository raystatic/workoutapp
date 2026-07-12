# App Distribution — run/check the app without a local machine

Builds are distributed straight from CI so you never need Android Studio or Xcode
locally.

## Android — Firebase App Distribution (active)

On every merge to `main`, the `distribute-android` job in `Main CI/CD`:
1. builds the debug APK,
2. uploads it as a downloadable **`app-debug-apk`** workflow artifact (works today,
   no setup needed — grab it from the Actions run), and
3. once the two secrets below exist, pushes it to **Firebase App Distribution** so
   your phone gets each build via a link/email.

### One-time setup
1. **Create a Firebase project** (or reuse `workoutapp-dev`) at
   <https://console.firebase.google.com>.
2. **Register the Android app** in that project using the app's
   `applicationId` (the `androidApp` module's id). Copy the **App ID** — it looks
   like `1:1234567890:android:abcdef123456`.
3. **App Distribution → Testers**: create a tester group named **`testers`** and
   add your own email.
4. **Create a service account** (Google Cloud → IAM → Service Accounts) with the
   **Firebase App Distribution Admin** role, and download its JSON key.
5. **Add two GitHub Actions secrets** (repo → Settings → Secrets and variables →
   Actions):
   - `FIREBASE_ANDROID_APP_ID` → the App ID from step 2.
   - `FIREBASE_SERVICE_ACCOUNT` → the entire JSON key file contents from step 4.

That's it. The next merge to `main` distributes the build to your phone. Install
the Firebase App Tester app (or use the email link) to run it.

> Until the secrets are set, the job still succeeds and the APK is available as
> the `app-debug-apk` artifact on the Actions run.

## iOS — follow-up (prerequisites required)

There is **no free path** to run an iOS build on a device. To distribute iOS
without a local Mac you need:

- An **Apple Developer Program** membership (~$99/yr).
- A **macOS CI runner** (`macos-latest` on GitHub Actions) to build and sign the
  `.ipa` — you still don't need your own Mac.
- **Signing assets** (distribution certificate + provisioning profile) stored as
  CI secrets. For Firebase App Distribution (iOS) each tester device's **UDID**
  must be in an ad-hoc provisioning profile; **TestFlight** is usually smoother
  (internal testers need no App Store review).

When you're ready to invest in the Apple account, this repo will add a
`distribute-ios` job (macOS runner → build/sign `.ipa` → TestFlight or Firebase
App Distribution). Until then, use Android App Distribution above, or the shared
UI can be checked via a web preview if that target is added later.
