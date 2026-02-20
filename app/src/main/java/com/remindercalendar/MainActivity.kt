package com.remindercalendar

import android.app.Activity
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

enum class CalendarView {
    DAY, WEEK, MONTH
}

enum class Screen {
    CALENDAR, SETTINGS
}

data class TimeRange(val start: LocalTime, val end: LocalTime)
data class Event(val id: String = UUID.randomUUID().toString(), val date: LocalDate, val time: LocalTime, val name: String, val person: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp(
    settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(SettingsManager(LocalContext.current))
    ),
    eventViewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(EventManager(LocalContext.current))
    )
) {
    var currentScreen by remember { mutableStateOf(Screen.CALENDAR) }
    val calendarName by settingsViewModel.calendarName.collectAsState()
    val headerColor by settingsViewModel.headerColor.collectAsState()
    val buttonsColor by settingsViewModel.buttonsColor.collectAsState()
    val currentDayColor by settingsViewModel.currentDayColor.collectAsState()
    val headerTextColor by settingsViewModel.headerTextColor.collectAsState()
    val buttonsTextColor by settingsViewModel.buttonsTextColor.collectAsState()
    val timeRanges by settingsViewModel.timeRanges.collectAsState()
    val events by eventViewModel.events.collectAsState()

    val activity = LocalContext.current as? Activity
    if (activity != null) {
        LaunchedEffect(headerTextColor) {
            val window = activity.window
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = headerTextColor == Color.Black
        }
    }

    when (currentScreen) {
        Screen.CALENDAR -> CalendarScreen(
            calendarName = calendarName,
            headerColor = headerColor,
            buttonsColor = buttonsColor,
            currentDayColor = currentDayColor,
            headerTextColor = headerTextColor,
            buttonsTextColor = buttonsTextColor,
            timeRanges = timeRanges,
            events = events,
            onSettingsClick = { currentScreen = Screen.SETTINGS },
            cambiarPeriodo = ::cambiarPeriodo,
            onAddEvent = { eventViewModel.addEvent(it) },
            onUpdateEvent = { eventViewModel.updateEvent(it) },
            onDeleteEvent = { eventViewModel.deleteEvent(it) }
        )

        Screen.SETTINGS -> SettingsScreen(
            calendarName = calendarName,
            headerColor = headerColor,
            buttonsColor = buttonsColor,
            currentDayColor = currentDayColor,
            headerTextColor = headerTextColor,
            buttonsTextColor = buttonsTextColor,
            timeRanges = timeRanges,
            onCalendarNameChange = { settingsViewModel.setCalendarName(it) },
            onHeaderColorChange = { settingsViewModel.setHeaderColor(it) },
            onButtonsColorChange = { settingsViewModel.setButtonsColor(it) },
            onCurrentDayColorChange = { settingsViewModel.setCurrentDayColor(it) },
            onHeaderTextColorChange = { settingsViewModel.setHeaderTextColor(it) },
            onButtonsTextColorChange = { settingsViewModel.setButtonsTextColor(it) },
            onTimeRangesChange = { settingsViewModel.setTimeRanges(it) },
            onBack = { currentScreen = Screen.CALENDAR }
        )
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
    onSettingsClick: () -> Unit,
    cambiarPeriodo: (LocalDate, CalendarView, Int) -> LocalDate,
    onAddEvent: (Event) -> Unit,
    onUpdateEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit
) {
    var fechaSeleccionada by remember { mutableStateOf(LocalDate.now()) }
    var vistaActual by remember { mutableStateOf(CalendarView.MONTH) }
    var expanded by remember { mutableStateOf(false) }
    var year by remember { mutableIntStateOf(fechaSeleccionada.year) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var showEventDialogForTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf<Event?>(null) }

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
            onConfirm = { eventName, person, timeString ->
                val time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
                if (editingEvent != null) {
                    val updatedEvent = editingEvent!!.copy(name = eventName, person = person, time = time)
                    onUpdateEvent(updatedEvent)
                } else {
                    val newEvent = Event(
                        date = fechaSeleccionada,
                        time = time,
                        name = eventName,
                        person = person
                    )
                    onAddEvent(newEvent)
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
                showDeleteConfirmationDialog?.let { onDeleteEvent(it) }
                showDeleteConfirmationDialog = null
                editingEvent = null
                showEventDialogForTime = null
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                Box(Modifier.fillMaxSize()) {
                    Column {
                        Text("Seleccionar vista", modifier = Modifier.padding(16.dp))

                        NavigationDrawerItem(
                            label = { Text("Mes") },
                            selected = vistaActual == CalendarView.MONTH,
                            onClick = {
                                vistaActual = CalendarView.MONTH
                                scope.launch { drawerState.close() }
                            }
                        )

                        NavigationDrawerItem(
                            label = { Text("Semana") },
                            selected = vistaActual == CalendarView.WEEK,
                            onClick = {
                                vistaActual = CalendarView.WEEK
                                scope.launch { drawerState.close() }
                            }
                        )

                        NavigationDrawerItem(
                            label = { Text("Día") },
                            selected = vistaActual == CalendarView.DAY,
                            onClick = {
                                vistaActual = CalendarView.DAY
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    IconButton(
                        onClick = {
                            onSettingsClick()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
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
                            val anioActual = fechaSeleccionada.year

                            Text(
                                text = "$mesActual $anioActual",
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
                            VistaDiaria(fechaSeleccionada)
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
                            onTimeSelected = { time -> showEventDialogForTime = time },
                            onEventSelected = { event -> editingEvent = event }
                        )
                    }

                    CalendarView.WEEK -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                    }

                    CalendarView.DAY -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
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
    onCalendarNameChange: (String) -> Unit,
    onHeaderColorChange: (Color) -> Unit,
    onButtonsColorChange: (Color) -> Unit,
    onCurrentDayColorChange: (Color) -> Unit,
    onHeaderTextColorChange: (Color) -> Unit,
    onButtonsTextColorChange: (Color) -> Unit,
    onTimeRangesChange: (List<TimeRange>) -> Unit,
    onBack: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var colorToEdit by remember { mutableStateOf<((Color) -> Unit)?>(null) }
    var showTextColorDialog by remember { mutableStateOf<((Color) -> Unit)?>(null) }
    var isCurrentDayColor by remember { mutableStateOf(false) }
    var showTimeRangeDialog by remember { mutableStateOf(false) }

    if (showNameDialog) {
        EditCalendarNameDialog(
            currentName = calendarName,
            onDismiss = { showNameDialog = false },
            onConfirm = {
                onCalendarNameChange(it)
                showNameDialog = false
            }
        )
    }

    colorToEdit?.let {
        ColorPickerDialog(
            isCurrentDayColor = isCurrentDayColor,
            onDismiss = { colorToEdit = null; isCurrentDayColor = false },
            onColorSelected = {
                it(it)
                colorToEdit = null
                isCurrentDayColor = false
            }
        )
    }

    showTextColorDialog?.let {
        TextColorDialog(
            onDismiss = { showTextColorDialog = null },
            onColorSelected = {
                it(it)
                showTextColorDialog = null
            }
        )
    }

    if (showTimeRangeDialog) {
        TimeRangeSettingsDialog(
            initialRanges = timeRanges,
            onDismiss = { showTimeRangeDialog = false },
            onConfirm = {
                onTimeRangesChange(it)
                showTimeRangeDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNameDialog = true }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nombre del calendario", style = MaterialTheme.typography.titleMedium)
                Text(calendarName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Colores",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { colorToEdit = onHeaderColorChange }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color de encabezado", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp),
                        color = headerColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {}
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .clickable { showTextColorDialog = onHeaderTextColorChange }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color texto encabezado", style = MaterialTheme.typography.bodyLarge)
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp),
                        color = headerTextColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {}
                }
            }
            HorizontalDivider()

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { colorToEdit = onButtonsColorChange }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color de botones", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp),
                        color = buttonsColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {}
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .clickable { showTextColorDialog = onButtonsTextColorChange }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color texto botones", style = MaterialTheme.typography.bodyLarge)
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp),
                        color = buttonsTextColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                Text("Color del día actual y seleccionado", style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp),
                    color = currentDayColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {}
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
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Editar rango horario"
                )
            }
        }
    }
}

@Composable
fun EventDialog(
    event: Event?,
    time: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    onDelete: (Event) -> Unit
) {
    var eventName by remember { mutableStateOf(event?.name ?: "") }
    var person by remember { mutableStateOf(event?.person ?: "") }
    val initialTime = event?.time ?: time ?: LocalTime.now()
    var eventTime by remember { mutableStateOf(initialTime.format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var isTimeValid by remember { mutableStateOf(true) }

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
                OutlinedTextField(value = person, onValueChange = { person = it }, label = { Text("Persona") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = eventTime, onValueChange = { eventTime = it; isTimeValid = validateTime(it) }, label = { Text("Hora") }, isError = !isTimeValid)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(eventName, person, eventTime) },
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
                        OutlinedTextField(value = range1Start, onValueChange = { range1Start = it; isRange1StartValid = validateTime(it) }, label = { Text("Inicio") }, modifier = Modifier.weight(1f).padding(end = 4.dp), isError = !isRange1StartValid)
                        OutlinedTextField(value = range1End, onValueChange = { range1End = it; isRange1EndValid = validateTime(it) }, label = { Text("Fin") }, modifier = Modifier.weight(1f).padding(start = 4.dp), isError = !isRange1EndValid)
                    }
                    Text("Tarde", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        OutlinedTextField(value = range2Start, onValueChange = { range2Start = it; isRange2StartValid = validateTime(it) }, label = { Text("Inicio") }, modifier = Modifier.weight(1f).padding(end = 4.dp), isError = !isRange2StartValid)
                        OutlinedTextField(value = range2End, onValueChange = { range2End = it; isRange2EndValid = validateTime(it) }, label = { Text("Fin") }, modifier = Modifier.weight(1f).padding(start = 4.dp), isError = !isRange2EndValid)
                    }
                } else {
                    Text("Horario Completo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        OutlinedTextField(value = range1Start, onValueChange = { range1Start = it; isRange1StartValid = validateTime(it) }, label = { Text("Inicio") }, modifier = Modifier.weight(1f).padding(end = 4.dp), isError = !isRange1StartValid)
                        OutlinedTextField(value = range1End, onValueChange = { range1End = it; isRange1EndValid = validateTime(it) }, label = { Text("Fin") }, modifier = Modifier.weight(1f).padding(start = 4.dp), isError = !isRange1EndValid)
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
                content = {
                    items(colors) { color ->
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(48.dp)
                                .clickable { onColorSelected(color) },
                            color = color,
                            border = if (color == Color.White) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                        ) {}
                    }
                }
            )
        },
        confirmButton = { }
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
fun VistaDiaria(fecha: LocalDate) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Vista diaria")
        Spacer(modifier = Modifier.height(8.dp))
        Text(fecha.format(formatter))
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
    onTimeSelected: (LocalTime) -> Unit,
    onEventSelected: (Event) -> Unit
) {
    val context = LocalContext.current
    val is24HourFormat = DateFormat.is24HourFormat(context)
    val formatter = remember(is24HourFormat) {
        DateTimeFormatter.ofPattern(if (is24HourFormat) "HH:mm" else "h:mm a")
    }

    val timeSlots = remember(timeRanges) {
        timeRanges.flatMap { range ->
            val slots = mutableListOf<LocalTime>()
            var currentTime = range.start
            while (currentTime.isBefore(range.end)) {
                slots.add(currentTime)
                currentTime = currentTime.plusMinutes(30)
            }
            slots
        }.sorted()
    }

    val halfSize = (timeSlots.size + 1) / 2
    val firstHalf = timeSlots.take(halfSize)
    val secondHalf = timeSlots.drop(halfSize)

    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            firstHalf.forEach { time ->
                val eventsForTime = events.filter { it.time == time }
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = time.format(formatter),
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTimeSelected(time) }
                            .padding(vertical = 4.dp)
                    )
                    eventsForTime.forEach { event ->
                        Text(
                            text = " • ${event.name} (${event.person})",
                            modifier = Modifier
                                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                                .fillMaxWidth()
                                .clickable { onEventSelected(event) }
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            secondHalf.forEach { time ->
                val eventsForTime = events.filter { it.time == time }
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = time.format(formatter),
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTimeSelected(time) }
                            .padding(vertical = 4.dp)
                    )
                    eventsForTime.forEach { event ->
                        Text(
                            text = " • ${event.name} (${event.person})",
                            modifier = Modifier
                                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                                .fillMaxWidth()
                                .clickable { onEventSelected(event) }
                        )
                    }
                }
            }
        }
    }
}
