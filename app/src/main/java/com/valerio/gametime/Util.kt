package com.valerio.gametime

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    val zone: ZoneId get() = ZoneId.systemDefault()

    /** Últimos n dias (yyyy-MM-dd), do mais antigo ao mais recente (hoje por último). */
    fun lastDays(n: Int): List<String> {
        val t = LocalDate.now()
        return (n - 1 downTo 0).map { t.minusDays(it.toLong()).toString() }
    }

    fun startOfDayMillis(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()
}

fun formatHours(ms: Long): String {
    val min = ms / 60000L
    if (min < 60L) return "$min min"
    return String.format(Locale("pt", "BR"), "%.1f h", ms / 3600000.0)
}

fun formatLastPlayed(ts: Long): String {
    if (ts <= 0L) return "Nunca"
    val d = Instant.ofEpochMilli(ts).atZone(Dates.zone).toLocalDate()
    val t = LocalDate.now()
    return when (d) {
        t -> "Hoje"
        t.minusDays(1) -> "Ontem"
        else -> d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
}

fun formatDateTime(ts: Long): String {
    if (ts <= 0L) return "nunca"
    return DateTimeFormatter.ofPattern("dd/MM HH:mm").format(Instant.ofEpochMilli(ts).atZone(Dates.zone))
}

fun launchGame(ctx: Context, pkg: String) {
    val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
    if (i == null) {
        Toast.makeText(ctx, "Não foi possível abrir (app instalado?)", Toast.LENGTH_SHORT).show()
        return
    }
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(i)
}

fun openUsageAccess(ctx: Context) {
    try {
        val i = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    } catch (e: Exception) {
        Toast.makeText(ctx, "Abra Configurações > Apps > Acesso especial > Acesso ao uso", Toast.LENGTH_LONG).show()
    }
}

fun openBatterySettings(ctx: Context) {
    try {
        val i = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    } catch (e: Exception) {
        Toast.makeText(ctx, "Não foi possível abrir as configurações de bateria", Toast.LENGTH_LONG).show()
    }
}
