package com.mizer.cyberos

import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.io.File

data class AppItem(
    val label: String,
    val pkg: String,
    val cls: String,
    val icon: ImageBitmap
)

fun loadApps(ctx: Context): List<AppItem> {
    return try {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .mapNotNull { ri ->
                try {
                    AppItem(
                        label = ri.loadLabel(pm).toString(),
                        pkg = ri.activityInfo.packageName,
                        cls = ri.activityInfo.name,
                        icon = ri.loadIcon(pm).toBitmap(128, 128).asImageBitmap()
                    )
                } catch (_: Throwable) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    } catch (_: Throwable) {
        emptyList()
    }
}

fun launchApp(ctx: Context, app: AppItem) {
    try {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.pkg, app.cls)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        ctx.startActivity(i)
    } catch (_: Exception) {
        Toast.makeText(ctx, "Не удалось запустить ${app.label}", Toast.LENGTH_SHORT).show()
    }
}

fun wallpaperFile(ctx: Context) = File(ctx.filesDir, "wallpaper.img")

fun copyWallpaper(ctx: Context, uri: Uri): Boolean {
    return try {
        val tmp = File(ctx.filesDir, "wallpaper.tmp")
        ctx.contentResolver.openInputStream(uri)!!.use { input ->
            tmp.outputStream().use { out -> input.copyTo(out) }
        }
        val f = wallpaperFile(ctx)
        if (f.exists()) f.delete()
        tmp.renameTo(f)
    } catch (_: Throwable) {
        false
    }
}

fun loadWallpaper(ctx: Context): ImageBitmap? {
    return try {
        val f = wallpaperFile(ctx)
        if (!f.exists()) return null
        val src = ImageDecoder.createSource(f)
        val bmp = ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            val big = maxOf(info.size.width, info.size.height)
            decoder.setTargetSampleSize(maxOf(1, big / 2048))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        bmp.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
