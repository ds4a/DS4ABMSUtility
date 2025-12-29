package com.smartbms.utility.data

import android.bluetooth.BluetoothDevice

/**
 * Represents a BMS device
 */
data class BMSDevice(
    val name: String,
    val address: String,
    val rssi: Int = 0,
    val bluetoothDevice: BluetoothDevice? = null,
    var isConnected: Boolean = false,
    var basicInfo: BasicInformation = BasicInformation(),
    var cellVoltages: CellVoltages = CellVoltages(),
    var configuration: Configuration = Configuration()
) {
    /**
     * Get display name for the device
     */
    fun getDisplayName(): String {
        return if (name.isNotEmpty() && name != "Unknown Device") {
            name
        } else {
            address
        }
    }
}
