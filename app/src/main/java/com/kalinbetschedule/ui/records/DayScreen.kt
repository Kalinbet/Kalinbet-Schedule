package com.kalinbetschedule.ui.records
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.MINUTES_IN_DAY
import com.kalinbetschedule.data.SEGMENT_MINUTES
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.Slot
import com.kalinbetschedule.data.SlotKind
import com.kalinbetschedule.data.formatMinutes
import com.kalinbetschedule.ui.DayState
import com.kalinbetschedule.ui.LocalNow
import com.kalinbetschedule.ui.dayTitleWithWeekday
import com.kalinbetschedule.ui.theme.LocalSlotColors
import com.kalinbetschedule.ui.weekdayShort
import java.time.LocalDate
import java.time.temporal.ChronoUnit
private val SEGMENT_HEIGHT = 42.dp
private const val DATE_STRIP_RADIUS = 180
private const val INITIAL_SCROLL_MINUTE = 8 * 60
@Composable
fun DayScreen(
    state: DayState,
    onBack: () -> Unit,
    onOpenSlot: (Long) -> Unit
) {
    var occupyStart by remember { mutableStateOf<Int?>(null) }
    var slotToDelete by remember { mutableStateOf<Slot?>(null) }
    Column(Modifier.fillMaxSize()) {
        DayTopPanel(
            selectedDate = state.selectedDate,
            anchorDate = state.anchorDate,
            onBack = onBack,
            onSelectDate = { state.selectedDate = it },
            onOpenSlot = onOpenSlot
        )
        DayTimeline(
            state = state,
            onOpenSlot = onOpenSlot,
            onCreateAt = { start -> occupyStart = start },
            onDeleteRequest = { slotToDelete = it },
            modifier = Modifier.weight(1f)
        )
    }
    occupyStart?.let { start ->
        OccupyTimeDialog(
            date = state.selectedDate,
            suggestedStart = start,
            onDismiss = { occupyStart = null }
        )
    }
    slotToDelete?.let { slot ->
        AlertDialog(
            onDismissRequest = { slotToDelete = null },
            title = { Text("Освободить время?") },
            text = {
                Text(
                    "${formatMinutes(slot.startMinute)} – ${formatMinutes(slot.endMinute)} " +
                        "будет удалено из расписания."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ScheduleRepository.deleteSlot(slot.id)
                    slotToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { slotToDelete = null }) { Text("Отмена") }
            }
        )
    }
}
@Composable
private fun DayTopPanel(
    selectedDate: LocalDate,
    anchorDate: LocalDate,
    onBack: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenSlot: (Long) -> Unit
) {
    val dates = remember(anchorDate) {
        (-DATE_STRIP_RADIUS..DATE_STRIP_RADIUS).map { anchorDate.plusDays(it.toLong()) }
    }
    val listState = rememberLazyListState()
    var stripInitialized by remember { mutableStateOf(false) }
    val today = LocalNow.current.toLocalDate()
    LaunchedEffect(selectedDate) {
        val index = (DATE_STRIP_RADIUS + ChronoUnit.DAYS.between(anchorDate, selectedDate))
            .toInt()
            .coerceIn(0, dates.lastIndex)
        val target = (index - 2).coerceAtLeast(0)
        if (stripInitialized) listState.animateScrollToItem(target)
        else {
            listState.scrollToItem(target)
            stripInitialized = true
        }
    }
    Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text(
                    dayTitleWithWeekday(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(dates, key = { it.toEpochDay() }) { date ->
                    DateChip(
                        date = date,
                        selected = date == selectedDate,
                        isToday = date == today,
                        onClick = { onSelectDate(date) }
                    )
                }
            }
            UntimedPanel(date = selectedDate, onOpenSlot = onOpenSlot)
            HorizontalDivider()
        }
    }
}
@Composable
private fun DateChip(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .width(46.dp)
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .then(
                if (isToday && !selected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val contentColor =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        Text(weekdayShort(date), style = MaterialTheme.typography.labelSmall, color = contentColor)
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}
@Composable
private fun UntimedPanel(date: LocalDate, onOpenSlot: (Long) -> Unit) {
    val untimed = ScheduleRepository.untimedSlotsFor(date)
    if (untimed.isEmpty()) return
    val slotColors = LocalSlotColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 132.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        untimed.forEach { slot ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(slotBackground(slot, slotColors))
                    .clickable { onOpenSlot(slot.id) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        slotTitle(slot),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = slotContent(slot, slotColors),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        slotSubtitle(slot),
                        style = MaterialTheme.typography.labelSmall,
                        color = slotContent(slot, slotColors).copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    slotNote(slot)?.let { note ->
                        Text(
                            note,
                            style = MaterialTheme.typography.labelSmall,
                            color = slotContent(slot, slotColors).copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayTimeline(
    state: DayState,
    onOpenSlot: (Long) -> Unit,
    onCreateAt: (Int) -> Unit,
    onDeleteRequest: (Slot) -> Unit,
    modifier: Modifier = Modifier
) {
    val slotColors = LocalSlotColors.current
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        if (!state.timelineAligned) {
            state.timelineAligned = true
            val offset = with(density) {
                (INITIAL_SCROLL_MINUTE / SEGMENT_MINUTES.toFloat() * SEGMENT_HEIGHT.toPx()).toInt()
            }
            state.timelineScroll.scrollTo(offset)
        }
    }
    val slots = ScheduleRepository.timedSlotsFor(state.selectedDate)
    val entries = remember(slots) { buildTimeline(slots) }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(state.timelineScroll)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        entries.forEach { entry ->
            val height = SEGMENT_HEIGHT * ((entry.end - entry.start) / SEGMENT_MINUTES.toFloat())
            Row(Modifier.fillMaxWidth().height(height)) {
                Text(
                    formatMinutes(entry.start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(46.dp).padding(top = 2.dp, end = 6.dp)
                )
                Box(Modifier.weight(1f).fillMaxHeight().padding(vertical = 1.dp)) {
                    when (entry) {
                        is TimelineEntry.Free -> Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(slotColors.free)
                                .border(1.dp, slotColors.gridLine, RoundedCornerShape(6.dp))
                                .clickable { onCreateAt(entry.start) }
                        )
                        is TimelineEntry.Occupied -> {
                            val slot = entry.slot
                            val isAppointment = slot.kind == SlotKind.APPOINTMENT
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(slotBackground(slot, slotColors))
                                    .combinedClickable(
                                        onClick = { if (isAppointment) onOpenSlot(slot.id) },
                                        onLongClick = { if (!isAppointment) onDeleteRequest(slot) }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                SlotBlockContent(
                                    slot = slot,
                                    contentColor = slotContent(slot, slotColors),
                                    height = height
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
@Composable
private fun SlotBlockContent(slot: Slot, contentColor: Color, height: Dp) {
    val segments = height / SEGMENT_HEIGHT
    val roomy = segments >= 1.9f
    Column {
        Text(
            slotTitle(slot),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = if (roomy) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
        if (segments >= 0.9f) {
            Text(
                slotSubtitle(slot),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (roomy) {
            slotNote(slot)?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = if (segments >= 2.9f) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
private sealed interface TimelineEntry {
    val start: Int
    val end: Int
    data class Free(override val start: Int, override val end: Int) : TimelineEntry
    data class Occupied(val slot: Slot) : TimelineEntry {
        override val start: Int get() = slot.startMinute
        override val end: Int get() = slot.endMinute
    }
}
private fun buildTimeline(slots: List<Slot>): List<TimelineEntry> {
    val result = mutableListOf<TimelineEntry>()
    var cursor = 0
    slots.sortedBy { it.startMinute }.forEach { slot ->
        if (slot.startMinute > cursor) result += freeSegments(cursor, slot.startMinute)
        if (slot.endMinute > cursor) {
            result += TimelineEntry.Occupied(slot)
            cursor = slot.endMinute
        }
    }
    if (cursor < MINUTES_IN_DAY) result += freeSegments(cursor, MINUTES_IN_DAY)
    return result
}
private fun freeSegments(from: Int, to: Int): List<TimelineEntry.Free> {
    val result = mutableListOf<TimelineEntry.Free>()
    var start = from
    while (start < to) {
        val end = minOf((start / SEGMENT_MINUTES + 1) * SEGMENT_MINUTES, to)
        result += TimelineEntry.Free(start, end)
        start = end
    }
    return result
}
