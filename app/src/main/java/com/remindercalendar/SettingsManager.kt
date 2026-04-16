package com.remindercalendar

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalTime


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(val context: Context) {

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
        val REMINDER_MESSAGE_KEY_2 = stringPreferencesKey("reminder_message_2")
        val DATE_FORMAT_KEY = stringPreferencesKey("date_format")
        val PREFERRED_SEND_METHOD_KEY = stringPreferencesKey("preferred_send_method")
        val DARK_MODE_KEY = stringPreferencesKey("dark_mode")
        private val DEFAULT_VIEW_KEY = stringPreferencesKey("default_view")
        private val SELECTED_CALENDARS_KEY = stringSetPreferencesKey("selected_calendars")
        private val SELECTED_CALENDAR_ID = longPreferencesKey("selected_calendar_id")
        val REMINDER_DAYS_THRESHOLD_KEY = intPreferencesKey("reminder_days_threshold")
        private val BATCH_SENDING_KEY = booleanPreferencesKey("batch_sending_enabled")
    }

    val selectedCalendarsFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[SELECTED_CALENDARS_KEY] ?: emptySet()
    }

    suspend fun toggleCalendar(calendarId: String) {
        dataStore.edit { preferences ->
            val current = preferences[SELECTED_CALENDARS_KEY] ?: emptySet()
            val next = if (current.contains(calendarId)) current - calendarId else current + calendarId
            preferences[SELECTED_CALENDARS_KEY] = next
        }
    }

    private fun <T> readSync(key: Preferences.Key<T>, default: T): T {
        return runBlocking {
            dataStore.data.map { it[key] }.first() ?: default
        }
    }

    fun getInitialDateFormat(): String {
        return readSync(DATE_FORMAT_KEY, "dd/MM/yyyy")
    }

    fun getInitialHeaderColor(): Color {
        val colorLong = readSync(HEADER_COLOR_KEY, Color.Blue.toArgb().toLong())
        return Color(colorLong.toInt())
    }

    fun getInitialDarkMode(): DarkModeConfig {
        val name = readSync(DARK_MODE_KEY, DarkModeConfig.SYSTEM.name)
        return try { DarkModeConfig.valueOf(name) } catch (e: Exception) { DarkModeConfig.SYSTEM }
    }

    fun getInitialButtonsColor(): Color {
        val colorLong = readSync(BUTTONS_COLOR_KEY, Color.Cyan.toArgb().toLong())
        return Color(colorLong.toInt())
    }

    fun getInitialCurrentDayColor(): Color {
        val colorLong = readSync(CURRENT_DAY_COLOR_KEY, Color.Red.toArgb().toLong())
        return Color(colorLong.toInt())
    }

    fun getInitialHeaderTextColor(): Color {
        val colorLong = readSync(HEADER_TEXT_COLOR_KEY, Color.White.toArgb().toLong())
        return Color(colorLong.toInt())
    }

    fun getInitialButtonsTextColor(): Color {
        val colorLong = readSync(BUTTONS_TEXT_COLOR_KEY, Color.Black.toArgb().toLong())
        return Color(colorLong.toInt())
    }
    fun getInitialCalendarName(): String {
        return readSync(CALENDAR_NAME_KEY, context.getString(R.string.cal_name_sel))
    }

    val selectedCalendarIdFlow: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[SELECTED_CALENDAR_ID]
    }

    suspend fun clearAndSetSingleCalendar(calendarId: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_CALENDARS_KEY] = setOf(calendarId)
        }
    }

    fun getInitialSelectedCalendarId(): Long? = runBlocking {
        dataStore.data.map { it[SELECTED_CALENDAR_ID] }.first()
    }

    suspend fun saveSelectedCalendarId(id: Long?) {
        dataStore.edit { preferences ->
            if (id != null) {
                preferences[SELECTED_CALENDAR_ID] = id
            } else {
                preferences.remove(SELECTED_CALENDAR_ID)
            }
        }
    }

    val darkModeFlow: Flow<DarkModeConfig> = dataStore.data.map { preferences ->
        val name = preferences[DARK_MODE_KEY] ?: DarkModeConfig.SYSTEM.name
        try { DarkModeConfig.valueOf(name) } catch (e: Exception) { DarkModeConfig.SYSTEM }
    }

    suspend fun setDarkMode(config: DarkModeConfig) {
        dataStore.edit { it[DARK_MODE_KEY] = config.name }
    }

    val calendarNameFlow: Flow<String> = dataStore.data.map {
        it[CALENDAR_NAME_KEY] ?: context.getString(R.string.cal_name_sel)
    }

    suspend fun setCalendarName(name: String) {
        dataStore.edit { it[CALENDAR_NAME_KEY] = name }
    }


    val preferredSendMethodFlow: Flow<NotificationMethod> = dataStore.data.map { preferences ->
        val methodId = preferences[PREFERRED_SEND_METHOD_KEY] ?: NotificationMethod.SMS.id
        NotificationMethod.all().find { it.id == methodId } ?: NotificationMethod.SMS
    }

    suspend fun setPreferredSendMethod(method: NotificationMethod) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_SEND_METHOD_KEY] = method.id
        }
    }

    val headerColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[HEADER_COLOR_KEY] ?: Color.Blue.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setHeaderColor(color: Color) {
        dataStore.edit { it[HEADER_COLOR_KEY] = color.toArgb().toLong() }
    }

    val buttonsColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[BUTTONS_COLOR_KEY] ?: Color.Cyan.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setButtonsColor(color: Color) {
        dataStore.edit { it[BUTTONS_COLOR_KEY] = color.toArgb().toLong() }
    }

    val currentDayColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[CURRENT_DAY_COLOR_KEY] ?: Color.Red.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setCurrentDayColor(color: Color) {
        dataStore.edit { it[CURRENT_DAY_COLOR_KEY] = color.toArgb().toLong() }
    }

    val headerTextColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[HEADER_TEXT_COLOR_KEY] ?: Color.White.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setHeaderTextColor(color: Color) {
        dataStore.edit { it[HEADER_TEXT_COLOR_KEY] = color.toArgb().toLong() }
    }

    val buttonsTextColorFlow: Flow<Color> = dataStore.data.map {
        val colorLong = it[BUTTONS_TEXT_COLOR_KEY] ?: Color.Black.toArgb().toLong()
        Color(colorLong.toInt())
    }

    suspend fun setButtonsTextColor(color: Color) {
        dataStore.edit { it[BUTTONS_TEXT_COLOR_KEY] = color.toArgb().toLong() }
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
                    } catch (e: Exception) { null }
                } else { null }
            }
        }
    }

    suspend fun setTimeRanges(timeRanges: List<TimeRange>) {
        dataStore.edit {
            val rangeStrings = timeRanges.map { "${it.start}-${it.end}" }.toSet()
            it[TIME_RANGES_KEY] = rangeStrings
        }
    }

    val reminderMessageFlow: Flow<String> = dataStore.data.map {
        it[REMINDER_MESSAGE_KEY] ?: context.getString(R.string.msg)
    }

    suspend fun setReminderMessage(message: String) {
        dataStore.edit { it[REMINDER_MESSAGE_KEY] = message }
    }

    val reminderMessageFlow2: Flow<String> = dataStore.data.map {
        it[REMINDER_MESSAGE_KEY_2] ?: context.getString(R.string.msg2)
    }

    suspend fun setReminderMessage2(message: String) {
        dataStore.edit { it[REMINDER_MESSAGE_KEY_2] = message }
    }

    val reminderDaysThresholdFlow: Flow<Int> = dataStore.data.map {
        it[REMINDER_DAYS_THRESHOLD_KEY] ?: 7
    }

    suspend fun setReminderDaysThreshold(days: Int) {
        dataStore.edit { it[REMINDER_DAYS_THRESHOLD_KEY] = days }
    }

    val dateFormatFlow: Flow<String> = dataStore.data.map {
        it[DATE_FORMAT_KEY] ?: "dd/MM/yyyy"
    }

    suspend fun setDateFormat(format: String) {
        dataStore.edit { it[DATE_FORMAT_KEY] = format }
    }

    val defaultViewFlow: Flow<CalendarView> = dataStore.data.map { preferences ->
        val viewName = preferences[DEFAULT_VIEW_KEY] ?: CalendarView.MONTH.name
        try { CalendarView.valueOf(viewName) } catch (e: Exception) { CalendarView.MONTH }
    }

    suspend fun setDefaultView(view: CalendarView) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_VIEW_KEY] = view.name
        }
    }

    val batchSendingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[BATCH_SENDING_KEY] ?: false
        }

    suspend fun saveBatchSendingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BATCH_SENDING_KEY] = enabled
        }
    }
}