# Test Plan

## Unit Tests (Future)
- Parse `adb devices -l` output across states.
- Timeout handling in ADBService.
- Ping parsing and status updates.

## Integration Tests (Future)
- Device discovery loop with mock ADB output.
- Settings updates and rescheduling behavior.

## Rig Soak Checklist
- 18 devices connected, continuous run for 6–12 hours.
- Inject disconnects and verify recovery.
- Measure UI responsiveness under load.
