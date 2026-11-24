# Habit Tracker

One-tap daily habit tracking for Android and the web, synced through Firebase.

- **Android:** Kotlin + Jetpack Compose (Material 3), dark-only, AlarmManager reminder.
- **Web:** React + TypeScript (Vite), dark-only, the same design tokens and streak rules.
- **Backend:** Firebase Authentication + Cloud Firestore, owner-only security rules.
- **Web hosting:** Cloudflare Pages via Wrangler.

Live web app: <https://habit-tracker-b1d88.pages.dev>

## Repository layout

```
docs/spec.md          The build specification
firebase/             firestore.rules, indexes, emulator config, rules tests
web/                  React + TypeScript client
android/              Kotlin + Compose app
scripts/build-android.sh
```

## Data model

Shared contract; field names and types are identical on both clients.

| Path | Fields |
| --- | --- |
| `users/{uid}/habits/{habitId}` | `name: string`, `scheduleType: "weekdays" \| "timesPerWeek" \| "timesPerMonth" \| "timesPerYear"`, `weekdays: number[]` (1 = Mon … 7 = Sun; weekdays type), `targetCount: number` (flexible types), `archived: boolean`, `createdAt`, `updatedAt` (server timestamps) |
| `users/{uid}/habitLogs/{habitId}_{yyyy-MM-dd}` | `habitId: string`, `date: string`, `createdAt` (server timestamp) |
| `users/{uid}/settings/preferences` | `reminderTime: "HH:mm" \| null` |

A `habitLogs` document means the habit was done that day. Unmarking deletes it.
The deterministic document id means two devices marking the same day write one
document. Dates are plain local-date strings so they never shift time zones.

Habits created before recurrence support have no `scheduleType`, so they default
to `weekdays` and keep working unchanged.

Clients load all habits (filtering and sorting in the client, no composite
indexes). Check-in history is recurrence-aware: at least 90 days (which covers
the 12-week window), 6 months for monthly habits, and 3 years for yearly habits.

### Recurrence rules

- **Specific days:** fixed weekdays, the original behaviour.
- **Times per week/month/year:** the target is met by that many check-ins inside
  the calendar period (Monday–Sunday, calendar month, calendar year). One
  check-in per day; extra check-ins are allowed but do not extend the streak.
- A flexible habit appears in Today every day of its period with progress like
  “2 of 3 this week”. The Today summary counts a flexible habit as done once its
  period target is met.
- A streak is the number of consecutive periods that met the target. The current
  period counts only once met; until then it does not break the streak. Periods
  before the habit was created are ignored.
- Completion rates use 30 days for weekday habits, 12 weeks for weekly, 6 months
  for monthly, and 3 years for yearly habits.

## Web

```bash
cd web
npm install
npm run dev      # http://localhost:5173
npm test         # streak/recurrence unit tests (34)
npm run build    # type-check + production bundle
npm run deploy   # build, then wrangler deploy
```

Configuration is read from `web/.env` (see `.env.example`). The values are the
Firebase web app configuration, which is not secret — the security rules are
what protect the data.

To run against local emulators instead of production:

```bash
cd firebase && firebase emulators:start --only auth,firestore
# in another shell
cd web && VITE_USE_EMULATOR=true npm run dev
```

## Android

Build and test in the provided Android SDK container:

```bash
./scripts/build-android.sh
# APK: android/app/build/outputs/apk/debug/app-debug.apk
```

Or manually:

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The build uses `android/keystore/debug.keystore` (a stable debug key, committed
on purpose) whose SHA-1 and SHA-256 are registered with the Firebase Android
app, so Google sign-in works on debug builds. `android/app/google-services.json`
is generated from the Firebase project and is git-ignored.

Reminders use `AlarmManager`: an exact alarm when the system allows it, an
inexact one otherwise, restored after reboot. WorkManager is not used. A
reminder fires while any weekday habit is unchecked today or any flexible
habit's period target is still unmet.

## Firebase

```bash
cd firebase
npm install
npm run test:rules   # Firestore emulator + rules tests (7)
firebase deploy --only firestore:rules
```

Emulator ports are Auth `9099` and Firestore `8085` (8080 is often taken).

## Google sign-in

- Android uses Credential Manager with the web client id from
  `google-services.json`; the debug keystore fingerprints are already registered.
- Web uses `signInWithPopup`. The deployed Workers domain is in the Firebase
  Auth authorized-domains list. Add any new deployment domain there before
  using Google sign-in on it.

## Releases

Every push to `main` runs `.github/workflows/android-release.yml`, which builds a
signed release APK, runs the unit tests, and publishes it on the GitHub Releases
page as `habit-tracker-1.0.<run>.apk`. You can also run the workflow manually
from the Actions tab.

Release APKs are signed with the stable sideload key in
`android/keystore/debug.keystore` (already registered with Firebase so Google
sign-in works). It is not a Play Store signing setup. `android/app/google-services.json`
is committed because it is Firebase client configuration, not a secret — it is
embedded in every APK anyway. Set the `GOOGLE_SERVICES_JSON` repository secret
(base64-encoded) if you ever want CI to use a different config.

## Notes

- The app is dark-only by design; both clients share the same palette,
  type scale, spacing, and component shapes.
- Exported JSON from either client contains every habit and check-in, not just
  the 90-day window.
- The free Firebase plan has no managed backups; export occasionally.
