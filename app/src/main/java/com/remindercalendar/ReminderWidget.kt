package com.remindercalendar

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.glance.color.ColorProvider
import kotlin.let

val DayOffsetKey = intPreferencesKey("day_offset")
val ForceRefreshKey = intPreferencesKey("force_refresh")

class ReminderWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val dayOffset = prefs[DayOffsetKey] ?: 0

            val context = LocalContext.current
            val settingsManager = remember { SettingsManager(context) }
            val eventManager = remember { EventManager(context) }
            val selectedCalendars by settingsManager.selectedCalendarsFlow.collectAsState(initial = emptySet())

            val headerColorInt = prefs[intPreferencesKey("widget_header_color")]
            val textColorInt = prefs[intPreferencesKey("widget_text_color")]
            val darkModeInt = prefs[stringPreferencesKey("widget_dark_mode")]

            val headerColorFlow by settingsManager.headerColorFlow.collectAsState(initial = Color.White)
            val headerTextColorFlow by settingsManager.headerTextColorFlow.collectAsState(initial = Color.White)
            val darkModeConfigFlow by settingsManager.darkModeFlow.collectAsState(initial = DarkModeConfig.SYSTEM)

            val headerColor = if (headerColorInt != null) Color(headerColorInt) else headerColorFlow
            val headerTextColor = if (textColorInt != null) Color(textColorInt) else headerTextColorFlow
            val darkModeConfig = if (darkModeInt != null) {
                try { DarkModeConfig.valueOf(darkModeInt) } catch (e: Exception) { darkModeConfigFlow }
            } else {
                darkModeConfigFlow
            }
            val dynamicBackground = ColorProvider(
                day = Color.White,
                night = Color(0xFF121212)
            )
            val dynamicBodyText = ColorProvider(
                day = Color.Black,
                night = Color.White
            )
            val backgroundColor = when (darkModeConfig) {
                DarkModeConfig.LIGHT -> ColorProvider(Color.White)
                DarkModeConfig.DARK -> ColorProvider(Color(0xFF121212))
                DarkModeConfig.SYSTEM -> dynamicBackground
            }

            val bodyTextColor = when (darkModeConfig) {
                DarkModeConfig.LIGHT -> ColorProvider(Color.Black)
                DarkModeConfig.DARK -> ColorProvider(Color.White)
                DarkModeConfig.SYSTEM -> dynamicBodyText
            }

            val viewedDate = LocalDate.now().plusDays(dayOffset.toLong())
            val dayOfWeekInitial = when (viewedDate.dayOfWeek.value) {
                1 -> context.getString(R.string.day_mon)
                2 -> context.getString(R.string.day_tue)
                3 -> context.getString(R.string.day_wed)
                4 -> context.getString(R.string.day_thu)
                5 -> context.getString(R.string.day_fri)
                6 -> context.getString(R.string.day_sat)
                7 -> context.getString(R.string.day_sun)
                else -> ""
            }
            val monthName = viewedDate.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            val formattedDate = "$dayOfWeekInitial ${viewedDate.dayOfMonth} $monthName"

            val allEvents = remember(selectedCalendars, viewedDate, prefs) {
                eventManager.getEventsFromSystem(selectedCalendars)
            }
            val dayEvents = allEvents.filter { it.date == viewedDate }.sortedBy { it.time }

            ReminderWidgetContent(
                headerColor = headerColor,
                headerTextColor = headerTextColor,
                backgroundColor = backgroundColor,
                bodyTextColor = bodyTextColor,
                formattedDate = formattedDate,
                events = dayEvents
            )
        }
    }
}

class ChangeDayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val offset = parameters[offsetKey] ?: 0
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[DayOffsetKey] ?: 0
            prefs[DayOffsetKey] = current + offset
        }
        ReminderWidget().update(context, glanceId)
    }

    companion object {
        val offsetKey = ActionParameters.Key<Int>("offset")
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[ForceRefreshKey] = (0..1000).random()
        }
        ReminderWidget().update(context, glanceId)
    }
}

class TodayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[DayOffsetKey] = 0
            prefs[ForceRefreshKey] = (0..1000).random()
        }
        ReminderWidget().update(context, glanceId)
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun ReminderWidgetContent(
    headerColor: Color,
    headerTextColor: Color,
    backgroundColor: ColorProvider,
    bodyTextColor: ColorProvider,
    formattedDate: String,
    events: List<Event>
) {
    val context = LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")
    val size = LocalSize.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formattedDate,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(headerTextColor)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                if (size.width < 340.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row {
                            IconImage(
                                R.drawable.ic_today,
                                context.getString(R.string.today),
                                headerTextColor, actionRunCallback<TodayAction>())
                            IconImage(
                                R.drawable.ic_refresh,
                                context.getString(R.string.refresh),
                                headerTextColor, actionRunCallback<RefreshAction>())
                        }
                        Row {
                            IconImage(
                                R.drawable.ic_arrow_back,
                                context.getString(R.string.prev),
                                headerTextColor, actionRunCallback<ChangeDayAction>(actionParametersOf(ChangeDayAction.offsetKey to -1)))
                            IconImage(
                                R.drawable.ic_arrow_forward,
                                context.getString(R.string.next),
                                headerTextColor, actionRunCallback<ChangeDayAction>(actionParametersOf(ChangeDayAction.offsetKey to 1)))
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconImage(
                            R.drawable.ic_today,
                            context.getString(R.string.today),
                            headerTextColor,
                            actionRunCallback<TodayAction>()
                        )
                        IconImage(
                            R.drawable.ic_refresh,
                            context.getString(R.string.refresh),
                            headerTextColor,
                            actionRunCallback<RefreshAction>()
                        )
                        IconImage(
                            R.drawable.ic_arrow_back,
                            context.getString(R.string.prev),
                            headerTextColor,
                            actionRunCallback<ChangeDayAction>(actionParametersOf(ChangeDayAction.offsetKey to -1))
                        )
                        IconImage(
                            R.drawable.ic_arrow_forward,
                            context.getString(R.string.next),
                            headerTextColor,
                            actionRunCallback<ChangeDayAction>(actionParametersOf(ChangeDayAction.offsetKey to 1))
                        )
                    }
                }
            }
        }

        if (events.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = context.getString(R.string.no_events),
                    style = TextStyle(color = bodyTextColor, fontSize = 16.sp)
                )
            }
        } else {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(events) { event ->
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = event.time.format(timeFormatter),
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = bodyTextColor
                            )
                        )
                        Text(
                            text = "${event.name} (${event.person})",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = bodyTextColor
                            ),
                            modifier = GlanceModifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun IconImage(resId: Int, contentDescription: String, color: Color, action: Action) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(ColorProvider(color)),
        modifier = GlanceModifier
            .padding(4.dp)
            .clickable(action)
    )
}

class ReminderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReminderWidget()
}

suspend fun forceWidgetUpdate(
    context: Context,
    newHeaderColor: Int? = null,
    newTextColor: Int? = null,
    newDarkMode: String? = null
){
    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(ReminderWidget::class.java)

    glanceIds.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[ForceRefreshKey] = (0..1000).random()
            newHeaderColor?.let{
                prefs[intPreferencesKey("widget_header_color")] = it
            }
            newTextColor?.let {
                prefs[intPreferencesKey("widget_text_color")] = it
            }
            newDarkMode?.let {
                prefs[stringPreferencesKey("widget_dark_mode")] = it
            }
        }
        ReminderWidget().update(context, glanceId)
    }
}