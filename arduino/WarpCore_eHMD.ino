/*
 * Warp Core eHMD — Electronic Heat Management Device
 * Arduino Nano ESP32 Firmware with BLE Support
 * 
 * Controls a heating element via PID-controlled PWM for hookah use.
 * Communicates with the Warp Core eHMD Android app over BLE.
 * 
 * Hardware:
 *   - MAX6675 thermocouple on pins D10 (SCK), D11 (CS), D12 (SO)
 *   - MOSFET on D4 (PWM channel 1) — was D9, moved due to GPIO 9 conflict
 *   - SSD1306 128x64 OLED on I2C
 *   - Buttons: D5 (press/select), D6 (right), D7 (left)
 *   - Buzzer on D8 (PWM channel 0)
 *   - Battery voltage divider on A7
 * 
 * BLE Protocol: See docs/BLE_PROTOCOL.md
 * 
 * Libraries required:
 *   - ESP32 Arduino Core v3.x (built-in BLE — uses NimBLE)
 *   - Adafruit GFX Library
 *   - Adafruit SSD1306
 *   - MAX6675
 *   - PID_v1
 *   - ArduinoJson v7 (by Benoit Blanchon)
 *
 * IMPORTANT: Do NOT use ArduinoBLE library — it requires SpiNINA 
 *   (NINA-W102 coprocessor) which the ESP32 Nano does not have.
 *   Use the built-in BLEDevice.h / BLEServer.h / BLEUtils.h / BLE2902.h.
 */

#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include "max6675.h"
#include <PID_v1.h>
#include <EEPROM.h>
#include "pitches.h"

// BLE — ESP32 built-in NimBLE (NOT ArduinoBLE — requires SpiNINA coprocessor)
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <ArduinoJson.h>
#include <math.h>

// ============================================================================
// BLE UUIDs
// ============================================================================
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHAR_WRITE_UUID    "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define CHAR_NOTIFY_UUID   "beb5483e-36e1-4688-b7f5-ea07361b26a9"

// ============================================================================
// EEPROM Layout
// ============================================================================
struct EEData {
  double setTemp;
  double Kp;
  double Ki;
  double Kd;
};

const int EEPROM_ADDR = 0;
const size_t EEPROM_SIZE = sizeof(EEData) + 64;

const double DEFAULT_SET_TEMP = 100.0;
const double DEFAULT_KP = 2.0;
const double DEFAULT_KI = 5.0;
const double DEFAULT_KD = 1.0;

// ============================================================================
// OLED Display
// ============================================================================
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET    -1
#define SCREEN_ADDRESS 0x3C
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// ============================================================================
// Hardware Pin Definitions (Arduino Nano ESP32)
// ============================================================================
const int PIN_MOSFET    = 4;
const int PIN_SO        = 12;
const int PIN_CS        = 11;
const int PIN_SCK       = 10;
const int BTN_LEFT      = 7;
const int BTN_RIGHT     = 6;
const int BTN_PRESS     = 5;
const int PIN_BUZZER    = 8;
const int VOLTAGE_PIN   = A7;

MAX6675 thermocouple(PIN_SCK, PIN_CS, PIN_SO);

// ============================================================================
// PWM Channel Assignments (ESP32 ledc)
// ============================================================================
const int BUZZER_PWM_CHANNEL  = 0;
// MOSFET PWM at 20 kHz (ultrasonic — above human hearing, no coil whine)
// MOSFET driver supports up to 20 kHz. 8-bit resolution is plenty for thermal.
const int MOSFET_PWM_CHANNEL  = 1;
const int MOSFET_PWM_FREQ     = 20000;

// ============================================================================
// Voltage Divider Calibration
// ============================================================================
const float rOhm1 = 30000.0;
const float rOhm2 = 7500.0;
const float baseVoltage = 3.3;
const float voltageCalibration = 1.1038;

// ============================================================================
// Safety Limits
// ============================================================================
const double MAX_TEMP = 400.0;
const double THERMAL_RUNAWAY_DELTA = 50.0;
const int THERMAL_RUNAWAY_COUNT = 5;

// ============================================================================
// State Variables
// ============================================================================
bool systemArmed = false;
bool setMode = false;
double setTemp = DEFAULT_SET_TEMP;
double pendingTemp = DEFAULT_SET_TEMP;
double actualTemp = 0.0;
double pidOutput = 0.0;
double Kp = DEFAULT_KP;
double Ki = DEFAULT_KI;
double Kd = DEFAULT_KD;

PID myPID(&actualTemp, &pidOutput, &setTemp, Kp, Ki, Kd, DIRECT);

unsigned long lastTempTime = 0;
unsigned long lastDisplayUpdate = 0;
unsigned long lastAlarmTime = 0;

float systemVoltage = 0.0;
int batteryPercent = 0;

unsigned long buttonPressedTime = 0;
const int LONG_PRESS_DURATION = 3000;

unsigned long sessionStartTime = 0;
unsigned long sessionElapsed = 0;

int thermalRunawayCount = 0;
bool redAlertActive = false;

bool targetReachedNotified = false;

// ============================================================================
// BLE State (ESP32 built-in NimBLE)
// ============================================================================
bool bleConnected = false;
bool bleActive = true;
unsigned long bleStartTime = 0;
const unsigned long BLE_PAIR_WINDOW = 120000;      // 2 minutes on boot
const unsigned long BLE_RECONNECT_WINDOW = 30000;  // 30 seconds on disconnect
unsigned long bleReconnectDeadline = 0;

BLEServer* pServer = nullptr;
BLECharacteristic* pNotifyChar = nullptr;
BLECharacteristic* pWriteChar = nullptr;
bool bleDisconnectTone = false;  // Flag for disconnect tone (played in main loop, not BLE callback)

unsigned long lastBleNotifyTime = 0;
const unsigned long BLE_NOTIFY_INTERVAL = 500;

// ============================================================================
// Audio — Melody State Machine
// ============================================================================
bool melodyPlaying = false;
int melodyNoteIndex = 0;
unsigned long nextActionTime = 0;
bool isNoteSilent = false;

int bootMelody[] = {
  NOTE_D4, -8, NOTE_G4, 16, NOTE_C5, -4, NOTE_B4, 8,
  NOTE_G4, -16, NOTE_E4, -16, NOTE_A4, -16, NOTE_D5, 2
};
int bootNotes = sizeof(bootMelody) / sizeof(bootMelody[0]) / 2;

int targetMelody[] = { NOTE_C4, -4, NOTE_G4, -4, NOTE_C5, 1 };
int targetNotes = sizeof(targetMelody) / sizeof(targetMelody[0]) / 2;

int entMelody[] = {
  NOTE_AS4, 8, NOTE_DS5, 8, NOTE_F5, 8, NOTE_F5, -4, NOTE_DS5, -2, REST, 1,
  NOTE_C5, 8, NOTE_AS4, 8, NOTE_AS4, 4, NOTE_AS4, 4, NOTE_G4, 8, NOTE_C5, 2, REST, 16,
  NOTE_AS4, 8, NOTE_DS5, 8, NOTE_F5, 8, NOTE_F5, -4, NOTE_DS5, -2, REST, -2,
  NOTE_DS5, 8, NOTE_F5, 4, NOTE_GS5, 4, NOTE_G5, 4, NOTE_F5, 4, NOTE_DS5, 8, NOTE_DS5, -2
};
int entNotes = sizeof(entMelody) / sizeof(entMelody[0]) / 2;

// ============================================================================
// Audio Helpers
// ============================================================================

void esp32Tone(int frequency, int duration = 0) {
  if (frequency == REST || frequency == 0) {
    ledcWriteTone(BUZZER_PWM_CHANNEL, 0);
  } else {
    ledcWriteTone(BUZZER_PWM_CHANNEL, frequency);
    if (duration > 0) {
      delay(duration);
      ledcWriteTone(BUZZER_PWM_CHANNEL, 0);
    }
  }
}

void esp32NoTone() {
  ledcWriteTone(BUZZER_PWM_CHANNEL, 0);
}

void playMelody(int melody[], int numNotes, int speed) {
  int wholenote = (60000 * 4) / speed;
  for (int thisNote = 0; thisNote < numNotes * 2; thisNote = thisNote + 2) {
    int divider = melody[thisNote + 1];
    int noteDuration = (divider > 0) ? (wholenote / divider) : (wholenote / abs(divider) * 1.5);
    
    esp32Tone(melody[thisNote]);
    unsigned long startDelay = millis();
    while (millis() - startDelay < (noteDuration * 0.9)) {
      yield();
    }
    esp32NoTone();
    startDelay = millis();
    while (millis() - startDelay < (noteDuration * 0.1)) {
      yield();
    }
  }
}

void updateMelody(int melody[], int numNotes, int speed) {
  if (!melodyPlaying) return;

  if (millis() >= nextActionTime) {
    if (!isNoteSilent) {
      if (melodyNoteIndex < numNotes * 2) {
        int divider = melody[melodyNoteIndex + 1];
        int wholenote = (60000 * 4) / speed;
        int noteDuration = (divider > 0) ? (wholenote / divider) : (wholenote / abs(divider) * 1.5);

        esp32Tone(melody[melodyNoteIndex]);
        nextActionTime = millis() + (noteDuration * 0.9);
        isNoteSilent = true;
      } else {
        melodyPlaying = false;
        melodyNoteIndex = 0;
        esp32NoTone();
      }
    } else {
      esp32NoTone();
      int divider = melody[melodyNoteIndex + 1];
      int wholenote = (60000 * 4) / speed;
      int noteDuration = (divider > 0) ? (wholenote / divider) : (wholenote / abs(divider) * 1.5);
      
      nextActionTime = millis() + (noteDuration * 0.1);
      isNoteSilent = false;
      melodyNoteIndex += 2;
    }
  }
}

// ============================================================================
// EEPROM Load/Save
// ============================================================================

void loadEEPROM() {
  EEData data;
  EEPROM.get(EEPROM_ADDR, data);
  
  if (isnan(data.setTemp) || data.setTemp < 0.0 || data.setTemp > MAX_TEMP) {
    data.setTemp = DEFAULT_SET_TEMP;
  }
  if (isnan(data.Kp) || data.Kp <= 0.0) data.Kp = DEFAULT_KP;
  if (isnan(data.Ki) || data.Ki < 0.0) data.Ki = DEFAULT_KI;
  if (isnan(data.Kd) || data.Kd < 0.0) data.Kd = DEFAULT_KD;
  
  setTemp = data.setTemp;
  pendingTemp = data.setTemp;
  Kp = data.Kp;
  Ki = data.Ki;
  Kd = data.Kd;
}

void saveEEPROM() {
  EEData data;
  data.setTemp = setTemp;
  data.Kp = Kp;
  data.Ki = Ki;
  data.Kd = Kd;
  EEPROM.put(EEPROM_ADDR, data);
  EEPROM.commit();
}

// ============================================================================
// Battery Monitoring
// ============================================================================

void readVoltage() {
  int rawADC = analogRead(VOLTAGE_PIN);
  float sensorOutVolts = (rawADC * baseVoltage) / 4095.0;
  systemVoltage = (sensorOutVolts * (rOhm1 + rOhm2) / rOhm2) * voltageCalibration;
  batteryPercent = map((int)(systemVoltage * 100.0), 640, 840, 0, 100);
  batteryPercent = constrain(batteryPercent, 0, 100);
}

// ============================================================================
// BLE Callbacks (ESP32 built-in NimBLE)
// ============================================================================

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) override {
    bleConnected = true;
    bleReconnectDeadline = 0;  // Cancel any reconnect deadline on fresh connect
    bleStartTime = millis();    // Reset timeout baseline
    Serial.println(F("BLE: Client connected"));
  }

  void onDisconnect(BLEServer* pServer) override {
    bleConnected = false;
    // Schedule reconnect tone/advertising from main loop — do NOT block here.
    // delay() inside BLE callbacks blocks the NimBLE task and can cause GATT 133.
    bleDisconnectTone = true;
    if (bleActive && pServer != nullptr) {
      // Restart advertising immediately (lightweight — doesn't block)
      pServer->getAdvertising()->start();
      bleReconnectDeadline = millis() + BLE_RECONNECT_WINDOW;
      Serial.println(F("BLE: Reconnect window open for 30s"));
    }
  }
};

class WriteCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) override {
    std::string value = pCharacteristic->getValue();
    if (value.length() == 0) return;

    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, value);
    if (error) {
      Serial.print(F("JSON parse error: "));
      Serial.println(error.c_str());
      return;
    }

    const char* cmd = doc["cmd"];

    if (strcmp(cmd, "set_temp") == 0) {
      double newTemp = doc["val"].as<double>();
      newTemp = constrain(newTemp, 0.0, MAX_TEMP);
      setTemp = newTemp;
      if (!setMode) pendingTemp = newTemp;
      saveEEPROM();
      Serial.print(F("BLE set_temp: "));
      Serial.println(setTemp);
    }
    else if (strcmp(cmd, "arm") == 0) {
      int val = doc["val"].as<int>();
      if (val == 1 && !systemArmed) {
        systemArmed = true;
        sessionStartTime = millis();
        sessionElapsed = 0;
        targetReachedNotified = false;
        thermalRunawayCount = 0;
        redAlertActive = false;
        esp32Tone(1500, 500);
        Serial.println(F("BLE: ARMED"));
      } else if (val == 0 && systemArmed) {
        systemArmed = false;
        ledcWrite(MOSFET_PWM_CHANNEL, 0);
        pidOutput = 0;
        sessionElapsed = 0;
        esp32Tone(500, 500);
        Serial.println(F("BLE: DISARMED"));
      }
    }
    else if (strcmp(cmd, "e_stop") == 0) {
      systemArmed = false;
      redAlertActive = false;
      ledcWrite(MOSFET_PWM_CHANNEL, 0);
      pidOutput = 0;
      sessionElapsed = 0;
      esp32Tone(3000, 300);
      delay(100);
      esp32Tone(3000, 300);
      delay(100);
      esp32Tone(3000, 300);
      Serial.println(F("BLE: EMERGENCY STOP"));
    }
    else if (strcmp(cmd, "get_status") == 0) {
      lastBleNotifyTime = 0;  // Force immediate status send
    }
    else if (strcmp(cmd, "set_pid") == 0) {
      if (doc.containsKey("kp")) Kp = doc["kp"].as<double>();
      if (doc.containsKey("ki")) Ki = doc["ki"].as<double>();
      if (doc.containsKey("kd")) Kd = doc["kd"].as<double>();
      myPID.SetTunings(Kp, Ki, Kd);
      saveEEPROM();
      Serial.print(F("BLE set_pid: Kp=")); Serial.print(Kp);
      Serial.print(F(" Ki=")); Serial.print(Ki);
      Serial.print(F(" Kd=")); Serial.println(Kd);
    }
  }
};

// ============================================================================
// BLE Setup (ESP32 built-in NimBLE)
// ============================================================================

void setupBLE() {
  BLEDevice::init("WarpCore-eHMD");
  
  // NimBLE v3.x: bonding disabled by default (sm_bonding=0)
  // No security config needed — Just Works pairing, no passkey
  // The 2-minute BLE advertising window is the access control,
  // not encryption. After window, BLE sleeps until reboot.
  // IMPORTANT: Forget this device on your phone before pairing!
  
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());
  
  BLEService* pService = pServer->createService(SERVICE_UUID);
  
  // Write characteristic — app sends JSON commands
  pWriteChar = pService->createCharacteristic(
    CHAR_WRITE_UUID,
    BLECharacteristic::PROPERTY_WRITE_NR | BLECharacteristic::PROPERTY_READ
  );
  pWriteChar->setCallbacks(new WriteCallbacks());
  
  // Notify characteristic — ESP32 pushes status JSON to app
  pNotifyChar = pService->createCharacteristic(
    CHAR_NOTIFY_UUID,
    BLECharacteristic::PROPERTY_NOTIFY | BLECharacteristic::PROPERTY_READ
  );
  pNotifyChar->addDescriptor(new BLE2902());
  
  pService->start();
  
  // Start advertising — 2-minute window
  BLEAdvertising* pAdvertising = pServer->getAdvertising();
  pAdvertising->start();
  bleStartTime = millis();
  bleActive = true;
  Serial.println(F("BLE: Advertising as 'WarpCore-eHMD' — 2 min window"));
}

// ============================================================================
// BLE Status Send
// ============================================================================

void sendBleStatus() {
  if (!bleConnected || !pNotifyChar) return;
  
  JsonDocument doc;
  
  if (redAlertActive) {
    doc["status"] = "red_alert";
  } else if (isnan(actualTemp)) {
    doc["status"] = "error";
    doc["msg"] = "sensor_fault";
  } else {
    doc["status"] = "ok";
  }
  
  doc["temp_set"] = (int)setTemp;
  doc["temp_actual"] = isnan(actualTemp) ? -1 : (int)(actualTemp + 0.5);
  doc["armed"] = systemArmed ? 1 : 0;
  doc["pwm"] = (int)((pidOutput / 255.0) * 100.0);
  doc["pid_raw"] = (int)pidOutput;
  doc["battery"] = batteryPercent;
  doc["session"] = (int)sessionElapsed;
  
  // Debug fields — PID internals
  JsonObject dbg = doc.createNestedObject("dbg");
  dbg["kp"] = roundf(Kp * 100.0) / 100.0;
  dbg["ki"] = roundf(Ki * 100.0) / 100.0;
  dbg["kd"] = roundf(Kd * 100.0) / 100.0;
  dbg["err"] = roundf((setTemp - actualTemp) * 10.0) / 10.0;
  dbg["pwm_raw"] = (int)pidOutput;
  dbg["voltage"] = roundf(systemVoltage * 100.0) / 100.0;
  dbg["pin_state"] = digitalRead(PIN_MOSFET);  // DIAGNOSTIC: actual pin state
  
  char buffer[384];
  serializeJson(doc, buffer, sizeof(buffer));
  pNotifyChar->setValue(buffer);
  pNotifyChar->notify();
}

// ============================================================================
// Button Handling
// ============================================================================

void handleButton() {
  static bool lastBtnState = HIGH;
  static bool longPressHandled = false;
  bool currentBtnState = digitalRead(BTN_PRESS);

  if (lastBtnState == HIGH && currentBtnState == LOW) {
    buttonPressedTime = millis();
    longPressHandled = false;
  }

  if (currentBtnState == LOW && !longPressHandled) {
    if (millis() - buttonPressedTime >= LONG_PRESS_DURATION) {
      systemArmed = !systemArmed;
      if (systemArmed) {
        sessionStartTime = millis();
        sessionElapsed = 0;
        targetReachedNotified = false;
        thermalRunawayCount = 0;
        redAlertActive = false;
        esp32Tone(1500, 500);
      } else {
        ledcWrite(MOSFET_PWM_CHANNEL, 0);
        pidOutput = 0;
        sessionElapsed = 0;
        esp32Tone(500, 500);
      }
      longPressHandled = true;
    }
  }

  if (lastBtnState == LOW && currentBtnState == HIGH) {
    unsigned long holdTime = millis() - buttonPressedTime;
    if (!longPressHandled && holdTime > 50) {
      setMode = !setMode;
      if (setMode) {
        pendingTemp = setTemp;
        esp32Tone(2000, 50);
      } else {
        setTemp = pendingTemp;
        saveEEPROM();
        esp32Tone(2500, 150);
      }
    }
  }
  
  lastBtnState = currentBtnState;
}

void handleJoystickNavigation() {
  if (!setMode) return;

  static int activeAction = 0;
  static unsigned long actionPressTime = 0;
  static unsigned long lastStepTime = 0;

  const int HOLD_DELAY = 400;
  const int REPEAT_RATE = 100;
  const int DEBOUNCE_TIME = 50;

  bool currentLeft = digitalRead(BTN_LEFT);
  bool currentRight = digitalRead(BTN_RIGHT);

  if (activeAction == 0) {
    if (currentLeft == LOW)       activeAction = 3;
    else if (currentRight == LOW) activeAction = 4;

    if (activeAction != 0) {
      actionPressTime = millis();
      lastStepTime = millis();

      if (activeAction == 3) {
        pendingTemp -= 1;
        esp32Tone(2500, 10);
      } else if (activeAction == 4) {
        pendingTemp += 1;
        esp32Tone(2500, 10);
      }
    }
  } else {
    bool stillPressed = false;
    if (activeAction == 3) stillPressed = (currentLeft == LOW);
    if (activeAction == 4) stillPressed = (currentRight == LOW);

    if (stillPressed) {
      if (millis() - actionPressTime > HOLD_DELAY) {
        if (millis() - lastStepTime > REPEAT_RATE) {
          if (activeAction == 3) { pendingTemp -= 5; esp32Tone(2400, 10); }
          if (activeAction == 4) { pendingTemp += 5; esp32Tone(2600, 10); }
          lastStepTime = millis();
        }
      }
    } else {
      if (millis() - lastStepTime > DEBOUNCE_TIME) {
        activeAction = 0;
      }
    }
  }

  if (pendingTemp > MAX_TEMP) pendingTemp = MAX_TEMP;
  if (pendingTemp < 0) pendingTemp = 0;
}

// ============================================================================
// Thermal Runaway Detection
// ============================================================================

void checkThermalRunaway() {
  if (!systemArmed || isnan(actualTemp)) return;
  
  if (actualTemp > (setTemp + THERMAL_RUNAWAY_DELTA)) {
    thermalRunawayCount++;
    if (thermalRunawayCount >= THERMAL_RUNAWAY_COUNT) {
      redAlertActive = true;
      systemArmed = false;
      ledcWrite(MOSFET_PWM_CHANNEL, 0);
      pidOutput = 0;
      sessionElapsed = 0;
      esp32Tone(3000, 300);
      delay(100);
      esp32Tone(3000, 300);
      delay(100);
      esp32Tone(3000, 500);
      Serial.println(F("!!! RED ALERT — THERMAL RUNAWAY !!!"));
    }
  } else {
    thermalRunawayCount = 0;
  }
}

// ============================================================================
// Display Update
// ============================================================================

void updateDisplay() {
  float pidPercent = (pidOutput / 255.0) * 100.0;
  
  display.clearDisplay();
  display.setTextSize(1);
  
  // Line 1: Status indicators
  display.setCursor(0, 0);
  if (redAlertActive) {
    display.print(F("RED ALERT"));
  } else if (systemArmed) {
    display.print(F("ARMED"));
  } else {
    display.print(F("DISARM"));
  }
  
  // BLE indicator (next to battery)
  display.setCursor(52, 0);
  if (bleConnected) {
    display.print(F("BT"));  // Solid — connected
  } else if (bleActive) {
    if (millis() % 2000 < 1000) {
      display.print(F("BT"));  // Blinking — advertising
    } else {
      display.print(F("  "));
    }
  }
  
  // Battery — right-aligned so it never wraps
  {
    char batBuf[16];
    snprintf(batBuf, sizeof(batBuf), "%.1fV%d%%", systemVoltage, batteryPercent);
    int batLen = strlen(batBuf);
    display.setCursor(128 - (batLen * 6), 0);
    display.print(batBuf);
  }

  // Line 2: Target temperature
  display.setCursor(0, 16);
  display.print(F("Target: "));
  if (setMode) {
    display.print(pendingTemp, 0);
  } else {
    display.print(setTemp, 0);
  }
  display.print(F(" C"));

  // Line 3: Actual temperature
  display.setCursor(0, 28);
  if (isnan(actualTemp)) {
    display.setTextSize(2);
    display.print(F("SENSOR ERR"));
  } else {
    display.setTextSize(3);
    display.print((int)actualTemp);
    display.setTextSize(1);
    display.print(F(" C"));
  }

  // Line 4: Set mode indicator or PWM power
  display.setTextSize(1);
  display.setCursor(0, 56);
  if (setMode) {
    display.setTextColor(SSD1306_BLACK, SSD1306_WHITE);
    display.print(F(">> SETTING TEMP <<"));
    display.setTextColor(SSD1306_WHITE);
  } else {
    display.print(F("PWR: "));
    display.print(pidPercent, 0);
    display.print(F("%"));
  }

  display.display();
}

// ============================================================================
// SETUP
// ============================================================================

void setup() {
  Serial.begin(115200);

  analogSetAttenuation(ADC_11db);
  analogReadResolution(12);

  EEPROM.begin(EEPROM_SIZE);
  loadEEPROM();

  ledcSetup(BUZZER_PWM_CHANNEL, 2000, 8);
  ledcAttachPin(PIN_BUZZER, BUZZER_PWM_CHANNEL);

  // MOSFET PWM — 20 kHz ultrasonic, no audible whine
  ledcSetup(MOSFET_PWM_CHANNEL, MOSFET_PWM_FREQ, 8);
  ledcAttachPin(PIN_MOSFET, MOSFET_PWM_CHANNEL);

  if (!display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS)) {
    Serial.println(F("SSD1306 allocation failed"));
    for (;;);
  }

  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.println(F("System Booting..."));
  display.display();
  display.println(F("ENGAGE..."));
  display.display();

  playMelody(bootMelody, bootNotes, 80);

  pinMode(PIN_CS, OUTPUT);
  digitalWrite(PIN_CS, HIGH);
  pinMode(PIN_MOSFET, OUTPUT);  // DIAGNOSTIC: ensure output mode
  pinMode(BTN_LEFT, INPUT_PULLUP);
  pinMode(BTN_RIGHT, INPUT_PULLUP);
  pinMode(BTN_PRESS, INPUT_PULLUP);

  esp32Tone(1000, 100);
  delay(150);
  esp32Tone(1500, 150);

  myPID.SetMode(AUTOMATIC);
  myPID.SetOutputLimits(0, 255);

  setupBLE();

  delay(1000);
}

// ============================================================================
// LOOP
// ============================================================================

void loop() {
  handleButton();
  handleJoystickNavigation();
  updateMelody(entMelody, entNotes, 120);

  // Read sensors every 250ms
  if (millis() - lastTempTime > 250) {
    actualTemp = thermocouple.readCelsius();
    readVoltage();
    lastTempTime = millis();

    if (systemArmed && !targetReachedNotified && !redAlertActive && actualTemp >= setTemp) {
      melodyPlaying = true;
      targetReachedNotified = true;
    }

    checkThermalRunaway();
  }

  if (systemArmed) {
    sessionElapsed = (millis() - sessionStartTime) / 1000;
  }

  // PID Control & Safety
  // DIAGNOSTIC: force pin HIGH with digitalWrite to bypass LEDC
  if (isnan(actualTemp)) {
    digitalWrite(PIN_MOSFET, LOW);
    ledcWrite(MOSFET_PWM_CHANNEL, 0);
    pidOutput = 0;
    if (millis() - lastAlarmTime > 1000) {
      esp32Tone(3000, 200);
      lastAlarmTime = millis();
    }
  } else if (!systemArmed) {
    digitalWrite(PIN_MOSFET, LOW);
    ledcWrite(MOSFET_PWM_CHANNEL, 0);
    pidOutput = 0;
  } else {
    if (myPID.Compute()) {
      ledcWrite(MOSFET_PWM_CHANNEL, (int)pidOutput);
      // DIAGNOSTIC: if ledcWrite didn't drive it, force with digitalWrite
      digitalWrite(PIN_MOSFET, HIGH);
    }
  }

  // Debug: print PID state every 2 seconds
  static unsigned long lastPidDebug = 0;
  if (millis() - lastPidDebug > 2000) {
    Serial.print(F("PID: out=")); Serial.print((int)pidOutput);
    Serial.print(F(" actual=")); Serial.print(actualTemp, 1);
    Serial.print(F(" set=")); Serial.print(setTemp, 1);
    Serial.print(F(" armed=")); Serial.print(systemArmed);
    Serial.print(F(" Kp=")); Serial.print(Kp, 2);
    Serial.print(F(" Ki=")); Serial.print(Ki, 2);
    Serial.print(F(" Kd=")); Serial.println(Kd, 2);
    lastPidDebug = millis();
  }

  if (redAlertActive) {
    if (millis() - lastAlarmTime > 500) {
      esp32Tone(3000, 200);
      lastAlarmTime = millis();
    }
  }

  if (millis() - lastDisplayUpdate > 500) {
    updateDisplay();
    lastDisplayUpdate = millis();
  }

  // Play disconnect tone in main loop (never in BLE callback — it blocks NimBLE)
  if (bleDisconnectTone) {
    bleDisconnectTone = false;
    esp32Tone(800, 150);
    delay(100);
    esp32Tone(400, 200);
  }

  // Send BLE status every 500ms when connected
  if (bleConnected && (millis() - lastBleNotifyTime > BLE_NOTIFY_INTERVAL)) {
    sendBleStatus();
    lastBleNotifyTime = millis();
  }

  // BLE timeout — stop advertising if no connection established
  if (bleActive && !bleConnected) {
    if (bleStartTime > 0 && bleReconnectDeadline == 0) {
      if (millis() - bleStartTime > BLE_PAIR_WINDOW) {
        pServer->getAdvertising()->stop();
        bleActive = false;
        Serial.println(F("BLE: Pairing window expired — BLE sleeping"));
      }
    }
    if (bleReconnectDeadline > 0 && millis() > bleReconnectDeadline) {
      pServer->getAdvertising()->stop();
      bleActive = false;
      bleReconnectDeadline = 0;
      Serial.println(F("BLE: Reconnect window expired — BLE sleeping"));
    }
  }
}