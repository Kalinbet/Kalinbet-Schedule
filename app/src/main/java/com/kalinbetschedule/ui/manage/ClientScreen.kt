package com.kalinbetschedule.ui.manage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.SlotStatus
import com.kalinbetschedule.data.formatMoney
import com.kalinbetschedule.ui.records.slotSubtitle
import com.kalinbetschedule.ui.components.AppTopBar
import com.kalinbetschedule.ui.components.EmptyState
import com.kalinbetschedule.ui.components.LabeledValue
import com.kalinbetschedule.ui.components.SectionTitle
import com.kalinbetschedule.ui.shortDate
import com.kalinbetschedule.ui.theme.LocalSlotColors
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(clientId: Long, onBack: () -> Unit, onOpenSlot: (Long) -> Unit) {
    val client = ScheduleRepository.clientById(clientId)
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val slotColors = LocalSlotColors.current
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Клиент",
            onBack = onBack,
            actions = {
                if (client != null) {
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = { deleting = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить клиента")
                    }
                }
            }
        )
        if (client == null) {
            EmptyState("Клиент не найден")
        } else {
            val visits = ScheduleRepository.appointmentsOf(client.id)
            val completed = visits.filter { it.status == SlotStatus.COMPLETED }
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(client.name, style = MaterialTheme.typography.headlineSmall)
                        LabeledValue("Телефон", client.phone.ifBlank { "—" })
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text("Всего записей", Modifier.weight(1f))
                            Text(visits.size.toString(), fontWeight = FontWeight.Medium)
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Text("Завершено", Modifier.weight(1f))
                            Text(completed.size.toString(), fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Оставлено денег",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                formatMoney(completed.sumOf { it.totalRub }),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                SectionTitle("История записей")
                if (visits.isEmpty()) {
                    Text(
                        "Записей ещё не было",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    visits.forEach { slot ->
                        Card(
                            onClick = { onOpenSlot(slot.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        ScheduleRepository.serviceById(slot.serviceId)?.name
                                            ?: "Без услуги",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "${shortDate(slot.date)} · ${slotSubtitle(slot)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val completedSlot = slot.status == SlotStatus.COMPLETED
                                Text(
                                    if (completedSlot) formatMoney(slot.totalRub)
                                    else "Запланирована",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (completedSlot) slotColors.appointment
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (editing && client != null) {
        ClientEditDialog(
            client = client,
            onDismiss = { editing = false },
            onSave = { name, phone ->
                ScheduleRepository.updateClient(
                    client.copy(name = name.trim(), phone = phone.trim())
                )
                editing = false
            }
        )
    }
    if (deleting && client != null) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Удалить клиента?") },
            text = { Text("Записи останутся в расписании, но потеряют привязку к клиенту.") },
            confirmButton = {
                TextButton(onClick = {
                    ScheduleRepository.deleteClient(client.id)
                    deleting = false
                    onBack()
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Отмена") } }
        )
    }
}
