package com.example.coolercontrol

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CoolerViewModel : ViewModel() {
    private val _state = MutableLiveData(CoolerState())
    val state: LiveData<CoolerState> = _state

    fun processLine(line: String) {
        _state.postValue(ArduinoParser.parse(line, _state.value ?: CoolerState()))
    }

    fun setConnected(value: Boolean) {
        _state.postValue((_state.value ?: CoolerState()).copy(connected = value))
    }
}
