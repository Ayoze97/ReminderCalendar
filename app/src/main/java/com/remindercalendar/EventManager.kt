package com.remindercalendar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "events")

class EventManager(context: Context) {
    private val dataStore = context.dataStore
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .create()

    val eventsFlow: Flow<List<Event>> = dataStore.data
        .map { preferences ->
            val json = preferences[EVENTS_KEY] ?: "[]"
            if (json == "[]") {
                emptyList()
            } else {
                val type = object : TypeToken<List<Event>>() {}.type
                gson.fromJson(json, type)
            }
        }

    suspend fun saveEvents(events: List<Event>) {
        val json = gson.toJson(events)
        dataStore.edit { preferences ->
            preferences[EVENTS_KEY] = json
        }
    }

    companion object {
        private val EVENTS_KEY = stringPreferencesKey("events_key")
    }
}

class LocalDateAdapter : com.google.gson.JsonSerializer<LocalDate>, com.google.gson.JsonDeserializer<LocalDate> {
    override fun serialize(src: LocalDate?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?): com.google.gson.JsonElement {
        return com.google.gson.JsonPrimitive(src.toString())
    }

    override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): LocalDate {
        return LocalDate.parse(json?.asString)
    }
}

class LocalTimeAdapter : com.google.gson.JsonSerializer<LocalTime>, com.google.gson.JsonDeserializer<LocalTime> {
    override fun serialize(src: LocalTime?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?): com.google.gson.JsonElement {
        return com.google.gson.JsonPrimitive(src.toString())
    }

    override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): LocalTime {
        return LocalTime.parse(json?.asString)
    }
}
