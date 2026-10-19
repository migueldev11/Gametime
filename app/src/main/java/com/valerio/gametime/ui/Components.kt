package com.valerio.gametime.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IconEntry(val image: ImageBitmap, val color: Color)

object IconCache {
    private val cache = LruCache<String, IconEntry>(200)

    fun peek(pkg: String): IconEntry? = cache.get(pkg)

    fun load(ctx: Context, pkg: String): IconEntry? {
        val hit = cache.get(pkg)
        if (hit != null) return hit
        return try {
            val d = ctx.packageManager.getApplicationIcon(pkg)
            val bmp = d.toBitmap(192, 192, Bitmap.Config.ARGB_8888)
            val one = Bitmap.createScaledBitmap(bmp, 1, 1, true)
            val entry = IconEntry(bmp.asImageBitmap(), Color(one.getPixel(0, 0)))
            cache.put(pkg, entry)
            entry
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun rememberIcon(pkg: String?): IconEntry? {
    val ctx = LocalContext.current
    val st = produceState<IconEntry?>(initialValue = if (pkg == null) null else IconCache.peek(pkg), pkg) {
        value = if (pkg == null) null else withContext(Dispatchers.IO) { IconCache.load(ctx, pkg) }
    }
    return st.value
}

fun colorFor(name: String): Color {
    val hue = Math.floorMod(name.hashCode(), 360).toFloat()
    return Color.hsv(hue, 0.5f, 0.45f)
}

@Composable
fun GameIcon(name: String, pkg: String?, size: Dp, modifier: Modifier = Modifier) {
    val icon = rememberIcon(pkg)
    val shape = RoundedCornerShape(size * 0.18f)
    if (icon != null) {
        Image(
            bitmap = icon.image,
            contentDescription = name,
            modifier = modifier.size(size).clip(shape)
        )
    } else {
        Box(
            modifier = modifier.size(size).clip(shape).background(colorFor(name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Steam.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = modifier.padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Steam.Muted, fontSize = 10.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

fun Modifier.panel(): Modifier = this.clip(RoundedCornerShape(6.dp)).background(Steam.Panel)

@Composable
fun GreenButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF75B022), Color(0xFF588A1B))))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun BlueButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Steam.BlueBtn)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Steam.Panel2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Steam.Text, fontWeight = FontWeight.Medium, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun steamFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Steam.Blue,
    unfocusedBorderColor = Color(0xFF3D5A73),
    cursorColor = Steam.Blue,
    focusedContainerColor = Steam.BgDark,
    unfocusedContainerColor = Steam.BgDark,
    focusedLabelColor = Steam.Blue,
    unfocusedLabelColor = Steam.Muted
)

@Composable
fun PermissionCard(onGrant: () -> Unit) {
    Column(
        Modifier
            .padding(14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF3B2A1A))
            .padding(14.dp)
    ) {
        Text("Permissão necessária", color = Steam.Gold, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Para medir o tempo de jogo, ative o \"Acesso ao uso\" para o GameTime na tela que vai abrir.",
            color = Steam.Text,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))
        BlueButton("ATIVAR ACESSO", onClick = onGrant)
    }
}

@Composable
fun BarChart(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = Steam.Blue,
    chartHeight: Dp = 120.dp
) {
    val max = maxOf(1L, values.maxOrNull() ?: 0L)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
            val n = values.size
            if (n == 0) return@Canvas
            val gap = 6.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, v ->
                val h = if (v <= 0L) 2.dp.toPx() else maxOf(4.dp.toPx(), (v.toFloat() / max) * size.height)
                drawRoundRect(
                    color = if (v > 0L) barColor else Steam.Muted.copy(alpha = 0.3f),
                    topLeft = Offset(i * (bw + gap), size.height - h),
                    size = Size(bw, h),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach {
                Text(it, color = Steam.Muted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}
