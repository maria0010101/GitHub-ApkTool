package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "git_projects")
data class GitProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,              // 自訂名稱或 Repo 名稱
    val owner: String,             // GitHub Owner (例如: google)
    val repo: String,              // GitHub Repo (例如: iosched)
    val latestVersion: String,     // 最新 Release Tag (例如: v1.2.0)
    val latestApkUrl: String?,     // APK 下載連結 (browser_download_url)
    val latestApkName: String?,    // APK 檔名
    val updatedAt: Long            // 最後更新時間戳
)
