# Warp Core eHMD — Arduino Firmware

## Overview
Electronic Heat Management Device firmware for Arduino Nano ESP32.
Controls a heating element via PID, reads temperature from MAX6675 thermocouple,
displays on SSD1306 OLED, and communicates with the Android app over BLE.

## Hardware Pin Mapping (Arduino Nano ESP32)

| Pin | Function |
|-----|----------|
| D9  | MOSFET PWM (heater control) |
| D10 | MAX6675 SCK |
| D11 | MAX6675 CS |
| D12 | MAX6675 SO (MISO) |
| D5  | Joystick Press (arm/disarm & set mode) |
| D6  | Joystick Right (temp up) |
| D7  | Joystick Left (temp down) |
| D8  | Buzzer (PWM audio) |
| A7  | Battery voltage divider |

## Required Libraries
Install via Arduino Library Manager (Sketch → Include Library → Manage Libraries):

1. **Adafruit GFX Library** — Display graphics
2. **Adafruit SSD1306** — OLED driver
3. **MAX6675** — Thermocouple library (or use the raw library included)
4. **PID_v1** — PID controller
5. **ArduinoJson** — v7, JSON parsing for BLE commands

ESP32 BLE libraries are built into the ESP32 Arduino Core — no separate install needed.

## Board Setup
- Board: "Arduino Nano ESP32" (or "ESP32 Dev Module" if using generic ESP32)
- The code uses `analogSetAttenuation`, `analogReadResolution`, and ESP32-specific `ledc` functions

## Building & Flashing
1. Open `WarpCore_eHMD.ino` in Arduino IDE
2. Select Board: Arduino Nano ESP32
3. Select Port: (your device's COM port)
4. Click Upload

## BLE Protocol
See `../docs/BLE_PROTOCOL.md` for the full specification.

## Local Controls (Buttons)
- **Long press (>3s)**: Arm / Disarm (3s hold)
- **Short press**: Enter / Exit set mode
- **Left/Right in set mode**: Adjust temperature (±1, hold for ±5)
- All local changes are reflected in the next BLE status update

## Safety Features
- Max temperature: 400°C (enforced in firmware and app)
- Thermal runaway: If actual temp exceeds set temp by 50°C for 5 consecutive readings (~1.25s), heater is killed, armed state cleared, alarm sounds
- Sensor fault: If thermocouple reads NaN, heater is killed and alarm sounds
- Emergency stop: App sends `{"cmd":"e_stop"}`, immediately kills heater

## EEPROM Layout
Stores: setTemp (double), Kp (double), Ki (double), Kd (double)
Values persist across power cycles. Defaults to 100°C, Kp=2, Ki=5, Kd=1.