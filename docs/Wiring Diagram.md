    Warp Core eHMD — Arduino Nano ESP32 Wiring Diagram


    ┌──────────────────────────────────────────┐
    │          ARDUINO NANO ESP32 (top view)   │
    │                                          │
    │  D13 ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D12    │
    │  3V3 ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D11    │
    │  REF ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D10    │
    │  A0  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D9     │
    │  A1  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D8     │
    │  A2  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D7     │
    │  A3  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D6     │
    │  A4  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D5     │
    │  A5  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D4     │
    │  A6  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D3     │
    │  A7  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  D2     │
    │  5V  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  GND    │
    │  RST ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  VIN    │
    │  GND ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  3V3_EN │
    └──────────────────────────────────────────┘




    Signal Connections

    | Component               | Wire       | ESP32 Pin      | Notes                    |
    |-------------------------|------------|----------------|--------------------------|
    | MAX6675 Thermocouple    |            |                |                          |
    |                         | SCK        | D10            | SPI clock                |
    |                         | CS         | D11            | Chip select              |
    |                         | SO         | D12            | SPI data out             |
    |                         | VCC        | 3V3            |                          |
    |                         | GND        | GND            |                          |
    |                         | + Probe    | + to eHMD      | This runs to the Mini XLR|
    |                         | - Probe    | - to eHMD      | This runs to the Mini XLR|
    | MOSFET Trigger Board    |            |                |                          |
    |                         | Signal     | D9             | PWM ch1, 20 kHz          |
    |                         | VCC        | 5V or VIN      | From battery via step-up |
    |                         | GND        | GND            |                          |
    | MCH Heater Element      |            |                |                          |
    |                         | Load+      | MOSFET output+ | Through MOSFET board     |
    |                         | Load−      | MOSFET output− | Through MOSFET board     |
    | Buzzer                  |            |                |                          |
    |                         | +          | D8             | PWM ch0                  |
    |                         | −          | GND            |                          |
    | Left Button             |            |                |                          |
    |                         | Common     | GND            |                          |
    |                         | NO         | D7             | INPUT_PULLUP in code     |
    | Right Button            |            |                |                          |
    |                         | Common     | GND            |                          |
    |                         | NO         | D6             | INPUT_PULLUP in code     |
    | Press/Select Button     |            |                |                          |
    |                         | Common     | GND            |                          |
    |                         | NO         | D5             | INPUT_PULLUP in code     |
    | OLED Display (SSD1306)  |            |                | I2C address 0x3C         |
    |                         | SDA        | A4             | I2C data                 |
    |                         | SCL        | A5             | I2C clock                |
    |                         | VCC        | 3V3            |                          |
    |                         | GND        | GND            |                          |
    | Voltage Detection Board |            |                |                          |
    |                         | Signal out | A7             | Analog read              |
    |                         | VCC        | Battery+       | Monitors pack voltage    |
    |                         | GND        | GND            |                          |



    Power Section (battery build only)

    | Component            | Wire  | Connects To                  | Notes                    |
    |----------------------|-------|------------------------------|--------------------------|
    | 2S BMS               |       |                              |                          |
    |                      | B+    | Battery pack +               | 4× 146074 cells in 2S2P  |
    |                      | B−    | Battery pack −               |                          |
    |                      | P+    | 140W Charging Module VIN+    |                          |
    |                      | P−    | 140W Charging Module VIN−    |                          |
    | 140W Charging Module |       |                              |                          |
    |                      | USB-C | —                            | Charge input             |
    |                      | OUT+  | ESP32 VIN + MOSFET VCC       | Powers everything        |
    |                      | OUT−  | Power Switch -               |                          |
    | Power Switch         |       |                              |                          |
    |                      | -     | 140W Out -                   | Charge input             |
    |                      | -     | ESP32 VIN + MOSFET VCC       | Powers everything        |
    | Min XLR Connector    |       |                      		  | eHMD Head                |
    |                      | Pin 1 | + on the LOAD side of MOSFET |                          |
    |                      | Pin 2 | - on the LOAD side of MOSFET |                          |
    |                      | Pin 3 | + Thermal Probe on Max6687   |                          |
    |                      | Pin 2 | - Thermal Probe on Max6687   |                          |
    | Battery Heatshrink   | —     | Wraps each cell              | Insulation between cells |



    Voltage Divider (for battery monitoring — built from resistors or uses the voltage detection board)


    Battery+ ──┬── 30kΩ (rOhm1) ──┬── A7
                │                   │
                │               7.5kΩ (rOhm2)
                │                   │
                └───────────────── GND


    Calibrated with voltageCalibration = 1.1038. If using the voltage detection board from the BOM, it likely has this divider built in — just wire its signal out to A7.



    Summary of all ESP32 pin assignments:

    | Pin | Function            | Direction                |
    |-----|---------------------|--------------------------|
    | D2  | Available           | —                        |
    | D3  | Available           | —                        |
    | D4  | Available           | —                        |
    | D5  | Press/Select Button | Input (PULLUP)           |
    | D6  | Right Button        | Input (PULLUP)           |
    | D7  | Left Button         | Input (PULLUP)           |
    | D8  | Buzzer              | Output (PWM ch0)         |
    | D9  | MOSFET Signal       | Output (PWM ch1, 20 kHz) |
    | D10 | MAX6675 SCK         | Output                   |
    | D11 | MAX6675 CS          | Output                   |
    | D12 | MAX6675 SO          | Input                    |
    | A4  | OLED SDA (I2C)      | I/O                      |
    | A5  | OLED SCL (I2C)      | Output                   |
    | A7  | Voltage Sense       | Input (Analog)           |
    | VIN | System power (5V)   | Power in                 |
    | 3V3 | Sensor power (3.3V) | Power out                |
    | GND | Common ground       | Ground                   |

    Buttons are wired between the pin and GND — the code uses INPUT_PULLUP so no external resistors needed. When pressed, the pin goes LOW.