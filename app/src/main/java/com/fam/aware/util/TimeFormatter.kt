package com.fam.aware.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Форматирование метки времени проверки в короткую локальную строку. */
object TimeFormatter {

    fun format(millis: Long): String = runCatching {
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
        format.format(Date(millis))
    }.getOrDefault("--")
}
