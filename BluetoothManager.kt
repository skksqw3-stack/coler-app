package com.example.coolercontrol

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BluetoothManager(private val context: Context) {
    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    interface Listener {
        fun onConnected(device: BluetoothDevice)
        fun onDisconnected()
        fun onLineReceived(line: String)
        fun onError(message: String)
    }

    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var output: OutputStream? = null
    @Volatile private var connected = false
    @Volatile private var shouldRead = false
    @Volatile private var listener: Listener? = null

    private val connectionExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val sendExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val outputLock = Any()

    fun setListener(callback: Listener?) { listener = callback }
    fun isConnected(): Boolean = connected

    @Suppress("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        checkConnectPermission()
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: throw IllegalStateException("این گوشی بلوتوث ندارد.")
        return adapter.bondedDevices.sortedBy { it.name ?: it.address }
    }

    fun connect(deviceAddress: String) {
        if (!hasConnectPermission()) {
            listener?.onError("مجوز BLUETOOTH_CONNECT داده نشده است.")
            return
        }
        connectionExecutor.execute {
            closeCurrent(notify = false)
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: throw IllegalStateException("بلوتوث در دسترس نیست.")
                if (!adapter.isEnabled) throw IllegalStateException("ابتدا بلوتوث گوشی را روشن کن.")
                val device = adapter.getRemoteDevice(deviceAddress)
                val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                newSocket.connect()
                socket = newSocket
                output = newSocket.outputStream
                connected = true
                shouldRead = true
                listener?.onConnected(device)
                readLoop(newSocket)
            } catch (e: Exception) {
                connected = false
                shouldRead = false
                closeSocket()
                listener?.onError(e.message ?: "خطا در اتصال بلوتوث")
                listener?.onDisconnected()
            }
        }
    }

    fun send(command: String) {
        sendExecutor.execute {
            try {
                val stream = output ?: throw IOException("اتصال برقرار نیست.")
                if (!connected) throw IOException("HC-05 متصل نیست.")
                synchronized(outputLock) {
                    stream.write((command.trim() + "\n").toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
            } catch (e: Exception) {
                listener?.onError("ارسال ناموفق: ${e.message}")
            }
        }
    }

    private fun readLoop(activeSocket: BluetoothSocket) {
        try {
            val reader = BufferedReader(InputStreamReader(activeSocket.inputStream, Charsets.UTF_8))
            while (shouldRead && activeSocket.isConnected) {
                val line = reader.readLine() ?: break
                if (line.isNotBlank()) listener?.onLineReceived(line.trim())
            }
        } catch (e: IOException) {
            if (shouldRead) listener?.onError("ارتباط قطع شد: ${e.message}")
        } finally {
            if (socket === activeSocket) closeCurrent(notify = true)
        }
    }

    fun disconnect() {
        shouldRead = false
        closeCurrent(notify = true)
    }

    @Synchronized
    private fun closeCurrent(notify: Boolean) {
        val wasConnected = connected
        shouldRead = false
        connected = false
        closeSocket()
        if (notify && wasConnected) listener?.onDisconnected()
    }

    @Synchronized
    private fun closeSocket() {
        try { synchronized(outputLock) { output?.close() } } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        output = null
        socket = null
    }

    private fun checkConnectPermission() {
        if (!hasConnectPermission()) throw SecurityException("مجوز BLUETOOTH_CONNECT داده نشده است.")
    }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
}
