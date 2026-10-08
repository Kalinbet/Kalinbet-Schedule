package com.kalinbetschedule.data
import java.time.LocalDate
data class Client(
    val id: Long,
    val name: String,
    val phone: String = ""
)
data class Service(
    val id: Long,
    val name: String,
    val durationMinutes: Int,
    val priceRub: Int,
    val nextProcedure: NextProcedure = NextProcedure.SAME,
    val nextServiceId: Long? = null,
    val nextOffsetDays: Int = DEFAULT_NEXT_OFFSET_DAYS
)
data class FollowUp(val service: Service, val date: LocalDate)
enum class SlotKind {
    APPOINTMENT,
    BUSY
}
enum class SlotStatus { PLANNED, COMPLETED }
enum class NextProcedure {
    SAME,
    NONE,
    SPECIFIC
}
data class Slot(
    val id: Long,
    val date: LocalDate,
    val startMinute: Int,
    val endMinute: Int,
    val kind: SlotKind,
    val clientId: Long? = null,
    val serviceId: Long? = null,
    val description: String = "",
    val status: SlotStatus = SlotStatus.PLANNED,
    val extraServiceIds: List<Long> = emptyList(),
    val tipsRub: Int = 0,
    val totalRub: Int = 0,
    val timed: Boolean = true
) {
    val durationMinutes: Int get() = if (timed) endMinute - startMinute else 0
    val isPendingUntimed: Boolean
        get() = !timed && kind == SlotKind.APPOINTMENT && status == SlotStatus.PLANNED
    fun overlaps(otherStart: Int, otherEnd: Int): Boolean =
        timed && startMinute < otherEnd && otherStart < endMinute
}
enum class ThemeMode { SYSTEM, LIGHT, DARK }
const val MINUTES_IN_DAY = 24 * 60
const val SEGMENT_MINUTES = 30
const val DEFAULT_NEXT_OFFSET_DAYS = 21
val NEXT_OFFSET_PRESETS = listOf(7, 14, 21, 28, 42, 56)
fun formatMinutes(minute: Int): String {
    val m = minute.coerceIn(0, MINUTES_IN_DAY)
    return "%02d:%02d".format(m / 60, m % 60)
}
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h ч $m мин"
        h > 0 -> "$h ч"
        else -> "$m мин"
    }
}
fun formatOffset(days: Int): String = when {
    days <= 0 -> "без смещения"
    days % 7 == 0 -> {
        val weeks = days / 7
        "$weeks ${plural(weeks, "неделя", "недели", "недель")}"
    }
    else -> "$days ${plural(days, "день", "дня", "дней")}"
}
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
fun formatMoney(rub: Int): String {
    val sign = if (rub < 0) "-" else ""
    val digits = kotlin.math.abs(rub).toString()
    val grouped = digits.reversed().chunked(3).joinToString(" ").reversed()
    return "$sign$grouped ₽"
}
