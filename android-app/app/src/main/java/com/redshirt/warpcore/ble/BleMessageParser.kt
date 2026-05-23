package com.redshirt.warpcore.ble

import com.redshirt.warpcore.data.DeviceStatus
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Parser for BLE JSON messages from the eHMD ESP32.
 * See docs/BLE_PROTOCOL.md for the full specification.
 */
object BleMessageParser {
    private val gson = Gson()

    /**
     * Parse a status notification from ESP32.
     * Expected JSON: {"status":"ok","temp_set":200,"temp_actual":198,"armed":1,"pwm":73,"battery":85,"session":1847}
     */
    fun parseStatus(jsonString: String): DeviceStatus? {
        return try {
            val json = gson.fromJson(jsonString, Map::class.java)
            DeviceStatus(
                status = json["status"] as? String ?: "ok",
                msg = json["msg"] as? String,
                tempSet = (json["temp_set"] as? Double)?.toInt() ?: 0,
                tempActual = (json["temp_actual"] as? Double)?.toInt() ?: -1,
                armed = (json["armed"] as? Double)?.toInt() == 1,
                pwm = (json["pwm"] as? Double)?.toInt() ?: 0,
                battery = (json["battery"] as? Double)?.toInt() ?: 0,
                session = (json["session"] as? Double)?.toInt() ?: 0
            )
        } catch (e: JsonSyntaxException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Build a set_temp command JSON.
     * val is in Celsius, 0-400.
     */
    fun buildSetTemp(tempCelsius: Int): String {
        return """{"cmd":"set_temp","val":$tempCelsius}"""
    }

    /**
     * Build an arm/disarm command JSON.
     */
    fun buildArm(armed: Boolean): String {
        val armVal = if (armed) 1 else 0
        return """{"cmd":"arm","val":$armVal}"""
    }

    /**
     * Build an emergency stop command JSON.
     */
    fun buildEStop(): String {
        return """{"cmd":"e_stop"}"""
    }

    /**
     * Build a get_status request command JSON.
     */
    fun buildGetStatus(): String {
        return """{"cmd":"get_status"}"""
    }

    /**
     * Build a set_pid command JSON.
     */
    fun buildSetPid(kp: Double, ki: Double, kd: Double): String {
        return """{"cmd":"set_pid","kp":$kp,"ki":$ki,"kd":$kd}"""
    }

    // BLE UUIDs
    const val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    const val WRITE_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
    const val NOTIFY_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a9"
    const val DEVICE_NAME = "Warp Core eHMD"
}