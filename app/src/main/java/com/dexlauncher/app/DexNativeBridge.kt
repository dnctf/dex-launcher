package com.dexlauncher.app

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object DexNativeBridge {
    private const val TAG = "DexNativeBridge"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("dex_core")
            isNativeLoaded = true
            Log.i(TAG, "Rust native dex_core library loaded successfully!")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Rust dex_core library not loaded; using Kotlin fallback", e)
            isNativeLoaded = false
        }
    }

    external fun getCoreVersion(): String
    external fun searchAppsNative(query: String, appsJson: String): String
    external fun getDefaultSettingsNative(): String

    fun isLoaded(): Boolean = isNativeLoaded

    /**
     * Search apps with query. Uses Rust fuzzy search if native library is loaded,
     * otherwise falls back to Kotlin filtering.
     */
    fun searchApps(query: String, allApps: List<AppItem>): List<AppItem> {
        if (query.isBlank()) return allApps

        if (isNativeLoaded) {
            try {
                val jsonArray = JSONArray()
                allApps.forEach { jsonArray.put(it.toJson()) }

                val rawResult = searchAppsNative(query, jsonArray.toString())
                val resultArray = JSONArray(rawResult)
                val rankedPackages = mutableListOf<String>()

                for (i in 0 until resultArray.length()) {
                    val obj = resultArray.getJSONObject(i)
                    rankedPackages.add(obj.getString("package_name"))
                }

                val appMap = allApps.associateBy { it.packageName }
                return rankedPackages.mapNotNull { appMap[it] }
            } catch (e: Exception) {
                Log.e(TAG, "Native search failed, falling back to Kotlin search", e)
            }
        }

        // Kotlin Fallback Search
        val q = query.trim().lowercase()
        return allApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }.sortedBy {
            val labelLower = it.label.lowercase()
            when {
                labelLower == q -> 0
                labelLower.startsWith(q) -> 1
                else -> 2
            }
        }
    }
}
