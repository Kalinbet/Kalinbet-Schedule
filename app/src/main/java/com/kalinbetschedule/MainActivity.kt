package com.kalinbetschedule
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.backup.BackupManager
import com.kalinbetschedule.ui.AppNavigator
import com.kalinbetschedule.ui.LocalNow
import com.kalinbetschedule.ui.Screen
import com.kalinbetschedule.ui.manage.ClientScreen
import com.kalinbetschedule.ui.manage.ClientsScreen
import com.kalinbetschedule.ui.manage.ManageScreen
import com.kalinbetschedule.ui.manage.ServicesScreen
import com.kalinbetschedule.ui.manage.StatisticsScreen
import com.kalinbetschedule.ui.records.AppointmentScreen
import com.kalinbetschedule.ui.records.CalendarScreen
import com.kalinbetschedule.ui.records.DayScreen
import com.kalinbetschedule.ui.settings.SettingsScreen
import com.kalinbetschedule.ui.theme.KalinbetScheduleTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScheduleRepository.init(this)
        BackupManager.init(this)
        enableEdgeToEdge()
        setContent { KalinbetScheduleApp() }
    }
}
private val TAB_TITLES = listOf("Записи", "Управление", "Настройки")
@Composable
fun KalinbetScheduleApp() {
    KalinbetScheduleTheme(themeMode = ScheduleRepository.themeMode) {
        val navigator = remember { AppNavigator() }
        var now by remember { mutableStateOf(LocalDateTime.now()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(30_000)
                now = LocalDateTime.now()
            }
        }
        val context = LocalContext.current
        val revision = ScheduleRepository.revision
        LaunchedEffect(revision) {
            if (revision > 0 && BackupManager.signedIn) {
                delay(BackupManager.UPLOAD_DELAY_MS)
                if (BackupManager.pending) BackupManager.backupNow(context)
            }
        }
        CompositionLocalProvider(LocalNow provides now) {
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar {
                        TAB_TITLES.forEachIndexed { index, title ->
                            val selected = navigator.tab == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigator.selectTab(index) },
                                icon = {
                                    Icon(
                                        if (selected) selectedIcons[index] else unselectedIcons[index],
                                        contentDescription = title
                                    )
                                },
                                label = { Text(title) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(Modifier.fillMaxSize().padding(innerPadding)) {
                    AppContent(navigator)
                }
            }
        }
    }
}
private val selectedIcons =
    listOf(Icons.Filled.DateRange, Icons.Filled.Menu, Icons.Filled.Settings)
private val unselectedIcons =
    listOf(Icons.Outlined.DateRange, Icons.Outlined.Menu, Icons.Outlined.Settings)
@Composable
private fun AppContent(navigator: AppNavigator) {
    when (val screen = navigator.current) {
        Screen.Calendar -> CalendarScreen(
            onOpenDay = { navigator.push(Screen.Day(it)) }
        )
        is Screen.Day -> DayScreen(
            state = screen.state,
            onBack = { navigator.back() },
            onOpenSlot = { navigator.push(Screen.SlotInfo(it)) }
        )
        is Screen.SlotInfo -> AppointmentScreen(
            slotId = screen.slotId,
            onBack = { navigator.back() },
            onOpenClient = { navigator.push(Screen.ClientInfo(it)) }
        )
        Screen.Manage -> ManageScreen(
            onOpenClients = { navigator.push(Screen.Clients) },
            onOpenServices = { navigator.push(Screen.Services) },
            onOpenStatistics = { navigator.push(Screen.Statistics) },
            onOpenClient = { navigator.push(Screen.ClientInfo(it)) },
            onOpenSlot = { navigator.push(Screen.SlotInfo(it)) }
        )
        Screen.Clients -> ClientsScreen(
            onBack = { navigator.back() },
            onOpenClient = { navigator.push(Screen.ClientInfo(it)) }
        )
        is Screen.ClientInfo -> ClientScreen(
            clientId = screen.clientId,
            onBack = { navigator.back() },
            onOpenSlot = { navigator.push(Screen.SlotInfo(it)) }
        )
        Screen.Services -> ServicesScreen(onBack = { navigator.back() })
        Screen.Statistics -> StatisticsScreen(onBack = { navigator.back() })
        Screen.Settings -> SettingsScreen()
    }
}
