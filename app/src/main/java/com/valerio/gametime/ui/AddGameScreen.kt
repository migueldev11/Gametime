package com.valerio.gametime.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerio.gametime.AppData
import com.valerio.gametime.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class InstalledApp(val pkg: String, val label: String)

@Suppress("DEPRECATION")
private suspend fun loadInstalledApps(ctx: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(intent, 0)
        .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.pkg }
        .filter { it.pkg != ctx.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun AddGameScreen(data: AppData, vm: MainViewModel, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Steam.Bg)) {
        Row(Modifier.fillMaxWidth().background(Steam.BgDark).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Text("Adicionar jogo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        TabRow(selectedTabIndex = tab, containerColor = Steam.BgDark, contentColor = Steam.Blue) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("APPS INSTALADOS", fontSize = 12.sp) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("MANUAL", fontSize = 12.sp) })
        }
        if (tab == 0) InstalledList(data, vm) else ManualForm(vm, onBack)
    }
}

@Composable
private fun InstalledList(data: AppData, vm: MainViewModel) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val apps by produceState<List<InstalledApp>?>(initialValue = null) { value = loadInstalledApps(ctx) }
    val inLibrary = remember(data) { data.games.map { it.id }.toSet() }
    val list = apps

    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Steam.Blue)
        }
        return
    }
    val filtered = list.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "Escolha qualquer app instalado (inclusive jogos de APK que o Android não reconhece como jogo).",
                    color = Steam.Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar app", color = Steam.Muted) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Steam.Muted) },
                    colors = steamFieldColors()
                )
            }
        }
        items(filtered, key = { it.pkg }) { app ->
            val added = app.pkg in inLibrary
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !added) { vm.addInstalled(app.pkg, app.label) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameIcon(app.label, app.pkg, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.label, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(app.pkg, color = Steam.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (added) Text("NA BIBLIOTECA", color = Steam.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                else Text("+ ADICIONAR", color = Steam.Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ManualForm(vm: MainViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "Para jogos que não aparecem na lista (emulador, jogo de PC/console, cloud gaming...). " +
                "Você informa as horas já jogadas e depois registra sessões pelo app.",
            color = Steam.Text,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nome do jogo") },
            singleLine = true,
            colors = steamFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = hours,
            onValueChange = { hours = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Horas já jogadas (opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = steamFieldColors()
        )
        Spacer(Modifier.height(16.dp))
        BlueButton("ADICIONAR JOGO", Modifier.fillMaxWidth()) {
            val n = name.trim()
            if (n.isNotEmpty()) {
                val h = hours.replace(',', '.').toDoubleOrNull() ?: 0.0
                vm.addCustom(n, if (h < 0.0) 0.0 else h)
                onBack()
            }
        }
    }
}
