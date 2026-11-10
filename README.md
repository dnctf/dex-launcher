# 🖥️ DeX Launcher

[![Release](https://img.shields.io/github/v/release/dnctf/dex-launcher?include_prereleases&style=flat-square&color=blue)](https://github.com/dnctf/dex-launcher/releases)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)
[![Rust](https://img.shields.io/badge/Rust-Core%20Engine-orange?style=flat-square&logo=rust)](rust-core/)
[![Android](https://img.shields.io/badge/Platform-Android%20%7C%20HarmonyOS-3DDC84?style=flat-square&logo=android)](app/)

**DeX Launcher** adalah peluncur layar utama (*Home Launcher*) bergaya desktop **Samsung DeX** untuk tablet Android dan HarmonyOS (dioptimalkan secara khusus untuk rasio layar 3:2 seperti **HUAWEI MatePad Air**).

Dibangun dengan arsitektur modern memadukan **Android Kotlin** di antarmuka sistem dan **Rust Core Engine (`dex_core`)** melalui JNI untuk pemrosesan pencarian cepat, serialisasi tata letak, dan performa tinggi.

---

## ✨ Fitur Utama

### 1. 🖥️ Pengalaman Desktop ala Samsung DeX
* **Taskbar Bawah:**
  * Tombol **Start Menu** (ikon grid 9 titik khas Samsung DeX).
  * Bilah aplikasi tersemat (*Pinned Apps*) dan aplikasi aktif.
  * **System Tray:** Jam digital, tanggal, persentase baterai real-time + status pengisian daya, dan indikator jaringan/WiFi.
  * Tombol **Show Desktop** di sudut kanan bawah untuk meminimalkan Start Menu dan fokus ke area kerja.
* **Start Menu / App Drawer:**
  * Menu pop-up mengambang (*floating card*) dengan bilah pencarian real-time di bagian atas.
  * Grid aplikasi terpasang dengan navigasi sentuh yang responsif.
  * Menu klik-tahan (*context menu*): Tambah ke Desktop, Sematkan ke Taskbar, Info Aplikasi, dan Copot Pemasangan.

### 2. 🦀 Rust Native Engine (`dex_core`)
* Menggunakan pustaka *native* Rust (`cdylib`) yang dikompilasi ke `arm64-v8a`, `armeabi-v7a`, dan `x86_64`.
* Algoritma pencarian **Fuzzy Match & Ranking** super cepat untuk memfilter ratusan aplikasi dalam hitungan milidetik.
* Arsitektur manajemen konfigurasi yang aman dari kebocoran memori (*memory-safe*).

### 3. 🎨 Kustomisasi Icon & Tampilan Desktop
* **Pilihan Bentuk Icon:**
  * *Samsung DeX Squircle* (Lengkungan halus khas One UI)
  * *Circle* (Lingkaran)
  * *Rounded Square* (Kotak melengkung)
  * *Teardrop* (Tetes air)
  * *Default* (Ikon asli bawaan aplikasi)
* **Slider Ukuran Icon:** Ubah ukuran icon dari 40dp hingga 72dp.
* **Tampilkan / Sembunyikan Label:** Pilihan menyembunyikan nama aplikasi untuk tampilan minimalis.
* **Pengaturan Grid Desktop:** Sesuaikan jumlah kolom (4 hingga 9 kolom).
* **Dukungan Wallpaper:** Mendukung wallpaper sistem transparan maupun *custom wallpaper picker*.

### 4. 🪟 Kompatibilitas Jendela Mengambang (Floating Windows)
* Menghormati arsitektur sistem operasi tanpa membajak *window manager*.
* Pada tablet **HUAWEI (HarmonyOS / EMUI)**, fitur bawaan seperti **Multi-Window Dock**, *split-screen*, dan *floating window* (geser dari samping) tetap bekerja 100% normal dan lancar.

---

## 📲 Cara Pemasangan di Tablet (HUAWEI MatePad / Android)

1. Buka halaman [Releases](https://github.com/dnctf/dex-launcher/releases).
2. Unduh file APK terbaru (`dex-launcher-v0.1.0-pre.apk`).
3. Pasang APK di tablet Anda.
4. Jadikan sebagai Peluncur Standar:
   * **HarmonyOS / EMUI:** Masuk ke **Pengaturan** > **Aplikasi** > **Aplikasi Standar** > **Beranda / Peluncur** > Pilih **DeX Launcher**.
   * **AOSP / One UI / MIUI:** Masuk ke **Settings** > **Apps** > **Default Apps** > **Home app** > Pilih **DeX Launcher**.

---

## 🛠️ Struktur Proyek

```
dex-launcher/
├── .github/workflows/
│   └── build-and-release.yml   # CI/CD otomatis kompilasi Rust NDK & rilis APK
├── app/                        # Modul Android Application (Kotlin)
│   ├── src/main/java/com/dexlauncher/app/
│   │   ├── MainActivity.kt     # Aktivitas Home Launcher utama
│   │   ├── DexNativeBridge.kt  # JNI Bridge ke modul Rust
│   │   ├── IconCustomizer.kt   # Mesin pembentuk & kustomisasi icon
│   │   ├── DesktopAdapter.kt   # Grid desktop
│   │   ├── StartMenuAdapter.kt # Grid menu Start
│   │   └── TaskbarAdapter.kt   # Bilah Taskbar
│   └── src/main/res/           # Layout, drawables DeX, vector icons
├── rust-core/                  # Modul Native Rust Engine
│   ├── Cargo.toml
│   └── src/lib.rs              # Fuzzy search & core layout logic (JNI)
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🏗️ Kompilasi dari Sumber (Build Locally)

### Prasyarat
* Android SDK (API 34) & NDK (r25c atau lebih baru)
* JDK 17
* Rust (stable) + target Android:
  ```bash
  rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
  cargo install cargo-ndk
  ```

### Langkah Kompilasi
```bash
# 1. Kompilasi pustaka Rust native JNI
cd rust-core
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o ../app/src/main/jniLibs build --release
cd ..

# 2. Bangun file APK
./gradlew assembleRelease
```
File APK akan berada di `app/build/outputs/apk/release/app-release.apk`.

---

## 📄 Lisensi
Didistribusikan di bawah Lisensi [MIT](LICENSE).
