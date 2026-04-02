package com.remindercalendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDateTime
import java.time.ZoneId

class EventManager(private val context: Context) {
    private val contentResolver = context.contentResolver

    fun getAvailableCalendars(): List<CalendarAccount> {
        val calendarList = mutableListOf<CalendarAccount>()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
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
            val typeCol = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
            val accessCol = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

            while (it.moveToNext()) {
                val accessLevel = it.getInt(accessCol)

                if (accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {

                    val accountType = it.getString(typeCol)
                    val rawAccountName = it.getString(nameCol)

                    val isLocal = accountType == CalendarContract.ACCOUNT_TYPE_LOCAL ||
                            rawAccountName.isNullOrBlank() ||
                            !rawAccountName.contains("@")

                    val finalAccountName = if (isLocal) {
                        context.getString(R.string.cal_local)
                    } else {
                        rawAccountName
                    }

                    calendarList.add(
                        CalendarAccount(
                            id = it.getLong(idCol),
                            accountName = finalAccountName,
                            displayName = it.getString(descCol),
                            color = it.getInt(colorCol)
                        )
                    )
                }
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

        val startRange = LocalDateTime.now().minusYears(1)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val endRange = LocalDateTime.now().plusYears(2)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val selection = "${CalendarContract.Events.CALENDAR_ID} IN (${selectedIds.joinToString(",")}) " +
                "AND ${CalendarContract.Events.DTSTART} >= ? " +
                "AND ${CalendarContract.Events.DTSTART} <= ?"

        val selectionArgs = arrayOf(startRange.toString(), endRange.toString())

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.CALENDAR_ID
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

                        var extractedPerson = ""
                        var extractedPhone = ""
                        var extractedEmail = ""

                        if (description.isNotEmpty()) {
                            description.lines().forEach { line ->
                                when {
                                    line.startsWith(context.getString(R.string.desc_person)) -> extractedPerson = line.removePrefix(context.getString(R.string.desc_person)).trim()
                                    line.startsWith(context.getString(R.string.desc_phone)) -> extractedPhone = line.removePrefix(context.getString(R.string.desc_phone)).trim()
                                    line.startsWith(context.getString(R.string.desc_mail)) -> extractedEmail = line.removePrefix(context.getString(R.string.desc_mail)).trim()
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
                                name = cursor.getString(titleIdx)?.substringBefore(" (") ?: context.getString(
                                    R.string.event
                                ),
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

        }

        return googleEvents
    }

    fun deleteGoogleEvent(eventId: Long): Boolean {
        return try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: Exception) {

            false
        }
    }

    fun syncLocalEventToGoogle(event: Event, calendarId: Long): Long? {
        return try {
            val values = ContentValues().apply {
                // 1. Título con nombre de persona
                put(CalendarContract.Events.TITLE, "${event.name} (${event.person})")

                val labelPerson = context.getString(R.string.desc_person)
                val labelPhone = context.getString(R.string.desc_phone)
                val labelEmail = context.getString(R.string.desc_mail)

                // 2. Descripción con etiquetas para que la lectura funcione
                val descripcionFormateada = """
                $labelPerson${event.person}
                $labelPhone${event.phone}
                $labelEmail${event.email}
                
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
                uri.lastPathSegment?.toLong()
            } else {
                null
            }
        } catch (e: Exception) {

            null
        }
    }

    fun updateGoogleEvent(event: Event): Boolean {

        val labelPerson = context.getString(R.string.desc_person)
        val labelPhone = context.getString(R.string.desc_phone)
        val labelEmail = context.getString(R.string.desc_mail)
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, "${event.name} (${event.person})")
            put(CalendarContract.Events.DESCRIPTION, "$labelPerson${event.person}\n$labelPhone${event.phone}\n$labelEmail${event.email}")

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