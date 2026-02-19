package com.remindercalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class CalendarView {
    DAY, WEEK, MONTH
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            )
        )

        setContent {
            Greeting(::cambiarPeriodo)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(cambiarPeriodo: (LocalDate, CalendarView, Int) -> LocalDate) {

    var fechaSeleccionada by remember { mutableStateOf(LocalDate.now()) }
    var vistaActual by remember { mutableStateOf(CalendarView.MONTH) }
    var expanded by remember { mutableStateOf(false) }
    var year by remember { mutableIntStateOf(fechaSeleccionada.year) }

    val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
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
        }
    ) {

        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text("Mi Calendario") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            fechaSeleccionada = LocalDate.now()
                        }) {
                            Text("Hoy")
                        }
                    }
                )

            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        var totalHorizontalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalHorizontalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalHorizontalDrag += dragAmount
                            },
                            onDragEnd = {
                                if (totalHorizontalDrag > 50) {
                                    // Deslizar derecha → anterior
                                    fechaSeleccionada = cambiarPeriodo(
                                        fechaSeleccionada,
                                        vistaActual,
                                        -1
                                    )
                                } else if (totalHorizontalDrag < -50) {
                                    // Deslizar izquierda → siguiente
                                    fechaSeleccionada = cambiarPeriodo(
                                        fechaSeleccionada,
                                        vistaActual,
                                        1
                                    )
                                }
                            }
                        )
                    }
            ) {

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        if (it) {
                            year = fechaSeleccionada.year
                        }
                        expanded = it
                    },
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                            .background(Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val mesActual = meses[fechaSeleccionada.monthValue - 1]
                        val anioActual = fechaSeleccionada.year

                        Text(
                            text = "$mesActual $anioActual",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Cambiar mes"
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

                when (vistaActual) {
                    CalendarView.MONTH -> VistaMensual(fechaSeleccionada)
                    CalendarView.WEEK -> VistaSemanal(fechaSeleccionada)
                    CalendarView.DAY -> VistaDiaria(fechaSeleccionada)
                }
            }
        }
    }
}

//Camiar periodo deslizando
fun cambiarPeriodo(
    fecha: LocalDate,
    vista: CalendarView,
    direccion: Int
): LocalDate {

    return when (vista) {
        CalendarView.DAY -> fecha.plusDays(direccion.toLong())
        CalendarView.WEEK -> fecha.plusWeeks(direccion.toLong())
        CalendarView.MONTH -> fecha.plusMonths(direccion.toLong())
    }
}

//Encabezado con los días
@Composable
fun EncabezadoSemana() {

    val dias = listOf("L", "M", "X", "J", "V", "S", "D")

    Row(modifier = Modifier.fillMaxWidth()) {
        dias.forEach {
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(it, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

//Vista diaria
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

//Vista semanal
@Composable
fun VistaSemanal(fecha: LocalDate) {

    val inicioSemana = fecha.minusDays((fecha.dayOfWeek.value - 1).toLong())

    Column {

        EncabezadoSemana()

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 0..6) {

                val dia = inicioSemana.plusDays(i.toLong())
                val hoy = LocalDate.now()
                val esHoy = dia == hoy

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (esHoy) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color.Red,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(dia.dayOfMonth.toString())
                            }
                        } else {
                            Text(dia.dayOfMonth.toString())
                        }
                    }
                }
            }
        }

    }
}


//Vista mensual
@Composable
fun VistaMensual(fecha: LocalDate) {

    val primerDiaMes = fecha.withDayOfMonth(1)
    val inicioCalendario =
        primerDiaMes.minusDays((primerDiaMes.dayOfWeek.value - 1).toLong())

    Column {

        EncabezadoSemana()

        Spacer(modifier = Modifier.height(8.dp))

        for (semana in 0 until 5) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                for (diaDaSemana in 0 until 7) {
                    val dia = inicioCalendario.plusDays((semana * 7 + diaDaSemana).toLong())
                    val hoy = LocalDate.now()
                    val esHoy = dia == hoy

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (esHoy) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .border(
                                            width = 2.dp,
                                            color = Color.Red,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(dia.dayOfMonth.toString())
                                }
                            } else {
                                Text(dia.dayOfMonth.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}
