package com.example.coolercontrol

data class CoolerState(
    val temperature: String = "--",
    val humidity: String = "--",
    val time: String = "--:--",
    val mode: String = "Unknown",
    val pump: String = "Unknown",
    val motor: String = "Unknown",
    val timer: String = "Unknown",
    val connected: Boolean = false,
    val lastMessage: String = ""
)
