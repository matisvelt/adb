# Architecture (RigSort v0.2)

## Layers
- UI: JavaFX (ribbon, table view, inspector, status/log).
- ADB Layer: `ADBService` (single execution gate).
- Device Orchestration: `DeviceManager` (polling, pinging, registry).
- Sorter Core:
  - ProjectManager (project config + manifest + audit)
  - FileScanner (manifest entries)
  - PreviewGenerator (host‑side previews)
  - WorkerClient + WorkerMonitor (ADB forward + HTTP)
  - Dispatcher (batching, retries, backoff)
  - RulesEngine (deterministic routing)
  - FileMover (safe move + undo)

## Data Flow
1. Scan builds manifest.
2. Previews generated on host cache.
3. Dispatcher sends preview batches via ADB forward.
4. Worker returns labels + metadata.
5. Rules applied; low confidence → Review.
6. FileMover moves originals + audit.

## Concurrency
- UI thread never blocked.
- ADB calls only via ADBService.
- Dispatcher uses background executor + backoff.
