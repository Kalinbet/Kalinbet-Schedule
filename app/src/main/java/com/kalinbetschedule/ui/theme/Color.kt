package com.kalinbetschedule.ui.theme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
@Immutable
data class SlotColors(
    val appointment: Color,
    val onAppointment: Color,
    val busy: Color,
    val onBusy: Color,
    val untimed: Color,
    val onUntimed: Color,
    val free: Color,
    val gridLine: Color
)
val LightSlotColors = SlotColors(
    appointment = Color(0xFF4CAF50),
    onAppointment = Color(0xFF0B2E0D),
    busy = Color(0xFFB0B6BC),
    onBusy = Color(0xFF25292E),
    untimed = Color(0xFFFFC107),
    onUntimed = Color(0xFF3A2A00),
    free = Color(0xFFF4F4F6),
    gridLine = Color(0x1F000000)
)
val DarkSlotColors = SlotColors(
    appointment = Color(0xFF2E7D32),
    onAppointment = Color(0xFFDFF5E0),
    busy = Color(0xFF4F555C),
    onBusy = Color(0xFFE3E5E8),
    untimed = Color(0xFFC79100),
    onUntimed = Color(0xFFFFF3CC),
    free = Color(0xFF1C1F22),
    gridLine = Color(0x1FFFFFFF)
)
val LocalSlotColors = staticCompositionLocalOf { LightSlotColors }
