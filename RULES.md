# RigControl Rules

This document defines the non‑negotiable rules for this codebase. Use it as the single source of truth for scope, architecture, and safety.

## 1) Project Goals (v0.1)
- Discover devices via `adb devices -l` on a fixed interval.
- Maintain a device registry keyed by serial.
- Display live device status and metadata in JavaFX.
- Ping devices periodically and on demand.

## 2) Non‑Goals (v0.1)
- No screen mirroring, streaming, or UI on devices.
- No job dispatch or worker execution.
- No Qt or QtScrcpy code reuse, linking, or copying.

## 3) Hard Constraints
- JavaFX for UI.
- Gradle build with all files present in repo.
- macOS now, Linux later; avoid OS‑specific APIs.

## 4) Architecture Boundaries
- UI never executes ADB directly.
- ADB commands go through `ADBService` only.
- `DeviceManager` owns scheduling, polling, pinging, and registry updates.

## 5) Concurrency & Performance
- Never block the JavaFX thread.
- All ADB calls must have timeouts.
- Respect ADB rate limits and ping concurrency caps.
- Recover gracefully from timeouts and disconnects.

## 6) Device Handling Rules
- Devices are headless compute workers.
- No manual interaction required at runtime.
- Registry is keyed by serial number only.
- Handle `device`, `offline`, `unauthorized`, `disconnected` states.

## 7) Logging & Observability
- Write logs to a local file and show a live UI log.
- Surface last error in the UI.
- Keep telemetry visible (poll counts, ping success rate).

## 8) Settings & Persistence
- ADB path must be user‑configurable.
- Store settings locally using Preferences.
- Persist window size and key UI preferences.

## 9) Safety & Security
- ADB keys remain local to the host.
- No data exfiltration or network calls beyond ADB.
- Avoid storing sensitive data in logs.

## 10) Roadmap Discipline
- v0.1 only implements discovery + ping + UI.
- Later phases must be additive and isolated.
- Do not implement Phase 0.2+ behavior without explicit approval.

## 11) Definition of Done (Per Phase)
- Each phase must define acceptance criteria before implementation.
- A phase is “done” only when criteria are met on real hardware.

## 12) Operational SLAs
- UI remains responsive under 18 devices with continuous polling.
- Poll and ping operations must complete within configured timeouts.
- ADB command rate stays within configured limits.

## 13) Data Retention
- Log files are stored locally and can be deleted by the operator.
- Exported snapshots are opt‑in only.
- Do not persist sensitive device data unless required.

## 14) Versioning & Compatibility
- Future worker protocol changes must be versioned.
- UI should display worker version mismatches clearly.
- Backwards‑compatible behavior is required unless a migration plan exists.

## 15) Rig Safety Constraints
- No device reboots or destructive operations without explicit operator action.
- Recovery routines must default to non‑destructive steps first.
