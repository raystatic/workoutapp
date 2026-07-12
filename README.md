# Workout App

A Hevy-class workout tracking + planning app with a social layer, built as a
**Compose Multiplatform** (Kotlin Multiplatform) app for **Android and iOS**.

- 📋 **[Execution Plan](EXECUTION_PLAN.md)** — roadmap, tech stack, phasing.
- 🔁 **[Development Workflow](docs/DEVELOPMENT_WORKFLOW.md)** — how issues,
  branches, the routine, and CI fit together.

## How we build

Every piece of work starts as a **GitHub issue**. A routine processes ready
issues one at a time: branch → implement (with unit + instrumentation tests) →
PR → review → merge. Progress is tracked on the GitHub Project board.

## CI/CD

- Push to any branch (except `main`) or open a PR → **unit tests** run.
- Merge to `main` → **full pipeline**: unit tests → instrumentation tests
  (Android emulator + Firebase Local Emulator Suite) → build.

## Testing

All code is testable. Unit tests cover shared/domain logic; instrumentation
tests cover UI and platform integration and run against the **Firebase Local
Emulator Suite** (`firebase.json`), never production Firebase.

## Status

The Gradle/Compose Multiplatform project is scaffolded (`composeApp` shared
module, `androidApp`, `iosApp`) and CI is live against it. Feature work now
proceeds issue by issue on top of this base.
