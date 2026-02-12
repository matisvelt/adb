# RigControl v0.1

RigControl is a JavaFX desktop app for monitoring headless Android compute boards via ADB. v0.1 focuses on discovery, live status, and health visibility.

## What It Does (v0.1)
- Discovers devices with `adb devices -l` on a fixed interval.
- Maintains a device registry keyed by serial.
- Displays live device status, model, Android version, and timestamps.
- Pings devices in parallel with configurable concurrency and backoff.
- Exposes ADB health, scan duration, and telemetry in the UI.
- Logs to a local file and shows a live console.

## Project Structure
- `src/main/java/com/antlab/rigcontrol/App.java`
  - JavaFX entry point. Builds the UI and binds to model properties.
- `src/main/java/com/antlab/rigcontrol/DeviceManager.java`
  - Orchestrates discovery, pinging, diffing, backoff, and rate limits.
- `src/main/java/com/antlab/rigcontrol/ADBService.java`
  - Executes ADB commands with timeouts and captures stdout/stderr.
- `src/main/java/com/antlab/rigcontrol/DeviceInfo.java`
  - Device model with JavaFX properties for binding.
- `src/main/java/com/antlab/rigcontrol/Settings.java`
  - Preferences-backed settings storage.
- `src/main/java/com/antlab/rigcontrol/SettingsDialog.java`
  - Settings UI for ADB path and timing controls.
- `src/main/java/com/antlab/rigcontrol/LogService.java`
  - File logging + in-app log list.
- `src/main/resources/app.css`
  - CIA-style dark UI theme.
- `src/main/resources/samples/adb_devices_sample.txt`
  - Demo ADB output for simulation mode.

## Event Flow (Each Key Event)
1. **Startup**
   - `App` initializes services and starts `DeviceManager`.
   - UI binds to observable device list and status properties.

2. **Discovery Tick**
   - `DeviceManager.pollDevices()` runs on the scheduler.
   - ADB call executes via `ADBService` (rate-limited).
   - Output parsed into devices; registry is updated.
   - UI list refreshes via JavaFX properties.

3. **Ping Tick**
   - `DeviceManager.autoPing()` schedules per-device pings.
   - Concurrency is capped by a semaphore.
   - Backoff is applied per device on failures.

4. **Ping Result**
   - Device ping status is updated (`OK`/`FAIL`).
   - Telemetry counters update ping success rate.

5. **Errors**
   - Errors update `lastError` and the ADB health badge.
   - Scheduler tasks are wrapped to avoid dying on exceptions.

6. **Settings Update**
   - The Settings dialog persists values to Preferences.
   - `DeviceManager.applySettings()` reschedules intervals.

7. **Demo Mode**
   - Demo button copies sample output to `~/.rigcontrol/demo`.
   - Simulation mode reads from that file instead of real ADB.

## UI Panels
- **Top Bar**: Rescan, Ping All, Demo Data, Copy CSV, Settings, Legend, Help
- **Center**: Device table
- **Bottom**: Logs, telemetry, last error, status line

## Settings
- ADB path
- Polling interval
- Ping interval
- ADB timeout
- Ping concurrency
- Ping backoff (step/max)
- ADB rate limit (tokens per interval)
- Simulation toggle + file path

## Running
```bash
cd "/Users/mac/Desktop/ADB connector"
JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" gradle run
```

## Notes
- Unauthorized devices require ADB key approval on the device.
- Demo mode avoids any real ADB usage.

