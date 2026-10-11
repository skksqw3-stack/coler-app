package com.example.coolercontrol

import android.Manifest
import android.app.TimePickerDialog
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.graphics.Typeface
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import java.util.Locale

class MainActivity : AppCompatActivity(), BluetoothManager.Listener {
    private lateinit var bt: BluetoothManager
    private lateinit var vm: CoolerViewModel
    private lateinit var prefs: PreferencesManager
    private lateinit var content: LinearLayout
    private lateinit var logText: TextView
    private val log = StringBuilder()
    private var selectedAddress = ""

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = requiredPermissions().all { permission ->
            result[permission] == true ||
                checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) showSettings() else toast("برای اتصال، مجوز بلوتوث را فعال کن.")
    }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        ) else emptyArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bt = BluetoothManager(this)
        bt.setListener(this)
        vm = ViewModelProvider(this)[CoolerViewModel::class.java]
        prefs = PreferencesManager(this)
        selectedAddress = prefs.lastDeviceAddress
        buildShell()
        showHome()
        vm.state.observe(this) { if (content.tag == "home") renderHome() }

        if (Build.VERSION.SDK_INT >= 31 && requiredPermissions().any {
                checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }) permissionLauncher.launch(requiredPermissions())
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 12)
            setBackgroundColor(0xFFF4F7FB.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Cooler Control"
            textSize = 24f
            setTextColor(0xFF172033.toInt())
            setPadding(0, 8, 0, 18)
        })
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "home"
        }
        root.addView(ScrollView(this).apply { addView(content) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("خانه" to ::showHome, "تایمر" to ::showTimer,
            "وضعیت" to ::showStatus, "تنظیمات" to ::showSettings).forEach { (label, action) ->
            nav.addView(Button(this).apply {
                text = label
                textSize = 12f
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        setContentView(root)
        applyVazirmatn(root)
    }

    private fun applyVazirmatn(view: View) {
        try {
            val font = Typeface.createFromAsset(assets, "fonts/Vazirmatn-Regular.ttf")
            fun walk(v: View) {
                if (v is TextView) v.typeface = font
                if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
            walk(view)
        } catch (_: Exception) { }
    }

    private fun clearPage(name: String) {
        content.removeAllViews()
        content.tag = name
    }

    private fun addText(text: String, size: Float = 18f) {
        content.addView(TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(0xFF172033.toInt())
            setPadding(4, 8, 4, 8)
        })
    }

    private fun addButton(label: String, color: Int? = null, action: () -> Unit) {
        content.addView(Button(this).apply {
            text = label
            if (color != null) setBackgroundColor(color)
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 5 })
    }

    private fun send(command: String) {
        if (bt.isConnected()) {
            bt.send(command)
            appendLog("TX: $command")
        } else toast("ابتدا به HC-05 وصل شو.")
    }

    private fun showHome() { clearPage("home"); renderHome() }

    private fun renderHome() {
        if (content.tag != "home") return
        content.removeAllViews()
        val s = vm.state.value ?: CoolerState()
        addText("دما: ${s.temperature}")
        addText("رطوبت: ${s.humidity}")
        addText("ساعت RTC: ${s.time}")
        addText(if (s.connected) "بلوتوث متصل است" else "بلوتوث قطع است", 14f)
        addButton("Pump", 0xFF2196F3.toInt()) { send("1") }
        addButton("Slow Motor", 0xFFFBC02D.toInt()) { send("2") }
        addButton("Fast Motor", 0xFFF57C00.toInt()) { send("3") }
        addButton("تغییر Auto / Manual", 0xFF7E57C2.toInt()) { send("4") }
        addButton("درخواست زمان (t)") { send("t") }
        addButton("درخواست وضعیت کامل (s)") { send("s") }
    }

    private fun showTimer() {
        clearPage("timer")
        addText("زمان‌بندی کولر")
        addButton("انتخاب زمان روشن‌شدن") { chooseTime(prefs.onTime) { prefs.onTime = it; showTimer() } }
        addText("روشن‌شدن: ${prefs.onTime}")
        addButton("انتخاب زمان خاموش‌شدن") { chooseTime(prefs.offTime) { prefs.offTime = it; showTimer() } }
        addText("خاموش‌شدن: ${prefs.offTime}")
        addButton("ذخیره و ارسال تایمر") {
            val on = prefs.onTime.replace(":", "")
            val off = prefs.offTime.replace(":", "")
            if (on.matches(Regex("\\d{4}")) && off.matches(Regex("\\d{4}"))) {
                send("ON$on")
                send("OFF$off")
            } else toast("زمان نامعتبر است.")
        }
        addButton("فرمان Timer (T)") { send("T") }
    }

    private fun chooseTime(initial: String, onChosen: (String) -> Unit) {
        val parts = initial.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
        TimePickerDialog(this, { _, hour, minute ->
            onChosen(String.format(Locale.US, "%02d:%02d", hour, minute))
        }, h, m, true).show()
    }

    private fun showStatus() {
        clearPage("status")
        addText("وضعیت کولر")
        addButton("درخواست وضعیت جدید") { send("s") }
        val s = vm.state.value ?: CoolerState()
        addText("Mode: ${s.mode}")
        addText("Pump: ${s.pump}")
        addText("Motor: ${s.motor}")
        addText("Timer: ${s.timer}")
        addText("لاگ بلوتوث")
        logText = TextView(this).apply { text = log.toString(); textSize = 12f }
        content.addView(logText)
    }

    private fun showSettings() {
        clearPage("settings")
        addText("تنظیمات بلوتوث")
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            addButton("درخواست مجوز بلوتوث") { permissionLauncher.launch(requiredPermissions()) }
            return
        }
        val devices = try { bt.getPairedDevices() } catch (e: Exception) {
            addText("خطا: ${e.message}"); emptyList()
        }
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            devices.map { "${it.name ?: "Bluetooth"} — ${it.address}" })
        content.addView(spinner)
        if (devices.isNotEmpty()) spinner.setSelection(devices.indexOfFirst {
            it.address == selectedAddress
        }.coerceAtLeast(0))
        addButton("اتصال") {
            if (devices.isEmpty()) toast("ابتدا HC-05 را در تنظیمات گوشی Pair کن.")
            else {
                val device = devices[spinner.selectedItemPosition]
                selectedAddress = device.address
                prefs.lastDeviceAddress = device.address
                bt.connect(device.address)
            }
        }
        addButton("قطع اتصال") { bt.disconnect() }
        addButton("تنظیم ساعت RTC") { showRtcInput() }
        addButton("Help فارسی") { send("hFA") }
        addButton("Help English") { send("hEN") }
        addText(if (bt.isConnected()) "وضعیت: متصل" else "وضعیت: قطع")
    }

    private fun showRtcInput() {
        val input = EditText(this).apply {
            hint = "مثلاً 1430"
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(4))
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("تنظیم ساعت آردوینو")
            .setMessage("ساعت را به شکل HHMM وارد کن.")
            .setView(input)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ارسال") { _, _ ->
                val value = input.text.toString()
                if (value.matches(Regex("\\d{4}")) &&
                    value.substring(0, 2).toInt() in 0..23 &&
                    value.substring(2, 4).toInt() in 0..59) send("S$value")
                else toast("ساعت نامعتبر است.")
            }.show()
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            log.append(message).append('\n')
            if (log.length > 5000) log.delete(0, log.length - 4000)
            if (::logText.isInitialized) logText.text = log.toString()
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onConnected(device: BluetoothDevice) {
        runOnUiThread {
            vm.setConnected(true)
            appendLog("Connected: ${device.address}")
            toast("اتصال برقرار شد")
        }
    }

    override fun onDisconnected() {
        runOnUiThread { vm.setConnected(false); appendLog("Disconnected") }
    }

    override fun onLineReceived(line: String) {
        vm.processLine(line)
        appendLog("RX: $line")
    }

    override fun onError(message: String) {
        runOnUiThread { appendLog("ERROR: $message"); toast(message) }
    }

    override fun onDestroy() {
        bt.setListener(null)
        bt.disconnect()
        super.onDestroy()
    }
}
