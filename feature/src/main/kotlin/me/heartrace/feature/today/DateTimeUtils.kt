package me.heartrace.feature.today

import java.util.Calendar
import java.util.TimeZone

internal fun localDateAsUtcMillis(timestamp: Long): Long {
    val localDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localDate.get(Calendar.YEAR),
            localDate.get(Calendar.MONTH),
            localDate.get(Calendar.DAY_OF_MONTH),
        )
    }.timeInMillis
}

internal fun Long.withLocalDateFromUtc(utcDateMillis: Long): Long {
    val selectedDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcDateMillis
    }
    return Calendar.getInstance().apply {
        timeInMillis = this@withLocalDateFromUtc
        set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
        set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
