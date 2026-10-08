package com.kalinbetschedule.ui.records
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.ui.LocalNow
import com.kalinbetschedule.ui.WEEKDAYS_SHORT
import com.kalinbetschedule.ui.monthTitle
import com.kalinbetschedule.ui.theme.LocalSlotColors
import com.kalinbetschedule.ui.theme.SlotColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
private const val MONTH_PAGE_COUNT = 2400
private const val MONTH_BASE_PAGE = MONTH_PAGE_COUNT / 2
private val CELL_TEXT_LINE = 9.dp
private val CELL_BLOCK_PADDING = 4.dp
private val CELL_BLOCK_GAP = 2.dp
private val CELL_HEADER_HEIGHT = 15.dp
private val CELL_GAP = 3.dp
private val GRID_PADDING = 2.dp
private val CELL_MIN_HEIGHT = 56.dp
private val CELL_PADDING_V = 4.dp
@Composable
fun CalendarScreen(onOpenDay: (LocalDate) -> Unit) {
    val today = LocalNow.current.toLocalDate()
    val baseMonth = remember(today.year, today.monthValue) { YearMonth.of(today.year, today.month) }
    val pagerState = rememberPagerState(initialPage = MONTH_BASE_PAGE) { MONTH_PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val visibleMonth by remember {
        derivedStateOf { baseMonth.plusMonths((pagerState.currentPage - MONTH_BASE_PAGE).toLong()) }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущий месяц"
                )
            }
            Text(
                monthTitle(visibleMonth),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Следующий месяц"
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            TextButton(
                enabled = pagerState.currentPage != MONTH_BASE_PAGE,
                onClick = { scope.launch { pagerState.animateScrollToPage(MONTH_BASE_PAGE) } }
            ) { Text("Текущий месяц") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            WEEKDAYS_SHORT.forEachIndexed { index, name ->
                Text(
                    name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index >= 5) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val cellHeight = ((maxHeight - GRID_PADDING * 2 - CELL_GAP * 5) / 6)
                .coerceAtLeast(CELL_MIN_HEIGHT)
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                MonthGrid(
                    month = baseMonth.plusMonths((page - MONTH_BASE_PAGE).toLong()),
                    today = today,
                    cellHeight = cellHeight,
                    onOpenDay = onOpenDay
                )
            }
        }
    }
}
@Immutable
private data class CellSlot(val text: String, val background: Color, val style: TextStyle)
private val CELL_TEXT_STYLE = TextStyle(fontSize = 8.sp, lineHeight = 9.sp)
@Immutable
private class CellStyles(
    val day: TextStyle,
    val today: TextStyle,
    val outside: TextStyle,
    val counter: TextStyle
)
@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    cellHeight: Dp,
    onOpenDay: (LocalDate) -> Unit
) {
    val firstOfMonth = month.atDay(1)
    val firstCell = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())
    val lastCell = firstCell.plusDays(41)
    val slotColors = LocalSlotColors.current
    val entriesByDate by remember(firstCell, slotColors) {
        derivedStateOf { monthEntries(firstCell, lastCell, slotColors) }
    }
    val numberStyle = MaterialTheme.typography.labelMedium
    val scheme = MaterialTheme.colorScheme
    val styles = remember(numberStyle, scheme) {
        CellStyles(
            day = numberStyle.copy(color = scheme.onSurface, fontWeight = FontWeight.Medium),
            today = numberStyle.copy(color = scheme.primary, fontWeight = FontWeight.Bold),
            outside = numberStyle.copy(color = scheme.outline, fontWeight = FontWeight.Medium),
            counter = CELL_TEXT_STYLE.copy(color = scheme.onSurfaceVariant)
        )
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = GRID_PADDING),
        verticalArrangement = Arrangement.spacedBy(CELL_GAP)
    ) {
        repeat(6) { week ->
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(CELL_GAP)
            ) {
                repeat(7) { dayOfWeek ->
                    val date = firstCell.plusDays((week * 7 + dayOfWeek).toLong())
                    DayCell(
                        date = date,
                        inMonth = date.month == month.month,
                        isToday = date == today,
                        entries = entriesByDate[date].orEmpty(),
                        cellHeight = cellHeight,
                        styles = styles,
                        onClick = { onOpenDay(date) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}
private fun monthEntries(
    firstCell: LocalDate,
    lastCell: LocalDate,
    colors: SlotColors
): Map<LocalDate, List<CellSlot>> {
    val styles = HashMap<Color, TextStyle>(4)
    return ScheduleRepository.slots
        .filter { it.date >= firstCell && it.date <= lastCell }
        .sortedWith(compareBy({ it.timed }, { it.startMinute }))
        .groupBy { it.date }
        .mapValues { (_, daySlots) ->
            daySlots.map { slot ->
                val content = slotContent(slot, colors)
                CellSlot(
                    text = slotLine(slot),
                    background = slotBackground(slot, colors),
                    style = styles.getOrPut(content) { CELL_TEXT_STYLE.copy(color = content) }
                )
            }
        }
}
@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    entries: List<CellSlot>,
    cellHeight: Dp,
    styles: CellStyles,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val background = MaterialTheme.colorScheme.surfaceVariant
        .copy(alpha = if (inMonth) 0.45f else 0.18f)
    val layout = cellLayout(cellHeight, entries.size)
    val counterStyle = styles.counter
    Column(
        modifier
            .heightIn(min = CELL_MIN_HEIGHT)
            .clip(shape)
            .background(background)
            .then(
                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        BasicText(
            date.dayOfMonth.toString(),
            style = when {
                !inMonth -> styles.outside
                isToday -> styles.today
                else -> styles.day
            },
            modifier = Modifier.height(CELL_HEADER_HEIGHT).padding(start = 2.dp)
        )
        entries.take(layout.visible).forEachIndexed { index, entry ->
            if (index > 0) Spacer(Modifier.height(CELL_BLOCK_GAP))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(layout.blockHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(entry.background)
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                BasicText(
                    entry.text,
                    style = entry.style,
                    maxLines = layout.lines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (layout.hidden > 0) {
            Spacer(Modifier.height(CELL_BLOCK_GAP))
            BasicText(
                "+${layout.hidden}",
                style = counterStyle,
                modifier = Modifier.padding(start = 3.dp)
            )
        }
    }
}
private data class CellLayout(
    val visible: Int,
    val blockHeight: Dp,
    val lines: Int,
    val hidden: Int
)
private fun cellLayout(cellHeight: Dp, count: Int): CellLayout {
    val empty = CellLayout(0, 0.dp, 1, count)
    val singleLine = CELL_TEXT_LINE + CELL_BLOCK_PADDING
    var budget = cellHeight - CELL_HEADER_HEIGHT - CELL_PADDING_V
    val capacity = ((budget + CELL_BLOCK_GAP) / (singleLine + CELL_BLOCK_GAP)).toInt()
    if (count == 0 || capacity <= 0) return empty
    val overflow = count > capacity
    val visible = if (overflow) capacity - 1 else count
    if (visible <= 0) return empty
    if (overflow) budget -= CELL_TEXT_LINE + CELL_BLOCK_GAP
    val blockHeight = (budget - CELL_BLOCK_GAP * (visible - 1)) / visible
    return CellLayout(
        visible = visible,
        blockHeight = blockHeight,
        lines = ((blockHeight - CELL_BLOCK_PADDING) / CELL_TEXT_LINE).toInt().coerceAtLeast(1),
        hidden = count - visible
    )
}
