package com.smartbms.utility.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartbms.utility.data.BMSDevice
import java.util.UUID

/**
 * Bluetooth LE Interface for BMS Communication
 * Handles device scanning, connection, and data transmission
 */
@SuppressLint("MissingPermission")
class BluetoothInterface(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothInterface"

        // BMS Service and Characteristic UUIDs
        val SERVICE_UUID: UUID = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")
        val RX_UUID: UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
        val TX_UUID: UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private var tempData = ByteArray(0)
    private var waitingForMultiPart = false

    // LiveData for discovered devices
    private val _discoveredDevices = MutableLiveData<List<BMSDevice>>()
    val discoveredDevices: LiveData<List<BMSDevice>> = _discoveredDevices

    private val deviceList = mutableListOf<BMSDevice>()

    // LiveData for connection state
    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState

    // LiveData for received data
    private val _receivedData = MutableLiveData<ByteArray>()
    val receivedData: LiveData<ByteArray> = _receivedData

    // LiveData for scanning state
    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    /**
     * Check if Bluetooth is supported and enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Start scanning for BMS devices
     */
    fun startScan() {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }

        deviceList.clear()
        _discoveredDevices.value = deviceList.toList()

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        _isScanning.value = true

        Log.d(TAG, "Started BLE scan")
    }

    /**
     * Stop scanning for devices
     */
    fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
        _isScanning.value = false
        Log.d(TAG, "Stopped BLE scan")
    }

    /**
     * Scan callback for discovered devices
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            val device = result.device
            val deviceName = device.name ?: "Unknown Device"
            val deviceAddress = device.address

            // Check if device already exists in list
            val existingDevice = deviceList.find { it.address == deviceAddress }

            if (existingDevice == null) {
                val bmsDevice = BMSDevice(
                    name = deviceName,
                    address = deviceAddress,
                    rssi = result.rssi,
                    bluetoothDevice = device
                )

                deviceList.add(bmsDevice)
                _discoveredDevices.value = deviceList.toList()

                Log.d(TAG, "Discovered device: $deviceName ($deviceAddress)")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _isScanning.value = false
        }
    }

    /**
     * Connect to a BMS device
     */
    fun connectToDevice(device: BMSDevice) {
        stopScan()

        device.bluetoothDevice?.let { btDevice ->
            _connectionState.value = ConnectionState.CONNECTING
            bluetoothGatt = btDevice.connectGatt(context, false, gattCallback)
            Log.d(TAG, "Connecting to ${device.name}")
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(TAG, "Disconnected from device")
    }

    /**
     * GATT Callback for connection and data events
     */
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    _connectionState.postValue(ConnectionState.CONNECTED)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectionState.postValue(ConnectionState.DISCONNECTED)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")

                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(RX_UUID)
                    txCharacteristic = service.getCharacteristic(TX_UUID)

                    // Enable notifications for RX characteristic
                    rxCharacteristic?.let { char ->
                        gatt.setCharacteristicNotification(char, true)

                        val descriptor = char.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        )
                        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)

                        Log.d(TAG, "Enabled notifications for RX characteristic")
                    }

                    _connectionState.postValue(ConnectionState.READY)
                } else {
                    Log.e(TAG, "BMS service not found")
                    _connectionState.postValue(ConnectionState.ERROR)
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
                _connectionState.postValue(ConnectionState.ERROR)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == RX_UUID) {
                val data = characteristic.value
                processReceivedData(data)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic write successful")
            } else {
                Log.e(TAG, "Characteristic write failed with status: $status")
            }
        }
    }

    /**
     * Process multi-part packet reassembly
     */
    private fun processReceivedData(data: ByteArray) {
        val startByte = 0xDD.toByte()
        val endByte = 0x77.toByte()

        when {
            // First packet of multi-part message (starts with 0xDD, doesn't end with 0x77, length = 20)
            data.firstOrNull() == startByte && data.lastOrNull() != endByte && data.size == 20 -> {
                tempData = data
                waitingForMultiPart = true
                Log.d(TAG, "Received first part of multi-part message")
            }

            // Middle packet (doesn't start with 0xDD, doesn't end with 0x77)
            waitingForMultiPart && data.firstOrNull() != startByte && data.lastOrNull() != endByte -> {
                tempData += data
                Log.d(TAG, "Received middle part of multi-part message")
            }

            // Last packet (doesn't start with 0xDD, ends with 0x77)
            waitingForMultiPart && data.firstOrNull() != startByte && data.lastOrNull() == endByte -> {
                tempData += data
                waitingForMultiPart = false
                _receivedData.postValue(tempData)
                Log.d(TAG, "Received complete multi-part message: ${tempData.size} bytes")
                tempData = ByteArray(0)
            }

            // Single complete packet (starts with 0xDD, ends with 0x77)
            data.firstOrNull() == startByte && data.lastOrNull() == endByte -> {
                tempData = data
                waitingForMultiPart = false
                _receivedData.postValue(tempData)
                Log.d(TAG, "Received complete single message: ${data.size} bytes")
                tempData = ByteArray(0)
            }
        }
    }

    /**
     * Send data to BMS device
     */
    fun sendData(data: ByteArray): Boolean {
        if (_connectionState.value != ConnectionState.READY) {
            Log.e(TAG, "Cannot send data: device not ready")
            return false
        }

        txCharacteristic?.let { char ->
            char.value = data
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            val success = bluetoothGatt?.writeCharacteristic(char) ?: false

            if (success) {
                Log.d(TAG, "Sent ${data.size} bytes: ${data.joinToString(" ") { "0x%02X".format(it) }}")
            } else {
                Log.e(TAG, "Failed to send data")
            }

            return success
        }

        Log.e(TAG, "TX characteristic not available")
        return false
    }

    /**
     * Connection state enum
     */
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        READY,
        ERROR
    }
}
