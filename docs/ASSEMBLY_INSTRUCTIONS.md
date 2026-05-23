# Warp Core eHMD — Arduino Nano ESP32 Assembly Instructions

## 1. 3D Print the Enclosure

Print the included STL files. Recommended settings:

- **Material:** PLA (or PETG for higher heat resistance near the heater)
- **Infill:** 20–30%
- **Walls:** 3 perimeters minimum
- **Supports:** Per STL — review each for overhangs before printing

Assemble printed parts with M3 screws.

---

## 2. Wire the Arduino Components

Place and wire all components following the wiring guide below. Secure each component with hot glue. Keep wire runs as short as practical. Use zip ties where needed to manage routing.

| Component | Wire | ESP32 Pin | Notes |
|-----------|------|-----------|-------|
| **MAX6675 Thermocouple** | | | |
| | SCK | D10 | SPI clock |
| | CS | D11 | Chip select |
| | SO | D12 | SPI data out |
| | VCC | 3V3 | |
| | GND | GND | |
| **MOSFET Trigger Board** | | | |
| | Signal | D9 | PWM ch1, 20 kHz (ultrasonic) |
| | VCC | VIN (5V from battery) | |
| | GND | GND | |
| **Buzzer** | | | |
| | + | D8 | PWM ch0 |
| | − | GND | |
| **Left Button** | | | |
| | Common | GND | |
| | NO | D7 | INPUT_PULLUP — no resistor needed |
| **Right Button** | | | |
| | Common | GND | |
| | NO | D6 | INPUT_PULLUP — no resistor needed |
| **Press/Select Button** | | | |
| | Common | GND | |
| | NO | D5 | INPUT_PULLUP — no resistor needed |
| **OLED Display (SSD1306)** | | | I2C address 0x3C |
| | SDA | A4 | I2C data |
| | SCL | A5 | I2C clock |
| | VCC | 3V3 | |
| | GND | GND | |
| **Voltage Detection Board** | | | |
| | Signal out | A7 | Analog read |
| | VCC | Battery + | Monitors pack voltage |
| | GND | GND | |

**Unused GPIOs:** D2, D3, D4 are available for future expansion.

**Power routing:**
- **VIN** receives 5V from the charging module output (powers the ESP32 and MOSFET board)
- **3V3** powers the thermocouple IC, OLED, and logic-level references
- **GND** is common to all components — star ground preferred

---

## 3. Assemble the 2S2P Battery Pack

### ⚠️ LIPO SAFETY BRIEFING — READ THIS ENTIRE SECTION BEFORE STARTING

Lithium Polymer (LiPo) batteries store enormous energy in a small package. Mishandling them can cause **fire, explosion, chemical burns, and property damage.** Treat them with the same caution you'd give to a loaded firearm.

**The risks are real:**

- A punctured, overcharged, or shorted LiPo cell can undergo **thermal runaway** — the cell vents hot gas, catches fire, and cannot be extinguished with water or a standard fire extinguisher. It burns until the fuel is spent.
- A **2S pack** sits at 8.4V when fully charged. Shorting the terminals will dump 30+ amps instantly — enough to melt wire, weld metal, and start a fire.
- **Never** charge a swollen, dented, or damaged cell. Dispose of it instead.
- **Never** charge unattended. Stay in the same room.
- **Never** puncture, crush, or disassemble a cell.
- **Never** short the terminals together, even momentarily.
- **Always** charge on a non-flammable surface (concrete, ceramic tile, steel). Not on a desk, not on carpet, not on wood.
- **Keep a LiPo fire bag or sand bucket nearby** when charging. A class D fire extinguisher is ideal; a standard ABC extinguisher will not stop a LiPo fire.
- If a cell **swells, gets hot to the touch, or smells sweet/burnt**, move it outdoors away from combustibles immediately and do not use it again.

**You are responsible for your own safety.** If you are not comfortable working with LiPo cells, ask someone with experience to help.

### Assembly Instructions

You need: **4× 146074 LiPo cells** (2 pairs in parallel, those 2 pairs in series), **1× 2S BMS board**, **silicone wire** (18AWG for power, 22–26AWG for balance), **heat shrink tubing** (for each cell and for the pack), **soldering iron**, **multimeter**, **Kapton tape**.

**Step 1 — Inspect and charge cells individually**

- Check each cell for swelling, dents, or damage. Discard any suspect cell.
- Charge each cell individually to **3.8V (storage charge)** using a balance charger. All four cells must be within **0.05V** of each other before proceeding. Mismatched cells will cause the BMS to cut off prematurely or, worse, overcharge one pair.

**Step 2 — Heat shrink each cell**

- Cut a length of battery heat shrink tubing for each cell. Slide it over the cell and shrink with a heat gun on **low setting** — keep the gun moving. Do not over-shrink; you need the tubing to lie flat. This insulates each cell from its neighbors.

**Step 3 — Create the parallel pairs**

- Place two cells **side by side, same orientation** (both + facing the same direction).
- Tape the pair together with Kapton tape. Wrap tightly but do not crush.
- Solder a short bus wire across the **positive terminals** of both cells. Keep the solder joint small and flat.
- Solder a short bus wire across the **negative terminals** of both cells.
- You now have a "1S2P" cell group rated at ~3.7V nominal, double the capacity. Repeat for the second pair.

**Step 4 — Wire the BMS**

Before soldering, study your BMS pinout. Most 2S BMS boards have these pads:

| Pad | Connects To |
|-----|------------|
| B− | Negative of the **lower** cell pair (Pack −) |
| B1 | Junction between the two cell pairs (balance tap) |
| B+ | Positive of the **upper** cell pair (Pack +) |
| P− | Pack output negative (to charging module) |
| P+ | Pack output positive (to charging module) |

- Solder **B−** to the negative of the lower pair.
- Solder **B1** to the junction between the pairs (the common midpoint).
- Solder **B+** to the positive of the upper pair.
- Use **short, thick (18AWG silicone) wires** for P− and P+. These carry full load current.
- Wrap the BMS in Kapton tape to prevent shorts against the cell cans.

**Step 5 — Shrink the full pack**

- Wrap the entire assembly (both pairs + BMS) in battery heat shrink. This holds it together and adds puncture resistance.
- Leave the P+ and P− leads extending out the top, along with the balance leads if your BMS has a separate balance connector.

**Step 6 — Verify with a multimeter**

- Measure the pack voltage at P+ / P−. It should read between **7.4V** (nominal) and **8.4V** (fully charged).
- Measure the midpoint (B1 relative to B−). It should read **~3.7V**.
- If either reading is off by more than 0.1V, **stop and troubleshoot.** A wiring mistake here will destroy the BMS or the cells on the first charge cycle.

---

## 4. Install the Arduino Software

### Install the Arduino IDE

1. Download the Arduino IDE from [https://www.arduino.cc/en/software](https://www.arduino.cc/en/software) (version 2.x recommended).
2. Install it for your operating system (Windows, macOS, or Linux).

### Add ESP32 Board Support

The Arduino Nano ESP32 is not in the default board package. You need to add Espressif's board definitions:

1. Open Arduino IDE → **File → Preferences**.
2. In **Additional Board Manager URLs**, add:
   ```
   https://espressif.github.io/arduino-esp32/package_esp32_index.json
   ```
   (If you already have other URLs, click the button next to the field and add this one on a new line.)
3. Click **OK**.
4. Go to **Tools → Board → Boards Manager**.
5. Search for **esp32**. Install **esp32 by Espressif Systems** (version 3.x or later).

### Select the Board and Port

1. Connect the Arduino Nano ESP32 to your computer via USB-C.
2. Go to **Tools → Board** → select **Arduino Nano ESP32** (under "ESP32 Arduino" group).
3. Go to **Tools → Port** → select the COM port (Windows) or `/dev/cu.usbmodem*` (macOS) that appeared when you plugged in the board.
4. If the board doesn't appear, double-tap the reset button on the Nano to put it in bootloader mode, then try again.

### Install Required Libraries

Go to **Sketch → Include Library → Manage Libraries** and install each of these:

| Library | Author | Version |
|---------|--------|---------|
| Adafruit GFX Library | Adafruit | Latest |
| Adafruit SSD1306 | Adafruit | Latest |
| MAX6675 | Adafruit | Latest |
| PID_v1 | Brett Beauregard | Latest |
| ArduinoJson | Benoit Blanchon | v7.x |

**Important:** Do **NOT** install the "ArduinoBLE" library — the ESP32 Nano uses its built-in NimBLE stack (BLEDevice.h / BLEServer.h), not ArduinoBLE. ArduinoBLE requires a NINA-W102 coprocessor that the ESP32 Nano does not have.

### Upload the Firmware

1. Open the file `arduino/WarpCore_eHMD.ino` from this project.
2. Make sure the board is set to **Arduino Nano ESP32** and the correct port is selected.
3. Click **Upload** (→ arrow button) or press **Ctrl+U**.
4. The IDE will compile and flash the firmware. The Nano's LED will pulse during upload and then the OLED should initialize on reboot.
5. Open **Tools → Serial Monitor** at **115200 baud** to verify boot messages and BLE advertising.

---

## 5. Install the Warp Core Android App

1. Copy the provided `app-debug.apk` to your Android phone.
2. On the phone, open the APK file. You may need to enable **"Install from unknown sources"** in Settings → Security.
3. Install and open the app.
4. Grant Bluetooth and Location permissions when prompted (required for BLE scanning on Android).

---

## 6. Assemble the eHMD

### 6a. Drill the Bottom Plate

- Drill **8× 6mm holes** equidistant around the perimeter. Each hole should have **at least 6mm** of material between the hole edge and the plate edge. These are airflow / ventilation holes for the heater.
- Drill **1× 3mm hole** in the center of the bottom plate. **Countersink** this hole from the top side so the screw head sits flush or slightly below the surface.
- Lightly deburr all holes with a file or deburring tool.

### 6b. Drill and Tap the Top Plate

- Drill a **2.5mm hole** in the center of the top plate (tap drill size for M3).
- **Tap** the hole with an **M3×0.5 thread tap**. Go slow, use cutting oil if available, back the tap out frequently to clear chips. The steel plate is thin — run the tap through 2–3 times to clean the threads.

### 6c. Prepare the Thermocouple Probe

The K-type thermocouple wire is two dissimilar metals (chromel and alumel) welded together at the tip. If you need to shorten or repair the probe:

1. **Strip** the end of the thermocouple wire to expose about 5mm of each conductor (the positive wire is typically non-magnetic chromel, the negative is magnetic alumel).
2. **Twist** the two bare conductors tightly together at the tip.
3. **Weld** the junction using a capacitor-discharge welder, a spot welder, or the "battery method":
   - Take a **fresh AA or 9V battery**.
   - Touch the twisted thermocouple junction briefly to the battery terminal while simultaneously brushing it against the other terminal. The rapid current pulse will resistance-weld the junction.
   - Alternatively, touch the twisted tip to a **lead** strip while applying power from a bench supply (3–5V, current-limited to ~10A) for a split second. The lead melts and fuses the junction.
4. **Test** the joint with your multimeter set to millivolts. Warm the tip with your fingers — you should see a small voltage change (roughly 0.04mV per °C).
5. Insulate the wires above the junction with a small piece of **Kapton tape** or fiberglass sleeving. The junction itself must remain exposed (metal-to-metal contact with the graphite sheet).

Cut the thermocouple to roughly **5 inches** (127mm) overall length before welding the tip. This gives enough wire to route from the eHMD down to the Arduino, with some slack for strain relief.

### 6d. Layer Assembly

Sandwich the eHMD layers in this order, bottom to top:

| Layer | Component | Notes |
|-------|-----------|-------|
| **L1** | Bottom plate | Countersunk center hole facing up |
| **L2** | Graphite sheet | Cut to match the MCH element footprint. Cut a small notch or pocket for the thermocouple tip — the tip must be in direct contact with the graphite. |
| **L3** | MCH heating element | Flat side against the graphite |
| **L4** | Carbon wool | Layer thick enough to fully cover the MCH. This is your thermal insulation and heat distributor. |
| **L5** | Top plate | M3 threaded center hole |

### 6e. Clamp Together

1. Run a **steel M3×20mm screw** up through the bottom plate (from below, countersunk head sits flush), through L2, L3, L4, and thread it into the tapped center hole of the top plate (L5).
2. Tighten firmly — the clamping force should be tight enough to compress the carbon wool slightly and ensure good thermal contact between all layers. Don't overtighten to the point of crushing the MCH element.
3. The 20mm screw leaves ~5–8mm of excess above the top plate. Thread a **wooden handle** onto this protruding stud for easy lifting of the hot eHMD.

### 6f. Wire the MCH and Thermocouple to the Cable

1. Solder the **MCH heating element** leads to a 4-conductor cable:
   - **MCH + (red)** → 18AWG wire (minimum) → XLR pin 1 (or per your XLR pinout)
   - **MCH − (black)** → 18AWG wire (minimum) → XLR pin 2
   - 18AWG is required because the MCH can draw 5–10A at operating voltage.
2. Solder the **K-type thermocouple** leads to the remaining 2 conductors in the cable:
   - **Chromel (+)** → smaller gauge wire → XLR pin 3
   - **Alumel (−)** → smaller gauge wire → XLR pin 4
3. Cover each solder joint with **heat shrink tubing**. Slide the tubing over the joint and shrink with a heat gun.
4. Wrap the entire cable junction with Kapton tape for strain relief and insulation.

### 6g. Install the Mini XLR Connector

At the **base station end** of the cable:

1. Install the **4-pin Mini XLR connector** on the cable end.
2. Pin assignment must match the female socket on the base station:
   - **Pins 1 & 2:** Load output from the MOSFET trigger board (MCH + and −)
   - **Pins 3 & 4:** Thermocouple input to the MAX6675 chip on the Arduino
3. Verify continuity with a multimeter before powering on. A swapped thermocouple polarity will cause the displayed temperature to **decrease** as the heater warms up — an easy mistake to catch.

---

## 7. Charge Before First Use

1. Connect the USB-C charging module to a **65W+ USB-C PD power supply** (laptop charger or similar).
2. A full charge from storage voltage takes approximately **1.5–2 hours**.
3. The BMS will balance the cells and cut off at 8.4V (4.2V per cell).
4. Do not leave charging unattended overnight for the first cycle. Monitor for any swelling, heat, or unusual odor.

---

## 8. Maiden Voyage

1. **Power on** the Arduino Nano ESP32 via USB-C or from the battery through the charging module.
2. The OLED will display the boot screen. The buzzer will play the startup melody.
3. BLE will advertise as **"WarpCore-eHMD"** for **2 minutes**. Open the Android app and tap **Connect**.
4. Once connected, the app transitions to the **Control** screen. You should see the current temperature, set temperature, and controls.
5. **Set your target temperature** using the slider or ± buttons. Default step size is ±1° (adjustable in Settings → Step Size).
6. Press **ARM** to begin heating. The MOSFET will energize and the PID controller will ramp up to target.
7. Monitor the temperature readout. The app updates every 500ms. The OLED also displays live status.
8. When done, press **DISARM** to shut off the heater. The MOSFET drops to 0% PWM.
9. **Emergency Stop** is always available on the Control screen — it immediately kills the heater and sounds a triple-tone alert.

**Congratulate yourself, Captain. The Warp Core is online.** 🖖