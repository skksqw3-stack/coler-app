package com.example.coolercontrol

import android.content.Context

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("cooler_settings", Context.MODE_PRIVATE)

    var lastDeviceAddress: String
        get() = prefs.getString("device_address", "") ?: ""
        set(value) { prefs.edit().putString("device_address", value).apply() }

    var onTime: String
        get() = prefs.getString("on_time", "07:30") ?: "07:30"
        set(value) { prefs.edit().putString("on_time", value).apply() }

    var offTime: String
        get() = prefs.getString("off_time", "22:00") ?: "22:00"
        set(value) { prefs.edit().putString("off_time", value).apply() }
}
