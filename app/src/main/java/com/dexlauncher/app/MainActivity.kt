package com.dexlauncher.app

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexlauncher.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private val allApps = mutableListOf<AppItem>()
    private val desktopApps = mutableListOf<AppItem>()
    private val pinnedTaskbarApps = mutableListOf<AppItem>()

    private lateinit var desktopAdapter: DesktopAdapter
    private lateinit var startMenuAdapter: StartMenuAdapter
    private lateinit var taskbarAdapter: TaskbarAdapter

    private var currentSettings = LauncherSettings()
    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

    private val timeRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            binding.tvClock.text = timeFormat.format(now)
            binding.tvDate.text = dateFormat.format(now)
            timeHandler.postDelayed(this, 1000)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                if (level >= 0 && scale > 0) {
                    val pct = (level * 100) / scale
                    binding.tvBattery.text = "$pct%"
                    binding.imgBattery.setImageResource(
                        if (isCharging) R.drawable.ic_battery_charging else R.drawable.ic_battery
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("dex_launcher_prefs", Context.MODE_PRIVATE)
        loadSettings()

        setupViews()
        setupClockAndBattery()
        loadInstalledApps()
    }

    private fun loadSettings() {
        currentSettings.iconSizeDp = prefs.getInt("icon_size", 56)
        currentSettings.iconShape = prefs.getString("icon_shape", "squircle") ?: "squircle"
        currentSettings.gridColumns = prefs.getInt("grid_columns", 7)
        currentSettings.showLabels = prefs.getBoolean("show_labels", true)
        currentSettings.customIconPack = prefs.getString("icon_pack", null)
    }

    private fun saveSettings() {
        prefs.edit().apply {
            putInt("icon_size", currentSettings.iconSizeDp)
            putString("icon_shape", currentSettings.iconShape)
            putInt("grid_columns", currentSettings.gridColumns)
            putBoolean("show_labels", currentSettings.showLabels)
            putString("icon_pack", currentSettings.customIconPack)
            apply()
        }
    }

    private fun setupViews() {
        // Desktop Grid
        desktopAdapter = DesktopAdapter(
            onAppClick = { launchApp(it) },
            onAppLongClick = { app, anchor -> showAppContextMenu(app, anchor, isDesktop = true) }
        )
        binding.rvDesktop.layoutManager = GridLayoutManager(this, currentSettings.gridColumns)
        binding.rvDesktop.adapter = desktopAdapter

        // Desktop Background Click & Long Click
        binding.desktopContainer.setOnClickListener {
            closeStartMenu()
        }
        binding.desktopContainer.setOnLongClickListener {
            showDesktopSettingsDialog()
            true
        }

        // Taskbar Pinned Apps
        taskbarAdapter = TaskbarAdapter(
            onAppClick = { launchApp(it) },
            onAppLongClick = { app, anchor -> showAppContextMenu(app, anchor, isTaskbar = true) }
        )
        binding.rvTaskbarApps.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTaskbarApps.adapter = taskbarAdapter

        // Start Menu Grid
        startMenuAdapter = StartMenuAdapter(
            onAppClick = {
                closeStartMenu()
                launchApp(it)
            },
            onAppLongClick = { app, anchor -> showAppContextMenu(app, anchor) }
        )
        binding.rvStartMenu.layoutManager = GridLayoutManager(this, 5)
        binding.rvStartMenu.adapter = startMenuAdapter

        // Start Button Toggle
        binding.btnStartMenu.setOnClickListener {
            toggleStartMenu()
        }

        // Show Desktop / Minimize all button
        binding.btnShowDesktop.setOnClickListener {
            closeStartMenu()
            binding.rvDesktop.smoothScrollToPosition(0)
        }

        // Quick Settings & Notification Triggers
        binding.trayStatusContainer.setOnClickListener {
            expandNotificationPanel()
        }

        // Start Menu Search
        binding.etSearchApps.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStartMenuApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Settings Shortcut in Start Menu
        binding.btnLauncherSettings.setOnClickListener {
            closeStartMenu()
            showDesktopSettingsDialog()
        }
    }

    private fun toggleStartMenu() {
        if (binding.startMenuCard.visibility == View.VISIBLE) {
            closeStartMenu()
        } else {
            binding.startMenuCard.visibility = View.VISIBLE
            binding.etSearchApps.setText("")
            binding.etSearchApps.requestFocus()
            filterStartMenuApps("")
        }
    }

    private fun closeStartMenu() {
        if (binding.startMenuCard.visibility == View.VISIBLE) {
            binding.startMenuCard.visibility = View.GONE
        }
    }

    private fun filterStartMenuApps(query: String) {
        val filtered = DexNativeBridge.searchApps(query, allApps)
        startMenuAdapter.updateData(filtered, currentSettings)
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch(Dispatchers.Default) {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val list = mutableListOf<AppItem>()

            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName) continue // Do not show Dex Launcher inside itself

                val label = ri.loadLabel(pm).toString()
                val icon = ri.loadIcon(pm)
                val isSystem = (ri.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                list.add(
                    AppItem(
                        packageName = pkg,
                        activityName = ri.activityInfo.name,
                        label = label,
                        icon = icon,
                        isSystemApp = isSystem
                    )
                )
            }

            // Sort alphabetically by default
            list.sortBy { it.label.lowercase() }

            withContext(Dispatchers.Main) {
                allApps.clear()
                allApps.addAll(list)

                // Populate Default Taskbar Apps (Brave, Files, Termux, WhatsApp, etc.)
                loadPinnedApps()

                // Populate Desktop Shortcuts
                loadDesktopApps()

                startMenuAdapter.updateData(allApps, currentSettings)
            }
        }
    }

    private fun loadPinnedApps() {
        val defaultPinnedPackages = listOf(
            "com.brave.browser",
            "com.android.chrome",
            "me.zhanghai.android.files",
            "com.huawei.filemanager",
            "com.foxdebug.acode",
            "com.termux",
            "com.whatsapp.w4b",
            "com.whatsapp"
        )

        pinnedTaskbarApps.clear()
        for (pkg in defaultPinnedPackages) {
            allApps.find { it.packageName == pkg }?.let { pinnedTaskbarApps.add(it) }
        }
        if (pinnedTaskbarApps.isEmpty() && allApps.isNotEmpty()) {
            pinnedTaskbarApps.addAll(allApps.take(5))
        }
        taskbarAdapter.updateData(pinnedTaskbarApps, currentSettings.iconShape)
    }

    private fun loadDesktopApps() {
        desktopApps.clear()
        desktopApps.addAll(allApps) // By default display apps on desktop
        desktopAdapter.updateData(desktopApps, currentSettings)
    }

    /**
     * Launch application.
     * Uses standard Intent flags compatible with Huawei HarmonyOS 4.2 native floating windows / multi-window dock.
     */
    private fun launchApp(app: AppItem) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Tidak dapat membuka aplikasi", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal meluncurkan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppContextMenu(
        app: AppItem,
        anchor: View,
        isDesktop: Boolean = false,
        isTaskbar: Boolean = false
    ) {
        val options = mutableListOf<String>()
        if (!isDesktop) options.add("Tambah ke Desktop")
        if (isDesktop) options.add("Hapus dari Desktop")

        if (!pinnedTaskbarApps.contains(app)) {
            options.add("Sematkan ke Taskbar")
        } else {
            options.add("Lepas dari Taskbar")
        }

        options.add("Info Aplikasi")
        options.add("Copot Pemasangan (Uninstall)")

        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Tambah ke Desktop" -> {
                        if (!desktopApps.contains(app)) {
                            desktopApps.add(app)
                            desktopAdapter.updateData(desktopApps, currentSettings)
                            Toast.makeText(this, "Ditambahkan ke Desktop", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Hapus dari Desktop" -> {
                        desktopApps.remove(app)
                        desktopAdapter.updateData(desktopApps, currentSettings)
                    }
                    "Sematkan ke Taskbar" -> {
                        pinnedTaskbarApps.add(app)
                        taskbarAdapter.updateData(pinnedTaskbarApps, currentSettings.iconShape)
                        Toast.makeText(this, "Disematkan ke Taskbar", Toast.LENGTH_SHORT).show()
                    }
                    "Lepas dari Taskbar" -> {
                        pinnedTaskbarApps.remove(app)
                        taskbarAdapter.updateData(pinnedTaskbarApps, currentSettings.iconShape)
                    }
                    "Info Aplikasi" -> {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                        }
                        startActivity(intent)
                    }
                    "Copot Pemasangan (Uninstall)" -> {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                        }
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun showDesktopSettingsDialog() {
        val shapes = arrayOf("squircle", "circle", "rounded_rect", "dex_teardrop", "default")
        val shapeNames = arrayOf("Samsung DeX Squircle", "Lingkaran (Circle)", "Kotak Melengkung (Rounded)", "Tetes Air (Teardrop)", "Default Asli")

        var selectedShapeIndex = shapes.indexOf(currentSettings.iconShape).takeIf { it >= 0 } ?: 0

        // New UI elements
        val btnChangeWallpaper = dialogView.findViewById<Button>(R.id.btn_change_wallpaper)
        val imgIconPreview = dialogView.findViewById<ImageView>(R.id.img_icon_preview)
        // Optional widget manager button (currently placeholder)
        // val btnManageWidgets = dialogView.findViewById<Button>(R.id.btn_manage_widgets)

        // Wallpaper picker
        btnChangeWallpaper.setOnClickListener {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
            startActivity(Intent.createChooser(intent, "Pilih Wallpaper"))
        }

        // Update icon preview when size changes
        seekIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sizeDp = progress + 40
                val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
                val params = imgIconPreview.layoutParams
                params.width = sizePx
                params.height = sizePx
                imgIconPreview.layoutParams = params
                tvIconSizeVal.text = "${sizeDp} dp"
                // Update current settings
                currentSettings.iconSizeDp = sizeDp
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
                desktopAdapter.updateData(desktopApps, currentSettings)
                taskbarAdapter.updateData(pinnedTaskbarApps, currentSettings.iconShape)
            }
        })

        // Existing listeners for other controls (unchanged)
        seekGridColumns.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvGridVal.text = "${progress + 4} Kolom"
                // Update current settings
                currentSettings.gridColumns = progress + 4
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
                // Rebuild desktop layout with new column count
                binding.rvDesktop.layoutManager = GridLayoutManager(this@MainActivity, currentSettings.gridColumns)
                desktopAdapter.updateData(desktopApps, currentSettings)
            }
        })

        cbShowLabels.setOnCheckedChangeListener { _, isChecked ->
            currentSettings.showLabels = isChecked
            saveSettings()
            desktopAdapter.updateData(desktopApps, currentSettings)
        }

        // Shape spinner unchanged
        spinnerShape.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                currentSettings.iconShape = shapes[position]
                saveSettings()
                desktopAdapter.updateData(desktopApps, currentSettings)
                taskbarAdapter.updateData(pinnedTaskbarApps, currentSettings.iconShape)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Show dialog
        AlertDialog.Builder(this)
            .setTitle("Pengaturan Launcher")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Reset Home") { _, _ ->
                // Clear desktop apps
                desktopApps.clear()
                desktopAdapter.updateData(desktopApps, currentSettings)
                Toast.makeText(this, "Homescreen dibersihkan", Toast.LENGTH_SHORT).show()
            }
            .show()

        // New UI elements
        val btnChangeWallpaper = dialogView.findViewById<Button>(R.id.btn_change_wallpaper)
        val imgIconPreview = dialogView.findViewById<ImageView>(R.id.img_icon_preview)
        // Optional widget manager button (currently placeholder)
        // val btnManageWidgets = dialogView.findViewById<Button>(R.id.btn_manage_widgets)

        // Setup Shape Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shapeNames)
        spinnerShape.adapter = adapter
        spinnerShape.setSelection(selectedShapeIndex)

        // Setup Size
        seekIconSize.progress = currentSettings.iconSizeDp - 40
        tvIconSizeVal.text = "${currentSettings.iconSizeDp} dp"
        seekIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvIconSizeVal.text = "${progress + 40} dp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Setup Grid
        seekGridColumns.progress = currentSettings.gridColumns - 4
        tvGridVal.text = "${currentSettings.gridColumns} Kolom"
        seekGridColumns.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvGridVal.text = "${progress + 4} Kolom"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        cbShowLabels.isChecked = currentSettings.showLabels

        AlertDialog.Builder(this)
            .setTitle("Pengaturan DeX Launcher")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                currentSettings.iconShape = shapes[spinnerShape.selectedItemPosition]
                currentSettings.iconSizeDp = seekIconSize.progress + 40
                currentSettings.gridColumns = seekGridColumns.progress + 4
                currentSettings.showLabels = cbShowLabels.isChecked
                saveSettings()

                // Refresh UI
                binding.rvDesktop.layoutManager = GridLayoutManager(this, currentSettings.gridColumns)
                desktopAdapter.updateData(desktopApps, currentSettings)
                taskbarAdapter.updateData(pinnedTaskbarApps, currentSettings.iconShape)
                startMenuAdapter.updateData(allApps, currentSettings)
                Toast.makeText(this, "Pengaturan diperbarui", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Ubah Wallpaper") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                    startActivity(Intent.createChooser(intent, "Pilih Wallpaper"))
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal membuka wallpaper picker", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun expandNotificationPanel() {
        try {
            val statusBarService = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            // Fallback for settings
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun setupClockAndBattery() {
        timeHandler.post(timeRunnable)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        checkNetworkStatus()
    }

    private fun checkNetworkStatus() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        binding.imgWifi.setImageResource(if (isWifi) R.drawable.ic_wifi else R.drawable.ic_network_cell)
    }

    override fun onDestroy() {
        super.onDestroy()
        timeHandler.removeCallbacks(timeRunnable)
        unregisterReceiver(batteryReceiver)
    }

    override fun onBackPressed() {
        if (binding.startMenuCard.visibility == View.VISIBLE) {
            closeStartMenu()
        } else {
            super.onBackPressed()
        }
    }
}
