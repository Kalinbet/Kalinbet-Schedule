package com.kalinbetschedule.ui.records
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.Client
import com.kalinbetschedule.data.MINUTES_IN_DAY
import com.kalinbetschedule.data.SEGMENT_MINUTES
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.Service
import com.kalinbetschedule.data.Slot
import com.kalinbetschedule.data.SlotKind
import com.kalinbetschedule.data.SlotStatus
import com.kalinbetschedule.data.formatDuration
import com.kalinbetschedule.data.formatMinutes
import com.kalinbetschedule.data.formatMoney
import com.kalinbetschedule.ui.components.DropdownField
import com.kalinbetschedule.ui.components.NameField
import com.kalinbetschedule.ui.components.NumberField
import com.kalinbetschedule.ui.components.PickDateDialog
import com.kalinbetschedule.ui.dayTitle
import com.kalinbetschedule.ui.dayTitleWithWeekday
import java.time.LocalDate
private val START_OPTIONS = (0 until MINUTES_IN_DAY step SEGMENT_MINUTES).toList()
private val END_OPTIONS = (SEGMENT_MINUTES..MINUTES_IN_DAY step SEGMENT_MINUTES).toList()
@Composable
fun OccupyTimeDialog(date: LocalDate, suggestedStart: Int, onDismiss: () -> Unit) {
    var kind by remember { mutableStateOf(SlotKind.APPOINTMENT) }
    var start by remember { mutableIntStateOf(suggestedStart) }
    var busyEnd by remember {
        mutableIntStateOf((suggestedStart + SEGMENT_MINUTES).coerceAtMost(MINUTES_IN_DAY))
    }
    var service by remember { mutableStateOf<Service?>(ScheduleRepository.services.firstOrNull()) }
    var client by remember { mutableStateOf<Client?>(null) }
    var newClientName by remember { mutableStateOf("") }
    var creatingClient by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    val end = if (kind == SlotKind.APPOINTMENT) {
        service?.let { (start + it.durationMinutes).coerceAtMost(MINUTES_IN_DAY) } ?: start
    } else {
        busyEnd
    }
    val clientChosen = client != null || (creatingClient && newClientName.isNotBlank())
    val canSave = end > start &&
        !ScheduleRepository.hasOverlap(date, start, end) &&
        (kind == SlotKind.BUSY || (service != null && clientChosen))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Занять время · ${dayTitle(date)}") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = kind == SlotKind.APPOINTMENT,
                            onClick = { kind = SlotKind.APPOINTMENT },
                            label = { Text("Запись") }
                        )
                        FilterChip(
                            selected = kind == SlotKind.BUSY,
                            onClick = { kind = SlotKind.BUSY },
                            label = { Text("Занятость") }
                        )
                    }
                    DropdownField(
                        label = "Начало",
                        selected = start,
                        options = START_OPTIONS,
                        optionLabel = ::formatMinutes,
                        onSelect = {
                            start = it
                            if (busyEnd <= it) {
                                busyEnd = (it + SEGMENT_MINUTES).coerceAtMost(MINUTES_IN_DAY)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (kind == SlotKind.APPOINTMENT) {
                        DropdownField(
                            label = "Тип процедуры",
                            selected = service,
                            options = ScheduleRepository.services,
                            optionLabel = {
                                "${it.name} · ${formatDuration(it.durationMinutes)} · ${formatMoney(it.priceRub)}"
                            },
                            onSelect = { service = it },
                            emptyHint = "Добавьте услуги во вкладке «Управление»",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Окончание: ${formatMinutes(end)}" +
                                (service?.let { " · ${formatDuration(it.durationMinutes)}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (creatingClient) {
                            NameField(
                                value = newClientName,
                                onValueChange = { newClientName = it },
                                label = "Имя нового клиента",
                                trailingIcon = {
                                    IconButton(onClick = {
                                        creatingClient = false
                                        newClientName = ""
                                    }) { Icon(Icons.Filled.Close, contentDescription = "Отменить") }
                                }
                            )
                        } else {
                            DropdownField(
                                label = "Клиент",
                                selected = client,
                                options = ScheduleRepository.clients,
                                optionLabel = { it.name },
                                onSelect = { client = it },
                                emptyHint = "Клиентов пока нет",
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(onClick = {
                                creatingClient = true
                                client = null
                            }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Новый клиент")
                            }
                        }
                    } else {
                        DropdownField(
                            label = "Окончание",
                            selected = busyEnd,
                            options = END_OPTIONS.filter { it > start },
                            optionLabel = ::formatMinutes,
                            onSelect = { busyEnd = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Длительность: ${formatDuration((end - start).coerceAtLeast(0))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (kind == SlotKind.APPOINTMENT) {
                        nextRecordHint(service, date)?.let { hint ->
                            HorizontalDivider()
                            Text(
                                hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    if (kind == SlotKind.APPOINTMENT) {
                        val target = client ?: ScheduleRepository.addClient(newClientName, "")
                        ScheduleRepository.addAppointment(
                            date = date,
                            startMinute = start,
                            endMinute = end,
                            clientId = target.id,
                            serviceId = service?.id,
                            description = description
                        )
                    } else {
                        ScheduleRepository.addBusy(date, start, end, description)
                    }
                    onDismiss()
                }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
fun nextRecordHint(service: Service?, from: LocalDate): String? {
    if (service == null) return null
    val next = ScheduleRepository.nextServiceOf(service) ?: return null
    return "${next.name} — ${dayTitle(from.plusDays(service.nextOffsetDays.toLong()))}"
}
@Composable
fun CompleteAppointmentDialog(slot: Slot, onDismiss: () -> Unit) {
    val extras = remember { mutableStateListOf<Long?>() }
    var tips by remember { mutableStateOf("") }
    val basePrice = ScheduleRepository.serviceById(slot.serviceId)?.priceRub ?: 0
    val extrasPrice = extras.sumOf { id -> ScheduleRepository.serviceById(id)?.priceRub ?: 0 }
    val tipsValue = tips.toIntOrNull() ?: 0
    val total = basePrice + extrasPrice + tipsValue
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Завершение записи") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        ScheduleRepository.serviceById(slot.serviceId)?.name ?: "Без услуги",
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatMoney(basePrice), fontWeight = FontWeight.Medium)
                }
                HorizontalDivider()
                Text(
                    "Дополнительные процедуры",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                extras.forEachIndexed { index, id ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DropdownField(
                            label = "Процедура ${index + 1}",
                            selected = ScheduleRepository.serviceById(id),
                            options = ScheduleRepository.services,
                            optionLabel = { "${it.name} · ${formatMoney(it.priceRub)}" },
                            onSelect = { extras[index] = it.id },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { extras.removeAt(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Убрать процедуру")
                        }
                    }
                }
                TextButton(onClick = { extras.add(null) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Добавить процедуру")
                }
                NumberField(
                    label = "Чаевые",
                    value = tips,
                    onValueChange = { tips = it },
                    suffix = "₽",
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        "Итого",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatMoney(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                val followUps = ScheduleRepository.followUpsFor(slot, extras.filterNotNull())
                if (followUps.isNotEmpty()) {
                    Text(
                        "После подтверждения появятся записи без времени:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    followUps.forEach { followUp ->
                        Text(
                            "${followUp.service.name} — ${dayTitle(followUp.date)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                ScheduleRepository.completeAppointment(
                    id = slot.id,
                    extraServiceIds = extras.filterNotNull(),
                    tipsRub = tipsValue
                )
                onDismiss()
            }) { Text("Подтвердить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
private data class StartOption(val minute: Int?) {
    val label: String get() = minute?.let(::formatMinutes) ?: "Не указано"
}
private val START_CHOICES = listOf(StartOption(null)) + START_OPTIONS.map(::StartOption)
private data class StatusOption(val status: SlotStatus) {
    val label: String get() = when (status) {
        SlotStatus.PLANNED -> "Запланирована"
        SlotStatus.COMPLETED -> "Завершена"
    }
}
private val STATUS_CHOICES = listOf(
    StatusOption(SlotStatus.PLANNED),
    StatusOption(SlotStatus.COMPLETED)
)
@Composable
fun EditAppointmentDialog(slot: Slot, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf(slot.date) }
    var start by remember { mutableStateOf(slot.startMinute.takeIf { slot.timed }) }
    var service by remember { mutableStateOf(ScheduleRepository.serviceById(slot.serviceId)) }
    var client by remember { mutableStateOf(ScheduleRepository.clientById(slot.clientId)) }
    var description by remember { mutableStateOf(slot.description) }
    var status by remember { mutableStateOf(slot.status) }
    var pickingDate by remember { mutableStateOf(false) }
    val keptDuration = (slot.endMinute - slot.startMinute).takeIf { slot.timed && it > 0 }
    val duration = service?.durationMinutes ?: keptDuration ?: SEGMENT_MINUTES
    val startMinute = start
    val end = startMinute?.let { (it + duration).coerceAtMost(MINUTES_IN_DAY) }
    val busy = startMinute != null && end != null &&
        ScheduleRepository.hasOverlap(date, startMinute, end, excludeId = slot.id)
    val canSave = startMinute == null || (end != null && end > startMinute && !busy)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменение записи") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { pickingDate = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(dayTitleWithWeekday(date)) }
                DropdownField(
                    label = "Начало",
                    selected = StartOption(startMinute),
                    options = START_CHOICES,
                    optionLabel = { it.label },
                    onSelect = { start = it.minute },
                    modifier = Modifier.fillMaxWidth()
                )
                end?.let { endMinute ->
                    Text(
                        "Окончание: ${formatMinutes(endMinute)} · ${formatDuration(duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (busy) {
                    Text(
                        "Это время уже занято другой записью",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                DropdownField(
                    label = "Статус",
                    selected = StatusOption(status),
                    options = STATUS_CHOICES,
                    optionLabel = { it.label },
                    onSelect = { status = it.status },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownField(
                    label = "Тип процедуры",
                    selected = service,
                    options = ScheduleRepository.services,
                    optionLabel = {
                        "${it.name} · ${formatDuration(it.durationMinutes)} · ${formatMoney(it.priceRub)}"
                    },
                    onSelect = { service = it },
                    emptyHint = "Добавьте услуги во вкладке «Управление»",
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownField(
                    label = "Клиент",
                    selected = client,
                    options = ScheduleRepository.clients,
                    optionLabel = { it.name },
                    onSelect = { client = it },
                    emptyHint = "Клиентов пока нет",
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                nextRecordHint(service, date)?.let { hint ->
                    HorizontalDivider()
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    ScheduleRepository.updateSlot(
                        slot.copy(
                            date = date,
                            startMinute = startMinute ?: 0,
                            endMinute = end ?: 0,
                            clientId = client?.id,
                            serviceId = service?.id,
                            description = description.trim(),
                            status = status,
                            timed = startMinute != null
                        )
                    )
                    onDismiss()
                }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
    if (pickingDate) {
        PickDateDialog(
            initial = date,
            onDismiss = { pickingDate = false },
            onPick = {
                date = it
                pickingDate = false
            }
        )
    }
}
