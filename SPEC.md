# RigSort v0.2 Spec

## Scope
- Offline, privacy‑local SSD sorting with ADB‑connected Android workers.
- Host scans a Source Root, builds a manifest, generates previews, dispatches jobs, applies rules, and safely moves originals.
- Phones never mount the SSD; devices only receive preview bytes and return metadata/labels.

## Supported Files (v0.2)
- Images: jpg/jpeg/png/heic/webp
- Documents: pdf
- Text: txt/docx (basic)

## Pipeline
1. Scan source root recursively, build manifest entries with stable FileId.
2. Generate preview on host (max long edge, WEBP preferred). Apply EXIF rotation.
3. Dispatch preview batches to devices via ADB forward → local HTTP.
4. Receive labels + metadata only.
5. Apply deterministic rules; low‑confidence items go to Review.
6. Move originals safely; audit every move.
7. Undo uses audit log.

## Labels
- PHOTO_PEOPLE
- PHOTO_NO_PEOPLE
- SCREENSHOT
- DOCUMENT_INVOICE
- DOCUMENT_OTHER
- UNKNOWN

## Safety
- No cloud services or external network calls beyond ADB.
- Originals moved only by host.
- Crash‑safe move strategy (atomic where possible, copy+verify fallback).
- Audit log + undo.

## Non‑Goals
- No screen mirroring or device UI control.
- No on‑device storage of originals.
- No third‑party online LLMs.
