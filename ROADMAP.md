# RigControl Roadmap (Deep)

This roadmap keeps the system safe for a headless compute rig, while progressively adding control, telemetry, and job execution. Each phase is small, testable, and isolates risk.

## Phase 0.2 – Worker Readiness Checks
**Goal:** Confirm each board is ready to accept jobs without manual interaction.

**What gets added**
- A readiness probe (per device) that verifies:
  - Worker service installed and running.
  - Worker version matches required minimum.
  - Free space threshold (ex: > 2GB).
  - CPU load and temperature within limits.
- A readiness badge in the UI with three states: `OK`, `WARN`, `FAIL`.
- A “Readiness” column in the device table.

**Why it matters**
- Prevents dispatching jobs to unhealthy devices.
- Allows fast isolation of problematic boards.

**Acceptance criteria**
- Readiness probes run on a timer without UI blocking.
- Devices can be filtered by readiness state.

## Phase 0.3 – Job Dispatch Skeleton
**Goal:** Prove end‑to‑end job transfer and execution without real work.

**What gets added**
- A Job definition schema (JSON).
- `adb push` to place job file on device.
- `adb shell` command to start a dummy job.
- `adb pull` to retrieve a result file.
- A simple Job History panel.

**Why it matters**
- Establishes the critical path for all future computation.
- Validates file transfer and command execution under load.

**Acceptance criteria**
- Jobs can be dispatched to 1 or many devices.
- Result files are retrieved reliably and shown in UI.

## Phase 0.4 – Watchdog & Recovery
**Goal:** Keep the rig stable under disconnects and failed jobs.

**What gets added**
- A per‑device watchdog to detect hung workers.
- Automatic restart of the worker service.
- A retry strategy with exponential backoff.
- Degraded status if failures persist.

**Why it matters**
- Rigs are physical systems; failure and churn are normal.
- Automatic recovery avoids manual maintenance.

**Acceptance criteria**
- Stuck jobs recover automatically.
- Devices move to degraded state after repeated failures.

## Phase 1.0 – Monte Carlo Batch Execution
**Goal:** Run Monte Carlo workloads across devices and aggregate results.

**What gets added**
- Batch planner that splits sample sets across devices.
- Scheduler that balances load based on device readiness.
- Aggregate results with mean/variance/quantiles.
- Visual distribution charts in UI.

**Why it matters**
- Monte Carlo is a natural parallel workload and a real rig use‑case.

**Acceptance criteria**
- A batch can run end‑to‑end with accurate aggregation.
- Results are reproducible with fixed seeds.

## Phase 2.0 – Neural Inference Layer
**Goal:** Add fast inference to approximate Monte Carlo results, with validation.

**What gets added**
- Model registry for versioned TFLite models.
- Deployment of models to devices.
- Inference job execution and latency tracking.
- Calibration checks against Monte Carlo samples.

**Why it matters**
- Inference provides rapid estimates with confidence bounds.
- Monte Carlo can act as a validator and fallback.

**Acceptance criteria**
- Models deploy cleanly to all ready devices.
- Inference results are shown with calibration confidence.

## Phase 3.0 – Linux Portability
**Goal:** Ensure the same workflow on Linux rigs with minimal friction.

**What gets added**
- Linux packaging and launch scripts.
- Verified ADB path handling on Linux.
- Validation of hub behavior and device enumeration on Linux.

**Why it matters**
- Portability expands the rig options and deployment flexibility.

**Acceptance criteria**
- Linux build runs without special-case code.
- Same UI and scheduling behavior as macOS.
