package com.valerio.gametime.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerio.gametime.AppData
import com.valerio.gametime.MainViewModel
import com.valerio.gametime.formatDateTime
import com.valerio.gametime.openBatterySettings
import com.valerio.gametime.openUsageAccess

@Composable
fun SettingsScreen(data: AppData, hasPermission: Boolean, vm: MainViewModel) {
    val ctx = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importFrom(uri) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        SectionTitle("PERMISSÃO")
        Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(14.dp)) {
            Text(
                if (hasPermission) "Acesso ao uso: ATIVO" else "Acesso ao uso: DESATIVADO",
                color = if (hasPermission) Steam.Green else Steam.Gold,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Sem essa permissão o Android não deixa o app ler quanto tempo cada jogo ficou aberto.",
                color = Steam.Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            BlueButton("ABRIR CONFIGURAÇÃO") { openUsageAccess(ctx) }
        }

        SectionTitle("SINCRONIZAÇÃO")
        Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(14.dp)) {
            Text("Última sincronização: ${formatDateTime(data.lastSync)}", color = Steam.Text, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "O app sincroniza ao abrir e a cada ~15 min em segundo plano. O Android só guarda cerca de 7 a 10 dias de detalhes, " +
                    "então o GameTime salva tudo localmente a partir daqui.",
                color = Steam.Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Row {
                GhostButton("Sincronizar agora", Modifier.weight(1f)) { vm.sync() }
                Spacer(Modifier.width(10.dp))
                GhostButton("Bateria: não otimizar", Modifier.weight(1f)) { openBatterySettings(ctx) }
            }
        }

        SectionTitle("BIBLIOTECA")
        Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Detectar jogos automaticamente", color = Color.White, fontSize = 14.sp)
                    Text("Adiciona apps que o Android classifica como jogo.", color = Steam.Muted, fontSize = 12.sp)
                }
                Switch(
                    checked = data.autoDetect,
                    onCheckedChange = { vm.setAutoDetect(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Steam.BlueBtn, checkedThumbColor = Color.White)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Meta semanal", color = Color.White, fontSize = 14.sp)
                    Text("Horas por semana (mostrado em Resumo)", color = Steam.Muted, fontSize = 12.sp)
                }
                TextButton(onClick = { vm.setWeeklyGoal(data.weeklyGoalHours - 1) }) { Text("−", color = Steam.Blue, fontSize = 20.sp) }
                Text("${data.weeklyGoalHours} h", color = Color.White, fontWeight = FontWeight.Bold)
                TextButton(onClick = { vm.setWeeklyGoal(data.weeklyGoalHours + 1) }) { Text("+", color = Steam.Blue, fontSize = 20.sp) }
            }
            Spacer(Modifier.height(8.dp))
            GhostButton("Restaurar jogos removidos (ignorados: ${data.ignored.size})", Modifier.fillMaxWidth()) {
                vm.restoreIgnored()
            }
        }

        SectionTitle("BACKUP")
        Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(14.dp)) {
            Text(
                "Exporte seus dados (jogos, horas, notas) para um arquivo e restaure quando trocar de celular.",
                color = Steam.Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Row {
                GhostButton("Exportar", Modifier.weight(1f)) { exportLauncher.launch("gametime-backup.json") }
                Spacer(Modifier.width(10.dp))
                GhostButton("Importar", Modifier.weight(1f)) { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
            }
        }

        SectionTitle("SOBRE")
        Text(
            "GameTime 1.0 — tempo de jogo medido pelo próprio Android, sem internet e sem anúncios. " +
                "O histórico anterior à instalação é uma estimativa; você pode corrigir em cada jogo (Definir tempo total).",
            color = Steam.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
