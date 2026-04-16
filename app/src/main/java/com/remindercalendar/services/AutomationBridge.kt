package com.remindercalendar.services

object AutomationBridge {
    // Lista de pares (Teléfono, Mensaje)
    private val queue = mutableListOf<Pair<String, String>>()
    var isRunning = false

    fun startQueue(newList: List<Pair<String, String>>) {
        queue.clear()
        queue.addAll(newList)
        isRunning = true
    }

    fun getNext(): Pair<String, String>? {
        if (queue.isEmpty()) {
            isRunning = false
            return null
        }
        return queue.removeAt(0)
    }
}