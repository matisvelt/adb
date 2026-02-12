# Operations

## Logs
- Location: `~/.rigcontrol/logs/`
- Each run writes a timestamped file.

## Troubleshooting Checklist
1. `adb devices -l` lists expected devices.
2. Unauthorized devices require ADB key approval.
3. If polling shows all disconnected, check ADB path in settings.
4. If pings fail, verify device is in `device` state.
5. For demo/testing without hardware, enable simulation and set a file path.

## Safe Restart
- Close the app window.
- Reopen with `gradle run`.
