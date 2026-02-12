# Data Model

## DeviceInfo
- `serial`: ADB serial number (primary key)
- `adbState`: device/offline/unauthorized/disconnected/unknown
- `model`: `ro.product.model` (optional)
- `androidVersion`: `ro.build.version.release` (optional)
- `lastSeen`: timestamp string in local time
- `pingStatus`: OK/FAIL/-
- `lastPing`: timestamp string in local time

## Planned Models (Future)

### WorkerStatus (Phase 0.2)
- `workerInstalled`: boolean
- `workerVersion`: string
- `freeSpaceMb`: number
- `cpuLoadPct`: number
- `temperatureC`: number
- `readinessState`: OK/WARN/FAIL

### JobDefinition (Phase 0.3)
- `jobId`: string
- `payloadPath`: string
- `createdAt`: timestamp
- `status`: QUEUED/RUNNING/COMPLETE/FAILED

### RunSummary (Phase 1.0)
- `batchId`: string
- `samples`: int
- `mean`: double
- `variance`: double
- `percentiles`: map
- `startedAt`: timestamp
- `completedAt`: timestamp
