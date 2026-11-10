package com.dexlauncher.app

import android.graphics.drawable.Drawable
import org.json.JSONArray
import org.json.JSONObject

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    var icon: Drawable? = null,
    val isSystemApp: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("package_name", packageName)
            put("label", label)
        }
    }
}

data class DesktopShortcut(
    val id: String,
    val packageName: String,
    var customLabel: String? = null,
    var positionIndex: Int = 0
)

data class LauncherSettings(
    var iconSizeDp: Int = 56,
    var iconShape: String = "squircle", // "default", "circle", "squircle", "rounded_rect", "dex_teardrop"
    var gridColumns: Int = 7,
    var showLabels: Boolean = true,
    var customIconPack: String? = null
)
