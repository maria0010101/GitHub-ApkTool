# GitHub ApkTool 🚀

<div align="center">

**GitHub Release APK 下載與版本管理工具 (Android App)**

一個遵循 **Clean Architecture** 與 **MVVM 架構**，以 **Kotlin** 與 **Jetpack Compose (Material 3)** 打造的 GitHub 開源 APK 追蹤、版本檢查與自動下載管理應用程式。

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-2.6.1-orange.svg?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

</div>

---

## 🌟 功能特點 (Features)

* 📦 **GitHub 專案追蹤**：支援以 `https://github.com/owner/repo` 網址直接貼上或輸入 `owner/repo` 加入追蹤。
* 🕒 **雙時間戳記追蹤（更新時間 vs 檢測時間）**：
  * **更新時間**：顯示 GitHub 專案該版本（Release）實際發布或檔案更新的日期時間，自動轉換為使用者系統時區。
  * **檢測時間**：顯示 App 本機發起網路檢查的時間，區分清晰不再混淆。
* 🔄 **即時檢查發佈版本**：支援單一專案手動刷新與一鍵全部專案更新檢查，自動獲取最新 Release Tag 與 APK 下載連結。
* 🚀 **支援 Pre-release 與各類發佈模式**：具備智能回退機制，完美支援僅發佈 Pre-release (如 RC、Beta 等候選版本) 的專案（如 `ReSukiSU`）。
* 💾 **追蹤清單匯出與匯入**：支援一鍵匯出 JSON 備份檔案、剪貼簿複製分享，並支援從備份檔案或純文字網址清單智慧匯入（支援合併與覆蓋，完整保留時間戳記）。
* ⚡ **智能 APK 匹配**：自動解析 Release Assets 中的 `.apk` 安裝包，優先適配 arm64-v8a 與 universal 架構，並自動過濾排除除錯版 (debug)。
* 📥 **原生背景下載**：整合 Android 原生 `DownloadManager`，下載進度即時通知與錯誤重試。
* 📲 **一鍵安裝 APK**：整合 `FileProvider` 與安裝 Intent，下載完成後可直接在 App 內啟動安裝程式。
* 🔍 **即時搜尋與過濾**：快速搜尋 Repo 名稱、擁有者或自訂顯示名稱。
* 🔑 **GitHub API Token 支援**：可於設定中設定 Personal Access Token (PAT)，輕鬆突破每小時 60 次的匿名速率限制。
* 📺 **Android TV 與大螢幕支援**：深度相容 Android TV、電視盒與大螢幕平板，提供 Android TV Leanback Launcher 橫幅 Banner、遙控器 D-pad 方向鍵焦點導航反饋、專屬電視多欄網格排版與一鍵加入熱門電視應用。
* 🎨 **Material 3 現代化 UI**：全新 3D 圖示、流暢的動畫、深色模式適配與優雅的雙時間卡片式設計。

---

## 🛠️ 技術棧 (Tech Stack)

| 模組 | 使用技術 |
| :--- | :--- |
| **UI 框架** | Jetpack Compose (Material 3) + FlowRow / AnimatedVisibility |
| **電視與大螢幕** | Android TV Leanback Launcher + D-pad Focus Feedback + Adaptive Grid |
| **架構模式** | Clean Architecture + MVVM (Model-View-ViewModel) |
| **非同步處理** | Kotlin Coroutines + Flow + StateFlow / SharedFlow |
| **本機資料庫** | Room Database + KSP (Kotlin Symbol Processing) |
| **網路請求** | Retrofit 2 + OkHttp 3 + Gson Converter + HttpLoggingInterceptor |
| **檔案下載** | Android 原生 DownloadManager + FileProvider |
| **支援系統** | minSdk 26 (Android 8.0) / targetSdk 36 (Android 15+) 相容手機與 Android TV |

---

## 📱 畫面結構 (Architecture)

```text
com.example.myapplication/
├── data/
│   ├── local/          # Room Entity, DAO, Database
│   │   ├── GitProject.kt
│   │   ├── GitProjectDao.kt
│   │   └── AppDatabase.kt
│   ├── remote/         # Retrofit API Service, DTOs & Client
│   │   ├── GitHubApiService.kt
│   │   ├── GitHubReleaseDto.kt
│   │   └── RetrofitClient.kt
│   └── repository/     # Repository Pattern 實作
│       └── GitProjectRepository.kt
├── downloader/         # DownloadManager 與 APK 安裝封裝
│   └── ApkDownloader.kt
├── ui/
│   ├── screens/        # Compose UI 畫面與對話框
│   │   ├── ProjectListScreen.kt     # 手機標準佈局
│   │   ├── TvProjectListScreen.kt   # Android TV 專屬多欄網格佈局
│   │   ├── AddEditProjectDialog.kt
│   │   ├── ImportExportDialog.kt
│   │   └── SettingsDialog.kt
│   ├── theme/          # Material 3 主題配置
│   └── viewmodel/      # ProjectViewModel 狀態管理
├── util/
│   └── DeviceUtils.kt  # Android TV / 螢幕特徵偵測與佈局切換
└── MainActivity.kt     # App 主要進入點與動態權限請求
```

---

## 📋 版本更新日誌 (Changelog)

### v0.4.1 (2026-09-27)
* 📺 **Android TV 深度相容與安裝支援**：
  * AndroidManifest 宣告 `android.software.leanback` 與 `android.hardware.touchscreen` 相容標籤（`required="false"`），確保手機與 Android TV 雙向通用。
  * 支援 Android TV 啟動器 `LEANBACK_LAUNCHER` 進入點，並配置標準 16:9 電視專屬橫幅 Banner（`tv_banner.png`）。
* 🖥️ **專屬 Android TV UI 佈局 (TvProjectListScreen)**：
  * 打造專為 10 呎客廳視距設計的大螢幕多欄自適應卡片網格佈局（`LazyVerticalGrid`）。
  * 整合 TV 頂部全功能控制欄，包含放大圖示與文字導航按鈕。
  * 專屬電視空狀態畫面：支援一鍵點選直接加入電視熱門開源專案（如 `SmartTube`、`ReSukiSU`、`Seal`、`NewPipe`、`Kodi`）。
* 🎮 **電視遙控器 D-pad 焦點導航支援**：
  * 全面適配電視遙控器上下左右方向鍵與確認鍵，卡片與按鈕獲焦時具備平滑放大縮放動畫、高亮主題色外框與懸浮陰影。
* 🔀 **多模式介面自由切換**：
  * 設定對話框新增「介面佈局模式」，支援「自動偵測」、「電視 / 大螢幕佈局」、「手機標準佈局」，頂部欄亦提供快捷切換。
* 📦 **版本升級與簽章打包**：
  * 升級版本號至 `versionCode: 5`、`versionName: 0.4.1`。
  * 產出並備份正式發佈之 `GitHub-ApkTool-v0.4.1.apk`。

### v0.4 (2026-09-13)
* 🕒 **Release 更新時間與檢測時間分離**：
  * 原卡片顯示的「更新時間」明確更名為「檢測時間」，真實反映 App 本機發起網路檢查的時間戳記。
  * 新增「更新時間」欄位，從 GitHub API 即時抓取該版本（Release）實際發布或檔案更新的 ISO 8601 時間戳記，並自動轉換為系統時區顯示。
* 🗄️ **Room 資料庫安全遷移 (Migration 1->2)**：
  * 資料模型 `GitProject` 擴充 `releaseTime` 欄位。
  * 實作 `MIGRATION_1_2` 自動執行 SQL `ALTER TABLE` 遷移，升級安裝時完整保留舊版所有專案資料不遺失。
* 💾 **備份匯出與匯入資料完整性提升**：
  * 備份 JSON 結構加入 `releaseTime` 映射，確保專案名單轉移或還原時時間資訊完好無損。
* 📱 **UI 排版視覺優化**：
  * 專案卡片右側整合雙時間顯示排版，視覺資訊層次分明。

### v0.3 (2026-09-05)
* 🎨 **全新 3D 應用程式圖示**：
  * 採用結合 GitHub Octocat、發佈通知鈴鐺、更新同步圖章與代碼視窗的精緻 3D 視覺風格。
  * 支援 Adaptive Icons 自適應圖示與 Legacy 圖示，完美適配 Android 8.0 至 Android 15+ 各種啟動器遮罩（圓形、圓角矩形等）。

### v0.2 (2026-08-23)
* 🚀 **Pre-release 與多發布模式支援**：
  * 完美相容僅發佈 Pre-release (如 RC、Beta) 的專案（如 `ReSukiSU`）。
* 💾 **追蹤清單匯出與匯入**：
  * 支援備份檔案匯出與剪貼簿複製分享，以及 JSON 檔案與純網址智慧匯入（合併 / 覆蓋）。
* ⚡ **APK 智能匹配優化**：
  * 自動適配 arm64-v8a 與 universal 架構，自動過濾排除 debug 安裝包。

### v0.1 (2026-08-21)
* 🎉 **初始版本發布**：
  * GitHub 專案追蹤、最新 Release 檢查、背景下載與本機 APK 安裝。

---

## 🚀 快速開始 (Getting Started)

### 編譯與打包

```bash
# Clone 專案
git clone https://github.com/maria0010101/GitHub-ApkTool.git
cd GitHub-ApkTool

# 編譯 Debug APK
./gradlew assembleDebug

# 安裝至連接的手機或模擬器
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 授權條款 (License)

本專案採用 [MIT License](LICENSE) 開源授權。
