package com.kalinbetschedule.ui.manage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.SlotKind
import com.kalinbetschedule.data.SlotStatus
import com.kalinbetschedule.data.formatMoney
import com.kalinbetschedule.ui.LocalNow
import com.kalinbetschedule.ui.components.AppTopBar
import com.kalinbetschedule.ui.components.PickDateDialog
import com.kalinbetschedule.ui.components.SectionTitle
import com.kalinbetschedule.ui.shortDate
import com.kalinbetschedule.ui.theme.LocalSlotColors
import java.time.LocalDate
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val today = LocalNow.current.toLocalDate()
    var from by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var to by remember { mutableStateOf(today) }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }
    val slotColors = LocalSlotColors.current
    val appointments = ScheduleRepository.slots.filter {
        it.kind == SlotKind.APPOINTMENT && !it.date.isBefore(from) && !it.date.isAfter(to)
    }
    val completed = appointments.filter { it.status == SlotStatus.COMPLETED }
    val planned = appointments.filter { it.status == SlotStatus.PLANNED }
    val earned = completed.sumOf { it.totalRub }
    val tips = completed.sumOf { it.tipsRub }
    val plannedSum = planned.sumOf { ScheduleRepository.estimatedTotal(it) }
    val perService = linkedMapOf<Long, Pair<Int, Int>>()
    completed.forEach { slot ->
        (listOfNotNull(slot.serviceId) + slot.extraServiceIds).forEach { id ->
            val price = ScheduleRepository.serviceById(id)?.priceRub ?: 0
            val current = perService[id] ?: (0 to 0)
            perService[id] = (current.first + 1) to (current.second + price)
        }
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Статистика", onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodChip("Сегодня", from == today && to == today) {
                    from = today
                    to = today
                }
                PeriodChip("Неделя", from == today.minusDays(6) && to == today) {
                    from = today.minusDays(6)
                    to = today
                }
                PeriodChip(
                    "Месяц",
                    from == today.withDayOfMonth(1) && to == today
                ) {
                    from = today.withDayOfMonth(1)
                    to = today
                }
                PeriodChip("Год", from == today.withDayOfYear(1) && to == today) {
                    from = today.withDayOfYear(1)
                    to = today
                }
                PeriodChip("Всё время", from == LocalDate.of(2000, 1, 1)) {
                    from = LocalDate.of(2000, 1, 1)
                    to = today.plusYears(5)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickingFrom = true }, modifier = Modifier.weight(1f)) {
                    Text("с ${shortDate(from)}")
                }
                OutlinedButton(onClick = { pickingTo = true }, modifier = Modifier.weight(1f)) {
                    Text("по ${shortDate(to)}")
                }
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Заработано за период",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatMoney(earned),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (tips > 0) {
                        Text(
                            "в том числе чаевые ${formatMoney(tips)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    StatRow("Завершено записей", completed.size.toString())
                    StatRow("Не завершено", planned.size.toString())
                    StatRow(
                        "Средний чек",
                        formatMoney(if (completed.isEmpty()) 0 else earned / completed.size)
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Ожидается",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            formatMoney(plannedSum),
                            style = MaterialTheme.typography.bodyMedium,
                            color = slotColors.appointment,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (perService.isNotEmpty()) {
                SectionTitle("По услугам")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        perService.entries
                            .sortedByDescending { it.value.second }
                            .forEach { (id, stats) ->
                                StatRow(
                                    "${ScheduleRepository.serviceById(id)?.name ?: "Удалённая услуга"} × ${stats.first}",
                                    formatMoney(stats.second)
                                )
                            }
                    }
                }
            }
        }
    }
    if (pickingFrom) {
        PickDateDialog(
            initial = from,
            onDismiss = { pickingFrom = false },
            onPick = {
                from = it
                if (to.isBefore(it)) to = it
                pickingFrom = false
            }
        )
    }
    if (pickingTo) {
        PickDateDialog(
            initial = to,
            onDismiss = { pickingTo = false },
            onPick = {
                to = it
                if (from.isAfter(it)) from = it
                pickingTo = false
            }
        )
    }
}
@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
