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
* 🔄 **即時檢查發佈版本**：支援單一專案檢查與一鍵全部專案更新檢查，自動獲取最新 Release Tag 與更新時間。
* 🚀 **支援 Pre-release 與各類發佈模式**：具備智能回退機制，完美支援僅發佈 Pre-release (如 RC、Beta 等候選版本) 的專案（如 `ReSukiSU`）。
* 💾 **追蹤清單匯出與匯入**：支援一鍵匯出 JSON 備份檔案、剪貼簿複製分享，並支援從備份檔案或純文字網址清單智慧匯入（支援合併與覆蓋）。
* ⚡ **智能 APK 匹配**：自動解析 Release Assets 中的 `.apk` 安裝包，優先適配 arm64-v8a 與 universal 架構，並自動過濾排除除錯版 (debug)。
* 📥 **原生背景下載**：整合 Android 原生 `DownloadManager`，下載進度即時通知與錯誤重試。
* 📲 **一鍵安裝 APK**：整合 `FileProvider` 與安裝 Intent，下載完成後可直接在 App 內啟動安裝程式。
* 🔍 **即時搜尋與過濾**：快速搜尋 Repo 名稱、擁有者或自訂顯示名稱。
* 🔑 **GitHub API Token 支援**：可於設定中設定 Personal Access Token (PAT)，輕鬆突破每小時 60 次的匿名速率限制。
* 🎨 **Material 3 現代化 UI**：流暢的動畫、深色模式適配與優雅的卡片式設計。

---

## 🛠️ 技術棧 (Tech Stack)

| 模組 | 使用技術 |
| :--- | :--- |
| **UI 框架** | Jetpack Compose (Material 3) + FlowRow / AnimatedVisibility |
| **架構模式** | Clean Architecture + MVVM (Model-View-ViewModel) |
| **非同步處理** | Kotlin Coroutines + Flow + StateFlow / SharedFlow |
| **本機資料庫** | Room Database + KSP (Kotlin Symbol Processing) |
| **網路請求** | Retrofit 2 + OkHttp 3 + Gson Converter + HttpLoggingInterceptor |
| **檔案下載** | Android 原生 DownloadManager + FileProvider |
| **支援系統** | minSdk 26 (Android 8.0) / targetSdk 36 (Android 15+) |

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
│   │   ├── ProjectListScreen.kt
│   │   ├── AddEditProjectDialog.kt
│   │   ├── ImportExportDialog.kt
│   │   └── SettingsDialog.kt
│   ├── theme/          # Material 3 主題配置
│   └── viewmodel/      # ProjectViewModel 狀態管理
└── MainActivity.kt     # App 主要進入點與動態權限請求
```

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
