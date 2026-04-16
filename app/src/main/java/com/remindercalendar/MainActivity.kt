package com.remindercalendar

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remindercalendar.services.AutomationBridge
import com.remindercalendar.ui.theme.ReminderCalendarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.core.net.toUri

enum class CalendarView {
    MONTH, WEEK, DAY
}

enum class Screen {
    CALENDAR, SETTINGS
}

data class TimeRange(val start: LocalTime, val end: LocalTime)
data class Event(
    val id: Long = 0,
    val googleCalendarId: Long = 0,
    val name: String,
    val person: String,
    val phone: String = "",
    val email: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val description: String = ""
)

class MainActivity : ComponentActivity() {

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val viewModel = ViewModelProvider(this@MainActivity)[SettingsViewModel::class.java]
            viewModel.installApk(context)
        }
    }

    private val eventViewModel: EventViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(onDownloadComplete, filter, RECEIVER_EXPORTED)

        splashScreen.setKeepOnScreenCondition {
            !eventViewModel.isReady.value
        }

        enableEdgeToEdge()
        setContent {
            MainApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

}

@Composable
fun MainApp() {
    val context = LocalContext.current

    val permissionWarning = stringResource(R.string.permission_warning)
    val grantPermissionWarning = stringResource(R.string.grant_perm_warning)
    val selStorage = stringResource(R.string.select_storage)

    val application = context.applicationContext as Application
    val settingsManager = remember { SettingsManager(context) }
    val eventManager = remember { EventManager(context) }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application, settingsManager, eventManager, context)
    )

    val eventViewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(eventManager, settingsManager, context)
    )
    val darkModeConfig: DarkModeConfig by settingsViewModel.darkMode.collectAsState(initial = DarkModeConfig.SYSTEM)
    val calendarName by settingsViewModel.calendarName.collectAsState()
    val headerColor by settingsViewModel.headerColor.collectAsState()
    val buttonsColor by settingsViewModel.buttonsColor.collectAsState()
    val currentDayColor by settingsViewModel.currentDayColor.collectAsState()
    val headerTextColor by settingsViewModel.headerTextColor.collectAsState()
    val buttonsTextColor by settingsViewModel.buttonsTextColor.collectAsState()
    val timeRanges by settingsViewModel.timeRanges.collectAsState()
    val selectedCalendarId by settingsViewModel.selectedCalendarId.collectAsState()
    val combinedEvents by eventViewModel.allEvents.collectAsState()
    val reminderMessage by settingsViewModel.reminderMessage.collectAsState()
    val reminderMessage2 by settingsViewModel.reminderMessage2.collectAsState()
    val daysThreshold = settingsViewModel.daysThreshold.collectAsState().value
    val onThresholdChange: (Int) -> Unit = { days ->
        settingsViewModel.setReminderDaysThreshold(days)
    }
    val dateFormat by settingsViewModel.dateFormat.collectAsState()
    val preferredSendMethod: NotificationMethod by settingsViewModel.preferredSendMethod.collectAsState()
    val defaultView by settingsViewModel.defaultView.collectAsState()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (settingsViewModel.selectedCalendarId.value == null) {
                settingsViewModel.setShowCalendarDialog(true)
            }
        } else {
            Toast.makeText(context,permissionWarning, Toast.LENGTH_SHORT).show()
        }
    }

    var currentScreen by remember { mutableStateOf(Screen.CALENDAR) }
    val useDarkTheme = when (darkModeConfig) {
        DarkModeConfig.LIGHT -> false
        DarkModeConfig.DARK -> true
        DarkModeConfig.SYSTEM -> isSystemInDarkTheme()
    }

    val batchSendingEnabled by settingsViewModel.batchSendingEnabled.collectAsState()

    ReminderCalendarTheme(darkTheme = useDarkTheme) {
        val activity = LocalActivity.current
        if (activity != null) {
            @Suppress("DEPRECATION")
            LaunchedEffect(headerColor, headerTextColor, useDarkTheme) {
                val window = activity.window

                window.apply {
                    statusBarColor = headerColor.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()
                }

                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !useDarkTheme && headerTextColor == Color.Black
            }
        }

        // Esto detecta cuando vuelves a la App para continuar enviando mensajes
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && AutomationBridge.isRunning) {
                    // La app ha vuelto a ser visible, lanzamos el siguiente si existe
                    val next = AutomationBridge.getNext()
                    if (next != null) {
                        val intent =
                            preferredSendMethod.intentBuilder(next.second, next.first, "", "")
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "¡Todos los mensajes enviados!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }


        Surface(color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {
                Screen.CALENDAR -> CalendarScreen(
                    calendarName = calendarName,
                    headerColor = headerColor,
                    buttonsColor = buttonsColor,
                    currentDayColor = currentDayColor,
                    headerTextColor = headerTextColor,
                    buttonsTextColor = buttonsTextColor,
                    timeRanges = timeRanges,
                    getReminderMessageFiltered = { date ->
                        settingsViewModel.getReminderMessageFiltered(date)
                    },
                    dateFormat = dateFormat,
                    defaultView = defaultView,
                    onSettingsClick = { currentScreen = Screen.SETTINGS },
                    cambiarPeriodo = ::cambiarPeriodo,
                    events = combinedEvents,
                    onAddEvent = { event, sync, _ ->
                        val calendarId = settingsViewModel.selectedCalendarId.value

                        val hasWritePermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_CALENDAR
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasWritePermission) {
                            calendarPermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
                            Toast.makeText(context, grantPermissionWarning, Toast.LENGTH_LONG).show()
                        } else if (calendarId == null) {
                            settingsViewModel.setShowCalendarDialog(true)
                            Toast.makeText(context, selStorage, Toast.LENGTH_LONG).show()
                        } else {
                            eventViewModel.addEvent(event, sync, calendarId)
                        }
                    },
                    onUpdateEvent = { eventViewModel.updateEvent(it) },
                    onDeleteEvent = { eventViewModel.deleteEvent(it) },
                    preferredSendMethod = preferredSendMethod,
                    onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                    eventViewModel = eventViewModel,
                    settingsViewModel = settingsViewModel
                )

                Screen.SETTINGS -> {
                    BackHandler {
                        currentScreen = Screen.CALENDAR
                    }
                    SettingsScreen(
                        calendarName = calendarName,
                        headerColor = headerColor,
                        buttonsColor = buttonsColor,
                        currentDayColor = currentDayColor,
                        headerTextColor = headerTextColor,
                        buttonsTextColor = buttonsTextColor,
                        timeRanges = timeRanges,
                        reminderMessage = reminderMessage,
                        reminderMessage2 = reminderMessage2,
                        daysThreshold = daysThreshold,
                        dateFormat = dateFormat,
                        onCalendarNameChange = { settingsViewModel.setCalendarName(it) },
                        onHeaderColorChange = { settingsViewModel.setHeaderColor(it) },
                        onButtonsColorChange = { settingsViewModel.setButtonsColor(it) },
                        onCurrentDayColorChange = { settingsViewModel.setCurrentDayColor(it) },
                        onHeaderTextColorChange = { settingsViewModel.setHeaderTextColor(it) },
                        onButtonsTextColorChange = { settingsViewModel.setButtonsTextColor(it) },
                        onTimeRangesChange = { settingsViewModel.setTimeRanges(it) },
                        onReminderMessageChange = { settingsViewModel.setReminderMessage(it) },
                        onReminderMessageChange2 = { settingsViewModel.setReminderMessage2(it) },
                        onThresholdChange = { days ->
                            settingsViewModel.setReminderDaysThreshold(days)
                        },
                        onDateFormatChange = { settingsViewModel.setDateFormat(it) },
                        preferredSendMethod = preferredSendMethod,
                        onPreferredSendMethodChange = { method ->
                            settingsViewModel.setPreferredSendMethod(method)
                        },
                        darkModeConfig = darkModeConfig,
                        onDarkModeChange = { settingsViewModel.setDarkMode(it) },
                        settingsViewModel = settingsViewModel,
                        onSettingsViewChange = { currentScreen = it },
                        onBatchSendingChange = { settingsViewModel.setBatchSendingEnabled(it) },
                        batchSendingEnabled = batchSendingEnabled,
                        onBack = { currentScreen = Screen.CALENDAR }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    calendarName: String,
    headerColor: Color,
    buttonsColor: Color,
    currentDayColor: Color,
    headerTextColor: Color,
    buttonsTextColor: Color,
    timeRanges: List<TimeRange>,
    events: List<Event>,
    getReminderMessageFiltered: (LocalDate) -> String,
    dateFormat: String,
    onSettingsClick: () -> Unit,
    cambiarPeriodo: (LocalDate, CalendarView, Int) -> LocalDate,
    onAddEvent: (Event, Boolean, Long?) -> Unit,
    onUpdateEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    preferredSendMethod: NotificationMethod,
    defaultView: CalendarView,
    eventViewModel: EventViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToSettings: () -> Unit,
) {
    var fechaSeleccionada by remember { mutableStateOf(LocalDate.now()) }
    var vistaActual by remember(defaultView) { mutableStateOf(defaultView) }
    var expanded by remember { mutableStateOf(false) }
    var year by remember { mutableIntStateOf(fechaSeleccionada.year) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var showEventDialogForTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf<Event?>(null) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedCalendarId by settingsViewModel.selectedCalendarId.collectAsState()
    val context = LocalContext.current
    val eventManager = remember { EventManager(context) }

    val delEventWarning = stringResource(R.string.del_event_warning)
    val calAccSet = stringResource(R.string.cal_account_setup)

    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: EventViewModel = viewModel()
    val meses = listOf(
        stringResource(R.string.month_jan),
        stringResource(R.string.month_feb),
        stringResource(R.string.month_mar), 
        stringResource(R.string.month_apr),
        stringResource(R.string.month_may), 
        stringResource(R.string.month_jun),
        stringResource(R.string.month_jul),
        stringResource(R.string.month_aug),
        stringResource(R.string.month_sep),
        stringResource(R.string.month_oct), 
        stringResource(R.string.month_nov),
        stringResource(R.string.month_dec)
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val batchSendingEnabled by settingsViewModel.batchSendingEnabled.collectAsState()

    if (editingEvent != null || showEventDialogForTime != null) {
        EventDialog(
            event = editingEvent,
            time = showEventDialogForTime,
            onDismiss = {
                editingEvent = null
                showEventDialogForTime = null
            },
            onConfirm = { eventName, person, phone, email, time ->
                val currentEditingEvent = editingEvent
                scope.launch {
                    if (currentEditingEvent != null) {
                        val updatedEvent = currentEditingEvent.copy(
                            name = eventName,
                            person = person,
                            phone = phone,
                            email = email,
                            time = time
                        )
                        onUpdateEvent(updatedEvent)
                    } else {
                        val newEvent = Event(
                            date = fechaSeleccionada,
                            time = time,
                            name = eventName,
                            person = person,
                            phone = phone,
                            email = email
                        )
                        onAddEvent(newEvent, true, null)
                    }
                    forceWidgetUpdate(context)
                }

                editingEvent = null
                showEventDialogForTime = null
            },
            onDelete = { event ->
                showDeleteConfirmationDialog = event
            }
        )
    }

    if (showDeleteConfirmationDialog != null) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteConfirmationDialog = null },
            onConfirm = {
                showDeleteConfirmationDialog?.let { event ->
                    onDeleteEvent(event)
                    val currentId = selectedCalendarId

                    scope.launch {
                        val isGoogle = withContext(Dispatchers.IO) {
                            val projection = arrayOf(android.provider.CalendarContract.Calendars.ACCOUNT_TYPE)
                            val selection = "${android.provider.CalendarContract.Calendars._ID} = ?"
                            val selectionArgs = arrayOf(currentId.toString())

                            var type: String? = null
                            context.contentResolver.query(
                                android.provider.CalendarContract.Calendars.CONTENT_URI,
                                projection,
                                selection,
                                selectionArgs,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    type = cursor.getString(0)
                                }
                            }
                            type == "com.google"
                        }

                        if (isGoogle) {
                            snackbarHostState.showSnackbar(
                                message = delEventWarning,
                                duration = SnackbarDuration.Short
                            )
                        }
                        forceWidgetUpdate(context)
                    }
                }
                showDeleteConfirmationDialog = null
                editingEvent = null
                showEventDialogForTime = null
            },
        )
    }

    val filteredEvents = remember(events, searchQuery) {
        if (searchQuery.isEmpty()) {
            events
        } else {
            events.filter { evento ->
                evento.name.contains(searchQuery, ignoreCase = true) ||
                        evento.person.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            BackHandler(enabled = drawerState.isOpen) {
                scope.launch {
                    drawerState.close()
                }
            }
            ModalDrawerSheet {
                Box(Modifier.fillMaxHeight()) {
                    Column(Modifier.fillMaxHeight()) {
                        Text(stringResource(R.string.sel_view), modifier = Modifier.padding(16.dp))

                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.month_view)) },
                            selected = vistaActual == CalendarView.MONTH,
                            onClick = {
                                vistaActual = CalendarView.MONTH
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.week_view)) },
                            selected = vistaActual == CalendarView.WEEK,
                            onClick = {
                                vistaActual = CalendarView.WEEK
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.ViewWeek, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.day_view)) },
                            selected = vistaActual == CalendarView.DAY,
                            onClick = {
                                vistaActual = CalendarView.DAY
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.ViewDay, contentDescription = null) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 28.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.refresh)) },
                            selected = false,
                            onClick = {
                                viewModel.refreshEvents()
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.weight(1f))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 28.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    IconButton(
                        onClick = {
                            onSettingsClick()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(calendarName, color = headerTextColor) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = headerTextColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = headerColor)
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = headerColor,
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = {
                            if (it) {
                                year = fechaSeleccionada.year
                            }
                            expanded = it
                        },
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                                .background(buttonsColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val mesActual = meses[fechaSeleccionada.monthValue - 1]
                            val añoActual = fechaSeleccionada.year

                            Text(
                                text = "$mesActual $añoActual",
                                style = MaterialTheme.typography.titleLarge,
                                color = buttonsTextColor
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.month_change),
                                tint = buttonsTextColor
                            )
                        }

                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            for (month in 1..12) {
                                DropdownMenuItem(
                                    text = { Text(meses[month - 1]) },
                                    onClick = {
                                        fechaSeleccionada = fechaSeleccionada.withYear(year).withMonth(month)
                                        expanded = false
                                    },
                                    modifier = Modifier.height(36.dp)
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.prev_year)) },
                                onClick = {
                                    year--
                                    fechaSeleccionada = fechaSeleccionada.withYear(year)
                                },
                                modifier = Modifier.padding(vertical = 0.dp)
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.next_year)) },
                                onClick = {
                                    year++
                                    fechaSeleccionada = fechaSeleccionada.withYear(year)
                                },
                                modifier = Modifier.padding(vertical = 0.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    val eventsForSelectedDate = remember(events, fechaSeleccionada) {
                        events.filter { it.date == fechaSeleccionada }
                    }
                    val formatter = DateTimeFormatter.ofPattern("HH:mm")
                    val insertedDate = stringResource(R.string.inserted_date)
                    val insertedTime = stringResource(R.string.inserted_time)
                    val isServiceEnabled = settingsViewModel.isAccessibilityServiceEnabled(context)
                    val noEvent = stringResource(R.string.no_event)

                    if (batchSendingEnabled && isServiceEnabled) {
                        IconButton(
                            onClick = {

                                if (eventsForSelectedDate.isNotEmpty()) {
                                    val listToSend = eventsForSelectedDate.map { event ->
                                        val msg = getReminderMessageFiltered(event.date)
                                            .replace(
                                                insertedDate,
                                                event.date.format(
                                                    DateTimeFormatter.ofPattern(dateFormat)
                                                )
                                            )
                                            .replace(insertedTime, event.time.format(formatter))

                                        event.phone to msg
                                    }
                                    AutomationBridge.startQueue(listToSend)

                                    val first = AutomationBridge.getNext()
                                    if (first != null) {
                                        val intent = preferredSendMethod.intentBuilder(
                                            first.second,
                                            first.first,
                                            "",
                                            ""
                                        )
                                        context.startActivity(intent)
                                    }
                                } else {
                                    Toast.makeText(context, noEvent, Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = buttonsTextColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            showSearchDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = buttonsTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(buttonsColor, RoundedCornerShape(8.dp))
                            .clickable(onClick = { fechaSeleccionada = LocalDate.now() })
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.today),
                            style = MaterialTheme.typography.titleLarge,
                            color = buttonsTextColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.pointerInput(Unit) {
                        var totalHorizontalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalHorizontalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalHorizontalDrag += dragAmount
                            },
                            onDragEnd = {
                                if (totalHorizontalDrag > 50) {
                                    fechaSeleccionada = cambiarPeriodo(fechaSeleccionada, vistaActual, -1)
                                } else if (totalHorizontalDrag < -50) {
                                    fechaSeleccionada = cambiarPeriodo(fechaSeleccionada, vistaActual, 1)
                                }
                            }
                        )
                    }
                ) {
                    when (vistaActual) {
                        CalendarView.MONTH -> {
                            VistaMensual(
                                fecha = fechaSeleccionada,
                                currentDayColor = currentDayColor,
                                selectedDate = fechaSeleccionada,
                                onDateSelected = { fechaSeleccionada = it }
                            )
                        }

                        CalendarView.WEEK -> {
                            VistaSemanal(
                                fecha = fechaSeleccionada,
                                currentDayColor = currentDayColor,
                                selectedDate = fechaSeleccionada,
                                onDateSelected = { fechaSeleccionada = it }
                            )
                        }

                        CalendarView.DAY -> {
                            VistaDiaria(
                                fecha = fechaSeleccionada,
                                currentDayColor = currentDayColor,
                                selectedDate = fechaSeleccionada,
                                onDateSelected = { fechaSeleccionada = it }
                            )
                        }
                    }
                }

                when (vistaActual) {
                    CalendarView.MONTH -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        val eventsForSelectedDate = remember(events, fechaSeleccionada) {
                            events.filter { it.date == fechaSeleccionada }
                        }
                        TimeSlotList(
                            timeRanges = timeRanges,
                            events = eventsForSelectedDate,
                            getReminderMessageFiltered = { fecha ->
                                settingsViewModel.getReminderMessageFiltered(fecha)
                            },
                            dateFormat = dateFormat,
                            onTimeSelected = { time ->
                                val hasWritePermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_CALENDAR
                                ) == PackageManager.PERMISSION_GRANTED

                                val hasAccount = settingsViewModel.selectedCalendarId.value != null

                                if (hasWritePermission && hasAccount) {
                                    showEventDialogForTime = time
                                } else {
                                    settingsViewModel.triggerCalendarSelector()

                                    Toast.makeText(context,calAccSet, Toast.LENGTH_SHORT).show()

                                    onNavigateToSettings()
                                }
                            },
                            onEventSelected = { event -> editingEvent = event },
                            preferredSendMethod = preferredSendMethod
                        )
                    }

                    CalendarView.WEEK -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        val eventsForSelectedDate = remember(events, fechaSeleccionada) {
                            events.filter { it.date == fechaSeleccionada }
                        }
                        TimeSlotList(
                            timeRanges = timeRanges,
                            events = eventsForSelectedDate,
                            getReminderMessageFiltered = { fecha ->
                                settingsViewModel.getReminderMessageFiltered(fecha)
                            },
                            dateFormat = dateFormat,
                            onTimeSelected = { time ->
                                val hasWritePermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_CALENDAR
                                ) == PackageManager.PERMISSION_GRANTED

                                val hasAccount = settingsViewModel.selectedCalendarId.value != null

                                if (hasWritePermission && hasAccount) {
                                    showEventDialogForTime = time
                                } else {
                                    settingsViewModel.triggerCalendarSelector()

                                    Toast.makeText(context,calAccSet, Toast.LENGTH_SHORT).show()

                                    onNavigateToSettings()
                                }
                            },
                            onEventSelected = { event -> editingEvent = event },
                            preferredSendMethod = preferredSendMethod
                        )
                    }

                    CalendarView.DAY -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        val eventsForSelectedDate = remember(events, fechaSeleccionada) {
                            events.filter { it.date == fechaSeleccionada }
                        }
                        TimeSlotList(
                            timeRanges = timeRanges,
                            events = eventsForSelectedDate,
                            getReminderMessageFiltered = { fecha ->
                                settingsViewModel.getReminderMessageFiltered(fecha)
                            },
                            dateFormat = dateFormat,
                            onTimeSelected = { time ->
                                val hasWritePermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_CALENDAR
                                ) == PackageManager.PERMISSION_GRANTED

                                val hasAccount = settingsViewModel.selectedCalendarId.value != null

                                if (hasWritePermission && hasAccount) {
                                    showEventDialogForTime = time
                                } else {
                                    settingsViewModel.triggerCalendarSelector()

                                    Toast.makeText(context,calAccSet, Toast.LENGTH_SHORT).show()

                                    onNavigateToSettings()
                                }
                            },
                            onEventSelected = { event -> editingEvent = event },
                            preferredSendMethod = preferredSendMethod
                        )
                    }
                }
            }
        }
    }

    if (showSearchDialog) {
        SearchDialog(
            allEvents = events,
            onDismiss = { showSearchDialog = false },
            onEventClick = { event ->
                fechaSeleccionada = event.date
            }
        )
    }

}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.del_confirm_warning_header)) },
        text = { Text(stringResource(R.string.del_confirm_warning_msg)) },
        confirmButton = {
            TextButton(onClick = onConfirm)
            {
                Text(stringResource(R.string.del))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@SuppressLint("UseKtx")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    calendarName: String,
    headerColor: Color,
    buttonsColor: Color,
    currentDayColor: Color,
    headerTextColor: Color,
    buttonsTextColor: Color,
    timeRanges: List<TimeRange>,
    reminderMessage: String,
    reminderMessage2: String,
    daysThreshold: Int,
    dateFormat: String,
    onCalendarNameChange: (String) -> Unit,
    onHeaderColorChange: (Color) -> Unit,
    onButtonsColorChange: (Color) -> Unit,
    onCurrentDayColorChange: (Color) -> Unit,
    onHeaderTextColorChange: (Color) -> Unit,
    onButtonsTextColorChange: (Color) -> Unit,
    onTimeRangesChange: (List<TimeRange>) -> Unit,
    onReminderMessageChange: (String) -> Unit,
    onReminderMessageChange2: (String) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onDateFormatChange: (String) -> Unit,
    preferredSendMethod: NotificationMethod,
    onPreferredSendMethodChange: (NotificationMethod) -> Unit,
    darkModeConfig: DarkModeConfig,
    onDarkModeChange: (DarkModeConfig) -> Unit,
    settingsViewModel: SettingsViewModel,
    onSettingsViewChange: (Screen) -> Unit,
    onBatchSendingChange: (Boolean) -> Unit,
    batchSendingEnabled: Boolean,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    var showNameDialog by remember { mutableStateOf(false) }
    var showReminderMessageDialog by remember { mutableStateOf(false) }
    var showReminderMessageDialog2 by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var colorToEdit by remember { mutableStateOf<((Color) -> Unit)?>(null) }
    var showTextColorDialog by remember { mutableStateOf<((Color) -> Unit)?>(null) }
    var isCurrentDayColor by remember { mutableStateOf(false) }
    var showTimeRangeDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val invFormat = stringResource(R.string.inv_format)
    val permWarning = stringResource(R.string.permission_warning)
    val interactionSource = remember { MutableInteractionSource() }
    val highlightState = settingsViewModel.highlightCalendarOption.collectAsState()
    val highlight = highlightState.value
    val forceOpen by settingsViewModel.forceShowCalendarSelector.collectAsState()
    val defaultView by settingsViewModel.defaultView.collectAsState()
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    val isDateNeeded = reminderMessage.contains(stringResource(R.string.inserted_date), ignoreCase = true) || reminderMessage2.contains(stringResource(R.string.inserted_date), ignoreCase = true)
    val updateStatus by settingsViewModel.updateStatus.collectAsState()
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val currentVersionName = packageInfo.versionName
    val currentVersion = "v$currentVersionName"
    val datePreview = remember(dateFormat) {
        try {
            LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat))
        } catch (e: Exception) {
            invFormat
        }
    }
    val animatedColor by animateColorAsState(
        targetValue = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "Highlighter"
    )

    val isServiceActuallyEnabled = remember(batchSendingEnabled) {
        settingsViewModel.isAccessibilityServiceEnabled(context)
    }

    LaunchedEffect(forceOpen) {
        if (forceOpen) {
            delay(800)
            showCalendarDialog = true
            settingsViewModel.clearCalendarSelectorTrigger()
        }
    }

    if (showNameDialog) {
        EditCalendarNameDialog(
            currentName = calendarName,
            onDismiss = { showNameDialog = false },
            onConfirm = { onCalendarNameChange(it); showNameDialog = false }
        )
    }
    if (showReminderMessageDialog) {
        EditReminderMessageDialog(
            currentMessage = reminderMessage,
            onDismiss = { showReminderMessageDialog = false },
            onConfirm = { onReminderMessageChange(it); showReminderMessageDialog = false }
        )
    }
    if (showReminderMessageDialog2) {
        EditReminderMessageDialog(
            currentMessage = reminderMessage2,
            onDismiss = { showReminderMessageDialog2 = false },
            onConfirm = { onReminderMessageChange2(it); showReminderMessageDialog2 = false }
        )
    }
    if (showThresholdDialog) {
        var tempDays by remember { mutableStateOf(daysThreshold.toString()) }

        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text(stringResource(R.string.time_until)) },
            text = {
                Column {
                    Text(stringResource(R.string.time_until_msg), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempDays,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tempDays = it },
                        label = { Text(stringResource(R.string.days)) },
                        placeholder = { Text("7") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val days = tempDays.toIntOrNull() ?: 7
                    onThresholdChange(days)
                    showThresholdDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showThresholdDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showDateFormatDialog) {
        EditDateFormatDialog(
            currentFormat = dateFormat,
            onDismiss = { showDateFormatDialog = false },
            onConfirm = { it ->
                onDateFormatChange(it)
                showDateFormatDialog = false }
        )
    }
    colorToEdit?.let {
        ColorPickerDialog(
            isCurrentDayColor = isCurrentDayColor,
            onDismiss = { colorToEdit = null; isCurrentDayColor = false },
            onColorSelected = { it(it); colorToEdit = null; isCurrentDayColor = false }
        )
    }
    showTextColorDialog?.let {
        TextColorDialog(
            onDismiss = { showTextColorDialog = null },
            onColorSelected = { it(it); showTextColorDialog = null }
        )
    }
    if (showTimeRangeDialog) {
        TimeRangeSettingsDialog(
            initialRanges = timeRanges,
            onDismiss = { showTimeRangeDialog = false },
            onConfirm = { onTimeRangesChange(it); showTimeRangeDialog = false }
        )
    }

    if (showDateFormatDialog) {
        EditDateFormatDialog(
            currentFormat = dateFormat,
            onDismiss = { showDateFormatDialog = false },
            onConfirm = { nuevoFormato ->
                onDateFormatChange(nuevoFormato)
                showDateFormatDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
                ) {
                Text(
                    text = stringResource(R.string.app_theme),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Box {
                    var expandedTema by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { expandedTema = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = when (darkModeConfig) {
                                DarkModeConfig.LIGHT -> stringResource(R.string.light_mode)
                                DarkModeConfig.DARK -> stringResource(R.string.dark_mode)
                                DarkModeConfig.SYSTEM -> stringResource(R.string.system_mode)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expandedTema,
                        onDismissRequest = { expandedTema = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.light_mode)) },
                            leadingIcon = { Icon(Icons.Default.LightMode, null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                onDarkModeChange(DarkModeConfig.LIGHT)
                                expandedTema = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dark_mode)) },
                            leadingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                onDarkModeChange(DarkModeConfig.DARK)
                                expandedTema = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.system_mode)) },
                            leadingIcon = { Icon(Icons.Default.SettingsSuggest, null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                onDarkModeChange(DarkModeConfig.SYSTEM)
                                expandedTema = false
                            }
                        )
                    }
                }
            }
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNameDialog = true }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.cal_name), style = MaterialTheme.typography.titleMedium)
                Text(calendarName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimeRangeDialog = true }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.range), style = MaterialTheme.typography.titleMedium)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showReminderMessageDialog = true }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.rem_msg_1), style = MaterialTheme.typography.titleMedium)
                    Text(reminderMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showReminderMessageDialog2 = true }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.rem_msg_2), style = MaterialTheme.typography.titleMedium)
                    Text(reminderMessage2, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
                    .clickable { showThresholdDialog = true }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.time_until), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = LocalResources.current.getQuantityString(R.plurals.time_until_msg_pv, daysThreshold, daysThreshold), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
                    .clickable(enabled = isDateNeeded) { showDateFormatDialog = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.date_format),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDateNeeded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                    if (isDateNeeded) {
                        Text(
                            text = dateFormat.ifBlank { "dd/MM/yyyy" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.date_msg_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                if (isDateNeeded) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(20.dp))
                }
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            var expandedEnvio by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.pref_send_method),
                    style = MaterialTheme.typography.titleMedium
                )

                Box {
                    OutlinedButton(
                        onClick = { expandedEnvio = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = getMethodName(preferredSendMethod),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expandedEnvio,
                        onDismissRequest = { expandedEnvio = false }
                    ) {
                        NotificationMethod.all().forEach { method ->
                            DropdownMenuItem(
                                text = {
                                    Text(text = getMethodName(method))
                                },
                                leadingIcon = {
                                    method.DrawIcon(modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    onPreferredSendMethodChange(method)
                                    expandedEnvio = false
                                }
                            )
                        }
                    }
                }
            }

            preferredSendMethod.warning?.let { resId ->
                Text(
                    text = stringResource(resId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.allow_mass_rem),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = batchSendingEnabled && isServiceActuallyEnabled,
                    onCheckedChange = { isChecked ->
                        onBatchSendingChange(isChecked)
                        if (isChecked) {

                            onBatchSendingChange(true)

                            if (!isServiceActuallyEnabled) {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.def_view),
                    style = MaterialTheme.typography.titleMedium
                )
                var expandedVista by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expandedVista = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = when(defaultView) {
                                CalendarView.MONTH -> stringResource(R.string.month_view)
                                CalendarView.WEEK -> stringResource(R.string.week_view)
                                CalendarView.DAY -> stringResource(R.string.day_view)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expandedVista,
                        onDismissRequest = { expandedVista = false }
                    ) {
                        CalendarView.entries.forEach { view ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when(view) {
                                            CalendarView.MONTH -> stringResource(R.string.month_view)
                                            CalendarView.WEEK -> stringResource(R.string.week_view)
                                            CalendarView.DAY -> stringResource(R.string.day_view)
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when(view) {
                                            CalendarView.MONTH -> Icons.Default.DateRange
                                            CalendarView.WEEK -> Icons.Default.ViewWeek
                                            CalendarView.DAY -> Icons.Default.ViewDay
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    settingsViewModel.setDefaultView(view)
                                    expandedVista = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            var showCalendarDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val readGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false
                val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] ?: false

                if (readGranted && writeGranted) {
                    settingsViewModel.loadCalendars()
                    showCalendarDialog = true
                } else {
                    Toast.makeText(context,permWarning , Toast.LENGTH_SHORT).show()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(animatedColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CALENDAR
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasWritePermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_CALENDAR
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission && hasWritePermission) {
                                settingsViewModel.loadCalendars()
                                showCalendarDialog = true
                            } else {
                                launcher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR
                                    )
                                )
                            }
                        }
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.cal_selected),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (showCalendarDialog) {
                val calendars by settingsViewModel.availableCalendars.collectAsState()
                val selectedCalendarId by settingsViewModel.selectedCalendarId.collectAsState()

                AlertDialog(
                    onDismissRequest = { showCalendarDialog = false },
                    title = { Text(stringResource(R.string.cal_select)) },
                    text = {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(calendars) { calendar ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            settingsViewModel.selectPrimaryCalendar(calendar.id.toString())
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(Color(calendar.color), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = calendar.displayName, style = MaterialTheme.typography.bodyLarge)
                                        Text(text = calendar.accountName, style = MaterialTheme.typography.bodySmall)
                                    }

                                    RadioButton(
                                        selected = calendar.id == selectedCalendarId,
                                        onClick = {
                                            settingsViewModel.selectPrimaryCalendar(calendar.id.toString())
                                        }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCalendarDialog = false }) {
                            Text(stringResource(R.string.accept))
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.visual_custom),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { colorToEdit = onHeaderColorChange }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.color_header), style = MaterialTheme.typography.titleMedium)
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = headerColor,
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {}
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp)
                        .clickable { showTextColorDialog = onHeaderTextColorChange }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.color_header_text), style = MaterialTheme.typography.bodyLarge)
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = headerTextColor,
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {}
                }
            }
            HorizontalDivider()

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { colorToEdit = onButtonsColorChange }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.color_button), style = MaterialTheme.typography.titleMedium)
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = buttonsColor,
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {}
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp)
                        .clickable { showTextColorDialog = onButtonsTextColorChange }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.color_button_text), style = MaterialTheme.typography.bodyLarge)
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = buttonsTextColor,
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {}
                }
            }
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCurrentDayColor = true; colorToEdit = onCurrentDayColorChange }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.color_day), style = MaterialTheme.typography.titleMedium)
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = currentDayColor,
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                }
            }
            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.current_ver, currentVersion),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { settingsViewModel.checkForUpdates(currentVersion) },
                    enabled = updateStatus !is SettingsViewModel.UpdateStatus.Checking,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonsColor)
                ) {
                    Text(
                        text = stringResource(R.string.check_upd),
                        color = buttonsTextColor
                    )
                }

                when (val status = updateStatus) {
                    is SettingsViewModel.UpdateStatus.Checking -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    is SettingsViewModel.UpdateStatus.NewVersionAvailable -> {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.new_ver, status.version),
                                color = Color.Green,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val downloading = stringResource(R.string.downloading)

                            Button(
                                onClick = {
                                    settingsViewModel.downloadAndInstallApk(status.url)
                                    Toast.makeText(context,downloading, Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonsColor,
                                    contentColor = buttonsTextColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.down_ins))
                            }

                            TextButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, status.url.toUri())
                                    context.startActivity(intent)
                                } catch (e: Exception) {

                                }
                            }) {
                                Text(stringResource(R.string.open_browser), fontSize = 12.sp)
                            }
                        }
                    }
                    is SettingsViewModel.UpdateStatus.UpToDate -> Text(stringResource(R.string.last_ver), color = Color.Gray)
                    is SettingsViewModel.UpdateStatus.Error -> Text(stringResource(R.string.check_error), color = Color.Red)
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun EditDateFormatDialog(
    currentFormat: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentFormat) }

    val preview = remember(text) {
        try {
            val pattern = if (text.isBlank()) "dd/MM/yyyy" else text
            LocalDate.now().format(DateTimeFormatter.ofPattern(pattern))
        } catch (e: Exception) {
            null
        }
    }
    val isError = preview == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_date_format)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.format)) },
                    isError = isError,
                    placeholder = { Text("dd/MM/yyyy") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (isError) {
                            Text(stringResource(R.string.invalid_char))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.preview),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = preview ?: stringResource(R.string.inv_date_format),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.date_example),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalFormat = text.ifBlank { "dd/MM/yyyy" }
                    onConfirm(finalFormat)
                },
                enabled = !isError
            ) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditReminderMessageDialog(
    currentMessage: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(currentMessage)) }

    val insertedDate = stringResource(R.string.inserted_date)
    val insertedTime = stringResource(R.string.inserted_time)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rem_msg_edit)) },
        text = {
            Column {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    label = { Text(stringResource(R.string.message)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TextButton(onClick = {
                        val newText = textFieldValue.text.replaceRange(textFieldValue.selection.start, textFieldValue.selection.end, insertedDate)
                        textFieldValue = TextFieldValue(newText)
                    }) {
                        Text(stringResource(R.string.insert_date))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val newText = textFieldValue.text.replaceRange(textFieldValue.selection.start, textFieldValue.selection.end, insertedTime)
                        textFieldValue = TextFieldValue(newText)
                    }) {
                        Text(stringResource(R.string.insert_time))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(textFieldValue.text) }) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EventDialog(
    event: Event?,
    time: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, LocalTime) -> Unit,
    onDelete: (Event) -> Unit
) {
    val context = LocalContext.current
    var eventName by remember { mutableStateOf(event?.name ?: "") }
    var person by remember { mutableStateOf(event?.person ?: "") }
    val initialTime = event?.time ?: time ?: LocalTime.now()
    val is24Hour = DateFormat.is24HourFormat(context)
    var eventTime by remember { mutableStateOf(initialTime.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a"))) }
    var isTimeValid by remember { mutableStateOf(true) }
    var phone by remember { mutableStateOf(event?.phone ?: "") }
    var email by remember { mutableStateOf(event?.email ?: "") }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { contactUri ->
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            )

            context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))

                    person = name

                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) {
                            val number = phoneCursor.getString(
                                phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            )
                            phone = number.replace(Regex("[^0-9]"), "")
                        } else {
                            phone = ""
                        }
                    }

                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )?.use { emailCursor ->
                        if (emailCursor.moveToFirst()) {
                            val mail = emailCursor.getString(
                                emailCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                            )
                            email = mail
                        } else {
                            email = ""
                        }
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                contactsLauncher.launch(null)
            } else {
                //
            }
        }
    )

    fun validateTime(time: String, is24Hour: Boolean): Boolean {
        return try {
            val pattern = if (is24Hour) "HH:mm" else "h:mm a"
            val formatter = DateTimeFormatter.ofPattern(pattern, java.util.Locale.US)

            LocalTime.parse(time.trim(),formatter)
            true
        } catch (e: Exception) {
            false
        }
    }

    val isConfirmEnabled = eventName.isNotBlank() && isTimeValid

    fun adjustTime(currentTime: String, minutesToAdd: Int, is24Hour: Boolean): String {
        val formatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "hh:mm a")
        return try {
            val time = LocalTime.parse(currentTime, formatter)
            val newTime = time.plusMinutes(minutesToAdd.toLong())
            newTime.format(formatter)
        } catch (e: Exception) {
            currentTime
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event != null) stringResource(R.string.event_edit) else stringResource(R.string.event_new)) },
        text = {
            Column {
                OutlinedTextField(value = eventName, onValueChange = { eventName = it }, label = { Text(
                    stringResource(R.string.event_name)
                ) })
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = person,
                        onValueChange = { person = it },
                        label = { Text(stringResource(R.string.person)) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) -> {
                                contactsLauncher.launch(null)
                            }
                            else -> {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    }) {
                        Icon(painterResource(id = R.drawable.contact_icon), contentDescription = stringResource(
                            R.string.contact_select
                        ))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = eventTime,
                        onValueChange = {
                            eventTime = it
                            isTimeValid = validateTime(it, is24Hour)
                        },
                        label = { Text(stringResource(R.string.hour)) },
                        isError = !isTimeValid,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AdjustmentButton("-1H") {
                                eventTime = adjustTime(eventTime, -60, is24Hour)
                                isTimeValid = validateTime(eventTime, is24Hour)
                            }
                            AdjustmentButton("-30min") {
                                eventTime = adjustTime(eventTime, -30, is24Hour)
                                isTimeValid = validateTime(eventTime, is24Hour)
                            }
                            AdjustmentButton("-15min") {
                                eventTime = adjustTime(eventTime, -15, is24Hour)
                                isTimeValid = validateTime(eventTime, is24Hour)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AdjustmentButton("+1H") {
                                eventTime = adjustTime(eventTime, 60, is24Hour)
                                isTimeValid = validateTime(eventTime, is24Hour)
                            }
                            AdjustmentButton("+30min") {
                                eventTime = adjustTime(eventTime, 30, is24Hour)
                                isTimeValid = validateTime(eventTime, is24Hour)
                            }
                            AdjustmentButton("+15min") {
                                eventTime = adjustTime(eventTime, 15, is24Hour)
                                isTimeValid = validateTime(eventTime, is24Hour)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.phone_wa_sms)) },
                    placeholder = { Text(stringResource(R.string.phone_example)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.pref_send_method_mail)) },
                    placeholder = { Text(stringResource(R.string.mail_example)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanTime = eventTime.trim()
                    val is24Hour = DateFormat.is24HourFormat(context)
                    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
                    val formatter = DateTimeFormatter.ofPattern(pattern, java.util.Locale.US)

                    val parsedTime = try {
                        LocalTime.parse(cleanTime, formatter)
                    } catch (e: Exception) {
                        try {
                            LocalTime.parse(cleanTime, DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (e2: Exception) {
                            LocalTime.now()
                        }
                    }

                    onConfirm(eventName, person, phone, email, parsedTime)
                },
                enabled = isConfirmEnabled
            ) {
                Text(if (event != null) stringResource(R.string.save) else stringResource(R.string.accept))
            }
        },
        dismissButton = {
            Row {
                if (event != null) {
                    TextButton(onClick = { onDelete(event) }) {
                        Text(stringResource(R.string.del))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
fun AdjustmentButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(width = 50.dp, height = 32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TimeRangeSettingsDialog(
    initialRanges: List<TimeRange>,
    onDismiss: () -> Unit,
    onConfirm: (List<TimeRange>) -> Unit
) {
    var isSplit by remember { mutableStateOf(initialRanges.size > 1) }

    var range1Start by remember { mutableStateOf(initialRanges.getOrNull(0)?.start?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "07:00") }
    var range1End by remember { mutableStateOf(initialRanges.getOrNull(0)?.end?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "20:00") }
    var range2Start by remember { mutableStateOf(initialRanges.getOrNull(1)?.start?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "16:00") }
    var range2End by remember { mutableStateOf(initialRanges.getOrNull(1)?.end?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "20:00") }

    var isRange1StartValid by remember { mutableStateOf(true) }
    var isRange1EndValid by remember { mutableStateOf(true) }
    var isRange2StartValid by remember { mutableStateOf(true) }
    var isRange2EndValid by remember { mutableStateOf(true) }

    fun validateTime(time: String): Boolean {
        return try {
            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }

    val isConfirmEnabled = if (isSplit) {
        isRange1StartValid && isRange1EndValid && isRange2StartValid && isRange2EndValid
    } else {
        isRange1StartValid && isRange1EndValid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.range_config)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSplit, onCheckedChange = { isSplit = it })
                    Text(stringResource(R.string.split_sch))
                }

                if (isSplit) {
                    Text(stringResource(R.string.morning), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        OutlinedTextField(value = range1Start, onValueChange = { range1Start = it; isRange1StartValid = validateTime(it) }, label = { Text(
                            stringResource(R.string.start)
                        ) }, modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp), isError = !isRange1StartValid)
                        OutlinedTextField(value = range1End, onValueChange = { range1End = it; isRange1EndValid = validateTime(it) }, label = { Text(
                            stringResource(R.string.end)
                        ) }, modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp), isError = !isRange1EndValid)
                    }
                    Text(stringResource(R.string.afternoon), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        OutlinedTextField(value = range2Start, onValueChange = { range2Start = it; isRange2StartValid = validateTime(it) }, label = { Text(stringResource(R.string.start)) }, modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp), isError = !isRange2StartValid)
                        OutlinedTextField(value = range2End, onValueChange = { range2End = it; isRange2EndValid = validateTime(it) }, label = { Text(stringResource(R.string.end)) }, modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp), isError = !isRange2EndValid)
                    }
                } else {
                    Text(stringResource(R.string.full_sch), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        OutlinedTextField(value = range1Start, onValueChange = { range1Start = it; isRange1StartValid = validateTime(it) }, label = { Text(stringResource(R.string.start)) }, modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp), isError = !isRange1StartValid)
                        OutlinedTextField(value = range1End, onValueChange = { range1End = it; isRange1EndValid = validateTime(it) }, label = { Text(stringResource(R.string.end)) }, modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp), isError = !isRange1EndValid)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ranges = mutableListOf<TimeRange>()
                    val formatter = DateTimeFormatter.ofPattern("HH:mm")

                    val r1s = LocalTime.parse(range1Start, formatter)
                    val r1e = LocalTime.parse(range1End, formatter)
                    ranges.add(TimeRange(r1s, r1e))

                    if (isSplit) {
                        val r2s = LocalTime.parse(range2Start, formatter)
                        val r2e = LocalTime.parse(range2End, formatter)
                        ranges.add(TimeRange(r2s, r2e))
                    }
                    onConfirm(ranges)
                    onDismiss()
                },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun TextColorDialog(onDismiss: () -> Unit, onColorSelected: (Color) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_text_dialog)) },
        text = {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onColorSelected(Color.White) },
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {}
                Surface(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onColorSelected(Color.Black) },
                    color = Color.Black
                ) {}
            }
        },
        confirmButton = { }
    )
}

@Composable
fun ColorPickerDialog(
    isCurrentDayColor: Boolean,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Magenta,
        Color.Cyan,
        Color.LightGray,
        Color.Black,
        Color.White
    ).filter { !isCurrentDayColor || it != Color.White }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_dialog)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                content = {
                    items(colors) { color ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(48.dp)
                                    .aspectRatio(1f)
                                    .clickable { onColorSelected(color) },
                                color = color,
                                border = if (color == Color.White) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                            ) {}
                        }
                    }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun EditCalendarNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cal_name_edit)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.name)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

fun cambiarPeriodo(fecha: LocalDate, vista: CalendarView, direccion: Int): LocalDate {
    return when (vista) {
        CalendarView.DAY -> fecha.plusDays(direccion.toLong())
        CalendarView.WEEK -> fecha.plusWeeks(direccion.toLong())
        CalendarView.MONTH -> fecha.plusMonths(direccion.toLong())
    }
}

@Composable
fun EncabezadoSemana() {
    val dias = listOf(
        stringResource(R.string.day_mon),
        stringResource(R.string.day_tue),
        stringResource(R.string.day_wed),
        stringResource(R.string.day_thu),
        stringResource(R.string.day_fri),
        stringResource(R.string.day_sat),
        stringResource(R.string.day_sun)
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        dias.forEach {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(it, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun VistaDiaria(
    fecha: LocalDate,
    currentDayColor: Color,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val diaSemanaLetra = when (fecha.dayOfWeek.value) {
        1 -> stringResource(R.string.day_mon)
        2 -> stringResource(R.string.day_tue)
        3 -> stringResource(R.string.day_wed)
        4 -> stringResource(R.string.day_thu)
        5 -> stringResource(R.string.day_fri)
        6 -> stringResource(R.string.day_sat)
        7 -> stringResource(R.string.day_sun)
        else -> ""
    }

    val esHoy = fecha == LocalDate.now()
    val esSeleccionado = fecha == selectedDate

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = diaSemanaLetra,
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.height(9.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDateSelected(fecha) },
            contentAlignment = Alignment.Center
        ) {
            var boxModifier = Modifier.fillMaxSize()

            if (esSeleccionado) {
                boxModifier = boxModifier.background(
                    color = currentDayColor.copy(alpha = 0.3f),
                    shape = CircleShape
                )
            }
            if (esHoy) {
                boxModifier = boxModifier.border(
                    width = 2.dp,
                    color = currentDayColor,
                    shape = CircleShape
                )
            }

            Box(
                modifier = boxModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fecha.dayOfMonth.toString(),
                )
            }
        }
    }
}

@Composable
fun VistaSemanal(
    fecha: LocalDate,
    currentDayColor: Color,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val inicioSemana = fecha.minusDays((fecha.dayOfWeek.value - 1).toLong())

    Column {
        EncabezadoSemana()
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 0..6) {
                val dia = inicioSemana.plusDays(i.toLong())
                val esHoy = dia == LocalDate.now()
                val esSeleccionado = dia == selectedDate

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 1.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDateSelected(dia) },
                    contentAlignment = Alignment.Center
                ) {
                    var modifier = Modifier.size(36.dp)
                    if (esSeleccionado) {
                        modifier = modifier.background(
                            color = currentDayColor.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                    }
                    if (esHoy) {
                        modifier = modifier.border(
                            width = 2.dp,
                            color = currentDayColor,
                            shape = CircleShape
                        )
                    }

                    Box(
                        modifier = modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(dia.dayOfMonth.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun VistaMensual(
    fecha: LocalDate,
    currentDayColor: Color,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val primerDiaMes = fecha.withDayOfMonth(1)
    val inicioCalendario = primerDiaMes.minusDays((primerDiaMes.dayOfWeek.value - 1).toLong())

    Column {
        EncabezadoSemana()
        Spacer(modifier = Modifier.height(8.dp))

        for (semana in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (diaDaSemana in 0 until 7) {
                    val dia = inicioCalendario.plusDays((semana * 7 + diaDaSemana).toLong())
                    val hoy = LocalDate.now()
                    val esHoy = dia == hoy
                    val esSeleccionado = dia == selectedDate
                    val esMesActual = dia.monthValue == fecha.monthValue

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 1.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onDateSelected(dia) },
                        contentAlignment = Alignment.Center
                    ) {
                        var modifier = Modifier.size(36.dp)
                        if (esSeleccionado) {
                            modifier = modifier.background(
                                color = currentDayColor.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                        }
                        if (esHoy) {
                            modifier = modifier.border(
                                width = 2.dp,
                                color = currentDayColor,
                                shape = CircleShape
                            )
                        }

                        Box(
                            modifier = modifier,
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dia.dayOfMonth.toString(),
                                color = if (esMesActual) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSlotList(
    timeRanges: List<TimeRange>,
    events: List<Event>,
    getReminderMessageFiltered: (LocalDate) -> String,
    dateFormat: String,
    onTimeSelected: (LocalTime) -> Unit,
    onEventSelected: (Event) -> Unit,
    preferredSendMethod: NotificationMethod
) {
    val context = LocalContext.current

    val insertedDate = stringResource(R.string.inserted_date)
    val insertedTime = stringResource(R.string.inserted_time)
    val mailConcept = stringResource(R.string.mail_concept)
    val remSend = stringResource(R.string.rem_send)

    val is24HourFormat = DateFormat.is24HourFormat(context)
    val formatter = remember(is24HourFormat) {
        DateTimeFormatter.ofPattern(if (is24HourFormat) "HH:mm" else "h:mm a")
    }

    val timeSlots = remember(timeRanges, events) {
        val baseSlots = timeRanges.flatMap { range ->
            val slots = mutableListOf<LocalTime>()
            var currentTime = range.start
            while (!currentTime.isAfter(range.end)) {
                slots.add(currentTime)
                currentTime = currentTime.plusMinutes(30)
            }
            slots
        }
        val eventTimes = events.map { it.time }
        (baseSlots + eventTimes).distinct().sorted()
    }

    val halfSize = (timeSlots.size + 1) / 2
    val firstHalf = timeSlots.take(halfSize)
    val secondHalf = timeSlots.drop(halfSize)

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
        ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top
            ) {
            firstHalf.forEach { time ->
                val eventsForTime = events.filter { it.time == time }
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = time.format(formatter),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTimeSelected(time) }
                            .padding(vertical = 2.dp),
                        fontWeight = if (time.minute == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (time.minute == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    eventsForTime.forEach { event ->
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val message = getReminderMessageFiltered(event.date)
                                        .replace(insertedDate, event.date.format(DateTimeFormatter.ofPattern(dateFormat)))
                                        .replace(insertedTime, event.time.format(formatter))

                                    val intent = preferredSendMethod.intentBuilder (
                                        message,
                                        event.phone,
                                        event.email,
                                        mailConcept
                                    )

                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val backupIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, message)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(backupIntent,
                                            remSend
                                            ))
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = stringResource(R.string.reminder),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${event.name} (${event.person})",
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onEventSelected(event)
                                    }
                            )
                        }
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top
            ) {
            secondHalf.forEach { time ->
                val eventsForTime = events.filter { it.time == time }
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = time.format(formatter),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTimeSelected(time) }
                            .padding(vertical = 2.dp),
                        fontWeight = if (time.minute == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (time.minute == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    eventsForTime.forEach { event ->
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val message = getReminderMessageFiltered(event.date)
                                        .replace(insertedDate, event.date.format(DateTimeFormatter.ofPattern(dateFormat)))
                                        .replace(insertedTime, event.time.format(formatter))

                                    val intent = preferredSendMethod.intentBuilder (
                                        message,
                                        event.phone,
                                        event.email,
                                        mailConcept
                                    )

                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val backupIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, message)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(backupIntent, remSend))
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = stringResource(R.string.reminder),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${event.name} (${event.person})",
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onEventSelected(event)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getMethodName(method: NotificationMethod): String {
    return when (val name = method.name) {
        is Int -> stringResource(name)
        is String -> name
        else -> ""
    }
}