# RigControl v0.1 Spec

## Scope
- Discover Android devices via `adb devices -l` every 2–3 seconds.
- Maintain a device registry keyed by serial number.
- Display device list with serial, ADB state, model, Android version, last seen, ping status, last ping.
- Ping devices via `adb -s <serial> shell echo PING` every ~10 seconds and on demand.
- Provide a simple settings panel to change ADB path and timing intervals.
- Optional simulation mode that reads a saved `adb devices -l` output file.
- UI affordances for ADB health, scan duration, CSV export, and filtering disconnected devices.
- ADB rate limit guard to prevent command storms.
- Exponential-like ping backoff with a maximum cap.
- Persisted window size.

## Non-goals
- No screen mirroring, streaming, or device UI interactions.
- No job dispatch or worker orchestration in v0.1.

## Device States
- `device`: online and responding to ADB.
- `offline`: ADB sees the device but it is not responsive.
- `unauthorized`: ADB key not authorized on the device.
- `disconnected`: previously seen, no longer present in latest scan.

## Timing
- Discovery poll interval: default 3 seconds (min 1).
- Ping interval: default 10 seconds (min 3).
- ADB call timeout: default 5 seconds (min 2).
- Ping concurrency: default 6 (min 1).
- Ping failure backoff: default 10 seconds (min 2).
- Ping backoff step: default 10 seconds (min 5).
- Ping backoff max: default 120 seconds (min 10).
- ADB rate limit: default 20 commands per 5 seconds.

## Error Handling
- ADB timeouts mark ping as FAIL and leave device in last known ADB state.
- Failure of `adb devices -l` marks all devices as `disconnected` until the next successful poll.

## Persistence
- Settings stored locally via Java Preferences.
- Logs written to `~/.rigcontrol/logs/` with timestamped filenames.
