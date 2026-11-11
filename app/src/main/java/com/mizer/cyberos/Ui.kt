package com.mizer.cyberos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.format.TextStyle as JTextStyle

val Cut = GenericShape { s, _ ->
    val c = 22f
    moveTo(c, 0f)
    lineTo(s.width, 0f)
    lineTo(s.width, s.height - c)
    lineTo(s.width - c, s.height)
    lineTo(0f, s.height)
    lineTo(0f, c)
    close()
}

fun glow(c: Color) = Shadow(c, Offset.Zero, 18f)

@Composable
fun Panel(accent: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .background(Color(0xB3060612), Cut)
            .border(1.dp, accent.copy(alpha = 0.75f), Cut)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun Home(store: Store, cfg: Cfg) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    var wp by remember { mutableStateOf<ImageBitmap?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }

    LaunchedEffect(reload) { apps = withContext(Dispatchers.Default) { loadApps(ctx) } }
    LaunchedEffect(cfg.wpVersion) { wp = withContext(Dispatchers.IO) { loadWallpaper(ctx) } }
    LaunchedEffect(Signals.home.intValue) {
        settingsOpen = false
        query = ""
        menuApp = null
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                reload++
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(ctx, receiver, f, ContextCompat.RECEIVER_EXPORTED)
        onDispose { try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    BackHandler {
        when {
            menuApp != null -> menuApp = null
            settingsOpen -> settingsOpen = false
            else -> query = ""
        }
    }

    val visible = remember(apps, cfg.hidden, query) {
        apps.filter { it.pkg !in cfg.hidden && (query.isBlank() || it.label.contains(query.trim(), true)) }
    }
    val land = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val cols = if (land) cfg.cols else (cfg.cols - 2).coerceAtLeast(3)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Wallpaper(wp, cfg)
        if (!cfg.lowPower) HudGrid(cfg.acc)

        Box(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
            if (land) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoPanel(
                        cfg, query, { query = it },
                        { visible.firstOrNull()?.let { launchApp(ctx, it) } },
                        { settingsOpen = true },
                        Modifier.weight(0.4f).fillMaxHeight()
                    )
                    AppsPanel(
                        Modifier.weight(0.6f).fillMaxHeight(), visible, cfg, cols,
                        { launchApp(ctx, it) }, { menuApp = it }
                    )
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoPanel(
                        cfg, query, { query = it },
                        { visible.firstOrNull()?.let { launchApp(ctx, it) } },
                        { settingsOpen = true },
                        Modifier.fillMaxWidth()
                    )
                    AppsPanel(
                        Modifier.weight(1f).fillMaxWidth(), visible, cfg, cols,
                        { launchApp(ctx, it) }, { menuApp = it }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = settingsOpen,
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut()
        ) {
            SettingsScreen(
                cfg, store, apps,
                onClose = { settingsOpen = false },
                modifier = Modifier.pointerInput(Unit) { detectTapGestures { } }
            )
        }

        if (cfg.borderOn) NeonBorder(cfg)
    }

    menuApp?.let { app ->
        AlertDialog(
            onDismissRequest = { menuApp = null },
            confirmButton = {},
            title = { Text(app.label) },
            text = {
                Column {
                    TextButton(onClick = {
                        scope.launch { store.edit { it[K.hidden] = cfg.hidden + app.pkg } }
                        menuApp = null
                    }) { Text("Скрыть с рабочего стола") }
                    TextButton(onClick = {
                        try {
                            ctx.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${app.pkg}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                        }
                        menuApp = null
                    }) { Text("О приложении") }
                }
            }
        )
    }
}

@Composable
fun Wallpaper(bmp: ImageBitmap?, cfg: Cfg) {
    if (bmp != null) {
        val cs = when (cfg.wpMode) {
            0 -> ContentScale.Crop
            1 -> ContentScale.Fit
            else -> ContentScale.FillBounds
        }
        Image(
            bitmap = bmp,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = cfg.wpScale
                scaleY = cfg.wpScale
            },
            contentScale = cs
        )
    } else {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF06010F), Color.Black, Color(0xFF020A14)))
            )
        )
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = cfg.wpDim)))
}

@Composable
fun HudGrid(acc: Color) {
    Box(
        Modifier.fillMaxSize().drawWithCache {
            val step = 48.dp.toPx()
            onDrawBehind {
                val line = acc.copy(alpha = 0.05f)
                var x = 0f
                while (x < size.width) {
                    drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        center = center,
                        radius = size.maxDimension * 0.75f
                    )
                )
            }
        }
    )
}

@Composable
fun NeonBorder(cfg: Cfg) {
    val col = cfg.borderCol
    val dur = ((if (cfg.lowPower) 9000 else 5000) / cfg.borderSpeed).toInt().coerceAtLeast(500)
    val prog: State<Float> = if (cfg.anim) {
        rememberInfiniteTransition(label = "border").animateFloat(
            0f, 1f, infiniteRepeatable(tween(dur, easing = LinearEasing)), label = "p"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    Box(
        Modifier.fillMaxSize().drawWithCache {
            val th = cfg.borderThick.dp.toPx()
            val inset = th / 2f
            val path = Path().apply {
                addRoundRect(
                    RoundRect(inset, inset, size.width - inset, size.height - inset, CornerRadius(th * 2f))
                )
            }
            val len = PathMeasure().also { it.setPath(path, false) }.length
            val seg = len * 0.22f
            onDrawBehind {
                if (!cfg.anim) {
                    if (!cfg.lowPower) drawPath(path, col.copy(alpha = 0.2f), style = Stroke(width = th * 3f))
                    drawPath(path, col, style = Stroke(width = th))
                } else {
                    drawPath(path, col.copy(alpha = 0.25f), style = Stroke(width = th * 0.5f))
                    val eff = PathEffect.dashPathEffect(floatArrayOf(seg, len - seg), prog.value * len)
                    if (!cfg.lowPower) {
                        drawPath(
                            path, col.copy(alpha = 0.22f),
                            style = Stroke(width = th * 3f, cap = StrokeCap.Round, pathEffect = eff)
                        )
                    }
                    drawPath(
                        path, col,
                        style = Stroke(width = th, cap = StrokeCap.Round, pathEffect = eff)
                    )
                }
            }
        }
    )
}

@Composable
fun InfoPanel(
    cfg: Cfg,
    query: String,
    onQuery: (String) -> Unit,
    onSearchGo: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier
) {
    val acc = cfg.acc
    Panel(acc, modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "CYBER//OS",
                color = acc,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 3.sp,
                style = TextStyle(shadow = glow(acc)),
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .clip(CircleShape)
                    .border(1.dp, acc, CircleShape)
                    .clickable(onClick = onSettings)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("НАСТРОЙКИ", color = acc, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (cfg.showClock) ClockBlock(cfg)
        if (cfg.showBattery) {
            Spacer(Modifier.height(14.dp))
            BatteryBar(cfg)
        }
        if (cfg.showSearch) {
            Spacer(Modifier.height(14.dp))
            SearchBox(cfg, query, onQuery, onSearchGo)
        }
    }
}

@Composable
fun ClockBlock(cfg: Cfg) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(cfg.seconds) {
        while (true) {
            now = LocalDateTime.now()
            val ms = System.currentTimeMillis()
            delay(if (cfg.seconds) 1000L - ms % 1000L else 60000L - ms % 60000L + 20L)
        }
    }
    val fmt = remember(cfg.clock24, cfg.seconds) {
        val p = (if (cfg.clock24) "HH:mm" else "hh:mm") +
            (if (cfg.seconds) ":ss" else "") +
            (if (cfg.clock24) "" else " a")
        DateTimeFormatter.ofPattern(p)
    }
    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }
    val acc = cfg.acc
    Column {
        Text(
            now.format(fmt),
            color = Color.White,
            fontSize = cfg.clockSize.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            style = TextStyle(shadow = Shadow(acc, Offset.Zero, 30f))
        )
        Text(
            now.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault()).uppercase(),
            color = acc,
            fontSize = 20.sp,
            letterSpacing = 4.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            now.toLocalDate().format(dateFmt),
            color = Color(0xCCFFFFFF),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun BatteryBar(cfg: Cfg) {
    val ctx = LocalContext.current
    var level by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (i != null) {
                    val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    level = if (l >= 0 && s > 0) l * 100 / s else -1
                    val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    charging = st == BatteryManager.BATTERY_STATUS_CHARGING ||
                        st == BatteryManager.BATTERY_STATUS_FULL
                }
            } catch (_: Exception) {
            }
            delay(20000)
        }
    }
    if (level >= 0) {
        val c = if (level <= 15 && !charging) NEON[5] else cfg.acc
        Column {
            Row(Modifier.fillMaxWidth()) {
                Text("БАТАРЕЯ", color = Color(0xAAFFFFFF), fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                Text("$level%" + if (charging) "  ⚡" else "", color = c, fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0x22FFFFFF))) {
                Box(Modifier.fillMaxWidth(level / 100f).fillMaxHeight().background(c))
            }
        }
    }
}

@Composable
fun SearchBox(cfg: Cfg, query: String, onQuery: (String) -> Unit, onGo: () -> Unit) {
    val kb = LocalSoftwareKeyboardController.current
    val acc = cfg.acc
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = FontFamily.Monospace),
        cursorBrush = SolidColor(acc),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            onGo()
            kb?.hide()
        }),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, acc.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("> ", color = acc, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("ПОИСК ПРИЛОЖЕНИЙ", color = Color(0x66FFFFFF), fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                    inner()
                }
            }
        }
    )
}

@Composable
fun AppsPanel(
    modifier: Modifier,
    apps: List<AppItem>,
    cfg: Cfg,
    cols: Int,
    onOpen: (AppItem) -> Unit,
    onMenu: (AppItem) -> Unit
) {
    Panel(cfg.acc, modifier) {
        Text(
            "ПРИЛОЖЕНИЯ · ${apps.size}",
            color = cfg.acc, fontSize = 12.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(apps, key = { it.pkg + "/" + it.cls }) { app ->
                AppCell(app, cfg, { onOpen(app) }, { onMenu(app) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCell(app: AppItem, cfg: Cfg, onClick: () -> Unit, onLong: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(cfg.iconSize.dp))
        if (cfg.labels) {
            Text(
                app.label,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
