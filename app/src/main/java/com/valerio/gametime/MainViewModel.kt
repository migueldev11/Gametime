package com.valerio.gametime

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app: Application = application

    val state: StateFlow<AppData> = Store.state
    val hasPermission = MutableStateFlow(UsageSync.hasPermission(application))
    val syncing = MutableStateFlow(false)

    private fun io(block: () -> Unit): Job = viewModelScope.launch(Dispatchers.IO) { block() }

    private fun toast(msg: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(app, msg, Toast.LENGTH_SHORT).show() }
    }

    fun refreshPermission() {
        hasPermission.value = UsageSync.hasPermission(app)
    }

    fun sync(): Job = io {
        syncing.value = true
        try {
            UsageSync.sync(app)
        } catch (e: Exception) {
            toast("Falha ao sincronizar")
        } finally {
            syncing.value = false
        }
    }

    fun addInstalled(pkg: String, name: String) = io {
        Store.update { d ->
            if (d.games.any { it.id == pkg }) d
            else d.copy(
                games = d.games + Game(id = pkg, name = name, packageName = pkg, addedAt = System.currentTimeMillis()),
                ignored = d.ignored - pkg
            )
        }
        toast("$name adicionado")
        UsageSync.sync(app)
    }

    fun addCustom(name: String, hours: Double) = io {
        val g = Game(
            id = "custom:" + UUID.randomUUID().toString(),
            name = name,
            packageName = null,
            addedAt = System.currentTimeMillis(),
            offsetMs = (hours * 3600000.0).toLong(),
            baselineDone = true
        )
        Store.update { d -> d.copy(games = d.games + g) }
        toast("$name adicionado")
    }

    fun removeGame(id: String) = io {
        Store.update { d ->
            val g = d.games.find { it.id == id } ?: return@update d
            d.copy(
                games = d.games.filter { it.id != id },
                tracked = d.tracked - id,
                manual = d.manual - id,
                ignored = if (g.packageName != null) d.ignored + id else d.ignored
            )
        }
    }

    fun toggleFavorite(id: String) = io {
        Store.update { d -> d.copy(games = d.games.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }) }
    }

    fun setNotes(id: String, notes: String) = io {
        Store.update { d -> d.copy(games = d.games.map { if (it.id == id) it.copy(notes = notes) else it }) }
        toast("Nota salva")
    }

    /** Define o tempo total desejado (em horas); a diferença vira um ajuste manual. */
    fun setTotalHours(id: String, hours: Double) = io {
        val target = (hours * 3600000.0).toLong()
        Store.update { d ->
            val g = d.games.find { it.id == id } ?: return@update d
            val raw = d.rawMs(g)
            d.copy(games = d.games.map { if (it.id == id) it.copy(offsetMs = target - raw) else it })
        }
    }

    fun addManualSession(id: String, minutes: Int) = io {
        val today = java.time.LocalDate.now().toString()
        val ms = minutes * 60000L
        Store.update { d ->
            val cur = (d.manual[id] ?: emptyMap()).toMutableMap()
            cur[today] = (cur[today] ?: 0L) + ms
            val manual = d.manual.toMutableMap()
            manual[id] = cur
            d.copy(
                manual = manual,
                games = d.games.map { if (it.id == id) it.copy(lastPlayedAt = System.currentTimeMillis()) else it }
            )
        }
    }

    fun setWeeklyGoal(hours: Int) = io {
        Store.update { d -> d.copy(weeklyGoalHours = hours.coerceIn(1, 168)) }
    }

    fun setAutoDetect(on: Boolean) = io {
        Store.update { d -> d.copy(autoDetect = on) }
        if (on) UsageSync.sync(app)
    }

    fun restoreIgnored() = io {
        Store.update { d -> d.copy(ignored = emptySet()) }
        toast("Lista de ignorados limpa")
        UsageSync.sync(app)
    }

    fun exportTo(uri: Uri) = io {
        try {
            app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(Store.export().toByteArray(Charsets.UTF_8)) }
            toast("Backup salvo")
        } catch (e: Exception) {
            toast("Falha ao salvar o backup")
        }
    }

    fun importFrom(uri: Uri) = io {
        try {
            val text = app.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw IllegalStateException("vazio")
            val data = Json.decode(text)
            Store.update { data }
            toast("Backup restaurado")
            UsageSync.sync(app)
        } catch (e: Exception) {
            toast("Arquivo de backup inválido")
        }
    }
}
