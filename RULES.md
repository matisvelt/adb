# RigSort Rules (v0.2)

## Non‑Negotiable
- JavaFX UI only.
- ADB usage must go through `ADBService` only.
- `DeviceManager` owns polling, pinging, registry updates.
- Offline‑first: no network calls except ADB.
- Phones never mount SSD; originals moved only by host.
- Always produce audit records for file moves.
- Provide undo for recent moves.

## Scope
- Device monitoring + inference dispatch + deterministic rules + review + safe move.

## Non‑Goals
- No on‑device storage of originals.
- No cloud services.
- No unrelated simulation modules.
