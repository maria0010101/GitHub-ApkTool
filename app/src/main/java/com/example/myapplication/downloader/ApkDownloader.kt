package com.example.myapplication.downloader

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class ApkDownloader(private val context: Context) {

    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    fun downloadApk(url: String, fileName: String, title: String): Long {
        try {
            val validFileName = if (fileName.endsWith(".apk", ignoreCase = true)) {
                fileName
            } else {
                "$fileName.apk"
            }

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(title)
                setDescription("正在下載 $validFileName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, validFileName)
                setMimeType("application/vnd.android.package-archive")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "已開始下載: $validFileName", Toast.LENGTH_SHORT).show()
            return downloadId
        } catch (e: Exception) {
            Toast.makeText(context, "下載失敗: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return -1L
        }
    }

    fun installDownloadedApk(fileName: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) {
            Toast.makeText(context, "找不到已下載的 APK 檔案: $fileName", Toast.LENGTH_SHORT).show()
            return
        }

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "無法啟動安裝程式: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
