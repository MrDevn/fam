package com.fam.aware.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Достаёт [Activity] из контекста Compose, чтобы корректно работать с runtime-разрешениями. */
fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
