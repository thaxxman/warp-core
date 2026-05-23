package com.redshirt.warpcore.data

/**
 * Represents the current status of the eHMD device.
 * Parsed from JSON sent by ESP32 over BLE notify characteristic.
 */
data class DeviceStatus(
    val status: String = "ok",       // "ok", "error", "red_alert"
    val msg: String? = null,         // Error message if status == "error"
    val tempSet: Int = 0,            // Target temp in °C
    val tempActual: Int = 0,         // Actual temp in °C, -1 if sensor fault
    val armed: Boolean = false,       // Armed state
    val pwm: Int = 0,                // PWM output percentage (0-100)
    val pidRaw: Int = 0,             // Raw PID output (0-255)
    val battery: Int = 0,             // Battery percentage
    val session: Int = 0,             // Session elapsed seconds
    // Debug sub-object
    val debugKp: Double = 0.0,
    val debugKi: Double = 0.0,
    val debugKd: Double = 0.0,
    val debugError: Double = 0.0,     // setTemp - actualTemp
    val debugPwmRaw: Int = 0,         // Same as pidRaw, from dbg object
    val debugVoltage: Double = 0.0     // System voltage
) {
    /** True if we're in a thermal runaway condition */
    val isRedAlert: Boolean get() = status == "red_alert"

    /** True if the thermocouple has a fault */
    val isSensorError: Boolean get() = status == "error" && msg == "sensor_fault"

    /** Session time formatted as HH:MM:SS */
    val sessionFormatted: String
        get() {
            val h = session / 3600
            val m = (session % 3600) / 60
            val s = session % 60
            return "%02d:%02d:%02d".format(h, m, s)
        }
}