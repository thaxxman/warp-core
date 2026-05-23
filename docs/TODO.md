# Warp Core eHMD — TODO List

## BLE Pairing (Goal — Not Yet Implemented)
- [ ] **Implement Numeric Comparison pairing** — display code on OLED, user taps Confirm on phone
  - Requires debugging v3.x NimBLE callback routing (onPassKeyNotify, onConfirmPIN)
  - The old Bluedroid callbacks don't fire in v3.x NimBLE core
  - May need direct ESP-IDF `esp_ble_gap_*` calls or switching back to Bluedroid mode

## BLE Connection (Current Status)
- [x] **Switched to ArduinoBLE.h** — Bluedroid API was broken on v3.x NimBLE core
- [x] **Just Works pairing works** — no PIN, no confirmation, just connects
- [ ] **Verify connection works end-to-end** — test actual data flow between app and ESP32
- [ ] **Test BLE timeout** — verify 2-min boot window and 30-sec reconnect window work

## Android App
- [x] **BLE Manager with permission checks** — SecurityException fix, scan-first connect flow
- [ ] **Verify JSON protocol flows** — send commands and receive status updates
- [ ] **Foldable layout** — Pixel 10 Fold support
- [ ] **Red Alert UI overlay** — visual warning when thermal runaway detected
- [ ] **Session logging** — save temp/PWM history to local DB

## Hardware / Firmware
- [ ] **OTA firmware updates** (v2 goal)
- [ ] **BLE wake button** — physical button to re-enable BLE without full reboot
- [x] **PWM standardized to ledc** — buzzer and MOSFET both use ledc API
- [x] **Thermal runaway detection** — Red Alert at 50°C over setTemp for 5 readings

## ArduinoBLE Migration Notes
- `BLEDevice::init()` → `BLE.begin()`
- `BLEServer::createService()` → `BLEService` constructor + `BLE.addService()`
- `BLECharacteristic::PROPERTY_WRITE_NR` → `BLEWriteWithoutResponse`
- `BLECharacteristic::PROPERTY_NOTIFY` → `BLENotify`
- `pCharacteristic->setValue()` + `notify()` → `writeValue()`
- `BLEServerCallbacks::onConnect/onDisconnect` → `BLE.poll()` + `BLE.central()` polling
- `BLECharacteristicCallbacks::onWrite` → `BLECharacteristic::setEventHandler(BLEWritten, handler)`
- No `BLE2902` descriptor needed — ArduinoBLE handles CCCD internally
- No `BLEDevice::setSecurityCallbacks()` — ArduinoBLE is no-config Just Works