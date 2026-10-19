package com.valerio.gametime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerio.gametime.ui.AddGameScreen
import com.valerio.gametime.ui.DetailScreen
import com.valerio.gametime.ui.GameTimeTheme
import com.valerio.gametime.ui.LibraryScreen
import com.valerio.gametime.ui.SettingsScreen
import com.valerio.gametime.ui.StatsScreen
import com.valerio.gametime.ui.Steam

sealed class Screen {
    object Main : Screen()
    object Add : Screen()
    data class Detail(val id: String) : Screen()
}

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GameTimeTheme { Root(vm) } }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshPermission()
        vm.sync()
    }
}

@Composable
private fun Root(vm: MainViewModel) {
    val ctx = LocalContext.current
    val data by vm.state.collectAsState()
    val hasPerm by vm.hasPermission.collectAsState()
    val syncing by vm.syncing.collectAsState()
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = screen !is Screen.Main) { screen = Screen.Main }

    when (val s = screen) {
        is Screen.Add -> AddGameScreen(data, vm) { screen = Screen.Main }
        is Screen.Detail -> {
            val game = data.games.find { it.id == s.id }
            if (game == null) {
                LaunchedEffect(s.id) { screen = Screen.Main }
            } else {
                DetailScreen(game, data, vm) { screen = Screen.Main }
            }
        }
        is Screen.Main -> {
            Scaffold(
                containerColor = Steam.Bg,
                contentColor = Steam.Text,
                topBar = {
                    Row(
                        Modifier.fillMaxWidth().background(Steam.BgDark).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "GAMETIME",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 3.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (syncing) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Steam.Blue, strokeWidth = 2.dp)
                            Spacer(Modifier.padding(horizontal = 6.dp))
                        }
                        IconButton(onClick = { vm.refreshPermission(); vm.sync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sincronizar", tint = Steam.Muted)
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = Steam.BgDark) {
                        val colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Steam.Blue,
                            selectedTextColor = Steam.Blue,
                            indicatorColor = Steam.Panel2,
                            unselectedIconColor = Steam.Muted,
                            unselectedTextColor = Steam.Muted
                        )
                        NavigationBarItem(
                            selected = tab == 0, onClick = { tab = 0 }, colors = colors,
                            icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text("Biblioteca") }
                        )
                        NavigationBarItem(
                            selected = tab == 1, onClick = { tab = 1 }, colors = colors,
                            icon = { Icon(Icons.Default.DateRange, contentDescription = null) }, label = { Text("Resumo") }
                        )
                        NavigationBarItem(
                            selected = tab == 2, onClick = { tab = 2 }, colors = colors,
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) }, label = { Text("Ajustes") }
                        )
                    }
                },
                floatingActionButton = {
                    if (tab == 0) {
                        FloatingActionButton(
                            onClick = { screen = Screen.Add },
                            containerColor = Steam.BlueBtn,
                            contentColor = Color.White
                        ) { Icon(Icons.Default.Add, contentDescription = "Adicionar jogo") }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (tab) {
                        0 -> LibraryScreen(data, hasPerm, onOpen = { screen = Screen.Detail(it) }, onGrant = { openUsageAccess(ctx) })
                        1 -> StatsScreen(data)
                        else -> SettingsScreen(data, hasPerm, vm)
                    }
                }
            }
        }
    }
}
