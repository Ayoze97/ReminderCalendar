package com.remindercalendar

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val CALENDAR_NAME_KEY = stringPreferencesKey("calendar_name")
        val HEADER_COLOR_KEY = longPreferencesKey("header_color")
        val BUTTONS_COLOR_KEY = longPreferencesKey("buttons_color")
        val CURRENT_DAY_COLOR_KEY = longPreferencesKey("current_day_color")
        val HEADER_TEXT_COLOR_KEY = longPreferencesKey("header_text_color")
        val BUTTONS_TEXT_COLOR_KEY = longPreferencesKey("buttons_text_color")
        val TIME_RANGES_KEY = stringSetPreferencesKey("time_ranges")
        val REMINDER_MESSAGE_KEY = stringPreferencesKey("reminder_message")
        val DATE_FORMAT_KEY = stringPreferencesKey("date_format")
        val PREFERRED_SEND_METHOD_KEY = stringPreferencesKey("preferred_send_method")
    }

    val calendarNameFlow: Flow<String> = dataStore.data.map {
        it[CALENDAR_NAME_KEY] ?: "Mi Calendario"
    }

        val preferredSendMethodFlow: Flow<SendMethod> = dataStore.data
        .map { preferences ->
            val methodStr = preferences[PREFERRED_SEND_METHOD_KEY] ?: SendMethod.SMS.name
            try {
                SendMethod.valueOf(methodStr)
            } catch (e: Exception) {
                SendMethod.SMS
            }
        }

    suspend fun setCalendarName(name: String) {
        dataStore.edit {
            it[CALENDAR_NAME_KEY] = name
        }
    }

        suspend fun setPreferredSendMethod(method: SendMethod) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_SEND_METHOD_KEY] = method.name
        }
    }

    val headerColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[HEADER_COLOR_KEY] ?: Color.Blue.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setHeaderColor(color: Color) {
        dataStore.edit {
            it[HEADER_COLOR_KEY] = color.toArgb().toLong()
        }
    }

    val buttonsColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[BUTTONS_COLOR_KEY] ?: Color.Cyan.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setButtonsColor(color: Color) {
        dataStore.edit {
            it[BUTTONS_COLOR_KEY] = color.toArgb().toLong()
        }
    }

    val currentDayColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[CURRENT_DAY_COLOR_KEY] ?: Color.Red.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setCurrentDayColor(color: Color) {
        dataStore.edit {
            it[CURRENT_DAY_COLOR_KEY] = color.toArgb().toLong()
        }
    }

    val headerTextColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[HEADER_TEXT_COLOR_KEY] ?: Color.White.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setHeaderTextColor(color: Color) {
        dataStore.edit {
            it[HEADER_TEXT_COLOR_KEY] = color.toArgb().toLong()
        }
    }

    val buttonsTextColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[BUTTONS_TEXT_COLOR_KEY] ?: Color.Black.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setButtonsTextColor(color: Color) {
        dataStore.edit {
            it[BUTTONS_TEXT_COLOR_KEY] = color.toArgb().toLong()
        }
    }

    val timeRangesFlow: Flow<List<TimeRange>> = dataStore.data.map {
        val defaultRanges = listOf(
            TimeRange(LocalTime.of(7, 0), LocalTime.of(13, 0)),
            TimeRange(LocalTime.of(16, 0), LocalTime.of(20, 0))
        )
        val storedRanges = it[TIME_RANGES_KEY]
        if (storedRanges.isNullOrEmpty()) {
            defaultRanges
        } else {
            storedRanges.mapNotNull { rangeString ->
                val parts = rangeString.split("-")
                if (parts.size == 2) {
                    try {
                        val start = LocalTime.parse(parts[0])
                        val end = LocalTime.parse(parts[1])
                        TimeRange(start, end)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }

    suspend fun setTimeRanges(timeRanges: List<TimeRange>) {
        dataStore.edit {
            val rangeStrings = timeRanges.map { range ->
                "${range.start}-${range.end}"
            }.toSet()
            it[TIME_RANGES_KEY] = rangeStrings
        }
    }

    val reminderMessageFlow: Flow<String> = dataStore.data.map {
        it[REMINDER_MESSAGE_KEY] ?: "Hola, te recuerdo tu cita."
    }

    suspend fun setReminderMessage(message: String) {
        dataStore.edit {
            it[REMINDER_MESSAGE_KEY] = message
        }
    }

    val dateFormatFlow: Flow<String> = dataStore.data.map {
        it[DATE_FORMAT_KEY] ?: "dd/MM/yyyy"
    }

    suspend fun setDateFormat(format: String) {
        dataStore.edit {
            it[DATE_FORMAT_KEY] = format
        }
    }
}