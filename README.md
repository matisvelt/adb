# RigSort v0.2

RigSort is an offline, privacy‑local JavaFX desktop app that extends the RigControl ADB monitor into a distributed SSD sorting pipeline. It scans a source folder, generates on‑host previews, dispatches inference jobs to Android SD888 workers via ADB port‑forwarding, applies deterministic rules, and safely moves original files with audit + undo.

## Core Principles
- Offline‑first. No network calls except ADB.
- Phones never mount the SSD and never move originals.
- Previews are generated on the host and dispatched to devices.
- Deterministic rules + human review before risky moves.
- Crash‑safe moves + audit trail + undo.

## What’s In This Repo
- Desktop JavaFX app (RigSort v0.2)
- Android worker app (RigSort Worker)

## Desktop App
### Requirements
- Java 17
- Gradle
- ADB installed and on PATH (or set ADB path in Settings)

### Run
```bash
cd "/Users/mac/Desktop/ADB connector"
JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" gradle run
```

### UI Layout
- Ribbon toolbar (Project, Scan, Rules, Move, Settings)
- Left nav (Monitor, Sorter, Rules, Review, Audit)
- Center table view (spreadsheet‑style)
- Right inspector (preview + metadata)
- Bottom status bar + collapsible logs

Sorter controls include batch size and max in‑flight jobs.

### Project Structure
A RigSort project is a folder containing:
- `project.json` (settings + rules)
- `manifest.jsonl` (file records)
- `audit.jsonl` (move history)
- `.rigsort/cache/previews/` (preview files)

### Supported File Types
- Images: jpg, jpeg, png, heic, webp
- Documents: pdf
- Text: txt, docx (treated as text)

HEIC/WebP support uses ImageIO plugins (nightmonkeys + luciad). If a platform lacks native decoders, the preview step will fall back to JPEG or mark the preview as unsupported.

### Preview Policy (Host‑side)
- Max long edge: default 1536px
- Format: WEBP preferred (JPEG fallback)
- Quality: default 85
- Previews are disposable unless “Keep previews” is enabled in `project.json`

### Rules Engine
Rules are simple boolean expressions:
- `label == DOCUMENT_INVOICE`
- `hasTextLikelihood > 0.7`
- `facesCount >= 1 && confidence >= 0.75`
- `label startsWith PHOTO`

Default rules (editable):
- `DOCUMENT_INVOICE` → `Work/Invoices`
- `DOCUMENT_OTHER` or `hasTextLikelihood > 0.7` → `Work/Documents`
- `SCREENSHOT` → `Work/Screenshots`
- `PHOTO_*` + faces → `Private/Family/People`
- `PHOTO_*` + no faces → `Private/Photos/Other`

Low‑confidence or UNKNOWN results go to Review.

### Safe Moves + Undo
- Uses atomic move when possible; fallback is copy + verify + delete.
- Writes audit log entries for every move.
- Undo uses the audit log to move files back.

## Android Worker App
The worker listens on device loopback `127.0.0.1:18080` and exposes:
- `GET /health`
- `POST /classifyBatch`

The worker runs as a foreground service so it can run headless (no display required).

### Build / Install
```bash
cd "/Users/mac/Desktop/ADB connector/android-worker"
gradle :app:assembleDebug
adb -s <SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <SERIAL> shell am start-foreground-service -n com.rigsort.worker/.WorkerService
```

### Port Forwarding
RigSort uses ADB port forwarding:
```
adb -s <SERIAL> forward tcp:18xxx tcp:18080
```
The desktop app automatically forwards a port per device before calling the worker.

### Worker Inference
- Face detection: ML Kit (local)
- Text recognition: ML Kit (local)
- Invoice detection: keyword heuristic on OCR text

The worker returns metadata only (no images stored or returned).

## Demo Walkthrough (Quick)
1. Create a new project in the desktop app.
2. Choose Source Root (SSD) + Destination Root.
3. Scan to build the manifest.
4. Start processing. Devices will run inference via ADB forward.
5. Review low‑confidence items and apply manual moves.
6. Use Audit tab to undo if needed.

## Tests
```bash
gradle test
```
Covers:
- Rules parsing/evaluation
- Preview scaling
- Move conflicts + undo

## Notes
- The desktop app keeps ADB commands in `ADBService` only (single execution gate).
- Device polling and pinging remain owned by `DeviceManager`.
- All analytics/simulation modules were removed for RigSort.
