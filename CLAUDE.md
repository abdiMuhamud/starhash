# StarHash (INNOVII)

Android QA app for USSD/VAS services (Telesom *122# balance, *400# INNOVII VAS). See README.md.

- `core/` is plain Kotlin: put USSD logic there and test it (`./gradlew -PcoreOnly=true :core:test` works without the
  Android SDK). `app/` is Compose UI + Android glue (`ussd/`).
- When a new menu wording appears (English or Somali), add it to `UssdText` with a test in `UssdTextTest`, and keep
  `DemoNetwork` close to the real menus.
- The APK is built by GitHub Actions (`.github/workflows/build.yml`); every push to main publishes a release
  `v<VERSION>.<run>`. Raise `VERSION` for a bigger change.
- Commit as `abdiMuhamud <a.abdi.muhamud@gmail.com>`; no model names in commits or code.
- Plain words in the UI; verdicts are Pass / Check / Fail / Blocked.
