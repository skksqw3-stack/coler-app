package com.example.coolercontrol

object ArduinoParser {
    fun parse(line: String, previous: CoolerState): CoolerState {
        val text = line.trim()
        return when {
            text.startsWith("Temp:", true) ->
                previous.copy(temperature = text.substringAfter(":").trim(), lastMessage = text)
            text.startsWith("Humidity:", true) ->
                previous.copy(humidity = text.substringAfter(":").trim(), lastMessage = text)
            text.startsWith("Time:", true) ->
                previous.copy(time = text.substringAfter(":").trim(), lastMessage = text)
            text.startsWith("Mode:", true) ->
                previous.copy(mode = text.substringAfter(":").trim(), lastMessage = text)
            text.startsWith("Pump:", true) ->
                previous.copy(pump = text.substringAfter(":").trim(), lastMessage = text)
            text.startsWith("Motor:", true) ->
                previous.copy(motor = text.substringAfter(":").trim(), lastMessage = text)
            text.startsWith("Timer:", true) ->
                previous.copy(timer = text.substringAfter(":").trim(), lastMessage = text)
            else -> previous.copy(lastMessage = text)
        }
    }
}
