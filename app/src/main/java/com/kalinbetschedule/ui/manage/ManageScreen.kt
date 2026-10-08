package com.kalinbetschedule.ui.manage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.Slot
import com.kalinbetschedule.data.SlotKind
import com.kalinbetschedule.ui.components.AppTopBar
import com.kalinbetschedule.ui.components.EmptyState
import com.kalinbetschedule.ui.components.SectionTitle
import com.kalinbetschedule.ui.records.slotSubtitle
import com.kalinbetschedule.ui.shortDate
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    onOpenClients: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenClient: (Long) -> Unit,
    onOpenSlot: (Long) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Управление")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text("Поиск по записям и клиентам") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Очистить")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (trimmed.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ManageButton(
                    icon = Icons.Filled.Person,
                    title = "Клиенты",
                    subtitle = "${ScheduleRepository.clients.size} в базе",
                    onClick = onOpenClients
                )
                ManageButton(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "Услуги",
                    subtitle = "${ScheduleRepository.services.size} типов процедур",
                    onClick = onOpenServices
                )
                ManageButton(
                    icon = Icons.Filled.ShoppingCart,
                    title = "Статистика",
                    subtitle = "Заработок за период",
                    onClick = onOpenStatistics
                )
            }
        } else {
            SearchResults(
                query = trimmed,
                onOpenClient = onOpenClient,
                onOpenSlot = onOpenSlot
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResults(
    query: String,
    onOpenClient: (Long) -> Unit,
    onOpenSlot: (Long) -> Unit
) {
    val needle = query.lowercase()
    val clients = ScheduleRepository.clients.filter { it.name.lowercase().contains(needle) }
    val slots = ScheduleRepository.slots
        .filter { it.description.lowercase().contains(needle) }
        .sortedWith(compareByDescending<Slot> { it.date }.thenBy { it.startMinute })
    if (clients.isEmpty() && slots.isEmpty()) {
        EmptyState("Ничего не найдено")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (clients.isNotEmpty()) {
            item { SectionTitle("Клиенты") }
            items(clients, key = { "c${it.id}" }) { client ->
                Card(onClick = { onOpenClient(client.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(client.name, style = MaterialTheme.typography.titleSmall)
                            if (client.phone.isNotBlank()) {
                                Text(
                                    client.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        if (slots.isNotEmpty()) {
            item { SectionTitle("Записи") }
            items(slots, key = { "s${it.id}" }) { slot ->
                val isAppointment = slot.kind == SlotKind.APPOINTMENT
                Card(
                    onClick = { if (isAppointment) onOpenSlot(slot.id) },
                    enabled = isAppointment,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            if (isAppointment) {
                                ScheduleRepository.clientById(slot.clientId)?.name ?: "Запись"
                            } else {
                                "Занятость"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "${shortDate(slot.date)} · ${slotSubtitle(slot)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            slot.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
