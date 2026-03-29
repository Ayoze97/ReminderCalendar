package com.remindercalendar

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime

enum class DarkModeConfig { LIGHT, DARK, SYSTEM }

class SettingsViewModel(
    application: Application,
    private val settingsManager: SettingsManager,
    private val eventManager: EventManager,
    val context: Context
) : AndroidViewModel(application) {

    init {
        clearOldUpdates()
    }

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

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        object Checking : UpdateStatus()
        data class NewVersionAvailable(val version: String, val url: String) : UpdateStatus()
        object UpToDate : UpdateStatus()
        object Error : UpdateStatus()
    }

    fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateStatus.value = UpdateStatus.Checking
            try {
                val url = URL("https://api.github.com/repos/Ayoze97/ReminderCalendar/releases/latest")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "ReminderCalendar-App")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()

                    val tagMatch = "\"tag_name\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(response)
                    val apkMatch = "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"".toRegex().find(response)

                    val remoteTag = tagMatch?.groupValues?.get(1) ?: ""
                    val apkUrl = apkMatch?.groupValues?.get(1) ?: ""

                    val remoteClean = remoteTag.replace("v", "").trim()
                    val localClean = currentVersion.replace("v", "").trim()

                    if (remoteClean.isNotEmpty() && remoteClean != localClean && apkUrl.isNotEmpty()) {
                        _updateStatus.value = UpdateStatus.NewVersionAvailable(remoteTag, apkUrl)
                    } else {
                        _updateStatus.value = UpdateStatus.UpToDate
                    }
                } else {
                    _updateStatus.value = UpdateStatus.Error
                }
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error
                e.printStackTrace()
            }
        }
    }

    val fileName = "ReminderCalendar_Update.apk"

    fun downloadAndInstallApk(apkUrl: String) {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    fun installApk(context: Context) {
        val downloadsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsFolder, fileName)

        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(installIntent)
            } catch (e: Exception) {

            }
        } else {
          //
        }
    }

    fun clearOldUpdates() {
        val downloadsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

        if (downloadsFolder != null && downloadsFolder.exists()) {
            val apkFiles = downloadsFolder.listFiles { file ->
                file.isFile && file.name.endsWith(".apk", ignoreCase = true)
            }

            apkFiles?.forEach { file ->
                val result = file.delete()
                android.util.Log.d("DEBUG_CLEANUP", "Borrando archivo antiguo: ${file.name} -> $result")
            }
        }
    }
}

class SettingsViewModelFactory(
    private val application: Application,
    private val settingsManager: SettingsManager,
    private val eventManager: EventManager,
    val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application, settingsManager, eventManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
