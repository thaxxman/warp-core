# Warp Core eHMD — BLE Protocol Specification

## GATT Service Definition

| Item | UUID |
|---|---|
| Service | `4fafc201-1fb5-459e-8fcc-c5c9c331914b` |
| Write Characteristic (App → ESP32) | `beb5483e-36e1-4688-b7f5-ea07361b26a8` |
| Notify Characteristic (ESP32 → App) | `beb5483e-36e1-4688-b7f5-ea07361b26a9` |

## MTU

Request 512-byte MTU on connection. All messages fit well within this.

## Connection Parameters

- Request connection interval: 30-50ms (for responsive slider control)
- ESP32 advertises as: `Warp Core eHMD`

## Data Format

All messages are JSON. One command per BLE write. One status update per notify.

---

## App → ESP32 Commands (Write Characteristic)

### Set Target Temperature
```json
{"cmd":"set_temp","val":200}
```
- `val`: integer, 0–400 (Celsius)
- ESP32 enforces 0–400 range independently

### Arm / Disarm
```json
{"cmd":"arm","val":1}
```
```json
{"cmd":"arm","val":0}
```
- `val`: 1 = arm, 0 = disarm

### Emergency Stop
```json
{"cmd":"e_stop"}
```
- ESP32 immediately kills heater PWM, sets armed = false, sounds alarm tone
- App should send this repeatedly until acknowledged

### Request Full Status
```json
{"cmd":"get_status"}
```
- ESP32 responds with full state notify

### Set PID Parameters
```json
{"cmd":"set_pid","kp":2.0,"ki":5.0,"kd":1.0}
```
- Values stored in EEPROM alongside setTemp
- Only available via app (advanced settings)

---

## ESP32 → App Notifications (Notify Characteristic)

### Periodic Status Update (every 500ms when connected)
```json
{"status":"ok","temp_set":200,"temp_actual":198,"armed":1,"pwm":73,"battery":85,"session":1847}
```

| Field | Type | Unit | Notes |
|---|---|---|---|
| `status` | string | — | "ok", "error", "red_alert" |
| `temp_set` | int | °C | Current target temperature |
| `temp_actual` | float | °C | From thermocouple, -1 if sensor error |
| `armed` | int | — | 1 = armed, 0 = disarmed |
| `pwm` | int | % | PWM output as percentage (0–100) |
| `battery` | int | % | Battery level |
| `session` | int | seconds | Elapsed seconds since arm, 0 if disarmed |

### Red Alert (Thermal Runaway)
```json
{"status":"red_alert","temp_set":200,"temp_actual":267,"armed":0,"pwm":0,"battery":85,"session":1847}
```
- Heater is killed (`armed` = 0, `pwm` = 0)
- Buzzer sounds on device
- App should show prominent Red Alert UI

### Sensor Error
```json
{"status":"error","msg":"sensor_fault","temp_actual":-1}
```

### Disconnection Notification
On BLE disconnect, ESP32:
- Continues current operation (stays armed if armed)
- Sounds disconnect tone (ascending then descending tone)
- Shows BT disconnect icon on OLED
- Does NOT shut down heater

---

## Session Timer Logic

- Timer starts when `armed` transitions 0 → 1
- Timer resets to 0 when `armed` transitions 1 → 0
- Timer value included in every status update as `session` (integer seconds)
- Timer continues even if BLE disconnects and reconnects

---

## Thermal Runaway Detection

- Triggered when `temp_actual` exceeds `temp_set` by 50°C for 5 consecutive readings (readings at 250ms intervals = ~1.25 seconds sustained)
- Actions: kill heater, set armed = 0, sound alarm tone on buzzer, send red_alert status, show "RED ALERT" on OLED
- Latched: requires manual disarm/rearm or app e_stop + rearm to clear

---

## F/C Conversion

All BLE communication uses Celsius. The app is responsible for:
- Displaying in user's preferred unit (set in app settings)
- Converting slider/input values from F→C before sending `set_temp`
- Converting received `temp_actual` and `temp_set` from C→F for display (when F mode active)

Conversion: F = C × 9/5 + 32, C = (F − 32) × 5/9

---

## Profiles

Profiles are stored only on the app side (not on ESP32). A profile contains:
- Name (string)
- Target Temp (integer, stored in preferred unit, sent as °C)

The app sends a normal `set_temp` command when applying a profile. No special protocol needed.