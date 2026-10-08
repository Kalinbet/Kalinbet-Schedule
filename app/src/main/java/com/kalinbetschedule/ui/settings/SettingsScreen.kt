package com.kalinbetschedule.ui.settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kalinbetschedule.data.ThemeMode
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.backup.BackupManager
import com.kalinbetschedule.ui.components.AppTopBar
import com.kalinbetschedule.ui.dayTitle
import java.time.Instant
import java.time.ZoneId
private val THEME_OPTIONS = listOf(
    ThemeMode.SYSTEM to "Системная",
    ThemeMode.LIGHT to "Светлая",
    ThemeMode.DARK to "Тёмная"
)
@Composable
fun SettingsScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        AppTopBar(title = "Настройки")
        ThemeCard()
        BackupCard()
    }
}
@Composable
private fun ThemeCard() {
    val current = ScheduleRepository.themeMode
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(vertical = 8.dp).selectableGroup()) {
            Text(
                "Тема оформления",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            THEME_OPTIONS.forEach { (mode, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = current == mode,
                            role = Role.RadioButton,
                            onClick = { ScheduleRepository.changeTheme(mode) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = current == mode, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
@Composable
private fun BackupCard() {
    val context = LocalContext.current
    var restoring by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Резервная копия", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (BackupManager.working) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    backupStatus(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (BackupManager.lastError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            when {
                !BackupManager.configured -> Text(
                    "Укажите идентификатор приложения с oauth.yandex.ru " +
                        "в YandexAuth.CLIENT_ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                !BackupManager.signedIn -> {
                    Text(
                        "Расписание и клиенты будут сами уезжать в папку " +
                            "«Приложения» на вашем Яндекс.Диске.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        enabled = !BackupManager.working,
                        onClick = { BackupManager.signIn(context) }
                    ) { Text("Войти в Яндекс ID") }
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (BackupManager.reconciled) {
                        TextButton(
                            enabled = !BackupManager.working,
                            onClick = { BackupManager.backupNow(context) }
                        ) { Text("Скопировать") }
                    } else {
                        TextButton(
                            enabled = !BackupManager.working,
                            onClick = { BackupManager.reconcile(context) }
                        ) { Text("Проверить") }
                    }
                    TextButton(
                        enabled = !BackupManager.working,
                        onClick = { restoring = true }
                    ) { Text("Восстановить") }
                    TextButton(
                        enabled = !BackupManager.working,
                        onClick = { signingOut = true }
                    ) { Text("Выйти") }
                }
            }
        }
    }
    BackupManager.conflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Данные на устройстве отличаются от копии на Диске.") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Выберите актуальный источник:")
                    Text("Это устройство — ${describe(conflict.local)}")
                    Text("Диск — ${describe(conflict.remote)}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    BackupManager.resolveConflict(context, fromDisk = true)
                }) { Text("Восстановить с Диска") }
            },
            dismissButton = {
                TextButton(onClick = {
                    BackupManager.resolveConflict(context, fromDisk = false)
                }) { Text("Скопировать с устройства") }
            }
        )
    }
    if (restoring) {
        AlertDialog(
            onDismissRequest = { restoring = false },
            title = { Text("Восстановить с Диска?") },
            text = {
                Text(
                    "Записи, клиенты и услуги на телефоне будут заменены копией " +
                        "с Диска."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    BackupManager.restoreFromDisk(context)
                    restoring = false
                }) { Text("Восстановить") }
            },
            dismissButton = {
                TextButton(onClick = { restoring = false }) { Text("Отмена") }
            }
        )
    }
    if (signingOut) {
        AlertDialog(
            onDismissRequest = { signingOut = false },
            title = { Text("Выйти из Яндекс ID?") },
            text = { Text("Копии перестанут обновляться. Данные на телефоне останутся.") },
            confirmButton = {
                TextButton(onClick = {
                    BackupManager.signOut(context)
                    signingOut = false
                }) { Text("Выйти") }
            },
            dismissButton = {
                TextButton(onClick = { signingOut = false }) { Text("Отмена") }
            }
        )
    }
}
private fun describe(summary: BackupManager.Summary): String = listOf(
    "${summary.clients} ${plural(summary.clients, "клиент", "клиента", "клиентов")}",
    "${summary.services} ${plural(summary.services, "услуга", "услуги", "услуг")}",
    "${summary.slots} ${plural(summary.slots, "запись", "записи", "записей")}"
).joinToString(", ")
private fun plural(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}
@Composable
private fun backupStatus(): String {
    BackupManager.lastError?.let { return it }
    return when {
        !BackupManager.configured -> "Не настроено"
        BackupManager.working -> "Проверка Диска…"
        !BackupManager.signedIn -> "Копии не создаются"
        BackupManager.conflict != null -> "Нужно выбрать, что оставить"
        !BackupManager.reconciled -> "Ещё не сверились с Диском"
        BackupManager.lastBackupAt == 0L -> "Копии ещё не было"
        else -> {
            val moment = Instant.ofEpochSecond(BackupManager.lastBackupAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            val time = "%02d:%02d".format(moment.hour, moment.minute)
            val suffix = if (BackupManager.pending) ", есть новые правки" else ""
            "Копия от ${dayTitle(moment.toLocalDate())}, $time$suffix"
        }
    }
}
