package com.dexlauncher.app

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

object IconCustomizer {

    fun shapeIcon(context: Context, original: Drawable, shape: String, sizePx: Int): Drawable {
        if (shape == "default" || sizePx <= 0) {
            return original
        }

        val bitmap = try {
            original.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            original.setBounds(0, 0, canvas.width, canvas.height)
            original.draw(canvas)
            bmp
        }

        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }

        val path = Path()
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())

        when (shape.lowercase()) {
            "circle" -> {
                path.addOval(rect, Path.Direction.CW)
            }
            "squircle" -> {
                // Continuous curvature squircle
                val radius = sizePx * 0.28f
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            "rounded_rect" -> {
                val radius = sizePx * 0.16f
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            "dex_teardrop" -> {
                val radii = floatArrayOf(
                    sizePx * 0.35f, sizePx * 0.35f, // Top-left
                    sizePx * 0.35f, sizePx * 0.35f, // Top-right
                    sizePx * 0.35f, sizePx * 0.35f, // Bottom-right
                    sizePx * 0.10f, sizePx * 0.10f  // Bottom-left
                )
                path.addRoundRect(rect, radii, Path.Direction.CW)
            }
            else -> {
                val radius = sizePx * 0.25f
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
        }

        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return BitmapDrawable(context.resources, output)
    }

    /**
     * Scan for installed icon pack applications on the system
     */
    fun getInstalledIconPacks(pm: PackageManager): List<Pair<String, String>> {
        val iconPacks = mutableListOf<Pair<String, String>>()
        val intentFilterThemes = listOf(
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.novalauncher.THEME"
        )

        for (action in intentFilterThemes) {
            val intent = android.content.Intent(action)
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                val label = ri.loadLabel(pm).toString()
                if (iconPacks.none { it.first == pkg }) {
                    iconPacks.add(Pair(pkg, label))
                }
            }
        }
        return iconPacks
    }
}
