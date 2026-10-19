package com.valerio.gametime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerio.gametime.AppData
import com.valerio.gametime.Dates
import com.valerio.gametime.Game
import com.valerio.gametime.MainViewModel
import com.valerio.gametime.dayMs
import com.valerio.gametime.formatHours
import com.valerio.gametime.formatLastPlayed
import com.valerio.gametime.launchGame
import com.valerio.gametime.periodMs
import com.valerio.gametime.totalMs
import java.util.Locale

@Composable
fun DetailScreen(game: Game, data: AppData, vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val icon = rememberIcon(game.packageName)
    val base = icon?.color ?: colorFor(game.name)
    val total = data.totalMs(game)
    val two = data.periodMs(game, 14)
    val hours = total / 3600000.0

    var showTotal by remember { mutableStateOf(false) }
    var showSession by remember { mutableStateOf(false) }
    var showRemove by remember { mutableStateOf(false) }
    var notes by remember(game.id) { mutableStateOf(game.notes) }

    val days = remember(data, game.id) { Dates.lastDays(14) }
    val values = remember(data, game.id) { days.map { data.dayMs(game.id, it) } }
    val labels = remember(data, game.id) { days.map { it.takeLast(2) } }

    Column(Modifier.fillMaxSize().background(Steam.Bg).verticalScroll(rememberScrollState())) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(Brush.verticalGradient(listOf(lerp(base, Color.Black, 0.45f), Steam.Bg)))
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            IconButton(
                onClick = { vm.toggleFavorite(game.id) },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Favorito",
                    tint = if (game.favorite) Steam.Gold else Color.White.copy(alpha = 0.5f)
                )
            }
            GameIcon(game.name, game.packageName, 120.dp, Modifier.align(Alignment.Center))
        }

        Text(
            game.name,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(14.dp))

        val pkg = game.packageName
        if (pkg != null) {
            GreenButton("▶  JOGAR", Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { launchGame(ctx, pkg) }
        } else {
            GreenButton("+  REGISTRAR SESSÃO", Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { showSession = true }
        }

        SectionTitle("TEMPO DE JOGO")
        Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(vertical = 16.dp)) {
            StatBox("TOTAL", formatHours(total), Modifier.weight(1f))
            StatBox("ÚLT. 2 SEMANAS", formatHours(two), Modifier.weight(1f))
            StatBox("ÚLTIMA VEZ", formatLastPlayed(game.lastPlayedAt), Modifier.weight(1f))
        }
        if (game.baselineMs > 0L) {
            Text(
                "Inclui ${formatHours(game.baselineMs)} de histórico anterior estimado pelo Android.",
                color = Steam.Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        SectionTitle("ÚLTIMOS 14 DIAS")
        Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(14.dp)) {
            BarChart(values, labels, chartHeight = 100.dp)
        }

        SectionTitle("MARCOS")
        Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(vertical = 12.dp, horizontal = 4.dp)) {
            listOf(1, 10, 50, 100, 250, 500, 1000).forEach { m ->
                val ok = hours >= m
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (ok) Steam.BlueBtn else Steam.Panel2.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (ok) "★" else "·", color = if (ok) Color.White else Steam.Muted, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${m}h", color = if (ok) Steam.Text else Steam.Muted, fontSize = 11.sp)
                }
            }
        }

        SectionTitle("NOTAS")
        Column(Modifier.padding(horizontal = 14.dp)) {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Anote conta, progresso, build, dicas...", color = Steam.Muted) },
                minLines = 3,
                colors = steamFieldColors()
            )
            if (notes != game.notes) {
                Spacer(Modifier.height(8.dp))
                BlueButton("SALVAR NOTA") { vm.setNotes(game.id, notes) }
            }
        }

        SectionTitle("AÇÕES")
        Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth()) {
            GhostButton("Definir tempo total", Modifier.weight(1f)) { showTotal = true }
            Spacer(Modifier.width(10.dp))
            GhostButton("Registrar sessão", Modifier.weight(1f)) { showSession = true }
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = { showRemove = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text("Remover da biblioteca", color = Steam.Danger)
        }
        Spacer(Modifier.height(30.dp))
    }

    if (showTotal) {
        var text by remember { mutableStateOf(String.format(Locale.US, "%.1f", hours)) }
        AlertDialog(
            onDismissRequest = { showTotal = false },
            containerColor = Steam.Panel,
            title = { Text("Definir tempo total", color = Color.White) },
            text = {
                Column {
                    Text("Informe o total de horas jogadas (ex.: 12,5). O app ajusta a diferença.", color = Steam.Text, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = steamFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = text.replace(',', '.').toDoubleOrNull()
                    if (h != null && h >= 0.0) vm.setTotalHours(game.id, h)
                    showTotal = false
                }) { Text("SALVAR", color = Steam.Blue) }
            },
            dismissButton = { TextButton(onClick = { showTotal = false }) { Text("CANCELAR", color = Steam.Muted) } }
        )
    }

    if (showSession) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSession = false },
            containerColor = Steam.Panel,
            title = { Text("Registrar sessão", color = Color.White) },
            text = {
                Column {
                    Text("Quantos minutos você jogou? Será somado ao dia de hoje.", color = Steam.Text, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.filter { c -> c.isDigit() } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = steamFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val m = text.toIntOrNull()
                    if (m != null && m > 0) vm.addManualSession(game.id, m)
                    showSession = false
                }) { Text("ADICIONAR", color = Steam.Blue) }
            },
            dismissButton = { TextButton(onClick = { showSession = false }) { Text("CANCELAR", color = Steam.Muted) } }
        )
    }

    if (showRemove) {
        AlertDialog(
            onDismissRequest = { showRemove = false },
            containerColor = Steam.Panel,
            title = { Text("Remover ${game.name}?", color = Color.White) },
            text = {
                Text(
                    "O histórico de tempo deste jogo no GameTime será apagado. Jogos detectados automaticamente não voltam sozinhos (dá para restaurar em Ajustes).",
                    color = Steam.Text,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemove = false
                    vm.removeGame(game.id)
                    onBack()
                }) { Text("REMOVER", color = Steam.Danger) }
            },
            dismissButton = { TextButton(onClick = { showRemove = false }) { Text("CANCELAR", color = Steam.Muted) } }
        )
    }
}
