package com.remindercalendar

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
    private val settingsManager: SettingsManager
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
        // Aquí iría tu lógica de:
        // settingsRepository.getTimeRanges()
        // eventRepository.getAllEvents()
        delay(500) // Este es el medio segundo que mencionas
    }

    // 1. Disparador manual para forzar el refresco de los datos de Google
    private val refreshTrigger = MutableStateFlow(0)

    // 2. Lista de eventos: Ahora solo obtiene datos de Google Calendar
    val allEvents: StateFlow<List<Event>> = combine(
        settingsManager.selectedCalendarsFlow,
        refreshTrigger
    ) { selectedIds, _ ->
        // Ejecutamos la consulta a Google en un hilo de IO
        withContext(Dispatchers.IO) {
            eventManager.getEventsFromSystem(selectedIds)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Mantenemos la variable 'events' por compatibilidad, pero ahora apunta a la lista de Google
    val events: StateFlow<List<Event>> = allEvents

    fun refreshEvents() {
        refreshTrigger.value += 1
    }

    // --- FUNCIONES DE BORRADO ---
    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            // Ahora siempre intentamos borrar de Google usando el id del evento
            val success = withContext(Dispatchers.IO) {
                eventManager.deleteGoogleEvent(event.id)
            }
            if (success) {
                // Actualizamos el trigger para que la lista se refresque automáticamente
                refreshEvents()
            }
        }
    }

    // --- FUNCIONES DE AGREGAR ---
    fun addEvent(event: Event, syncToGoogle: Boolean = true, calendarId: Long? = 1L) {
        viewModelScope.launch(Dispatchers.IO) {
            // Solo guardamos en Google si se solicita y hay un ID de calendario
            if (syncToGoogle && calendarId != null) {
                eventManager.syncLocalEventToGoogle(event, calendarId)
            }

            // Refrescar la UI para que aparezca el nuevo evento desde el sistema
            withContext(Dispatchers.Main) {
                refreshEvents()
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = eventManager.updateGoogleEvent(event)
            if (success) {
                refreshEvents() // Recarga la lista para que la UI se actualice
            }
        }
    }
}