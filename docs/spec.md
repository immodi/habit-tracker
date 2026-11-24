# Habit Tracker: Build Specification

*A spec for building a standalone Android app (Kotlin + Jetpack Compose) and web client (React + TypeScript) on Firebase. Draft, November 21, 2026; implementation decisions recorded November 23, 2026.*

## 1. Purpose

A small personal app for tracking daily habits. Mark a habit done for the day with one tap, see how long the streak is, and keep the phone and the browser in sync.

Design principles:

- **Simple.** Tapping a habit to mark it done for today is the main interaction.
- **Forgiving.** A missed day never deletes anything, and forgotten check-ins can be filled in later.
- **Synced.** A check-in made on one device appears on the other within seconds.
- **Dark.** One dark theme shared by both clients, with the same palette, type scale, and shapes.
- **Private.** Only the owner can read or write their data.

## 2. Scope

**In scope for v1:** user accounts (email/password and Google sign-in), an Android app, a web client, sync through Firebase, streaks and completion rates, an optional Android reminder, and JSON export.

**Out of scope for v1:** counted habits (such as eight glasses of water), per-habit reminders, reordering, categories, sharing, home-screen widgets, permanently deleting a habit, an iOS app, push notifications on the web.

## 3. Tech stack

- **Backend:** Firebase Authentication and Cloud Firestore on the free (Spark) plan. No custom server and no Cloud Functions in v1. This app gets its own Firebase project.
- **Android:** Kotlin, minimum SDK 24, Jetpack Compose with Material 3, ViewModel with StateFlow, Firebase Android SDK (via the Firebase BoM), AlarmManager for the reminder.
- **Web:** React with Vite and TypeScript, React Router, and the Firebase JS SDK (modular API).
- **Web hosting:** Cloudflare Pages, deployed with Wrangler. Firebase Hosting is not used.

Both clients talk directly to Firebase. That means the Firestore security rules in section 5 are the only thing protecting the data, so they must be written and deployed before any real data is stored.

## 4. Data model

This is the shared contract between both clients. Field names and types must be identical on Android and web.

### Habits

Path: `users/{uid}/habits/{habitId}`

- `name`: string
- `scheduleType`: `"weekdays"`, `"timesPerWeek"`, `"timesPerMonth"`, or `"timesPerYear"`. Missing values on older documents mean `"weekdays"`.
- `weekdays`: array of integers from 1 (Monday) to 7 (Sunday), used when `scheduleType` is `"weekdays"`. All seven by default.
- `targetCount`: number of check-ins per period for the flexible `scheduleType`s (`1` for weekday habits).
- `archived`: boolean, false by default
- `createdAt` and `updatedAt`: server timestamps

### Check-ins

Path: `users/{uid}/habitLogs/{habitId}_{yyyy-MM-dd}`

- `habitId`: string
- `date`: string in `yyyy-MM-dd` form, the user’s local calendar date
- `createdAt`: server timestamp

Rules for this data:

- A document in `habitLogs` means the habit was done that day. Marking done creates it and unmarking deletes it. There is no record for “not done”.
- The document ID is built from the habit ID and the date, so marking the same day twice, even from two devices, writes the same document and can’t create a duplicate.
- Dates are stored as plain strings so they don’t shift with time zones. The day changes at local midnight.
- Both clients load all habits (there will be few, so filter out archived ones and sort by `createdAt` in the client, which avoids needing a composite index). Check-in history is loaded according to the longest recurrence in use: at least 90 days (which covers the 12-week window), 6 months when a monthly habit exists, and 3 years when a yearly habit exists, using a range query on `date`.
- Use server timestamps, not device clocks.

### Settings

Path: `users/{uid}/settings/preferences`

- `reminderTime`: string in `HH:mm` form, or null for off. Default is null.

## 5. Security rules

Firebase web configuration values are not secrets. The rules below are what protect the data. Each user may only read and write documents under their own user ID.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }
  }
}
```

The agent should test these with the Firebase emulator, confirming that a signed-in user cannot read another user’s habits or check-ins and that a signed-out client cannot read anything.

## 6. Screens and behaviour

Both clients implement the same screens.

### 6.1 Sign in

Email and password sign-in and registration, plus Google sign-in on both clients. Sign-out lives in Settings.

### 6.2 Today (the home screen)

- Lists the non-archived habits scheduled for today, plus every flexible habit (times per week, month, or year). Weekday habits not scheduled today are hidden here.
- Each row shows the name, a large tap target to toggle done, the current streak, and a strip of the last 7 days as small dots (done, missed, empty, or not scheduled). Flexible rows show period progress such as “2 of 3 this week” instead of “Done today”.
- A summary line such as “3 of 5 done today”, or “3 of 5 done” once flexible habits are present. A flexible habit counts as done once its period target is met.
- An “Add habit” button, a link to Settings, and a link at the bottom to archived habits.
- An empty state with a prompt to add a first habit.

### 6.3 Add or edit habit

A name (required) and a recurrence choice: specific weekdays (all seven selected by default, at least one required) or a target of N times per week, month, or year (one check-in per day; extra check-ins are allowed). When editing, an Archive button.

### 6.4 Habit detail

Opened by tapping a habit’s name.

- Current streak and the completion rate (last 30 days, 12 weeks, 6 months, or 3 years depending on the recurrence).
- For flexible habits, a current-period progress card such as “This week · 2 of 3”.
- A grid of the last 90 days, laid out in weeks.
- Tap a past day or today to toggle it. Future days are disabled.
- For weekday habits, days outside the schedule are muted and can’t be toggled. If a check-in already exists on such a day (for example after the schedule was changed), it still shows as done but doesn’t count toward the streak or rate. For flexible habits, any day from creation onward can be toggled.

### 6.5 Archived habits

A list with a Restore action on each. Archiving hides a habit and keeps its history.

### 6.6 Settings

Reminder time with an off option (Android), export all data as JSON (required on web, optional on Android), and sign out.

## 7. Streaks and completion rate

Weekday habits count scheduled days. Flexible habits (N times per week, month, or year) count calendar periods.

- **Weekday streak:** the number of consecutive scheduled days marked done, counting back from today. If today is scheduled and not yet done, start from the most recent scheduled day before today, so today doesn’t break the streak until the day has passed. Days outside the weekdays neither extend nor break it.
- **Flexible streak:** the number of consecutive periods that met the target, counting back from the current period. The current period counts only once its target is met; until then it neither extends nor breaks the streak. Periods that began before the habit was created are ignored.
- **Weekday completion rate:** scheduled days marked done divided by scheduled days in the last 30 days, ignoring days before creation.
- **Flexible completion rate:** periods whose target was met divided by elapsed periods, over 12 weeks, 6 months, or 3 years. The current period is included.
- Weekday calculations use the 90-day window and display “90+” at the edge. Flexible streaks are capped by their period window and display a “+” when they fill it.
- One check-in per day counts; extra check-ins are allowed but do not extend a flexible streak.
- The logic runs in the client and must behave identically on Android and web. Unit tests cover at least: a daily habit, a weekday-only habit across a weekend, today not yet done, a gap in the middle of a streak, a habit created three days ago, a weekly target with the current period unmet, met weeks/months, an unfinished earlier period, and recurrence-aware history windows.

## 8. Reminder (Android only)

Optional and off by default. When `reminderTime` is set, an AlarmManager alarm runs a daily check at that local time. It counts the habits that still need attention: weekday habits scheduled today with no check-in, and flexible habits whose period target is unmet. If the count is above zero it posts one notification such as “2 habits still need a check-in”, and if everything is done it posts nothing. Tapping the notification opens the Today screen.

- Request the notification permission on Android 13 and later when the reminder is enabled.
- Use an exact alarm when the system allows them, with an inexact alarm as fallback.
- Reschedule when the reminder time changes, after sign-in, after each delivery, and after a reboot.
- Alarm timing may be approximate when exact alarms are unavailable, which is acceptable here.

The web client has no reminders in v1 and only shows the “x of y done” summary, which also appears in the browser tab title, for example “(2) Habits”, while habits remain unchecked.

## 9. Offline and sync

- Android: keep Firestore’s default offline persistence on.
- Web: enable persistent local cache when initialising Firestore.
- Use real-time snapshot listeners so a change on one device appears on the other.
- Writes made offline sync when the connection returns. Last write wins, which is fine for a single user.

## 10. Project structure

Use one repository with three top-level folders:

- `/firebase`: `firestore.rules`, `firestore.indexes.json` (no custom indexes are needed), `firebase.json`
- `/android`: the Java app
- `/web`: the React app

**Android layout.** Packages for `ui` (Compose screens, navigation, and an `AndroidViewModel`), `data` (the `Habit`, `HabitLog`, and `Preferences` model classes plus repositories wrapping Firebase Auth and Firestore and exposing Flow), `logic` (pure Kotlin date and streak code with no Android dependencies, so it can be unit tested), and `reminders` (the AlarmManager scheduler, notification helper, and receivers). The model classes are Kotlin data classes whose properties all have defaults so Firestore can map them, with `@DocumentId` and `@ServerTimestamp` marking the special fields.

**Web layout.** `src/firebase.ts` for initialisation, `src/hooks/useHabits.tsx` for the data provider, `src/lib/streaks.ts` for the streak logic as pure functions with tests, `src/pages/` for screens, `src/components/` for shared pieces, and `src/styles.css` for the shared dark design tokens.

## 11. Setup steps for the owner

These are done by you in the Firebase console and on your machine, not by the coding agent:

1. Create a new Firebase project on the free plan, separate from any other app.
2. Enable Authentication with the email and password provider, and Google if wanted.
3. Create a Firestore database in production mode. Choose a region close to you, because it can’t be changed afterwards.
4. Register the Android app with its package name (add the SHA-1 fingerprint if using Google sign-in) and download `google-services.json` into `android/app/`.
5. Register a web app and copy its configuration values into `web/.env` as `VITE_` variables.
6. Install the Firebase CLI and deploy the rules from `/firebase`.
7. Install Wrangler for the Cloudflare account and deploy the web build with `cd web && npm run deploy`.

## 12. Suggested build order

Building the web client first makes the data model easy to test before the Android work begins.

1. **Foundations:** repository skeleton, Firebase configuration, security rules.
2. **Streak logic:** the pure streak functions with their unit tests, so both clients can follow the same rules.
3. **Web:** sign in, Today, add and edit, detail, archive, settings, JSON export.
4. **Android:** the same screens, with the streak calculator ported and tested against the same cases.
5. **Android reminder:** the daily AlarmManager check and the settings control.
6. **Verification:** walk through the acceptance criteria below on both clients.

## 13. Acceptance criteria

- A new user can register, sign in, and sign out on both clients.
- A habit created on one client appears on the other within a few seconds.
- Marking today done on one device shows as done on the other, and unmarking removes it.
- Marking the same habit and day on two devices produces a single check-in document.
- A check-in made offline appears on the other client after the connection returns.
- A habit scheduled Monday to Friday keeps its streak across a weekend.
- A 3×/week habit shows “2 of 3 this week”, counts a streak only after meeting the target, and requires no specific weekday.
- Monthly and yearly habits use calendar periods and load enough history for their streak and completion stats.
- An unchecked habit today doesn’t reset the streak until the day has ended.
- A forgotten day within the last 90 days can be filled in from the habit detail screen.
- Archived habits disappear from Today, keep their history, and can be restored.
- With the reminder on, Android posts a notification only when at least one habit still needs a check-in.
- Exported JSON contains every habit and check-in with all fields.
- Security rules block one user from reading another user’s data, verified in the emulator.

## 14. Notes and risks

- **Free tier limits can change.** Loading only the history each recurrence needs (90 days for weekday and weekly habits, more for monthly and yearly) keeps Firestore reads low, but check Firebase’s pricing page for the current free-tier read and write limits.
- **Separate project, separate account.** Because this app has its own Firebase project, you will register and sign in to it separately from any other app of yours.
- **Backups.** Export to JSON now and then, since the free plan offers no managed backups of its own.
- **Keep it light.** If a feature adds friction to ticking off a habit, leave it out of v1.

## 15. Implementation decisions

Recorded November 23, 2026, so the spec matches what was built.

- Android is Kotlin with Jetpack Compose and Material 3. ViewModel state is exposed as StateFlow. The theme is dark-only.
- Habits support fixed weekdays and “N times per week/month/year” targets. A check-in counts per day; extra check-ins are allowed. Flexible streaks count consecutive met calendar periods.
- The web client is TypeScript. Both clients share the same dark design tokens (near-black surfaces, mint accent, warm streak colour).
- Reminders use AlarmManager with a notification receiver, a boot receiver, and a locally cached reminder time, not WorkManager.
- Google sign-in is implemented on both clients. The stable debug keystore's SHA-1 and SHA-256 are registered with Firebase through the CLI; `google-services.json` was regenerated afterwards.
- The web app deploys to Cloudflare Pages with `npm run deploy` (Wrangler). Firebase Hosting is not configured.
- Tests: 34 web streak/recurrence tests, 27 Android unit tests (the same cases, including the 90+ cap, creation-date rules, met/unmet periods, and history windows), and 7 Firestore emulator tests for the security rules.
- Live web app: <https://habit-tracker-b1d88.pages.dev>

