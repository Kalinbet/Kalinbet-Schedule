package com.kalinbetschedule.ui.manage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.DEFAULT_NEXT_OFFSET_DAYS
import com.kalinbetschedule.data.NEXT_OFFSET_PRESETS
import com.kalinbetschedule.data.NextProcedure
import com.kalinbetschedule.data.SEGMENT_MINUTES
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.Service
import com.kalinbetschedule.data.formatDuration
import com.kalinbetschedule.data.formatMoney
import com.kalinbetschedule.data.formatOffset
import com.kalinbetschedule.ui.components.AppTopBar
import com.kalinbetschedule.ui.components.DropdownField
import com.kalinbetschedule.ui.components.EmptyState
import com.kalinbetschedule.ui.components.NumberField
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Service?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Service?>(null) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Услуги",
            onBack = onBack,
            actions = {
                IconButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить услугу")
                }
            }
        )
        if (ScheduleRepository.services.isEmpty()) {
            EmptyState("Услуг пока нет. Добавьте первую кнопкой «+».")
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ScheduleRepository.services.toList(), key = { it.id }) { service ->
                    Card(onClick = { editing = service }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(service.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    formatDuration(service.durationMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                chainLabel(service)?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                formatMoney(service.priceRub),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = { deleting = service }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Удалить услугу")
                            }
                        }
                    }
                }
            }
        }
    }
    if (adding) {
        ServiceEditDialog(
            service = null,
            onDismiss = { adding = false },
            onSave = { draft ->
                ScheduleRepository.addService(
                    name = draft.name,
                    durationMinutes = draft.durationMinutes,
                    priceRub = draft.priceRub,
                    nextProcedure = draft.nextProcedure,
                    nextServiceId = draft.nextServiceId,
                    nextOffsetDays = draft.nextOffsetDays
                )
                adding = false
            }
        )
    }
    editing?.let { service ->
        ServiceEditDialog(
            service = service,
            onDismiss = { editing = null },
            onSave = { draft ->
                ScheduleRepository.updateService(
                    service.copy(
                        name = draft.name.trim(),
                        durationMinutes = draft.durationMinutes,
                        priceRub = draft.priceRub,
                        nextProcedure = draft.nextProcedure,
                        nextServiceId = draft.nextServiceId,
                        nextOffsetDays = draft.nextOffsetDays
                    )
                )
                editing = null
            }
        )
    }
    deleting?.let { service ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Удалить услугу?") },
            text = {
                Text(
                    "«${service.name}» исчезнет из списка. " +
                        "В уже созданных записях услуга перестанет отображаться."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ScheduleRepository.deleteService(service.id)
                    deleting = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена") } }
        )
    }
}
private data class ServiceDraft(
    val name: String,
    val durationMinutes: Int,
    val priceRub: Int,
    val nextProcedure: NextProcedure,
    val nextServiceId: Long?,
    val nextOffsetDays: Int
)
@Composable
private fun ServiceEditDialog(
    service: Service?,
    onDismiss: () -> Unit,
    onSave: (ServiceDraft) -> Unit
) {
    var name by remember { mutableStateOf(service?.name.orEmpty()) }
    var duration by remember { mutableStateOf(service?.durationMinutes?.toString() ?: "60") }
    var price by remember { mutableStateOf(service?.priceRub?.toString() ?: "") }
    var nextProcedure by remember {
        mutableStateOf(service?.nextProcedure ?: NextProcedure.SAME)
    }
    var nextService by remember {
        mutableStateOf(ScheduleRepository.serviceById(service?.nextServiceId))
    }
    val savedOffset = service?.nextOffsetDays ?: DEFAULT_NEXT_OFFSET_DAYS
    var offsetDays by remember { mutableIntStateOf(savedOffset) }
    var customOffset by remember { mutableStateOf(savedOffset !in NEXT_OFFSET_PRESETS) }
    var customText by remember { mutableStateOf(savedOffset.takeIf { it > 0 }?.toString() ?: "") }
    val chained = nextProcedure != NextProcedure.NONE
    val durationValue = duration.toIntOrNull() ?: 0
    val canSave = name.isNotBlank() &&
        durationValue >= SEGMENT_MINUTES &&
        (nextProcedure != NextProcedure.SPECIFIC || nextService != null) &&
        (!chained || offsetDays > 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (service == null) "Новая услуга" else "Редактирование услуги") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                NumberField(
                    label = "Время процедуры",
                    value = duration,
                    onValueChange = { duration = it },
                    suffix = "мин",
                    modifier = Modifier.fillMaxWidth()
                )
                NumberField(
                    label = "Стоимость",
                    value = price,
                    onValueChange = { price = it },
                    suffix = "₽",
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                DropdownField(
                    label = "Следующая услуга",
                    selected = NextOption(nextProcedure, nextService),
                    options = listOf(
                        NextOption(NextProcedure.SAME, null),
                        NextOption(NextProcedure.NONE, null)
                    ) + ScheduleRepository.services
                        .filter { it.id != service?.id }
                        .map { NextOption(NextProcedure.SPECIFIC, it) },
                    optionLabel = { it.label },
                    onSelect = {
                        nextProcedure = it.procedure
                        nextService = it.service
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (chained) {
                    DropdownField(
                        label = "Следующая запись через",
                        selected = OffsetOption(if (customOffset) null else offsetDays),
                        options = NEXT_OFFSET_PRESETS.map { OffsetOption(it) } + OffsetOption(null),
                        optionLabel = { it.label },
                        onSelect = { option ->
                            if (option.days == null) {
                                customOffset = true
                                offsetDays = customText.toIntOrNull() ?: 0
                            } else {
                                customOffset = false
                                offsetDays = option.days
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (customOffset) {
                        NumberField(
                            label = "Количество дней",
                            value = customText,
                            onValueChange = {
                                customText = it
                                offsetDays = it.toIntOrNull() ?: 0
                            },
                            suffix = "дн.",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        ServiceDraft(
                            name = name,
                            durationMinutes = durationValue,
                            priceRub = price.toIntOrNull() ?: 0,
                            nextProcedure = nextProcedure,
                            nextServiceId = nextService?.id,
                            nextOffsetDays = offsetDays
                        )
                    )
                }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
private fun chainLabel(service: Service): String? = when (service.nextProcedure) {
    NextProcedure.NONE -> null
    NextProcedure.SAME -> "Повтор через ${formatOffset(service.nextOffsetDays)}"
    NextProcedure.SPECIFIC -> ScheduleRepository.serviceById(service.nextServiceId)
        ?.let { "→ ${it.name} через ${formatOffset(service.nextOffsetDays)}" }
}
private data class NextOption(val procedure: NextProcedure, val service: Service?) {
    val label: String
        get() = when (procedure) {
            NextProcedure.SAME -> "Повторная процедура"
            NextProcedure.NONE -> "Без процедуры"
            NextProcedure.SPECIFIC -> service?.name ?: "Не выбрана"
        }
}
private data class OffsetOption(val days: Int?) {
    val label: String get() = days?.let(::formatOffset) ?: "Другое…"
}
