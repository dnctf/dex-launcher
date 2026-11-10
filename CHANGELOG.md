# Changelog

Semua perubahan penting pada proyek **DeX Launcher** akan didokumentasikan di berkas ini.

Format berkas ini mengacu pada standar [Keep a Changelog](https://keepachangelog.com/id/1.0.0/).

---

## [0.1.0-pre] - 2026-11-10

### 🚀 Ditambahkan (Initial Pre-Release)
- **Tampilan Desktop Samsung DeX:**
  - Bilah Taskbar bawah permanen dengan tinggi 48dp bergaya *glassmorphism*.
  - Tombol **Start Menu** DeX dengan ikon 9 titik.
  - Bilah aplikasi tersemat (*pinned apps*) horizontal yang dapat disesuaikan.
  - **System Tray:** Jam real-time, tanggal, persentase & status pengisian baterai via `BroadcastReceiver`, indikator WiFi/jaringan, dan akses cepat ke panel notifikasi/pengaturan.
  - Tombol **Show Desktop** di sudut kanan bawah.
- **Start Menu (App Drawer Pop-Up):**
  - Jendela pop-up melayang di sudut kiri bawah dengan latar belakang transparan gelap dan sudut membulat.
  - Bilah pencarian aplikasi instan di bagian atas.
  - Menu aksi klik-tahan (*context menu*): Tambah ke Desktop, Sematkan ke Taskbar, Info Aplikasi, dan Copot Pemasangan.
- **Mesin Rust Core (`dex_core`):**
  - Modul native Rust terintegrasi via JNI (`DexNativeBridge`).
  - Algoritma pencarian aplikasi *fuzzy ranking* berkinerja tinggi.
  - Skema konfigurasi tata letak desktop berbasis JSON.
  - Fallback otomatis ke Kotlin jika binary native belum dimuat.
- **Mesin Kustomisasi Icon:**
  - Pilihan bentuk icon: *Samsung DeX Squircle*, *Circle*, *Rounded Rectangle*, *Teardrop*, dan *Default*.
  - Pengatur ukuran icon (*Icon Size Slider*) dari 40dp hingga 72dp.
  - Pengaturan jumlah kolom grid desktop (4 hingga 9 kolom).
  - Sakelar untuk menampilkan atau menyembunyikan label nama aplikasi di desktop.
  - Pemindai tema icon pack pihak ketiga yang terinstal di sistem.
- **Kompatibilitas Multi-Window & HarmonyOS:**
  - Penggunaan Intent flags standar (`FLAG_ACTIVITY_NEW_TASK`) tanpa membajak *window manager* sistem operasi.
  - Kompatibel penuh dengan *Huawei Multi-Window Dock*, jendela melayang (*floating windows*), dan *split-screen* bawaan HarmonyOS 4.2 / EMUI.
- **Otomasi CI/CD:**
  - Alur kerja GitHub Actions (`build-and-release.yml`) untuk kompilasi lintas-arsitektur Rust NDK (`arm64-v8a`, `armeabi-v7a`, `x86_64`) dan pembuatan file APK otomatis.
  - Publikasi otomatis rilis GitHub dengan label **Pre-release**.
