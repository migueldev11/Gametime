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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerio.gametime.AppData
import com.valerio.gametime.Dates
import com.valerio.gametime.allDayMs
import com.valerio.gametime.allPeriodMs
import com.valerio.gametime.formatHours
import com.valerio.gametime.periodMs
import com.valerio.gametime.totalMs
import java.time.LocalDate

private fun streak(data: AppData): Int {
    var d = LocalDate.now()
    if (data.allDayMs(d.toString()) <= 0L) d = d.minusDays(1)
    var n = 0
    while (n < 730 && data.allDayMs(d.toString()) > 0L) {
        n++
        d = d.minusDays(1)
    }
    return n
}

@Composable
fun StatsScreen(data: AppData) {
    var period by rememberSaveable { mutableIntStateOf(1) }
    val today = data.allPeriodMs(1)
    val week = data.allPeriodMs(7)
    val month = data.allPeriodMs(30)
    val goalMs = data.weeklyGoalHours * 3600000L
    val progress = if (goalMs > 0) (week.toFloat() / goalMs).coerceIn(0f, 1f) else 0f
    val days = remember(data) { Dates.lastDays(14) }
    val values = remember(data) { days.map { data.allDayMs(it) } }
    val labels = remember(data) { days.map { it.takeLast(2) } }
    val streak = remember(data) { streak(data) }
    val ranking = remember(data, period) {
        data.games
            .map { g -> g to (if (period == 0) data.periodMs(g, 7) else if (period == 1) data.periodMs(g, 30) else data.totalMs(g)) }
            .filter { it.second > 0L }
            .sortedByDescending { it.second }
            .take(5)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        SectionTitle("RESUMO")
        Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(vertical = 16.dp)) {
            StatBox("HOJE", formatHours(today), Modifier.weight(1f))
            StatBox("7 DIAS", formatHours(week), Modifier.weight(1f))
            StatBox("30 DIAS", formatHours(month), Modifier.weight(1f))
        }

        SectionTitle("META SEMANAL")
        Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(14.dp)) {
            Text(
                "${formatHours(week)} de ${data.weeklyGoalHours} h (últimos 7 dias)",
                color = Steam.Text,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Steam.BgDark)) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (progress >= 1f) Steam.Gold else Steam.Blue)
                )
            }
            if (progress >= 1f) {
                Spacer(Modifier.height(6.dp))
                Text("Meta atingida! Que tal uma pausa?", color = Steam.Gold, fontSize = 12.sp)
            }
        }

        SectionTitle("ÚLTIMOS 14 DIAS")
        Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(14.dp)) {
            BarChart(values, labels)
        }

        SectionTitle("CURIOSIDADES")
        Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(vertical = 16.dp)) {
            StatBox("SEQUÊNCIA", "$streak dia(s)", Modifier.weight(1f))
            StatBox("MÉDIA/DIA (30d)", formatHours(month / 30), Modifier.weight(1f))
            StatBox("JOGOS", "${data.games.size}", Modifier.weight(1f))
        }

        SectionTitle("MAIS JOGADOS")
        Row(Modifier.padding(horizontal = 14.dp)) {
            listOf("7 dias", "30 dias", "Total").forEachIndexed { i, s ->
                FilterChip(
                    selected = period == i,
                    onClick = { period = i },
                    label = { Text(s) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Steam.Panel,
                        labelColor = Steam.Text,
                        selectedContainerColor = Steam.BlueBtn,
                        selectedLabelColor = Color.White
                    )
                )
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (ranking.isEmpty()) {
            Text("Sem dados ainda neste período.", color = Steam.Muted, modifier = Modifier.padding(14.dp))
        } else {
            val top = ranking.first().second.toFloat()
            Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().panel().padding(12.dp)) {
                ranking.forEach { (g, ms) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        GameIcon(g.name, g.packageName, 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(
                                    g.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(formatHours(ms), color = Steam.Text, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Steam.BgDark)) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((ms / top).coerceIn(0.02f, 1f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Steam.Blue)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
