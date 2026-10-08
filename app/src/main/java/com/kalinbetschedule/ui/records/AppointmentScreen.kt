package com.kalinbetschedule.ui.records
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.SlotStatus
import com.kalinbetschedule.data.formatDuration
import com.kalinbetschedule.data.formatMinutes
import com.kalinbetschedule.data.formatMoney
import com.kalinbetschedule.ui.components.AppTopBar
import com.kalinbetschedule.ui.components.EmptyState
import com.kalinbetschedule.ui.dayTitle
import com.kalinbetschedule.ui.dayTitleWithWeekday
import com.kalinbetschedule.ui.theme.LocalSlotColors
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(
    slotId: Long,
    onBack: () -> Unit,
    onOpenClient: (Long) -> Unit
) {
    val slot = ScheduleRepository.slotById(slotId)
    var completing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val slotColors = LocalSlotColors.current
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Запись",
            onBack = onBack,
            actions = {
                if (slot != null) {
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Изменить запись")
                    }
                    IconButton(onClick = { deleting = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить запись")
                    }
                }
            }
        )
        if (slot == null) {
            EmptyState("Запись не найдена")
        } else {
            val client = ScheduleRepository.clientById(slot.clientId)
            val service = ScheduleRepository.serviceById(slot.serviceId)
            val completed = slot.status == SlotStatus.COMPLETED
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val statusColor = when {
                    completed -> slotColors.appointment
                    !slot.timed -> slotColors.untimed
                    else -> MaterialTheme.colorScheme.primary
                }
                val statusText = when {
                    completed -> "Завершена"
                    !slot.timed -> "Запланирована, время не назначено"
                    else -> "Запланирована"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }
                Card(
                    onClick = { slot.clientId?.let(onOpenClient) },
                    enabled = slot.clientId != null,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Клиент",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                    .copy(alpha = 0.7f)
                            )
                            Text(
                                client?.name ?: "Не указан",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (slot.clientId != null) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Открыть карточку клиента"
                            )
                        }
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            service?.let {
                                "${it.name} · ${formatDuration(it.durationMinutes)} · ${formatMoney(it.priceRub)}"
                            } ?: "Не указан"
                        )
                        Text(
                            if (slot.timed) {
                                "${dayTitleWithWeekday(slot.date)}\n" +
                                    "${formatMinutes(slot.startMinute)} – " +
                                    "${formatMinutes(slot.endMinute)} " +
                                    "(${formatDuration(slot.durationMinutes)})"
                            } else {
                                "${dayTitleWithWeekday(slot.date)}\nВремя не назначено"
                            },
                            Modifier.padding(top = 24.dp)
                        )
                        if (slot.description != "") {
                            Text(slot.description, Modifier.padding(top = 24.dp))
                        }
                        if (!slot.timed && !completed) {
                            OutlinedButton(
                                onClick = { editing = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) { Text("Назначить время") }
                        }
                    }
                }
                if (!completed) {
                    nextRecordHint(service, slot.date)?.let { hint ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Следующая запись",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    hint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (completed) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Итоги", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            MoneyRow(service?.name ?: "Основная услуга", service?.priceRub ?: 0)
                            slot.extraServiceIds.forEach { id ->
                                val extra = ScheduleRepository.serviceById(id)
                                MoneyRow(
                                    extra?.name ?: "Дополнительная процедура",
                                    extra?.priceRub ?: 0
                                )
                            }
                            if (slot.tipsRub > 0) MoneyRow("Чаевые", slot.tipsRub)
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Итого",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    formatMoney(slot.totalRub),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            ScheduleRepository.followUpsFor(slot).forEachIndexed { index, next ->
                                Text(
                                    "Следующая запись: ${next.service.name} — " +
                                        dayTitle(next.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = if (index == 0) 8.dp else 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { completing = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Завершить") }
                }
            }
        }
    }
    if (completing && slot != null) {
        CompleteAppointmentDialog(slot = slot, onDismiss = { completing = false })
    }
    if (editing && slot != null) {
        EditAppointmentDialog(slot = slot, onDismiss = { editing = false })
    }
    if (deleting && slot != null) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Удалить запись?") },
            text = { Text("Запись будет удалена из расписания без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    ScheduleRepository.deleteSlot(slot.id)
                    deleting = false
                    onBack()
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Отмена") } }
        )
    }
}
@Composable
private fun MoneyRow(label: String, amount: Int) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(formatMoney(amount), style = MaterialTheme.typography.bodyMedium)
    }
}
