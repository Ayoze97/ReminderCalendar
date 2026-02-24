package com.remindercalendar

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    val calendarName: StateFlow<String> = settingsManager.calendarNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Mi Calendario"
        )

    fun setCalendarName(name: String) {
        viewModelScope.launch {
            settingsManager.setCalendarName(name)
        }
    }

    val preferredSendMethod: StateFlow<SendMethod> = settingsManager.preferredSendMethodFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SendMethod.SMS
        )

        fun setPreferredSendMethod(method: SendMethod) {
        viewModelScope.launch {
            settingsManager.setPreferredSendMethod(method)
        }
    }

    val headerColor: StateFlow<Color> = settingsManager.headerColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Color.Blue
        )

    fun setHeaderColor(color: Color) {
        viewModelScope.launch {
            settingsManager.setHeaderColor(color)
            if (color == Color.Black) {
                settingsManager.setHeaderTextColor(Color.White)
            } else if (color == Color.White) {
                settingsManager.setHeaderTextColor(Color.Black)
            }
        }
    }

    val buttonsColor: StateFlow<Color> = settingsManager.buttonsColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Color.Cyan
        )

    fun setButtonsColor(color: Color) {
        viewModelScope.launch {
            settingsManager.setButtonsColor(color)
            if (color == Color.Black) {
                settingsManager.setButtonsTextColor(Color.White)
            } else if (color == Color.White) {
                settingsManager.setButtonsTextColor(Color.Black)
            }
        }
    }

    val currentDayColor: StateFlow<Color> = settingsManager.currentDayColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Color.Red
        )

    fun setCurrentDayColor(color: Color) {
        viewModelScope.launch {
            settingsManager.setCurrentDayColor(color)
        }
    }

    val headerTextColor: StateFlow<Color> = settingsManager.headerTextColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Color.Black
        )

    fun setHeaderTextColor(color: Color) {
        viewModelScope.launch {
            settingsManager.setHeaderTextColor(color)
        }
    }

    val buttonsTextColor: StateFlow<Color> = settingsManager.buttonsTextColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Color.Black
        )

    fun setButtonsTextColor(color: Color) {
        viewModelScope.launch {
            settingsManager.setButtonsTextColor(color)
        }
    }

    val timeRanges: StateFlow<List<TimeRange>> = settingsManager.timeRangesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(
                TimeRange(LocalTime.of(7, 0), LocalTime.of(13, 0)),
                TimeRange(LocalTime.of(16, 0), LocalTime.of(20, 0))
            )
        )

    fun setTimeRanges(timeRanges: List<TimeRange>) {
        viewModelScope.launch {
            settingsManager.setTimeRanges(timeRanges)
        }
    }

    val reminderMessage: StateFlow<String> = settingsManager.reminderMessageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Hola, te recuerdo tu cita el {fecha} a las {hora}."
        )
    val isDateFormatNeeded: StateFlow<Boolean> = reminderMessage
        .map { it.contains("{fecha}") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setReminderMessage(message: String) {
        viewModelScope.launch {
            settingsManager.setReminderMessage(message)
        }
    }

    val dateFormat: StateFlow<String> = settingsManager.dateFormatFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "dd/MM/yyyy"
        )

    fun setDateFormat(format: String) {
        viewModelScope.launch {
            settingsManager.setDateFormat(format)
        }
    }
}

class SettingsViewModelFactory(private val settingsManager: SettingsManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}