package com.dialect.launcher.appindex

import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Converts a launcher activity's badged icon Drawable into a Compose-ready ImageBitmap, once, at index-build time. */
class IconLoader {
    fun load(info: LauncherActivityInfo): ImageBitmap? {
        return try {
            drawableToImageBitmap(info.getBadgedIcon(0))
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }
}
