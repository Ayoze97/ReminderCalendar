package com.remindercalendar

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventViewModel(
    private val eventManager: EventManager,
    private val settingsManager: SettingsManager,
    private val context: Context
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            // Simula o ejecuta tus cargas reales aquí
            loadInitialData()
            _isReady.value = true // Marcamos como listo
        }
    }

    private suspend fun loadInitialData() {
        delay(500)
    }

    private val refreshTrigger = MutableStateFlow(0)

    val allEvents: StateFlow<List<Event>> = combine(
        settingsManager.selectedCalendarsFlow,
        refreshTrigger
    ) { selectedIds, _ ->
        withContext(Dispatchers.IO) {
            eventManager.getEventsFromSystem(selectedIds)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val events: StateFlow<List<Event>> = allEvents

    fun refreshEvents() {
        refreshTrigger.value += 1
        updateWidget()
    }

    private fun updateWidget() {
        viewModelScope.launch {
            ReminderWidget().updateAll(context)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                eventManager.deleteGoogleEvent(event.id)
            }
            if (success) {
                refreshEvents()
            }
        }
    }

    fun addEvent(event: Event, syncToGoogle: Boolean = true, calendarId: Long? = 1L) {
        viewModelScope.launch(Dispatchers.IO) {
            if (syncToGoogle && calendarId != null) {
                eventManager.syncLocalEventToGoogle(event, calendarId)
            }

            withContext(Dispatchers.Main) {
                refreshEvents()
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = eventManager.updateGoogleEvent(event)
            if (success) {
                withContext(Dispatchers.Main) {
                    refreshEvents()
                }
            }
        }
    }
}