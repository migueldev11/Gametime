package com.valerio.gametime

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** Armazenamento local em JSON (um único arquivo), compartilhado entre UI e Worker. */
object Store {
    private var file: File? = null
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state.asStateFlow()

    @Synchronized
    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.applicationContext.filesDir, "gametime.json")
        file = f
        if (f.exists()) {
            try {
                _state.value = Json.decode(f.readText())
            } catch (e: Exception) {
                try {
                    f.copyTo(File(f.parentFile, "gametime.corrupt.json"), overwrite = true)
                } catch (e2: Exception) {
                    // ignora
                }
            }
        }
    }

    @Synchronized
    fun update(block: (AppData) -> AppData) {
        val n = block(_state.value)
        _state.value = n
        persist(n)
    }

    fun export(): String = Json.encode(_state.value)

    private fun persist(d: AppData) {
        val f = file ?: return
        try {
            val tmp = File(f.parentFile, "gametime.json.tmp")
            tmp.writeText(Json.encode(d))
            if (!tmp.renameTo(f)) f.writeText(Json.encode(d))
        } catch (e: Exception) {
            // falha de disco: mantém estado em memória
        }
    }
}
