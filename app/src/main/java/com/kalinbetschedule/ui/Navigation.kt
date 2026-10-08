package com.kalinbetschedule.ui
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
sealed interface Screen {
    data object Calendar : Screen
    class Day(anchorDate: LocalDate) : Screen {
        val state = DayState(anchorDate)
    }
    data class SlotInfo(val slotId: Long) : Screen
    data object Manage : Screen
    data object Clients : Screen
    data class ClientInfo(val clientId: Long) : Screen
    data object Services : Screen
    data object Statistics : Screen
    data object Settings : Screen
}
@Stable
class DayState(val anchorDate: LocalDate) {
    var selectedDate by mutableStateOf(anchorDate)
    val timelineScroll = ScrollState(0)
    var timelineAligned = false
}
const val TAB_RECORDS = 0
@Stable
class AppNavigator {
    private val stacks = listOf(
        mutableStateListOf<Screen>(Screen.Calendar),
        mutableStateListOf<Screen>(Screen.Manage),
        mutableStateListOf<Screen>(Screen.Settings)
    )
    var tab by mutableIntStateOf(TAB_RECORDS)
        private set
    val current: Screen get() = stacks[tab].last()
    val canGoBack: Boolean get() = stacks[tab].size > 1
    fun push(screen: Screen) {
        stacks[tab].add(screen)
    }
    fun selectTab(index: Int) {
        if (index == tab) {
            val stack = stacks[index]
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
        } else {
            tab = index
        }
    }
    fun back(): Boolean {
        val stack = stacks[tab]
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}
