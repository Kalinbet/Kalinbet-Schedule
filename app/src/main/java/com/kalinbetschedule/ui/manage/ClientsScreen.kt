package com.kalinbetschedule.ui.manage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.Client
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.ui.components.AppTopBar
import com.kalinbetschedule.ui.components.EmptyState
import com.kalinbetschedule.ui.components.NameField
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(onBack: () -> Unit, onOpenClient: (Long) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf<Int?>(null) }
    val clients = ScheduleRepository.clients.sortedBy { it.name.lowercase() }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Клиенты",
            onBack = onBack,
            actions = {
                IconButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить клиента")
                }
                ClientsMenu(onImport = { importing = true })
            }
        )
        if (clients.isEmpty()) {
            Column(Modifier.fillMaxWidth()) {
                EmptyState("Клиентов пока нет. Добавьте первого кнопкой «+».")
                TextButton(
                    onClick = { importing = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Импорт из контактов")
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clients, key = { it.id }) { client ->
                    Card(
                        onClick = { onOpenClient(client.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(client.name, style = MaterialTheme.typography.titleSmall)
                                val visits = ScheduleRepository.visitsCount(client.id)
                                Text(
                                    if (client.phone.isBlank()) "Записей: $visits"
                                    else "${client.phone} · записей: $visits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
    if (adding) {
        ClientEditDialog(
            client = null,
            onDismiss = { adding = false },
            onSave = { name, phone ->
                ScheduleRepository.addClient(name, phone)
                adding = false
            }
        )
    }
    if (importing) {
        ImportContactsDialog(
            onDismiss = { importing = false },
            onImported = { count ->
                importing = false
                importedCount = count
            }
        )
    }
    importedCount?.let { count ->
        AlertDialog(
            onDismissRequest = { importedCount = null },
            title = { Text("Импорт из контактов") },
            text = {
                Text(
                    if (count > 0) "Добавлено клиентов: $count"
                    else "Новых клиентов не добавилось."
                )
            },
            confirmButton = {
                TextButton(onClick = { importedCount = null }) { Text("Понятно") }
            }
        )
    }
}
@Composable
private fun ClientsMenu(onImport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Ещё")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Импорт из контактов") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                onClick = {
                    expanded = false
                    onImport()
                }
            )
        }
    }
}
@Composable
fun ClientEditDialog(
    client: Client?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(client?.name.orEmpty()) }
    var phone by remember { mutableStateOf(client?.phone.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (client == null) "Новый клиент" else "Редактирование клиента") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NameField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Имя"
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name, phone) }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
