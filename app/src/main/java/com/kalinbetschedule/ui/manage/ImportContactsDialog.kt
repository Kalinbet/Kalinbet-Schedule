package com.kalinbetschedule.ui.manage
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.PhoneContact
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.hasContactsPermission
import com.kalinbetschedule.data.normalizePhone
import com.kalinbetschedule.data.readPhoneContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable
fun ImportContactsDialog(onDismiss: () -> Unit, onImported: (Int) -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasContactsPermission(context)) }
    var refused by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<PhoneContact>?>(null) }
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { allowed ->
        granted = allowed
        refused = !allowed
    }
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
    LaunchedEffect(granted) {
        if (granted && contacts == null) {
            contacts = withContext(Dispatchers.IO) { readPhoneContacts(context) }
        }
    }
    val loaded = contacts.orEmpty()
    val known = remember(loaded, ScheduleRepository.clients.toList()) {
        ScheduleRepository.knownContactIds(loaded)
    }
    val needle = query.trim().lowercase()
    val visible = if (needle.isEmpty()) loaded else loaded.filter { contact ->
        contact.name.lowercase().contains(needle) ||
            normalizePhone(contact.phone).contains(normalizePhone(needle))
    }
    val selectedCount = selected.count { it.value }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Импорт из контактов") },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    !granted -> PermissionNotice(
                        refused = refused,
                        onRequest = {
                            if (hasContactsPermission(context)) {
                                granted = true
                                refused = false
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                    )
                    contacts == null -> Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    loaded.isEmpty() -> Text(
                        "В телефонной книге нет контактов с номерами.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            label = { Text("Поиск") },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Очистить"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val selectable = visible.filterNot { it.id in known }
                        val allChosen = selectable.isNotEmpty() &&
                            selectable.all { selected[it.id] == true }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Выбрано: $selectedCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                enabled = selectable.isNotEmpty(),
                                onClick = {
                                    selectable.forEach { selected[it.id] = !allChosen }
                                }
                            ) {
                                Text(if (allChosen) "Снять выбор" else "Выбрать всех")
                            }
                        }
                        if (visible.isEmpty()) {
                            Text(
                                "Ничего не найдено",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                                items(visible, key = { it.id }) { contact ->
                                    ContactRow(
                                        contact = contact,
                                        alreadyClient = contact.id in known,
                                        checked = selected[contact.id] == true,
                                        onCheckedChange = { selected[contact.id] = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedCount > 0,
                onClick = {
                    val chosen = loaded.filter { selected[it.id] == true }
                    onImported(ScheduleRepository.importContacts(chosen))
                }
            ) { Text(if (selectedCount > 0) "Добавить ($selectedCount)" else "Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
@Composable
private fun ContactRow(
    contact: PhoneContact,
    alreadyClient: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !alreadyClient) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked && !alreadyClient,
            onCheckedChange = null,
            enabled = !alreadyClient
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                contact.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (alreadyClient) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            val phoneText = contact.phone.ifBlank { "Без номера" }
            Text(
                if (alreadyClient) "$phoneText · уже в базе" else phoneText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun PermissionNotice(
    refused: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (refused) {
                "Нет доступа к контактам. Разрешение можно выдать здесь " +
                    "или в настройках приложения."
            } else {
                "Приложению нужен доступ к контактам, чтобы показать их список."
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRequest) { Text("Запросить доступ") }
            if (refused) {
                TextButton(onClick = onOpenSettings) { Text("Настройки") }
            }
        }
    }
}
