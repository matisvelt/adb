# Operations (RigSort v0.2)

## Logs
- Desktop logs: `~/.rigsort/logs/`

## Troubleshooting
1. Verify `adb devices -l` shows expected devices.
2. If worker health fails, ensure the worker app is running on the device.
3. Forward a port manually: `adb -s <SERIAL> forward tcp:18090 tcp:18080`.
4. Check ADB path in Settings.

## Safe Restart
- Close the app window.
- Reopen with `gradle run`.
