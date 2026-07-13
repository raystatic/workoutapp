# Development Workflow

This repo is built with an **issue-driven, routine-processed** workflow. Nothing
gets built without an issue, and issues are worked one at a time.

## The loop

```
        ┌────────────────────────────────────────────────────────┐
        │ 1. Create an issue for every piece of work              │
        │    (feature / bug / task / docs) using the templates.   │
        │    New issues start at status:pending.                  │
        └───────────────────────────┬────────────────────────────┘
                                    │
        ┌───────────────────────────▼────────────────────────────┐
        │ 2. Triage: when an issue is ready to be built, label it │
        │    status:ready. This is the queue the routine pulls    │
        │    from. Ordering: dependencies first, then priority,   │
        │    then issue number.                                   │
        └───────────────────────────┬────────────────────────────┘
                                    │
        ┌───────────────────────────▼────────────────────────────┐
        │ 3. The routine picks the next status:ready issue,       │
        │    moves it to status:in-progress, branches from main   │
        │    (feat/<issue#>-slug), implements it WITH unit +      │
        │    instrumentation tests, and opens a PR that Closes it. │
        └───────────────────────────┬────────────────────────────┘
                                    │
        ┌───────────────────────────▼────────────────────────────┐
        │ 4. PR runs Unit Tests CI. Human reviews + merges to     │
        │    main. Merge triggers the full Main CI/CD pipeline     │
        │    (unit -> instrumentation on Firebase emulator ->      │
        │    build). Merging closes the issue (status:done).      │
        └────────────────────────────────────────────────────────┘
```

## Branching

| Branch | Purpose |
|---|---|
| `main` | Default branch. Protected. Target of all merges. Full CI/CD runs here. |
| `feat/<issue#>-<slug>` | One branch per issue. Merges into `main` via PR. |
| `workout-app` | Long-lived integration/staging branch (mirror of the initial baseline). |

## CI/CD rules

- **Push to any branch except `main`, and any PR** → `Unit Tests` workflow only
  (fast feedback). See `.github/workflows/unit-tests.yml`.
- **Merge/push to `main`** → full `Main CI/CD` pipeline: unit tests →
  instrumentation tests (Android emulator + **Firebase Local Emulator Suite**) →
  build. See `.github/workflows/main-ci.yml`.

## Testing requirements (non-negotiable)

Every code change ships with tests:

- **Unit tests** — shared/domain logic (`commonTest`), Android unit tests
  (`testDebugUnitTest`). Fast, no device.
- **Instrumentation tests** — UI flows and platform integration
  (`connectedDebugAndroidTest`). Run on an Android emulator against the
  **Firebase Local Emulator Suite** (Auth, Firestore, Storage) so tests never
  touch production Firebase. Config lives in `firebase.json`; emulator host from
  the Android emulator is `10.0.2.2`. `FirestoreEmulatorSmokeTest` proves this
  wiring end to end (anonymous sign-in via the Auth emulator, then a Firestore
  document round-trip) and needs **no GitHub secrets** — the app talks to the
  emulator with a throwaway `FirebaseOptions` (fake project/app id, no real
  Firebase project involved). The only CI secrets in this repo are
  `FIREBASE_ANDROID_APP_ID` / `FIREBASE_SERVICE_ACCOUNT`, used solely by the
  unrelated `distribute-android` job (see `docs/APP_DISTRIBUTION.md`).

## Status labels (the board)

Issues are tracked on a GitHub Project board with these status labels:

| Label | Meaning |
|---|---|
| `status:pending` | Filed, not yet ready to build (needs triage/detail). |
| `status:ready` | Ready for the routine to pick up. |
| `status:in-progress` | Being worked on (branch/PR open). |
| `status:blocked` | Waiting on a dependency. |
| `status:done` | Merged to main. |

Type labels: `type:feature`, `type:bug`, `type:task`, `type:docs`,
`type:enhancement`, `type:epic`. Phase labels: `phase:0` … `phase:4`.

## GitHub Project board

Tracking happens on a **GitHub Project** (Projects v2) board — columns map to the
status labels above (Pending → Ready → In Progress → Done). See the repo's
Projects tab. (The board itself is created in the GitHub UI / via GraphQL; the
labels here keep issues sortable even outside the board.)
