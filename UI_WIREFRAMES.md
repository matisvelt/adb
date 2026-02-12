# UI Wireframes (Text)

## v0.1 Dashboard
- Top bar: `Rescan` `Ping all` `Settings` device count chip
- Center: device table
- Bottom: logs + status line

## Phase 1.0 Monte Carlo Dashboard

### Top Bar
- `Run Batch` `Pause` `Resume` `Stop` `Settings`
- Batch ID + overall state

### Left Panel: Batch Config
- Distribution selector
- Parameters input (mu/sigma or min/max)
- Sample count
- Seed strategy
- Output format + destination

### Center: Device Allocation
- Table: device, assigned samples, progress, ETA, error count
- Actions: `rebalance`, `drain`, `blacklist`

### Right Panel: Progress & Statistics
- Overall progress bar
- Live metrics: mean, variance, percentiles
- Histogram (simple bar chart)

### Bottom: Logs & Events
- Timeline of batch events
- Warnings and errors

## Phase 2.0 Neural Inference Dashboard

### Top Bar
- `Deploy Model` `Validate` `Run Inference` `Rollback`

### Left Panel: Model Registry
- Model list with version, hash, size, targets
- Status: deployed/validated

### Center: Inference Runs
- Table: run id, devices, accuracy, latency, status

### Right Panel: Confidence & Calibration
- Calibration curve
- Confidence distribution
- Accuracy/latency summary

### Bottom: Logs & Alerts
- Deployment logs
- Validation failures or drift alerts
