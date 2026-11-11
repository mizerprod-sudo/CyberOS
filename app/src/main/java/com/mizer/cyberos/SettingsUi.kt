package com.mizer.cyberos

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.MutablePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    cfg: Cfg,
    store: Store,
    apps: List<AppItem>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val acc = cfg.acc

    fun save(block: (MutablePreferences) -> Unit) {
        scope.launch { store.edit(block) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { copyWallpaper(ctx, uri) }
                if (ok) {
                    store.edit { it[K.wpVersion] = (it[K.wpVersion] ?: 0) + 1 }
                } else {
                    Toast.makeText(ctx, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xF2030308))
            .systemBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "НАСТРОЙКИ",
                    color = acc, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 4.sp,
                    style = TextStyle(shadow = glow(acc)),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClose) { Text("✕ ЗАКРЫТЬ", color = acc) }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

                Section("ЦВЕТ ИНТЕРФЕЙСА", acc) {
                    ColorRow(cfg.accent) { i -> save { it[K.accent] = i } }
                }

                Section("ВНЕШНИЙ ВИД", acc) {
                    ToggleRow("Показывать часы", cfg.showClock, acc) { v -> save { it[K.showClock] = v } }
                    ToggleRow("Показывать батарею", cfg.showBattery, acc) { v -> save { it[K.showBattery] = v } }
                    ToggleRow("Показывать поиск", cfg.showSearch, acc) { v -> save { it[K.showSearch] = v } }
                }

                Section("АНИМАЦИИ", acc) {
                    ToggleRow("Анимации включены", cfg.anim, acc) { v -> save { it[K.anim] = v } }
                    ToggleRow("Интро после перезагрузки", cfg.intro, acc) { v -> save { it[K.intro] = v } }
                }

                Section("НЕОНОВАЯ РАМКА", acc) {
                    ToggleRow("Рамка включена", cfg.borderOn, acc) { v -> save { it[K.borderOn] = v } }
                    Label("Цвет рамки")
                    ColorRow(cfg.borderColor) { i -> save { it[K.borderColor] = i } }
                    SliderRow("Скорость", cfg.borderSpeed, 0.3f..3f, acc, { "x%.1f".format(it) }) { v ->
                        save { it[K.borderSpeed] = v }
                    }
                    SliderRow("Толщина", cfg.borderThick, 1f..8f, acc, { "%.1f dp".format(it) }) { v ->
                        save { it[K.borderThick] = v }
                    }
                }

                Section("ОБОИ", acc) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton("ВЫБРАТЬ ИЗ ГАЛЕРЕИ", acc, Modifier.weight(1f)) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        ActionButton("СБРОСИТЬ ОБОИ", acc, Modifier.weight(1f)) {
                            scope.launch {
                                withContext(Dispatchers.IO) { wallpaperFile(ctx).delete() }
                                store.edit { it[K.wpVersion] = (it[K.wpVersion] ?: 0) + 1 }
                            }
                        }
                    }
                    Label("Способ заполнения")
                    Chips(listOf("ЗАПОЛНИТЬ", "ВПИСАТЬ", "РАСТЯНУТЬ"), cfg.wpMode, acc) { i ->
                        save { it[K.wpMode] = i }
                    }
                    SliderRow("Масштаб", cfg.wpScale, 1f..2.5f, acc, { "x%.2f".format(it) }) { v ->
                        save { it[K.wpScale] = v }
                    }
                    SliderRow("Затемнение", cfg.wpDim, 0f..0.9f, acc, { "${(it * 100).toInt()}%" }) { v ->
                        save { it[K.wpDim] = v }
                    }
                }

                Section("ИКОНКИ", acc) {
                    SliderRow("Колонок в сетке (горизонтально)", cfg.cols.toFloat(), 3f..8f, acc,
                        { "${it.toInt()}" }, steps = 4) { v -> save { it[K.cols] = v.toInt() } }
                    SliderRow("Размер иконок", cfg.iconSize, 40f..96f, acc, { "${it.toInt()} dp" }) { v ->
                        save { it[K.iconSize] = v }
                    }
                    ToggleRow("Подписи под иконками", cfg.labels, acc) { v -> save { it[K.labels] = v } }
                }

                Section("ЧАСЫ", acc) {
                    ToggleRow("24-часовой формат", cfg.clock24, acc) { v -> save { it[K.clock24] = v } }
                    ToggleRow("Показывать секунды", cfg.seconds, acc) { v -> save { it[K.seconds] = v } }
                    SliderRow("Размер часов", cfg.clockSize, 36f..110f, acc, { "${it.toInt()} sp" }) { v ->
                        save { it[K.clockSize] = v }
                    }
                }

                Section("ПРОИЗВОДИТЕЛЬНОСТЬ", acc) {
                    ToggleRow("Экономный режим", cfg.lowPower, acc) { v -> save { it[K.lowPower] = v } }
                    Text(
                        "Убирает свечение рамки и фоновую HUD-сетку, замедляет бегущую полосу.",
                        color = Color(0x99FFFFFF), fontSize = 12.sp
                    )
                }

                Section("СКРЫТЫЕ ПРИЛОЖЕНИЯ", acc) {
                    val hiddenApps = apps.filter { it.pkg in cfg.hidden }
                    if (hiddenApps.isEmpty()) {
                        Text("Нет скрытых. Удерживайте иконку на рабочем столе, чтобы скрыть.",
                            color = Color(0x99FFFFFF), fontSize = 13.sp)
                    }
                    hiddenApps.forEach { a ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(a.label, color = Color.White, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                save { it[K.hidden] = cfg.hidden - a.pkg }
                            }) { Text("ПОКАЗАТЬ", color = acc) }
                        }
                    }
                }

                Section("ГЛАВНЫЙ ЭКРАН ANDROID", acc) {
                    Text(
                        "Здесь выбирается, какой лаунчер будет главным. Чтобы вернуть стандартный, выберите его в этом же списке.",
                        color = Color(0x99FFFFFF), fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    ActionButton("ОТКРЫТЬ ВЫБОР ГЛАВНОГО ЭКРАНА", acc, Modifier.fillMaxWidth()) {
                        try {
                            ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {
                            try {
                                ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                Section("СБРОС", acc) {
                    ActionButton("ВЕРНУТЬ СТАНДАРТНОЕ ОФОРМЛЕНИЕ", NEON[5], Modifier.fillMaxWidth()) {
                        scope.launch {
                            withContext(Dispatchers.IO) { wallpaperFile(ctx).delete() }
                            store.reset()
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun Section(title: String, acc: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(
            "// $title", color = acc, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            fontSize = 13.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp)
        )
        Panel(acc, Modifier.fillMaxWidth(), content)
    }
}

@Composable
fun Label(text: String) {
    Text(text, color = Color(0xCCFFFFFF), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
fun ToggleRow(label: String, checked: Boolean, acc: Color, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = acc)
        )
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    acc: Color,
    fmt: (Float) -> String,
    steps: Int = 0,
    onDone: (Float) -> Unit
) {
    var v by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = Color(0xCCFFFFFF), fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(fmt(v), color = acc, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = v,
            onValueChange = { v = it },
            onValueChangeFinished = { onDone(v) },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = acc, activeTrackColor = acc)
        )
    }
}

@Composable
fun Chips(options: List<String>, selected: Int, acc: Color, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) acc.copy(alpha = 0.25f) else Color.Transparent)
                    .border(1.dp, if (sel) acc else Color(0x44FFFFFF), RoundedCornerShape(8.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(o, color = if (sel) acc else Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun ColorRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NEON.forEachIndexed { i, c ->
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(c)
                    .border(if (i == selected) 3.dp else 0.dp, Color.White, CircleShape)
                    .clickable { onSelect(i) }
            )
        }
    }
}

@Composable
fun ActionButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
    }
}
