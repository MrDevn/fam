package com.fam.aware.util

/** Короткое человекочитаемое описание ошибки для экрана Error. */
fun Throwable.describe(): String = message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName
