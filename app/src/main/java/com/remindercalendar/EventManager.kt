package com.remindercalendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.time.ZoneId

class EventManager(private val context: Context) {
    private val contentResolver = context.contentResolver

    fun getAvailableCalendars(): List<CalendarAccount> {
        val calendarList = mutableListOf<CalendarAccount>()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )
        val cursor = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )
        cursor?.use {
            val idCol = it.getColumnIndex(CalendarContract.Calendars._ID)
            val nameCol = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val descCol = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val colorCol = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)

            while (it.moveToNext()) {
                calendarList.add(
                    CalendarAccount(
                        id = it.getLong(idCol),
                        accountName = it.getString(nameCol),
                        displayName = it.getString(descCol),
                        color = it.getInt(colorCol)
                    )
                )
            }
        }
        return calendarList
    }

    fun getEventsFromSystem(selectedIds: Set<String>): List<Event> {
        if (selectedIds.isEmpty()) return emptyList()

        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return emptyList()

        val googleEvents = mutableListOf<Event>()
        val uri = CalendarContract.Events.CONTENT_URI

        val now = System.currentTimeMillis()
        val startRange = now - (30L * 24 * 60 * 60 * 1000)
        val endRange = now + (180L * 24 * 60 * 60 * 1000)

        val selection = "${CalendarContract.Events.CALENDAR_ID} IN (${selectedIds.joinToString(",")}) " +
                "AND ${CalendarContract.Events.DTSTART} >= ? " +
                "AND ${CalendarContract.Events.DTSTART} <= ?"

        val selectionArgs = arrayOf(startRange.toString(), endRange.toString())

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.CALENDAR_ID // Añadimos esto para mantener el ID del calendario
        )

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, "${CalendarContract.Events.DTSTART} ASC")?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val descIdx = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val calIdIdx = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID)

                if (idIdx != -1 && titleIdx != -1 && startIdx != -1 && descIdx != -1) {
                    while (cursor.moveToNext()) {
                        val description = cursor.getString(descIdx) ?: ""

                        // --- Lógica de extracción de datos ---
                        // Buscamos lo que guardamos en la descripción al crear el evento
                        var extractedPerson = ""
                        var extractedPhone = ""
                        var extractedEmail = ""

                        if (description.isNotEmpty()) {
                            description.lines().forEach { line ->
                                when {
                                    line.startsWith("Persona: ") -> extractedPerson = line.removePrefix("Persona: ").trim()
                                    line.startsWith("Tel: ") -> extractedPhone = line.removePrefix("Tel: ").trim()
                                    line.startsWith("Email: ") -> extractedEmail = line.removePrefix("Email: ").trim()
                                }
                            }
                        }

                        // Si no encontramos etiqueta "Persona", intentamos limpiar el título
                        // (por si el evento se creó fuera de nuestra app)
                        if (extractedPerson.isEmpty()) {
                            val title = cursor.getString(titleIdx) ?: ""
                            // Si el título tiene el formato "Cita (Nombre)", extraemos el nombre
                            if (title.contains("(") && title.endsWith(")")) {
                                extractedPerson = title.substringAfterLast("(").substringBeforeLast(")")
                            }
                        }

                        val googleId = cursor.getLong(idIdx)
                        val calId = cursor.getLong(calIdIdx)
                        val millis = cursor.getLong(startIdx)
                        val zonedDateTime = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())

                        googleEvents.add(
                            Event(
                                id = googleId,
                                googleCalendarId = calId, // Importante para ediciones futuras
                                date = zonedDateTime.toLocalDate(),
                                time = zonedDateTime.toLocalTime(),
                                name = cursor.getString(titleIdx)?.substringBefore(" (") ?: "Evento",
                                person = extractedPerson, // <--- Ahora es dinámico
                                description = description,
                                phone = extractedPhone,    // <--- Ahora es dinámico
                                email = extractedEmail      // <--- Ahora es dinámico
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EventManager", "Error cargando eventos de Google: ${e.message}")
        }

        return googleEvents
    }

    fun deleteGoogleEvent(eventId: Long): Boolean {
        return try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: Exception) {
            android.util.Log.e("EventManager", "Error al borrar evento: ${e.message}")
            false
        }
    }

    fun syncLocalEventToGoogle(event: Event, calendarId: Long): Long? {
        return try {
            val values = ContentValues().apply {
                // 1. Título con nombre de persona
                put(CalendarContract.Events.TITLE, "${event.name} (${event.person})")

                // 2. Descripción con etiquetas para que la lectura funcione
                val descripcionFormateada = """
                Persona: ${event.person}
                Tel: ${event.phone}
                Email: ${event.email}
                
                ${event.description}
            """.trimIndent()

                put(CalendarContract.Events.DESCRIPTION, descripcionFormateada)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)

                val zonedDateTime = event.date.atTime(event.time).atZone(ZoneId.systemDefault())
                val startMillis = zonedDateTime.toInstant().toEpochMilli()

                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 3600000)
                put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

            if (uri != null) {
                android.util.Log.d("GoogleSync", "✅ EXITO: ID ${uri.lastPathSegment}")
                uri.lastPathSegment?.toLong()
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("GoogleSync", "❌ ERROR: ${e.message}")
            null
        }
    }

    fun updateGoogleEvent(event: Event): Boolean {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, "${event.name} (${event.person})")
            put(CalendarContract.Events.DESCRIPTION, "Persona: ${event.person}\nTel: ${event.phone}\nEmail: ${event.email}")

            val startMillis = event.date.atTime(event.time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, startMillis + 30 * 60 * 1000) // +30 min
        }

        val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
        return try {
            val rows = context.contentResolver.update(updateUri, values, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}

data class CalendarAccount(
    val id: Long,
    val accountName: String,
    val displayName: String,
    val color: Int
)