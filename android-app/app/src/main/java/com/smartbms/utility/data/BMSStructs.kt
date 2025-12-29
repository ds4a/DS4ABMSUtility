package com.smartbms.utility.data

import kotlin.experimental.and

/**
 * BMS Protocol Request Builder
 * Constructs request packets for BMS communication
 */
class BMSRequest {
    private val startByte: Byte = 0xDD.toByte()
    private var statusByte: Byte = 0xA5.toByte() // Read
    private var commandByte: Byte = 0x03 // Basic information
    private var lengthByte: Byte = 0x00
    private var dataBytes: ByteArray = byteArrayOf()
    private var checksumByte: UShort = 0xFFFFu
    private val stopByte: Byte = 0x77

    /**
     * Generate byte array for transmission
     */
    fun getBytes(): ByteArray {
        val result = mutableListOf<Byte>()

        result.add(startByte)
        result.add(statusByte)
        result.add(commandByte)
        result.add(dataBytes.size.toByte())

        if (dataBytes.isNotEmpty()) {
            result.addAll(dataBytes.toList())
        }

        val checksum = getChecksum()
        result.add(((checksum.toInt() shr 8) and 0xFF).toByte())
        result.add((checksum.toInt() and 0xFF).toByte())
        result.add(stopByte)

        return result.toByteArray()
    }

    /**
     * Generate basic information request (0x03)
     */
    fun generateBasicInfoRequest(): ByteArray {
        statusByte = 0xA5.toByte()
        commandByte = 0x03
        dataBytes = byteArrayOf()
        lengthByte = dataBytes.size.toByte()
        checksumByte = getChecksum()
        return getBytes()
    }

    /**
     * Generate cell voltages request (0x04)
     */
    fun generateVoltageRequest(): ByteArray {
        statusByte = 0xA5.toByte()
        commandByte = 0x04
        dataBytes = byteArrayOf()
        lengthByte = dataBytes.size.toByte()
        checksumByte = getChecksum()
        return getBytes()
    }

    /**
     * Calculate checksum for the packet
     */
    private fun getChecksum(): UShort {
        var checksum = 0
        checksum += commandByte.toInt() and 0xFF
        checksum += lengthByte.toInt() and 0xFF

        for (data in dataBytes) {
            checksum += data.toInt() and 0xFF
        }

        return (65536 - checksum).toUShort()
    }

    companion object {
        /**
         * Convert UShort to pair of bytes
         */
        fun toByteArray(value: UShort): Pair<Byte, Byte> {
            val one = ((value.toInt() and 0xFF00) shr 8).toByte()
            val two = (value.toInt() and 0x00FF).toByte()
            return Pair(one, two)
        }
    }
}

/**
 * BMS Protocol Response Structure
 */
data class BMSResponse(
    val startByte: Byte = 0xDD.toByte(),
    val commandByte: Byte = 0x03,
    val statusByte: Byte = 0x00,
    val lengthByte: Byte = 0x00,
    val dataBytes: ByteArray = byteArrayOf(0x00),
    val checksumByte: UShort = 0xFFFFu,
    val stopByte: Byte = 0x77
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BMSResponse

        if (startByte != other.startByte) return false
        if (commandByte != other.commandByte) return false
        if (statusByte != other.statusByte) return false
        if (lengthByte != other.lengthByte) return false
        if (!dataBytes.contentEquals(other.dataBytes)) return false
        if (checksumByte != other.checksumByte) return false
        if (stopByte != other.stopByte) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startByte.toInt()
        result = 31 * result + commandByte
        result = 31 * result + statusByte
        result = 31 * result + lengthByte
        result = 31 * result + dataBytes.contentHashCode()
        result = 31 * result + checksumByte.hashCode()
        result = 31 * result + stopByte
        return result
    }
}

/**
 * Basic BMS Information (Command 0x03)
 */
data class BasicInformation(
    var totalVoltage: UShort? = null,
    var current: Short? = null,
    var residualCapacity: UShort? = null,
    var nominalCapacity: UShort? = null,
    var cycleLife: UShort? = null,
    var productDate: UShort? = null,
    var balanceCells: BooleanArray = BooleanArray(32) { true },
    var protection: ProtectionStatus = ProtectionStatus(),
    var version: UByte? = null,
    var rsoc: UByte? = null,
    var controlStatus: UByte? = null,
    var numberOfCells: UByte? = null,
    var numberOfTempSensors: UByte? = null,
    var temperatureReadings: DoubleArray = DoubleArray(8) { 0.0 },
    var chargingPort: Boolean = true,
    var dischargingPort: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BasicInformation

        if (totalVoltage != other.totalVoltage) return false
        if (current != other.current) return false
        if (!balanceCells.contentEquals(other.balanceCells)) return false
        if (!temperatureReadings.contentEquals(other.temperatureReadings)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = totalVoltage?.hashCode() ?: 0
        result = 31 * result + (current?.hashCode() ?: 0)
        result = 31 * result + balanceCells.contentHashCode()
        result = 31 * result + temperatureReadings.contentHashCode()
        return result
    }
}

/**
 * Protection Status Flags
 */
data class ProtectionStatus(
    var cellBlockOverVoltage: Boolean? = null,
    var cellBlockUnderVoltage: Boolean? = null,
    var batteryOverVoltage: Boolean? = null,
    var batteryUnderVoltage: Boolean? = null,
    var chargingOverTemp: Boolean? = null,
    var chargingUnderTemp: Boolean? = null,
    var dischargingOverTemp: Boolean? = null,
    var dischargingUnderTemp: Boolean? = null,
    var chargingOverCurr: Boolean? = null,
    var dischargingOverCurr: Boolean? = null,
    var shortCircuit: Boolean? = null,
    var icError: Boolean? = null,
    var mosLockIn: Boolean? = null
)

/**
 * Cell Voltages (Command 0x04)
 */
data class CellVoltages(
    val voltageOfCell: UShortArray = UShortArray(32) { 0u }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CellVoltages

        if (!voltageOfCell.contentEquals(other.voltageOfCell)) return false

        return true
    }

    override fun hashCode(): Int {
        return voltageOfCell.contentHashCode()
    }
}

/**
 * BMS Version Information
 */
data class BMSVersion(
    var version: UInt? = null
)

/**
 * Configuration Parameter Types
 */
enum class ConfigurationType(val value: UByte) {
    FULL_CAPACITY(0x10u),
    CYCLE_CAPACITY(0x11u),
    CELL_FULL_VOLTAGE(0x12u),
    CELL_EMPTY_VOLTAGE(0x13u),
    RATE_DSG(0x14u),
    PROD_DATE(0x15u),
    CYCLE_COUNT(0x17u),
    CHG_OTP_TRIG(0x18u),
    CHG_OTP_REL(0x19u),
    CHG_UTP_TRIG(0x1Au),
    CHG_UTP_REL(0x1Bu),
    DSG_OTP_TRIG(0x1Cu),
    DSG_OTP_REL(0x1Du),
    DSG_UTP_TRIG(0x1Eu),
    DSG_UTP_REL(0x1Fu),
    PACK_OVP_TRIG(0x20u),
    PACK_OVP_REL(0x21u),
    PACK_UVP_TRIG(0x22u),
    PACK_UVP_REL(0x23u),
    CELL_OVP_TRIG(0x24u),
    CELL_OVP_REL(0x25u),
    CELL_UVP_TRIG(0x26u),
    CELL_UVP_REL(0x27u),
    CHG_OCP(0x28u),
    DSG_OCP(0x29u),
    BALANCE_START_VOLTAGE(0x2Au),
    BALANCE_VOLTAGE_DELTA(0x2Bu),
    BALANCE_SWITCHES(0x2Du),
    NTC_SENSOR_ENABLE(0x2Eu),
    CELL_COUNT(0x2Fu),
    CAPACITY_80(0x32u),
    CAPACITY_60(0x33u),
    CAPACITY_40(0x34u),
    CAPACITY_20(0x35u),
    HARD_CELL_OVP(0x36u),
    HARD_CELL_UVP(0x37u),
    CHARGE_TEMP_DELAY(0x3Au),
    DISCHARGE_TEMP_DELAY(0x3Bu),
    PACK_VOLTAGE_PROTECTION_DELAY(0x3Cu),
    CELL_VOLTAGE_PROTECTION_DELAY(0x3Du),
    CHARGE_OVERCURRENT(0x3Eu),
    DISCHARGE_OVERCURRENT(0x3Fu),
    SERIAL_NUMBER(0xA0u),
    MODEL(0xA1u),
    BARCODE(0xA2u);

    companion object {
        fun fromValue(value: UByte): ConfigurationType? {
            return values().find { it.value == value }
        }
    }
}

/**
 * BMS Configuration Parameters
 */
data class Configuration(
    var fullCapacity: UShort? = null,
    var cycleCapacity: UShort? = null,
    var cellFullVoltage: UShort? = null,
    var cellEmptyVoltage: UShort? = null,
    var rateDsg: UShort? = null,
    var prodDate: String? = null,
    var cycleCount: UShort? = null,
    var chgOTPtrig: UShort? = null,
    var chgOTPrel: UShort? = null,
    var chgUTPtrig: UShort? = null,
    var chgUTPrel: UShort? = null,
    var dsgOTPtrig: UShort? = null,
    var dsgOTPrel: UShort? = null,
    var dsgUTPtrig: UShort? = null,
    var dsgUTPrel: UShort? = null,
    var packOVPtrig: UShort? = null,
    var packOVPrel: UShort? = null,
    var packUVPtrig: UShort? = null,
    var packUVPrel: UShort? = null,
    var cellOVPtrig: UShort? = null,
    var cellOVPrel: UShort? = null,
    var cellUVPtrig: UShort? = null,
    var cellUVPrel: UShort? = null,
    var chgOCP: UShort? = null,
    var dsgOCP: UShort? = null,
    var balanceStartVoltage: UShort? = null,
    var balanceVoltageDelta: UShort? = null,
    var ledCapacityIndicator: Boolean = false,
    var ledEnable: Boolean = false,
    var balanceOnlyWhileCharging: Boolean = false,
    var balanceEnable: Boolean = false,
    var loadDetect: Boolean = false,
    var hardwareSwitch: Boolean = false,
    var ntcSensorEnable: BooleanArray = BooleanArray(8) { false },
    var cellCount: UByte? = null,
    var capacity80: UShort? = null,
    var capacity60: UShort? = null,
    var capacity40: UShort? = null,
    var capacity20: UShort? = null,
    var hardCellOVP: UShort? = null,
    var hardCellUVP: UShort? = null,
    var chgUTPdel: UByte? = null,
    var chgOTPdel: UByte? = null,
    var dsgUTPdel: UByte? = null,
    var dsgOTPdel: UByte? = null,
    var packUVPdel: UByte? = null,
    var packOVPdel: UByte? = null,
    var cellOVPdel: UByte? = null,
    var cellUVPdel: UByte? = null,
    var chgOCPdel: UByte? = null,
    var chgOCPrel: UByte? = null,
    var dsgOCPdel: UByte? = null,
    var dsgOCPrel: UByte? = null,
    var serialNumber: String? = null,
    var model: String? = null,
    var barcode: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Configuration

        if (!ntcSensorEnable.contentEquals(other.ntcSensorEnable)) return false

        return true
    }

    override fun hashCode(): Int {
        return ntcSensorEnable.contentHashCode()
    }

    fun printConfiguration() {
        println("FullCapacity: ${fullCapacity ?: 0}")
        println("CycleCapacity: ${cycleCapacity ?: 0}")
        println("CellFullVoltage: ${cellFullVoltage ?: 0}")
        println("CellEmptyVoltage: ${cellEmptyVoltage ?: 0}")
        println("CellCount: ${cellCount ?: 0}")
        println("SerialNumber: ${serialNumber ?: ""}")
        println("Model: ${model ?: ""}")
        // ... additional fields as needed
    }
}
