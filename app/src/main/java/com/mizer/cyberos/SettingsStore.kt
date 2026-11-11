package com.mizer.cyberos

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cyberStore by preferencesDataStore(name = "cyberos_settings")

val NEON = listOf(
    Color(0xFF00F0FF), // голубой
    Color(0xFFB026FF), // фиолетовый
    Color(0xFFFF2BD6), // розовый
    Color(0xFF39FF14), // зелёный
    Color(0xFFFFE600), // жёлтый
    Color(0xFFFF3B3B)  // красный
)

data class Cfg(
    val accent: Int = 0,
    val borderOn: Boolean = true,
    val borderColor: Int = 1,
    val borderSpeed: Float = 1f,
    val borderThick: Float = 3f,
    val wpMode: Int = 0,
    val wpScale: Float = 1f,
    val wpDim: Float = 0.35f,
    val wpVersion: Int = 0,
    val cols: Int = 5,
    val iconSize: Float = 56f,
    val labels: Boolean = true,
    val clock24: Boolean = true,
    val clockSize: Float = 64f,
    val seconds: Boolean = false,
    val anim: Boolean = true,
    val lowPower: Boolean = false,
    val intro: Boolean = true,
    val showClock: Boolean = true,
    val showBattery: Boolean = true,
    val showSearch: Boolean = true,
    val hidden: Set<String> = emptySet()
) {
    val acc: Color get() = NEON[accent.coerceIn(0, NEON.lastIndex)]
    val borderCol: Color get() = NEON[borderColor.coerceIn(0, NEON.lastIndex)]
}

object K {
    val accent = intPreferencesKey("accent")
    val borderOn = booleanPreferencesKey("borderOn")
    val borderColor = intPreferencesKey("borderColor")
    val borderSpeed = floatPreferencesKey("borderSpeed")
    val borderThick = floatPreferencesKey("borderThick")
    val wpMode = intPreferencesKey("wpMode")
    val wpScale = floatPreferencesKey("wpScale")
    val wpDim = floatPreferencesKey("wpDim")
    val wpVersion = intPreferencesKey("wpVersion")
    val cols = intPreferencesKey("cols")
    val iconSize = floatPreferencesKey("iconSize")
    val labels = booleanPreferencesKey("labels")
    val clock24 = booleanPreferencesKey("clock24")
    val clockSize = floatPreferencesKey("clockSize")
    val seconds = booleanPreferencesKey("seconds")
    val anim = booleanPreferencesKey("anim")
    val lowPower = booleanPreferencesKey("lowPower")
    val intro = booleanPreferencesKey("intro")
    val showClock = booleanPreferencesKey("showClock")
    val showBattery = booleanPreferencesKey("showBattery")
    val showSearch = booleanPreferencesKey("showSearch")
    val hidden = stringSetPreferencesKey("hidden")
    val boot = intPreferencesKey("bootCount")
}

private fun Preferences.toCfg(): Cfg {
    val d = Cfg()
    return Cfg(
        accent = this[K.accent] ?: d.accent,
        borderOn = this[K.borderOn] ?: d.borderOn,
        borderColor = this[K.borderColor] ?: d.borderColor,
        borderSpeed = this[K.borderSpeed] ?: d.borderSpeed,
        borderThick = this[K.borderThick] ?: d.borderThick,
        wpMode = this[K.wpMode] ?: d.wpMode,
        wpScale = this[K.wpScale] ?: d.wpScale,
        wpDim = this[K.wpDim] ?: d.wpDim,
        wpVersion = this[K.wpVersion] ?: d.wpVersion,
        cols = this[K.cols] ?: d.cols,
        iconSize = this[K.iconSize] ?: d.iconSize,
        labels = this[K.labels] ?: d.labels,
        clock24 = this[K.clock24] ?: d.clock24,
        clockSize = this[K.clockSize] ?: d.clockSize,
        seconds = this[K.seconds] ?: d.seconds,
        anim = this[K.anim] ?: d.anim,
        lowPower = this[K.lowPower] ?: d.lowPower,
        intro = this[K.intro] ?: d.intro,
        showClock = this[K.showClock] ?: d.showClock,
        showBattery = this[K.showBattery] ?: d.showBattery,
        showSearch = this[K.showSearch] ?: d.showSearch,
        hidden = this[K.hidden] ?: d.hidden
    )
}

class Store(private val ctx: Context) {

    val flow: Flow<Cfg> = ctx.cyberStore.data
        .catch { emit(emptyPreferences()) }
        .map { it.toCfg() }

    suspend fun edit(block: (MutablePreferences) -> Unit) {
        try {
            ctx.cyberStore.edit { block(it) }
        } catch (_: Exception) {
        }
    }

    /** Сброс всех настроек (номер загрузки сохраняем, чтобы интро не вылезло снова). */
    suspend fun reset() {
        try {
            ctx.cyberStore.edit {
                val b = it[K.boot]
                it.clear()
                if (b != null) it[K.boot] = b
            }
        } catch (_: Exception) {
        }
    }

    /** true, если это первый запуск лаунчера после перезагрузки планшета. */
    suspend fun shouldShowIntro(): Boolean {
        return try {
            val cur = Settings.Global.getInt(ctx.contentResolver, Settings.Global.BOOT_COUNT, -1)
            val p = ctx.cyberStore.data.first()
            val last = p[K.boot] ?: -2
            val enabled = p[K.intro] ?: true
            if (cur != last) edit { it[K.boot] = cur }
            enabled && cur != last
        } catch (_: Exception) {
            false
        }
    }
}
