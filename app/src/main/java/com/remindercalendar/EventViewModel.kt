package com.remindercalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventViewModel(private val eventManager: EventManager) : ViewModel() {
    val events: StateFlow<List<Event>> = eventManager.eventsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addEvent(event: Event) {
        viewModelScope.launch {
            val updatedEvents = events.value + event
            eventManager.saveEvents(updatedEvents)
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            val updatedEvents = events.value.map { if (it.id == event.id) event else it }
            eventManager.saveEvents(updatedEvents)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            val updatedEvents = events.value.filter { it.id != event.id }
            eventManager.saveEvents(updatedEvents)
        }
    }
}
