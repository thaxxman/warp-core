# 🔥 Warp Core eHMD

An open-source Electronic Heat Management Device for hookah. The Warp Core replaces traditional charcoal with a PID-controlled MCH heating element, delivering precise, consistent temperature management through a sleek Android app and an on-device OLED display.

![ESP32](https://img.shields.io/badge/MCU-Arduino_Nano_ESP32-blue)
![Kotlin](https://img.shields.io/badge/App-Kotlin_%2B_Compose-purple)
![License](https://img.shields.io/badge/License-MIT-green)

<!-- TODO: Add real photos here -->
<!--
![Warp Core eHMD Assembled](docs/images/hero-photo.jpg)
![Warp Core eHMD on Hookah](docs/images/on-hookah-photo.jpg)
![Warp Core App Screenshot](docs/images/app-screenshot.png)
-->

---

## What Is It?

The Warp Core eHMD is a DIY electronic heat management device that sits on top of a hookah bowl, replacing charcoal entirely. A K-type thermocouple reads the real-time temperature, a PID controller drives a ceramic MCH heating element via silent 20 kHz PWM, and an OLED screen on the base station shows you exactly what's happening — no guesswork.

The companion Android app connects over Bluetooth Low Energy and gives you a temperature slider, ±step buttons (configurable 1–10° per tap), preset profiles, live session graphs, and an emergency stop button. Set it, arm it, and let the Warp Core hold your temperature within a few degrees for the entire session.

**Key features:**

- **PID temperature control** — holds your set point within ±2°C at steady state
- **Silent operation** — 20 kHz MOSFET PWM, no audible coil whine
- **0–400°C range** — adjust from the app or on-device buttons
- **BLE Android app** — temperature control, profiles, session logging, CSV export
- **On-device OLED** — live temp, PWM output, battery level, and session timer
- **Safety first** — thermal runaway detection, emergency stop, auto-disarm on fault
- **Configurable step size** — ±1° to ±10° per button tap, set in the app's Settings
- **Open source** — hardware + firmware + app, MIT license, build your own

---

## How It Works

The device has two physical assemblies:

**The eHMD Head** — a layered sandwich that sits on the hookah bowl:
1. Bottom steel plate — the foundation, drilled for the M3 clamp screw
2. Graphite thermal sheet — distributes heat evenly across the MCH element
3. MCH ceramic heating element — the business end, driven by the MOSFET
4. Carbon wool — insulates heat upward into the bowl, not downward
5. Top steel plate — clamps everything down, countersunk for the M3 screw

A K-type thermocouple tip sits in a cutout in the graphite layer, reading the actual interface temperature. The whole stack is clamped together with a steel M3 screw through the center, with a wooden handle on the protruding end.

**The Base Station** — the electronics box that houses:
- Arduino Nano ESP32 (brains + BLE radio)
- SSD1306 128×64 OLED display
- 3 navigation buttons (left, right, press/select)
- MOSFET trigger board driving the MCH element
- MAX6675 thermocouple amplifier
- Piezo buzzer for alerts
- 2S2P LiPo battery pack with USB-C charging (or run externally powered)
- Voltage divider for battery monitoring

The head and base station connect via a 4-conductor cable with Mini XLR connectors — two wires for MCH power (18 AWG minimum) and two for the thermocouple signal.

---

## Bill of Materials

### Per-Unit Hardware (what goes into one device)

| Component | Unit Cost | Source |
|-----------|-----------|--------|
| Steel Plate (2×) | $9 | [Amazon](https://www.amazon.com/dp/B0FWJNRSWB) |
| MCH Heating Element | $16 | [AliExpress](https://www.aliexpress.us/item/3256808961658582.html) |
| Arduino Nano ESP32 | $21 | [Amazon](https://www.amazon.com/dp/B0C947BHK5) |
| OLED Screen (1 of 5-pack) | $2.60 | [Amazon](https://www.amazon.com/dp/B0F5WPZJ92) |
| Voltage Detection Board (1 of 5) | $1.20 | [Amazon](https://www.amazon.com/dp/B01HTC4XKY) |
| MOSFET Trigger Board (1 of 6) | $0.83 | [Amazon](https://www.amazon.com/dp/B0FMJH3DML) |
| Push Buttons (3 of 5) | $3.60 | [Amazon](https://www.amazon.com/dp/B0FM8QKSM1) |
| Arduino Expansion Board (1 of 3) | $4.00 | [Amazon](https://www.amazon.com/dp/B0C9TMRL13) |
| Graphite Thermal Sheet | $0.29 | [Amazon](https://www.amazon.com/dp/B0C69BDLPX) |
| Carbon Wool | $0.10 | [Amazon](https://www.amazon.com/dp/B0CFL5PZWC) |
| K-Type Thermocouple (1 of 5) | $2.40 | [Amazon](https://www.amazon.com/dp/B0D17S7N5B) |
| **Per-unit subtotal** | **~$61** | |

### Battery Build (add to per-unit if building portable)

| Component | Unit Cost | Source |
|-----------|-----------|--------|
| Mini XLR M/F Connector | $6 | [AliExpress](https://www.aliexpress.us/item/2255800871368173.html) |
| 4× 146074 LiPo Cells | $31 | [AliExpress](https://www.aliexpress.us/item/3256810240158946.html) |
| 140W USB-C Charging Module | $20 | [Amazon](https://www.amazon.com/dp/B0FRLBGFTH) |
| 2S BMS (1 of 5) | $2.40 | [Amazon](https://www.amazon.com/dp/B0F3TMC5JT) |
| **Battery build add** | **~$60** | |

**Per-unit total: ~$61 (battery-less) / ~$121 (with battery)**

### Consumables (buy once, use across many builds)

| Item | Cost | Per-Unit Burn |
|------|------|---------------|
| Kapton Tape | $10 | ~$0.25 |
| Screw Kit | $6 | ~$0.15 |
| Silicone Wire Kit | $12 | ~$0.50 |
| PLA Filament (1 kg, uses ~250g) | $19 | $4.75 |
| Battery Heatshrink (battery build only) | $7 | ~$0.20 |

**Max out-of-pocket: $171 (battery-less) / $247 (with battery)**

---

## Wiring Diagram

| Pin | Function | Direction |
|-----|----------|-----------|
| D5 | Press/Select Button | Input (PULLUP) |
| D6 | Right Button | Input (PULLUP) |
| D7 | Left Button | Input (PULLUP) |
| D8 | Buzzer | Output (PWM ch0) |
| D9 | MOSFET Signal | Output (PWM ch1, 20 kHz) |
| D10 | MAX6675 SCK | Output |
| D11 | MAX6675 CS | Output |
| D12 | MAX6675 SO | Input |
| A4 | OLED SDA (I2C) | I/O |
| A5 | OLED SCL (I2C) | Output |
| A7 | Battery Voltage Sense | Input (Analog) |
| VIN | System power (5V) | Power in |
| 3V3 | Sensor power (3.3V) | Power out |

All buttons wire between their pin and GND — internal pull-ups are enabled, no external resistors needed.

See [Wiring Diagram](docs/Wiring%20Diagram.md) for full connection details.

---

## Assembly Instructions

Complete step-by-step build guide including LiPo safety briefing, battery pack assembly, Arduino IDE setup, thermocouple welding, and eHMD head construction:

**[Assembly Instructions →](docs/ASSEMBLY_INSTRUCTIONS.md)**

---

## Quick Start

### Flash the Arduino

1. Install [Arduino IDE 2.x](https://www.arduino.cc/en/software)
2. Add ESP32 board support — `File → Preferences → Additional Board Manager URLs`:
   ```
   https://espressif.github.io/arduino-esp32/package_esp32_index.json
   ```
3. Install the board package: `Tools → Board → Boards Manager → search "esp32" → Install`
4. Select **Arduino Nano ESP32** and the correct serial port
5. Install these libraries via `Sketch → Include Library → Manage Libraries`:

| Library | Author |
|---------|--------|
| Adafruit GFX Library | Adafruit |
| Adafruit SSD1306 | Adafruit |
| MAX6675 | Adafruit |
| PID_v1 | Brett Beauregard |
| ArduinoJson | Benoit Blanchon (v7) |

6. Open `arduino/WarpCore_eHMD.ino` and click **Upload**
7. Open Serial Monitor at 115200 baud to verify boot

### Install the Android App

1. Download `WarpCore-v1.0-debug.apk` from [Releases](../../releases)
2. Enable "Install from unknown sources" on your Android device
3. Install the APK
4. Grant Bluetooth and Location permissions on first launch
5. Power on the Warp Core, tap **Connect**

### Build from Source

```bash
cd android-app
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/WarpCore-v1.0-debug.apk
```

---

## BLE Protocol

The ESP32 advertises as **"WarpCore-eHMD"** and exposes a single service:

- **Write characteristic** — app sends JSON: `{"cmd":"set_temp","val":200}`, `{"cmd":"arm","val":1}`, `{"cmd":"e_stop"}`, etc.
- **Notify characteristic** — ESP32 pushes status every 500ms: `{"status":"ok","temp_set":200,"temp_actual":198,"armed":1,"pwm":45,"battery":78,"session":120}`

Full protocol docs: [docs/BLE_PROTOCOL.md](docs/BLE_PROTOCOL.md)

---

## 3D Printed Enclosure

STL files in `docs/`:
- Box Bottom
- Box Lid
- Box Latch

---

## Project Structure

```
WarpCore-eHMD/
├── android-app/          # Kotlin + Jetpack Compose Android app
│   └── app/src/main/java/com/redshirt/warpcore/
│       ├── ble/          # BLE scan, connect, JSON protocol
│       ├── data/         # Room database (profiles, session logs)
│       ├── ui/           # Compose UI (screens, components, theme, navigation)
│       └── viewmodel/    # ConnectionViewModel, SettingsViewModel
├── arduino/             # ESP32 firmware
│   ├── WarpCore_eHMD.ino
│   └── pitches.h
├── docs/
│   ├── ASSEMBLY_INSTRUCTIONS.md
│   ├── BLE_PROTOCOL.md
│   ├── TODO.md
│   ├── Wiring Diagram.md
│   ├── Warp Core - Box Bottom.stl
│   ├── Warp Core - Box Lid.stl
│   └── Warp Core - Box Latch.stl
├── LICENSE
└── README.md
```

---

## Safety

This device controls a heating element that can reach **400°C (752°F)**.

- Thermal runaway protection is implemented in firmware (auto-disarm at +50°C over target)
- **Never** leave the device unattended while armed
- **Read the LiPo safety briefing** in the assembly instructions before building the battery pack
- The emergency stop button (long-press on device or in-app button) immediately kills heater power

You are responsible for safe assembly and operation. This project is provided as-is with no warranty.

---

## License

[MIT License](LICENSE) — use it, mod it, sell it. Just include the original license.

---

*Built by [George Thaxton](https://github.com/Thaxxman). Open source, open invitation.*

[☕ Buy me a Bowl](https://paypal.me/GThaxton)