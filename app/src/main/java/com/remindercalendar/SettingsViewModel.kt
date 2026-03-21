package com.remindercalendar

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

enum class DarkModeConfig { LIGHT, DARK, SYSTEM }

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val eventManager: EventManager,
    val context: Context
) : ViewModel() {

    private val _highlightCalendarOption = MutableStateFlow(false)
    val highlightCalendarOption = _highlightCalendarOption.asStateFlow()

    fun setHighlightCalendar(highlight: Boolean) {
        _highlightCalendarOption.value = highlight
    }

    // --- ID CALENDARIO PARA NUEVOS EVENTOS ---
    // Ahora lo exponemos como un StateFlow que viene del Manager para que sea persistente
    val selectedCalendarId: StateFlow<Long?> = settingsManager.selectedCalendarIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.getInitialSelectedCalendarId()
        )

    fun saveSelectedCalendarId(id: Long?) {
        viewModelScope.launch {
            settingsManager.saveSelectedCalendarId(id)
        }
    }

    // --- MODO OSCURO ---
    val darkMode: StateFlow<DarkModeConfig> = settingsManager.darkModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.getInitialDarkMode()
        )

    fun setDarkMode(config: DarkModeConfig) {
        viewModelScope.launch {
            settingsManager.setDarkMode(config)
            forceWidgetUpdate(context = context, newDarkMode = config.name)
        }
    }

    // --- NOMBRE CALENDARIO ---
    val calendarName: StateFlow<String> = settingsManager.calendarNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.getInitialCalendarName()
        )

    fun setCalendarName(name: String) {
        viewModelScope.launch {
            settingsManager.setCalendarName(name)
        }
    }

    val preferredSendMethod: StateFlow<NotificationMethod> = settingsManager.preferredSendMethodFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationMethod.SMS
        )

    fun setPreferredSendMethod(method: NotificationMethod) {
        viewModelScope.launch {
            settingsManager.setPreferredSendMethod(method)
        }
    }

    // --- COLOR ENCABEZADO (HEADER) ---
    val headerColor: StateFlow<Color> = settingsManager.headerColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.getInitialHeaderColor()
        )

    fun setHeaderColor(color: Color) {
        viewModelScope.launch {
            settingsManager.setHeaderColor(color)
            if (color == Color.Black) {
                settingsManager.setHeaderTextColor(Color.White)
            } else if (color == Color.White) {
                settingsManager.setHeaderTextColor(Color.Black)
            }
            forceWidgetUpdate(context = context, newHeaderColor = color.toArgb())
        }
    }

    // --- COLOR BOTONES ---
    val buttonsColor: StateFlow<Color> = settingsManager.buttonsColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.getInitialButtonsColor()
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

    // --- COLOR DÍA ACTUAL (CÍRCULO) ---
    val currentDayColor: StateFlow<Color> = settingsManager.currentDayColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.getInitialCurrentDayColor()
        )

    fun setCurrentDayColor(color: Color) {
        viewModelScope.launch {
            settingsManager.setCurrentDayColor(color)
        }
    }

    // --- TEXTO ENCABEZADO ---
    val headerTextColor: StateFlow<Color> = settingsManager.headerTextColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.getInitialHeaderTextColor()
        )

    fun setHeaderTextColor(color: Color) {
        viewModelScope.launch {
            settingsManager.setHeaderTextColor(color)
            forceWidgetUpdate(context = context, newTextColor = color.toArgb())
        }
    }

    // --- TEXTO BOTONES ---
    val buttonsTextColor: StateFlow<Color> = settingsManager.buttonsTextColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.getInitialButtonsTextColor()
        )

    fun setButtonsTextColor(color: Color) {
        viewModelScope.launch {
            settingsManager.setButtonsTextColor(color)
        }
    }

    // --- RANGOS HORARIOS ---
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

    // --- MENSAJE RECORDATORIO ---
    val reminderMessage: StateFlow<String> = settingsManager.reminderMessageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = context.getString(R.string.msg2)
        )

    val isDateFormatNeeded: StateFlow<Boolean> = reminderMessage
        .map { it.contains(context.getString(R.string.inserted_date)) }
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

    // --- FORMATO DE FECHA ---
    val dateFormat: StateFlow<String> = settingsManager.dateFormatFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.getInitialDateFormat()
        )

    fun setDateFormat(format: String) {
        viewModelScope.launch {
            settingsManager.setDateFormat(format)
        }
    }

    val defaultView: StateFlow<CalendarView> = settingsManager.defaultViewFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CalendarView.MONTH
        )

    fun setDefaultView(view: CalendarView) {
        viewModelScope.launch {
            settingsManager.setDefaultView(view)
        }
    }

    // --- GESTIÓN DE CALENDARIOS DISPONIBLES ---
    private val _availableCalendars = MutableStateFlow<List<CalendarAccount>>(emptyList())
    val availableCalendars: StateFlow<List<CalendarAccount>> = _availableCalendars

    fun loadCalendars() {
        viewModelScope.launch {
            _availableCalendars.value = eventManager.getAvailableCalendars()
        }
    }

    val selectedCalendars = settingsManager.selectedCalendarsFlow

    fun selectPrimaryCalendar(id: String) {
        viewModelScope.launch {
            // 1. Guardamos el ID para escribir (el Long)
            settingsManager.saveSelectedCalendarId(id.toLongOrNull())

            // 2. Guardamos el ID para leer (el Set con un solo elemento)
            // Esto mantendrá la compatibilidad con tu lógica actual de lectura
            settingsManager.clearAndSetSingleCalendar(id)
        }
    }

    private val _showCalendarDialog = MutableStateFlow(false)
    val showCalendarDialog: StateFlow<Boolean> = _showCalendarDialog.asStateFlow()

    fun setShowCalendarDialog(show: Boolean) {
        _showCalendarDialog.value = show
    }
    private val _forceShowCalendarSelector = MutableStateFlow(false)
    val forceShowCalendarSelector = _forceShowCalendarSelector.asStateFlow()

    fun triggerCalendarSelector() {
        _forceShowCalendarSelector.value = true
    }

    fun clearCalendarSelectorTrigger() {
        _forceShowCalendarSelector.value = false
    }

}

class SettingsViewModelFactory(
    private val settingsManager: SettingsManager,
    private val eventManager: EventManager,
    val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsManager, eventManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
