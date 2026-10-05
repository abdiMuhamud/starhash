# StarHash: USSD QA by INNOVII

**StarHash** (`*` … `#`) is an Android app that tests USSD services the way a customer uses them, without anyone
pressing keys:

1. **Is the service up?** It dials `*122#`, `*400#` (or any code), checks a menu comes back, walks the menus you set,
   and can open every item of a menu once to see each service answers.
2. **Was the customer charged?** For a VAS service it reads the balance (`*122#` → 1), subscribes through the menus
   (e.g. `*400#` → 4 Mobile Market → 1 Furo → 1 Hargeysa → 1), waits, reads the balance again, and checks the
   difference is the service price (it reads "qiimihiisu waa 0.5$" from the menu). It also catches the welcome SMS
   (from `400`) and the balance SMS.
3. **Reports.** Every run is saved with each pop-up, each answer typed, the balances, the charge and the SMS, with a
   verdict per test: **Pass**, **Check** (e.g. charged the wrong amount, no SMS), **Fail** (no answer, menu broken,
   subscribed but *not* charged) or **Blocked** (balance too low). Share as a message (WhatsApp), HTML or CSV.

## Install

Download `StarHash.apk` from the [latest release](../../releases/latest), install it on an Android 8+ phone with the
test SIM, open it and follow the **Get the phone ready** card:

- **Phone** permission: to dial the codes and choose the SIM (dual-SIM phones: pick the SIM in Settings).
- **SMS** permission (optional): to read the balance SMS and the services' welcome SMS.
- **Accessibility → StarHash USSD tester → On**: reads the USSD pop-ups and types the menu answers. It acts only while
  a test runs. On Android 13+, if the switch is greyed out: *App info → ⋮ → Allow restricted settings*. On Xiaomi,
  Redmi, Tecno and Infinix, also allow *Autostart* and set *Battery saver* to *No restrictions*.

During a run the USSD pop-ups appear and close by themselves; keep the screen on and don't touch them. The app comes
back with the results at the end.

## Engines (Settings)

| Engine | How | Use for |
|---|---|---|
| Screen reader (default) | Dials like a person, reads and answers the pop-ups through the accessibility service | Menus (`*400#`), every phone |
| One-shot API | Android's `sendUssdRequest`, one request per test; menus sent as one chained code `*400*4*1*1*1#` | Networks that accept chained codes; no accessibility needed |
| Demo network | A copy of the Telesom `*122#` / `*400#` menus inside the app, with a 1.00 USD balance | Training and demos: no SIM, no charge |

## Tests

A new install has: Balance (`*122#` → 1), `*122#` menu, `*400#` main menu, `*400#` every service opens, Mobile Market
menu, and **Subscribe: Mobile Market (Hargeysa)** (`*400#` → 4 1 1 1, 0.50 USD, SMS from 400: not selected by default
because it charges). Add your own on the Tests page: type the code and the answers you would type ("4 1 1 1").
Subscription tests always ask before charging, and can unsubscribe afterwards.

## Build

```bash
./gradlew :core:test            # the USSD logic: menus, balances, prices, charge verdicts (plain Kotlin)
./gradlew :app:assembleRelease  # the APK (needs the Android SDK)
./gradlew -PcoreOnly=true :core:test   # on a machine without the Android SDK
```

- `core/`: plain Kotlin, no Android: models, `UssdText` (reads menus, balances, prices, answers in English and
  Somali), `QaRunner` (runs the tests, gives verdicts), `DemoNetwork`, `ReportFormat`. Tested in `core/src/test`.
- `app/`: the Android app (Jetpack Compose). `ussd/` has the accessibility service, the screen and one-shot drivers,
  the SIM picker and the SMS watcher.
- GitHub Actions builds every push; every push to `main` publishes a release `v<VERSION>.<build>` with the APK.
  `VERSION` holds the major.minor number.
