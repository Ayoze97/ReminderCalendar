package com.remindercalendar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remindercalendar.ui.theme.ReminderCalendarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

enum class CalendarView {
    MONTH, WEEK, DAY
}

enum class Screen {
    CALENDAR, SETTINGS
}

enum class SendMethod { SMS, WhatsApp, Mail }

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
    private val eventViewModel: EventViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !eventViewModel.isReady.value
        }

        enableEdgeToEdge()
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current

    // 1. Creamos las dependencias una sola vez
    val settingsManager = remember { SettingsManager(context) }
    val eventManager = remember { EventManager(context) }

    // 2. Pasamos las dependencias a las Factorías
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(settingsManager, eventManager) // <--- Añadido eventManager
    )

    val eventViewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(eventManager, settingsManager)
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
    val dateFormat by settingsViewModel.dateFormat.collectAsState()
    val preferredSendMethod by settingsViewModel.preferredSendMethod.collectAsState()
    val defaultView by settingsViewModel.defaultView.collectAsState()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Si el permiso se acaba de dar, comprobamos si falta la cuenta
            if (settingsViewModel.selectedCalendarId.value == null) {
                settingsViewModel.setShowCalendarDialog(true)
            }
        } else {
            Toast.makeText(context, "Sin permiso no se pueden guardar citas", Toast.LENGTH_SHORT).show()
        }
    }

    var currentScreen by remember { mutableStateOf(Screen.CALENDAR) }
    val useDarkTheme = when (darkModeConfig) {
        DarkModeConfig.LIGHT -> false
        DarkModeConfig.DARK -> true
        DarkModeConfig.SYSTEM -> isSystemInDarkTheme()
    }
    ReminderCalendarTheme(darkTheme = useDarkTheme) {
        val activity = LocalContext.current as? Activity
        if (activity != null) {
            // Escuchamos los cambios en el color del encabezado
            @Suppress("DEPRECATION")
            LaunchedEffect(headerColor, headerTextColor, useDarkTheme) {
                val window = activity.window

                // Esta es la forma actual de gestionar el color de fondo sin que "grite" el IDE
                window.apply {
                    // Sigue siendo necesario para el fondo, pero asegúrate de que
                    // no estás usando una versión de API extremadamente antigua
                    statusBarColor = headerColor.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb() // O el color de tu fondo
                }

                // Para el estilo de los iconos (claro/oscuro)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !useDarkTheme && headerTextColor == Color.Black
            }
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
                    reminderMessage = reminderMessage,
                    dateFormat = dateFormat,
                    defaultView = defaultView,
                    onSettingsClick = { currentScreen = Screen.SETTINGS },
                    cambiarPeriodo = ::cambiarPeriodo,
                    events = combinedEvents,
                    onAddEvent = { event, sync, _ ->
                        // Obtenemos el ID real del ViewModel
                        val calendarId = settingsViewModel.selectedCalendarId.value

                        // Verificamos si tenemos permiso de escritura usando el 'context' obtenido arriba
                        val hasWritePermission = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.WRITE_CALENDAR
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        if (!hasWritePermission) {
                            // Para lanzar el permiso, usamos el launcher (que sí existe en el scope de la Activity)
                            calendarPermissionLauncher.launch(android.Manifest.permission.WRITE_CALENDAR)
                            android.widget.Toast.makeText(context, "Concede permisos para guardar", android.widget.Toast.LENGTH_LONG).show()
                        } else if (calendarId == null) {
                            // Abrimos el selector si no hay cuenta
                            settingsViewModel.setShowCalendarDialog(true)
                            android.widget.Toast.makeText(context, "Selecciona una cuenta para guardar", android.widget.Toast.LENGTH_LONG).show()
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
                        dateFormat = dateFormat,
                        onCalendarNameChange = { settingsViewModel.setCalendarName(it) },
                        onHeaderColorChange = { settingsViewModel.setHeaderColor(it) },
                        onButtonsColorChange = { settingsViewModel.setButtonsColor(it) },
                        onCurrentDayColorChange = { settingsViewModel.setCurrentDayColor(it) },
                        onHeaderTextColorChange = { settingsViewModel.setHeaderTextColor(it) },
                        onButtonsTextColorChange = { settingsViewModel.setButtonsTextColor(it) },
                        onTimeRangesChange = { settingsViewModel.setTimeRanges(it) },
                        onReminderMessageChange = { settingsViewModel.setReminderMessage(it) },
                        onDateFormatChange = { settingsViewModel.setDateFormat(it) },
                        preferredSendMethod = preferredSendMethod,
                        onPreferredSendMethodChange = { method ->
                            settingsViewModel.setPreferredSendMethod(method)
                        },
                        darkModeConfig = darkModeConfig,
                        onDarkModeChange = { settingsViewModel.setDarkMode(it) },
                        settingsViewModel = settingsViewModel,
                        onSettingsViewChange = { currentScreen = it },
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
    reminderMessage: String,
    dateFormat: String,
    onSettingsClick: () -> Unit,
    cambiarPeriodo: (LocalDate, CalendarView, Int) -> LocalDate,
    onAddEvent: (Event, Boolean, Long?) -> Unit,
    onUpdateEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    preferredSendMethod: SendMethod,
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
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: EventViewModel = viewModel()
    val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (editingEvent != null || showEventDialogForTime != null) {
        EventDialog(
            event = editingEvent,
            time = showEventDialogForTime,
            onDismiss = {
                editingEvent = null
                showEventDialogForTime = null
            },
            onConfirm = { eventName, person, phone, email, time ->
                if (editingEvent != null) {
                    val updatedEvent = editingEvent!!.copy(
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
                    // Lanzamos el mensaje de aviso
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Evento eliminado. La sincronización total con Google puede tardar unos minutos.",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                showDeleteConfirmationDialog = null
                editingEvent = null
                showEventDialogForTime = null
            },
            confirmButtonColor = Color.Red,
            dismissButtonColor = buttonsColor
        )
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
                        Text("Seleccionar vista", modifier = Modifier.padding(16.dp))

                        NavigationDrawerItem(
                            label = { Text("Mes") },
                            selected = vistaActual == CalendarView.MONTH,
                            onClick = {
                                vistaActual = CalendarView.MONTH
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("Semana") },
                            selected = vistaActual == CalendarView.WEEK,
                            onClick = {
                                vistaActual = CalendarView.WEEK
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.ViewWeek, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("Día") },
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
                            label = { Text("Actualizar") },
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
                            contentDescription = "Ajustes",
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
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(buttonsColor, RoundedCornerShape(8.dp))
                                .clickable(onClick = { fechaSeleccionada = LocalDate.now() })
                        ) {
                            Text(
                                text = "Hoy",
                                style = MaterialTheme.typography.bodyMedium,
                                color = buttonsTextColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
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
                                contentDescription = "Cambiar mes",
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
                                text = { Text("Año anterior") },
                                onClick = {
                                    year--
                                    fechaSeleccionada = fechaSeleccionada.withYear(year)
                                },
                                modifier = Modifier.padding(vertical = 0.dp)
                            )

                            DropdownMenuItem(
                                text = { Text("Año siguiente") },
                                onClick = {
                                    year++
                                    fechaSeleccionada = fechaSeleccionada.withYear(year)
                                },
                                modifier = Modifier.padding(vertical = 0.dp)
                            )
                        }
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
                            reminderMessage = reminderMessage,
                            dateFormat = dateFormat,
                            onTimeSelected = { time ->
                                // 1. Verificamos permisos de escritura
                                val hasWritePermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_CALENDAR
                                ) == PackageManager.PERMISSION_GRANTED

                                // 2. Verificamos si hay una cuenta seleccionada (usando el State de tu ViewModel)
                                val hasAccount = settingsViewModel.selectedCalendarId.value != null

                                if (hasWritePermission && hasAccount) {
                                    showEventDialogForTime = time
                                } else {
                                    // FALLA ALGO: Activamos el auto-selector y navegamos a ajustes
                                    settingsViewModel.triggerCalendarSelector()

                                    // Mostramos un aviso rápido al usuario
                                    Toast.makeText(context, "Configura tu cuenta de calendario", Toast.LENGTH_SHORT).show()

                                    // Llamamos a la función de navegación que tengas (ej. de tu NavHost)
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
                            reminderMessage = reminderMessage,
                            dateFormat = dateFormat,
                            onTimeSelected = { time ->
                                // 1. Verificamos permisos de escritura
                                val hasWritePermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_CALENDAR
                                ) == PackageManager.PERMISSION_GRANTED

                                // 2. Verificamos si hay una cuenta seleccionada (usando el State de tu ViewModel)
                                val hasAccount = settingsViewModel.selectedCalendarId.value != null

                                if (hasWritePermission && hasAccount) {
                                    showEventDialogForTime = time
                                } else {
                                    // FALLA ALGO: Activamos el auto-selector y navegamos a ajustes
                                    settingsViewModel.triggerCalendarSelector()

                                    // Mostramos un aviso rápido al usuario
                                    Toast.makeText(context, "Configura tu cuenta de calendario", Toast.LENGTH_SHORT).show()

                                    // Llamamos a la función de navegación que tengas (ej. de tu NavHost)
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
                            reminderMessage = reminderMessage,
                            dateFormat = dateFormat,
                            onTimeSelected = { time ->
                                // 1. Verificamos permisos de escritura
                                val hasWritePermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_CALENDAR
                                ) == PackageManager.PERMISSION_GRANTED

                                // 2. Verificamos si hay una cuenta seleccionada (usando el State de tu ViewModel)
                                val hasAccount = settingsViewModel.selectedCalendarId.value != null

                                if (hasWritePermission && hasAccount) {
                                    showEventDialogForTime = time
                                } else {
                                    // FALLA ALGO: Activamos el auto-selector y navegamos a ajustes
                                    settingsViewModel.triggerCalendarSelector()

                                    // Mostramos un aviso rápido al usuario
                                    Toast.makeText(context, "Configura tu cuenta de calendario", Toast.LENGTH_SHORT).show()

                                    // Llamamos a la función de navegación que tengas (ej. de tu NavHost)
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
}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmButtonColor: Color = Color.Red,
    dismissButtonColor: Color = Color.Gray
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar eliminación") },
        text = { Text("¿Estás seguro de que quieres eliminar este evento?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

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
    dateFormat: String,
    onCalendarNameChange: (String) -> Unit,
    onHeaderColorChange: (Color) -> Unit,
    onButtonsColorChange: (Color) -> Unit,
    onCurrentDayColorChange: (Color) -> Unit,
    onHeaderTextColorChange: (Color) -> Unit,
    onButtonsTextColorChange: (Color) -> Unit,
    onTimeRangesChange: (List<TimeRange>) -> Unit,
    onReminderMessageChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    preferredSendMethod: SendMethod,
    onPreferredSendMethodChange: (SendMethod) -> Unit,
    darkModeConfig: DarkModeConfig,
    onDarkModeChange: (DarkModeConfig) -> Unit,
    settingsViewModel: SettingsViewModel,
    onSettingsViewChange: (Screen) -> Unit,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    var showNameDialog by remember { mutableStateOf(false) }
    var showReminderMessageDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var colorToEdit by remember { mutableStateOf<((Color) -> Unit)?>(null) }
    var showTextColorDialog by remember { mutableStateOf<((Color) -> Unit)?>(null) }
    var isCurrentDayColor by remember { mutableStateOf(false) }
    var showTimeRangeDialog by remember { mutableStateOf(false) }
    //var text by remember { mutableStateOf(dateFormat) }
    var expanded by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val highlightState = settingsViewModel.highlightCalendarOption.collectAsState()
    val highlight = highlightState.value
    val forceOpen by settingsViewModel.forceShowCalendarSelector.collectAsState()
    val defaultView by settingsViewModel.defaultView.collectAsState()
    var showCalendarDialog by remember { mutableStateOf(false) }
    val isDateNeeded = reminderMessage.contains("{fecha}", ignoreCase = true)
    val datePreview = remember(dateFormat) {
        try {
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern(dateFormat))
        } catch (e: Exception) {
            "Formato inválido"
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "Highlighter"
    )

    LaunchedEffect(forceOpen) {
        if (forceOpen) {
            delay(800)
            // Activamos el estado que abre tu AlertDialog actual
            showCalendarDialog = true
            // Limpiamos el trigger para que no se abra cada vez que vuelvas
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
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
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
                horizontalArrangement = Arrangement.SpaceBetween // Separa el texto a la izquierda y el menú a la derecha
            ) {
                Text(
                    text = "Tema de la aplicación",
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
                                DarkModeConfig.LIGHT -> "Claro"
                                DarkModeConfig.DARK -> "Oscuro"
                                DarkModeConfig.SYSTEM -> "Sistema"
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
                            text = { Text("Claro") },
                            leadingIcon = { Icon(Icons.Default.LightMode, null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                onDarkModeChange(DarkModeConfig.LIGHT)
                                expandedTema = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oscuro") },
                            leadingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                onDarkModeChange(DarkModeConfig.DARK)
                                expandedTema = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sistema") },
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
                Text("Nombre del calendario", style = MaterialTheme.typography.titleMedium)
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
                Text("Rango horario", style = MaterialTheme.typography.titleMedium)
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
                    Text("Mensaje de recordatorio", style = MaterialTheme.typography.titleMedium)
                    Text(reminderMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        text = "Formato de fecha",
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
                            text = "No se usa {fecha} en el mensaje",
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
                    text = "Método de envío preferido",
                    style = MaterialTheme.typography.titleMedium
                )

                Box {
                    OutlinedButton(
                        onClick = { expandedEnvio = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = when(preferredSendMethod) {
                                SendMethod.WhatsApp -> "WhatsApp"
                                SendMethod.SMS -> "SMS"
                                SendMethod.Mail -> "Correo"
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
                        expanded = expandedEnvio,
                        onDismissRequest = { expandedEnvio = false }
                    ) {
                        SendMethod.entries.forEach { method ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when(method) {
                                            SendMethod.WhatsApp -> "WhatsApp"
                                            SendMethod.SMS -> "SMS"
                                            SendMethod.Mail -> "Correo Electrónico"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    val iconPainter = when(method) {
                                        SendMethod.WhatsApp -> painterResource(id = R.drawable.whatsapp_icon)
                                        SendMethod.SMS -> rememberVectorPainter(Icons.Default.Sms)
                                        SendMethod.Mail -> rememberVectorPainter(Icons.Default.Email)
                                    }
                                    Icon(
                                        painter = iconPainter,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
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

            if (preferredSendMethod == SendMethod.Mail) {
                Text(
                    text = "Nota: El contacto debe tener un correo guardado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
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
                    text = "Vista predeterminada",
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
                                CalendarView.MONTH -> "Mes"
                                CalendarView.WEEK -> "Semana"
                                CalendarView.DAY -> "Día"
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
                                            CalendarView.MONTH -> "Mes"
                                            CalendarView.WEEK -> "Semana"
                                            CalendarView.DAY -> "Día"
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

            // Estado para controlar si el diálogo está abierto
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
                    Toast.makeText(context, "Permiso necesario para ver calendarios", Toast.LENGTH_SHORT).show()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(animatedColor)
                    .clickable(
                        interactionSource = interactionSource, // <--- VINCULAMOS AQUÍ
                        indication = ripple(), // <--- EFECTO ONDA
                        onClick = {
                            // Tu lógica de permisos existente se mantiene intacta
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
                                launcher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                            }
                        }
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cuenta de Google seleccionada",
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight, // O ArrowForwardIos
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (showCalendarDialog) {
                val calendars by settingsViewModel.availableCalendars.collectAsState()
                val selectedCalendarId by settingsViewModel.selectedCalendarId.collectAsState()

                AlertDialog(
                    onDismissRequest = { showCalendarDialog = false },
                    title = { Text("Seleccionar Cuenta") },
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

                                    // Usamos RadioButton para indicar que solo se elige UNO
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
                            Text("Aceptar")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Personalización Visual",
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
                    Text("Color de encabezado", style = MaterialTheme.typography.titleMedium)
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
                    Text("Color texto encabezado", style = MaterialTheme.typography.bodyLarge)
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
                    Text("Color de botones", style = MaterialTheme.typography.titleMedium)
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
                    Text("Color texto botones", style = MaterialTheme.typography.bodyLarge)
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
                Text("Color día seleccionado y actual", style = MaterialTheme.typography.titleMedium)
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = currentDayColor,
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {}
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
        title = { Text("Editar formato de fecha") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Formato") },
                    isError = isError,
                    placeholder = { Text("dd/MM/yyyy") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (isError) {
                            Text("Caracteres de hora no válidos (H, m, s...)")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Vista previa:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = preview ?: "Formato inválido para fecha",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ej: dd/MM/yyyy, EEEE d MMMM",
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
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar mensaje de recordatorio") },
        text = {
            Column {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    label = { Text("Mensaje") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TextButton(onClick = {
                        val newText = textFieldValue.text.replaceRange(textFieldValue.selection.start, textFieldValue.selection.end, "{fecha}")
                        textFieldValue = TextFieldValue(newText)
                    }) {
                        Text("Insertar Fecha")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val newText = textFieldValue.text.replaceRange(textFieldValue.selection.start, textFieldValue.selection.end, "{hora}")
                        textFieldValue = TextFieldValue(newText)
                    }) {
                        Text("Insertar Hora")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(textFieldValue.text) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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
    var eventName by remember { mutableStateOf(event?.name ?: "") }
    var person by remember { mutableStateOf(event?.person ?: "") }
    val initialTime = event?.time ?: time ?: LocalTime.now()
    var eventTime by remember { mutableStateOf(initialTime.format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var isTimeValid by remember { mutableStateOf(true) }
    var phone by remember { mutableStateOf(event?.phone ?: "") }
    var email by remember { mutableStateOf(event?.email ?: "") }

    val context = LocalContext.current
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

                    // 1. Actualizamos el nombre
                    person = name

                    // 2. Buscamos el TELÉFONO
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
                            // Limpiamos el número para evitar errores en WhatsApp (quitamos espacios y símbolos)
                            phone = number.replace(Regex("[^0-9]"), "")
                        } else {
                            phone = "" // Limpiar si el contacto no tiene teléfono
                        }
                    }

                    // 3. Buscamos el EMAIL
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
                            email = "" // Limpiar si el contacto no tiene email
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

    fun validateTime(time: String): Boolean {
        return try {
            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }

    val isConfirmEnabled = eventName.isNotBlank() && isTimeValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event != null) "Editar evento" else "Crear evento") },
        text = {
            Column {
                OutlinedTextField(value = eventName, onValueChange = { eventName = it }, label = { Text("Nombre del evento") })
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = person,
                        onValueChange = { person = it },
                        label = { Text("Persona") },
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
                        Icon(painterResource(id = R.drawable.contact_icon), contentDescription = "Seleccionar contacto")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = eventTime, onValueChange = { eventTime = it; isTimeValid = validateTime(it) }, label = { Text("Hora") }, isError = !isTimeValid)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono (WhatsApp/SMS)") },
                    placeholder = { Text("Ej: 34600112233") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("ejemplo@correo.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanTime = eventTime.trim()

                    val parsedTime = try {
                        LocalTime.parse(cleanTime, DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) {
                        try {
                            LocalTime.parse(cleanTime, DateTimeFormatter.ofPattern("H:mm"))
                        } catch (e2: Exception) {
                            LocalTime.now()
                        }
                    }

                    onConfirm(eventName, person, phone, email, parsedTime)
                },
                enabled = isConfirmEnabled
            ) {
                Text(if (event != null) "Guardar" else "Confirmar")
            }
        },
        dismissButton = {
            Row {
                if (event != null) {
                    TextButton(onClick = { onDelete(event) }) {
                        Text("Eliminar")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
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
        title = { Text("Configurar Rango Horario") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSplit, onCheckedChange = { isSplit = it })
                    Text("Horario partido")
                }

                if (isSplit) {
                    Text("Mañana", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        OutlinedTextField(value = range1Start, onValueChange = { range1Start = it; isRange1StartValid = validateTime(it) }, label = { Text("Inicio") }, modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp), isError = !isRange1StartValid)
                        OutlinedTextField(value = range1End, onValueChange = { range1End = it; isRange1EndValid = validateTime(it) }, label = { Text("Fin") }, modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp), isError = !isRange1EndValid)
                    }
                    Text("Tarde", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        OutlinedTextField(value = range2Start, onValueChange = { range2Start = it; isRange2StartValid = validateTime(it) }, label = { Text("Inicio") }, modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp), isError = !isRange2StartValid)
                        OutlinedTextField(value = range2End, onValueChange = { range2End = it; isRange2EndValid = validateTime(it) }, label = { Text("Fin") }, modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp), isError = !isRange2EndValid)
                    }
                } else {
                    Text("Horario Completo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        OutlinedTextField(value = range1Start, onValueChange = { range1Start = it; isRange1StartValid = validateTime(it) }, label = { Text("Inicio") }, modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp), isError = !isRange1StartValid)
                        OutlinedTextField(value = range1End, onValueChange = { range1End = it; isRange1EndValid = validateTime(it) }, label = { Text("Fin") }, modifier = Modifier
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
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TextColorDialog(onDismiss: () -> Unit, onColorSelected: (Color) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar color de texto") },
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
        title = { Text("Seleccionar color") },
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
                Text("Cerrar")
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
        title = { Text("Editar nombre del calendario") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nombre") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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
    val dias = listOf("L", "M", "X", "J", "V", "S", "D")

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
        1 -> "L"
        2 -> "M"
        3 -> "X"
        4 -> "J"
        5 -> "V"
        6 -> "S"
        7 -> "D"
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
    reminderMessage: String,
    dateFormat: String,
    onTimeSelected: (LocalTime) -> Unit,
    onEventSelected: (Event) -> Unit,
    preferredSendMethod: SendMethod
) {
    val context = LocalContext.current
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
                                    val message = reminderMessage
                                        .replace("{fecha}", event.date.format(DateTimeFormatter.ofPattern(dateFormat)))
                                        .replace("{hora}", event.time.format(formatter))

                                    val phoneNumber = event.phone
                                    val emailAddress = event.email

                                    val intent = when (preferredSendMethod) {
                                        SendMethod.WhatsApp -> {
                                            Intent(Intent.ACTION_VIEW).apply {
                                                val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
                                                data = Uri.parse(url)
                                            }
                                        }
                                        SendMethod.SMS -> {
                                            Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("smsto:$phoneNumber")
                                                putExtra("sms_body", message)
                                            }
                                        }
                                        SendMethod.Mail -> {
                                            Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:$emailAddress")
                                                putExtra(Intent.EXTRA_SUBJECT, "Recordatorio de cita")
                                                putExtra(Intent.EXTRA_TEXT, message)
                                            }
                                        }
                                        else -> {
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, message)
                                            }
                                        }
                                    }

                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val backupIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, message)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(backupIntent, "Enviar recordatorio"))
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Recordatorio",
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
                                    val message = reminderMessage
                                        .replace("{fecha}", event.date.format(DateTimeFormatter.ofPattern(dateFormat)))
                                        .replace("{hora}", event.time.format(formatter))
                                    val phoneNumber = event.phone
                                    val emailAddress = event.email

                                    val intent = when (preferredSendMethod) {
                                        SendMethod.WhatsApp -> {
                                            Intent(Intent.ACTION_VIEW).apply {
                                                val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
                                                data = Uri.parse(url)
                                            }
                                        }
                                        SendMethod.SMS -> {
                                            Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("smsto:$phoneNumber")
                                                putExtra("sms_body", message)
                                            }
                                        }
                                        SendMethod.Mail -> {
                                            Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:$emailAddress")
                                                putExtra(Intent.EXTRA_SUBJECT, "Recordatorio de cita")
                                                putExtra(Intent.EXTRA_TEXT, message)
                                            }
                                        }
                                        else -> {
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, message)
                                            }
                                        }
                                    }

                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val backupIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, message)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(backupIntent, "Enviar recordatorio"))
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Recordatorio",
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
