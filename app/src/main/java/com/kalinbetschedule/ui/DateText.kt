package com.kalinbetschedule.ui
import androidx.compose.runtime.compositionLocalOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
val MONTHS_NOMINATIVE = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)
val MONTHS_GENITIVE = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
)
val WEEKDAYS_SHORT = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
val WEEKDAYS_FULL = listOf(
    "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"
)
fun monthTitle(month: YearMonth): String =
    "${MONTHS_NOMINATIVE[month.monthValue - 1]} ${month.year}"
fun dayTitle(date: LocalDate): String =
    "${date.dayOfMonth} ${MONTHS_GENITIVE[date.monthValue - 1]}"
fun dayTitleWithWeekday(date: LocalDate): String =
    "${dayTitle(date)}, ${WEEKDAYS_FULL[date.dayOfWeek.value - 1]}"
fun shortDate(date: LocalDate): String =
    "%02d.%02d.%d".format(date.dayOfMonth, date.monthValue, date.year)
fun weekdayShort(date: LocalDate): String = WEEKDAYS_SHORT[date.dayOfWeek.value - 1]
val LocalNow = compositionLocalOf { LocalDateTime.now() }
