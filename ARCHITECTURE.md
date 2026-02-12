# Architecture

## Layers
- UI layer: JavaFX, binds to observable device list and properties.
- DeviceManager: schedules polling/pinging and updates the registry.
- ADBService: runs adb commands with timeouts and captures stdout/stderr.
- Models: DeviceInfo + DeviceStatus.

## Threading Model
- UI updates always on JavaFX thread via `Platform.runLater`.
- Device polling and pinging on background scheduler and executor pools.
- ADB I/O on a dedicated cached thread pool.

## Data Flow
1. Scheduler triggers `adb devices -l`.
2. Output is parsed to `ParsedDevice` objects.
3. Registry is updated and diffed against prior scan.
4. UI list is updated via observable bindings.
5. Pings run in parallel and update device ping status.

## Extension Points
- ADBService is the only execution path for ADB commands.
- DeviceManager is the orchestration hub; future phases extend here.
- UI is bound to models; additional tabs/panels can subscribe to the same observable state.
