package com.valerio.gametime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerio.gametime.AppData
import com.valerio.gametime.Game
import com.valerio.gametime.formatHours
import com.valerio.gametime.formatLastPlayed
import com.valerio.gametime.periodMs
import com.valerio.gametime.totalMs

@Composable
fun LibraryScreen(
    data: AppData,
    hasPermission: Boolean,
    onOpen: (String) -> Unit,
    onGrant: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sortIdx by rememberSaveable { mutableIntStateOf(0) }
    var onlyFav by rememberSaveable { mutableStateOf(false) }
    val sorts = listOf("Mais jogados", "Recentes", "2 semanas", "A–Z")

    val filtered = remember(data, query, sortIdx, onlyFav) {
        val q = query.trim().lowercase()
        val base = data.games.filter {
            (q.isEmpty() || it.name.lowercase().contains(q)) && (!onlyFav || it.favorite)
        }
        when (sortIdx) {
            0 -> base.sortedByDescending { data.totalMs(it) }
            1 -> base.sortedByDescending { it.lastPlayedAt }
            2 -> base.sortedByDescending { data.periodMs(it, 14) }
            else -> base.sortedBy { it.name.lowercase() }
        }
    }
    val recent = remember(data) {
        data.games.filter { it.lastPlayedAt > 0L }.sortedByDescending { it.lastPlayedAt }.take(10)
    }

    LazyColumn(Modifier.fillMaxSize()) {
        if (!hasPermission) item { PermissionCard(onGrant) }

        item {
            Column(Modifier.padding(top = 10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    placeholder = { Text("Buscar na biblioteca", color = Steam.Muted) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Steam.Muted) },
                    colors = steamFieldColors(),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = onlyFav,
                            onClick = { onlyFav = !onlyFav },
                            label = { Text("★ Favoritos") },
                            colors = chipColors()
                        )
                    }
                    items(sorts.size) { i ->
                        FilterChip(
                            selected = sortIdx == i,
                            onClick = { sortIdx = i },
                            label = { Text(sorts[i]) },
                            colors = chipColors()
                        )
                    }
                }
            }
        }

        if (query.isEmpty() && !onlyFav && recent.isNotEmpty()) {
            item {
                Column {
                    SectionTitle("JOGADOS RECENTEMENTE")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recent, key = { it.id }) { g -> RecentCard(g, data) { onOpen(g.id) } }
                    }
                }
            }
        }

        item { SectionTitle("BIBLIOTECA (${filtered.size})") }

        if (filtered.isEmpty()) {
            item {
                Text(
                    if (data.games.isEmpty())
                        "Sua biblioteca está vazia.\nToque em + para adicionar jogos ou aguarde a detecção automática."
                    else "Nenhum jogo encontrado.",
                    color = Steam.Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        items(filtered, key = { it.id }) { g -> GameRow(g, data) { onOpen(g.id) } }

        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Steam.Panel,
    labelColor = Steam.Text,
    selectedContainerColor = Steam.BlueBtn,
    selectedLabelColor = Color.White
)

@Composable
private fun RecentCard(game: Game, data: AppData, onClick: () -> Unit) {
    Column(
        Modifier.width(108.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameIcon(game.name, game.packageName, 104.dp)
        Spacer(Modifier.height(6.dp))
        Text(game.name, color = Steam.Text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatHours(data.periodMs(game, 14)) + " / 2 sem.", color = Steam.Muted, fontSize = 11.sp)
    }
}

@Composable
fun GameRow(game: Game, data: AppData, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameIcon(game.name, game.packageName, 58.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        game.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (game.favorite) Text(" ★", color = Steam.Gold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(2.dp))
                Text("${formatHours(data.totalMs(game))} de tempo de jogo", color = Steam.Text, fontSize = 13.sp)
                Text(
                    "Últimas 2 semanas: ${formatHours(data.periodMs(game, 14))}  •  ${formatLastPlayed(game.lastPlayedAt)}",
                    color = Steam.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
    }
}
