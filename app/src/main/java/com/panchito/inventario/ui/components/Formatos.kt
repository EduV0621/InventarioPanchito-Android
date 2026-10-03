package com.panchito.inventario.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Double.comoPrecio(): String = "S/ " + String.format(Locale.US, "%.2f", this)

fun Double.comoTextoEditable(): String = String.format(Locale.US, "%.2f", this)

fun Date.comoFecha(): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(this)

fun Date.aMillisUtcDelDia(): Long {
    val local = Calendar.getInstance().apply { time = this@aMillisUtcDelDia }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

fun fechaLocalDesdeMillisUtc(millisUtc: Long): Date {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millisUtc }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.time
}
