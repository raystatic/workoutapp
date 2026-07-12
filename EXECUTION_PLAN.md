# Workout App — Execution Plan

> A Hevy-class workout tracking + planning app with a social layer.
> Built as a **Compose Multiplatform** (Kotlin Multiplatform) app for **Android and iOS**.
> Strategy: **logging-first MVP, social later.**

This plan translates the Hevy feature review into an engineering roadmap: a tech
stack, architecture, data model, and a phased backlog mapped to the report's
Core User Journeys (CUJs), gaps, and KPIs.

---

## 1. Guiding Principles

1. **Logging is the retention engine.** Everything in Phase 1 serves fast,
   frictionless, offline-first workout logging. Social and analytics are
   built on top of a rock-solid logging core, not alongside it.
2. **Offline-first, sync-second.** A gym has bad signal. All writes hit local
   storage instantly; sync is a background reconciliation, never a blocker.
3. **Share code aggressively, fork UI only where the platform demands it.**
   Business logic, data, and most UI live in shared Kotlin. Platform-native
   code is reserved for HealthKit/Google Fit, watch apps, camera, and share
   sheets.
4. **Instrument from day one.** Every CUJ from the report ships with the KPIs
   defined for it so we can run the proposed A/B tests early.
5. **Free tier stays generous.** Monetization gates advanced/unlimited
   features, never core logging — mirroring Hevy's low-friction acquisition.

---

## 2. Technology Stack

### Shared (Kotlin Multiplatform)
| Concern | Choice | Rationale |
|---|---|---|
| UI | **Compose Multiplatform** | One UI codebase for Android + iOS |
| Architecture pattern | **MVI / Unidirectional data flow** | Predictable state for complex logging screens |
| Async | Kotlin Coroutines + Flow | Reactive local DB → UI |
| DI | **Koin** | KMP-friendly, low ceremony |
| Local DB | **SQLDelight** | Type-safe SQL, multiplatform, reactive queries |
| Networking | **Ktor Client** + kotlinx.serialization | Multiplatform HTTP + JSON |
| Key-value/prefs | **multiplatform-settings** | Timers, flags, onboarding state |
| Navigation | **Compose Navigation (multiplatform)** or Decompose | Shared nav graph |
| Date/time | kotlinx-datetime | Timezone-correct workout timestamps |
| Image loading | **Coil 3 (multiplatform)** | Exercise GIFs, progress photos |
| Charts | Compose-native charts (custom canvas) | Analytics graphs render locally |

### Platform-native (thin layers behind shared interfaces)
- **Android:** Health Connect, Wear OS companion (Compose for Wear), camera/photo picker, share intents, WorkManager for sync.
- **iOS:** HealthKit, Apple Watch app (SwiftUI/WatchKit — separate target), PhotosPicker, share sheet, BGTaskScheduler for sync.

### Backend (needed once we cross into cloud sync + social)
| Concern | Choice | Rationale |
|---|---|---|
| API | **Ktor server (Kotlin)** or Supabase | Reuse Kotlin models end-to-end |
| DB | PostgreSQL | Relational workout data + social graph |
| Auth | OAuth (Apple/Google) + email | Standard mobile auth |
| Object storage | S3-compatible | Progress photos, workout media |
| Push | FCM + APNs | Social + rest-timer notifications |

> **Decision deferred:** Build-your-own Ktor backend vs. BaaS (Supabase/Firebase).
> Phase 1 MVP is **local-only**, so this can be decided at the Phase 2 boundary
> without blocking logging work.

---

## 3. Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│  composeApp (shared)                                       │
│  ├── ui/            Compose screens + MVI ViewModels        │
│  ├── domain/        Use cases, entities, business rules     │
│  ├── data/          Repositories, SQLDelight, Ktor, sync    │
│  └── di/            Koin modules                            │
├──────────────────────────────────────────────────────────┤
│  androidApp            │  iosApp (SwiftUI host)             │
│  - Health Connect      │  - HealthKit bridge               │
│  - Wear OS module      │  - Apple Watch target             │
│  - Camera / share      │  - Photos / share sheet           │
└──────────────────────────────────────────────────────────┘
```

- **Repositories expose Flows** off SQLDelight so the UI reacts to local writes
  instantly. Sync workers write to the same tables; the UI never distinguishes
  "local" vs "synced" data.
- **`expect`/`actual`** interfaces isolate platform APIs (health, media, share,
  notifications) so shared code stays pure.
- **Feature-module boundaries** map to CUJs: `logging`, `routines`, `library`,
  `progress`, `social`, `profile`.

---

## 4. Data Model (core entities)

```
Exercise         id, name, primaryMuscle, secondaryMuscles[], equipment,
                 mediaUrl, isCustom, instructions
Routine          id, name, folderId, order, notes
RoutineExercise  id, routineId, exerciseId, order, supersetGroup, restSeconds, notes
RoutineSet       id, routineExerciseId, targetReps, targetWeight, setType
Workout          id, name, startedAt, finishedAt, note, privacy, media[]
WorkoutExercise  id, workoutId, exerciseId, order, supersetGroup, notes
WorkoutSet       id, workoutExerciseId, reps, weight, durationSec, rpe,
                 setType(NORMAL|WARMUP|DROP|FAILURE), completed
BodyMeasurement  id, takenAt, type(weight|bodyFat|circumference...), value, photoUrl
PersonalRecord   id, exerciseId, type(1RM|maxWeight|bestVolume...), value, workoutId
UserProfile      id, displayName, avatar, isPublic, streak, proUntil
-- Phase 3 (social)
Follow           followerId, followeeId
FeedPost         id, workoutId, authorId, createdAt
Like / Comment   ...
```

Local schema ships in Phase 1; the `syncStatus` + `updatedAt` + `serverId`
columns are added on every table now (cheap) so Phase 2 sync is additive, not a
migration nightmare.

---

## 5. Phased Roadmap

Effort is relative sizing (S/M/L/XL), not a committed calendar. A rough
solo/small-team calendar is sketched in §7.

### Phase 0 — Foundation (Weeks 1–2)  `L`
Goal: a running CMP app on both platforms with the plumbing in place.
- [ ] KMP + Compose Multiplatform project scaffold; Android + iOS targets build.
- [ ] Koin DI, SQLDelight setup, base MVI scaffolding, navigation graph.
- [ ] Design system: theme (light/dark), typography, spacing, core components
      (buttons, inputs, list rows, bottom tab bar).
- [ ] Seed exercise library (400+) as a bundled DB asset (name, muscles,
      equipment, media reference).
- [ ] CI: build both targets, run tests, lint (Detekt/ktlint).

### Phase 1 — Logging-First MVP (Weeks 3–9)  `XL`
Goal: a person can create routines and log real workouts, fully offline. This is
the shippable MVP.

**CUJ 1 & 2 — Log Workout (routine + empty)**
- [ ] Workout tab: routines list (folders), "Start Empty Workout" CTA.
- [ ] Active workout screen: running duration timer; per-exercise set rows.
- [ ] Set entry: weight/reps/duration inputs, mark-complete ✓, add/remove set
      (swipe to delete), set types (Normal/Warm-up/Drop/Failure).
- [ ] **"Previous" values** shown per set (last time you did this exercise).
- [ ] Supersets: group exercises via ••• menu; UI progresses through pairs.
- [ ] **Rest timer**: auto-start on set complete; global + per-exercise defaults;
      background + local notification when it ends.
- [ ] Finish/Save screen: name, date/time/duration edit, note, privacy flag,
      photo attach (media stored, social share deferred).
- [ ] Post-save summary: workout count, streak, PRs hit.

**CUJ 3 — Create/Edit Routine**
- [ ] Routine builder: name, folder, add exercises, default sets/reps/weight/rest,
      supersets, per-exercise notes, reorder.
- [ ] Edit/duplicate/delete routine.
- [ ] "Save last workout as routine" (addresses report's routine-friction gap).

**Exercise Library**
- [ ] Searchable/filterable library (equipment, muscle) in-app and inside the
      Add-Exercise flow.
- [ ] Exercise detail: instructions, media, per-exercise history (basic).
- [ ] Custom exercises (name, muscles, equipment, media) — free cap of 7.

**Utilities (report-called-out, cheap wins)**
- [ ] Plate calculator, warm-up set calculator.

**Onboarding (addresses report's "learning curve" gap)**
- [ ] First-run walkthrough: log a set, rest timer, start a routine.
- [ ] Empty states that teach (no routines → template suggestions).

**Instrumentation**
- [ ] Analytics events for Logging + Routine KPIs (§6).

**Phase 1 exit criteria:** offline logging is fast and reliable; a beta user can
run their real weekly training with it. Local-only, no account required.

### Phase 2 — Accounts, Sync & Analytics (Weeks 10–15)  `L`
Goal: data survives device loss; users get the motivating analytics.
- [ ] Auth (Apple/Google/email) + optional account (logging still works
      logged-out; account unlocks sync).
- [ ] Cloud sync engine (last-write-wins per record + conflict handling),
      background workers (WorkManager / BGTaskScheduler).
- [ ] Progress/Stats: sets-per-muscle, muscle-volume distribution, timeframe
      filters, per-exercise performance (1RM, best set/session volume, history
      graphs), strength-level estimate.
- [ ] Body measurements + progress photos with trend graphs.
- [ ] Calendar view + streaks.
- [ ] Health integration: HealthKit / Health Connect (opt-in).
- [ ] Instrumentation for Progress KPIs.

### Phase 3 — Social Layer (Weeks 16–22)  `L`
Goal: the differentiator — but built well to avoid the report's discovery gap.
- [ ] Public/private profiles; follow graph.
- [ ] Home feed (followed users' workouts); like, comment, save-routine.
- [ ] **Discovery done right (report's #1 high-priority gap):** goal/level-based
      suggestions, not random — filter by training goal, experience level,
      location/language.
- [ ] Athlete profile + PR "Compare" view.
- [ ] Share cards (Instagram/Strava) + Strava auto-post integration.
- [ ] Push notifications (new posts, comments, follows).
- [ ] Instrumentation for Social KPIs; run the discovery A/B test (goal-match vs
      random).

### Phase 4 — Platform Depth & Monetization (Weeks 23+)  `M`
- [ ] **Apple Watch + Wear OS** apps: live set logging, timer, HR.
- [ ] Pro subscription (StoreKit / Play Billing): unlimited routines, unlimited
      custom exercises, extended history / all-time stats.
- [ ] Contextual upsell (surface Pro exactly when a free user hits a limit —
      report's monetization recommendation).
- [ ] Accessibility pass: VoiceOver/TalkBack labels for dynamic set data
      ("8 reps, 100 kg"), ≥44pt touch targets, color-blind-safe set-type
      indicators (icon + label, not color alone), in-app text scaling.

---

## 6. KPIs & A/B Tests (instrumented per phase)

Directly from the report's per-CUJ metrics:

| Journey | KPIs | First A/B test |
|---|---|---|
| **Logging** | workouts started/completed, avg log time, abandonment, rest-timer usage | Default rest timer ON vs OFF |
| **Routine creation** | routines created, routines used in logs, save funnel | "Quick template" vs blank builder |
| **Library** | searches/session, filter usage, % workouts using library | Trending suggestions vs blank search |
| **Progress/Stats** | stats-open frequency, time in charts, PR shares | Flat single-screen vs tabbed stats |
| **Social** | DAU ratio, % following ≥1, likes/comments per user, invite conversion | Goal-matched vs random discovery |

Onboarding tutorial (on vs off) → measured against **D2/D7 retention**, the
single most important early signal for the logging MVP.

---

## 7. Rough Timeline (solo / small team)

```
Month 1     Phase 0 + start Phase 1 logging core
Month 2     Phase 1 — routines, library, rest timer, save flow
Month 3     Phase 1 — utilities, onboarding, polish → BETA (logging MVP)
Month 4     Phase 2 — auth + sync + core analytics
Month 5     Phase 2 finish + Phase 3 social start
Month 6     Phase 3 social + Discovery; public v1.0
Month 7+    Phase 4 — watch apps, Pro subscription, accessibility
```

Ship the **logging-MVP beta at the end of Month 3** and get real gym usage before
investing in sync/social.

---

## 8. Key Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Sync conflicts corrupt workout data | High | Offline-first, per-record versioning, add sync columns from day 1; ship local-only MVP before any sync |
| iOS Compose Multiplatform edge cases (share sheet, photos, watch) | Medium | Keep platform-specific surfaces behind `expect`/`actual`; Apple Watch is a native SwiftUI target, not CMP |
| Social cold-start (empty feed) — report's core social weakness | Medium | Delay social to Phase 3; launch with goal-based discovery + friend invites, not a random feed |
| Feature bloat overwhelming beginners (report heuristic finding) | Medium | Onboarding walkthrough + progressive disclosure of advanced controls (RPE, drop sets) |
| Exercise media/library licensing | Medium | Source/produce a licensed 400+ exercise media set early in Phase 0 |
| Scope creep into full parity before MVP | High | Hard gate: no analytics/social work until logging beta passes exit criteria |

---

## 9. Immediate Next Steps

1. Scaffold the Compose Multiplatform project (Android + iOS targets building).
2. Stand up SQLDelight schema for the Phase 1 entities (§4) with sync columns.
3. Source/seed the exercise library dataset.
4. Build the active-workout logging screen — the highest-value, highest-risk
   surface — first.
5. Wire logging + routine analytics events so KPIs exist from the first beta.
```
```

*This plan intentionally sequences the report's recommendations by leverage:
logging reliability → data permanence & analytics → the social differentiator →
platform depth & monetization.*
