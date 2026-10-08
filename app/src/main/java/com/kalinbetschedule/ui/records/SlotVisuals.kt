package com.kalinbetschedule.ui.records
import androidx.compose.ui.graphics.Color
import com.kalinbetschedule.data.ScheduleRepository
import com.kalinbetschedule.data.Slot
import com.kalinbetschedule.data.SlotKind
import com.kalinbetschedule.data.formatMinutes
import com.kalinbetschedule.ui.theme.SlotColors
fun slotBackground(slot: Slot, colors: SlotColors): Color = when {
    slot.isPendingUntimed -> colors.untimed
    slot.kind == SlotKind.APPOINTMENT -> colors.appointment
    else -> colors.busy
}
fun slotContent(slot: Slot, colors: SlotColors): Color = when {
    slot.isPendingUntimed -> colors.onUntimed
    slot.kind == SlotKind.APPOINTMENT -> colors.onAppointment
    else -> colors.onBusy
}
fun slotTitle(slot: Slot): String = if (slot.kind == SlotKind.APPOINTMENT) {
    ScheduleRepository.clientById(slot.clientId)?.name
        ?: ScheduleRepository.serviceById(slot.serviceId)?.name
        ?: "Запись"
} else {
    slot.description.ifBlank { "Занято" }
}
fun slotNote(slot: Slot): String? = slot.description
    .takeIf { it.isNotBlank() && slot.kind == SlotKind.APPOINTMENT }
fun slotLine(slot: Slot): String =
    listOfNotNull(slotTitle(slot), slotNote(slot)).joinToString(" · ")
fun slotSubtitle(slot: Slot): String {
    val time = if (slot.timed) {
        "${formatMinutes(slot.startMinute)} – ${formatMinutes(slot.endMinute)}"
    } else {
        "Время не назначено"
    }
    val service = if (slot.kind == SlotKind.APPOINTMENT) {
        ScheduleRepository.serviceById(slot.serviceId)?.name
    } else {
        null
    }
    return listOfNotNull(service, time).joinToString(" · ")
}
