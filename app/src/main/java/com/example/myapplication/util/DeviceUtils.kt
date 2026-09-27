package com.example.myapplication.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object DeviceUtils {

    /**
     * 判斷是否應啟用電視 / 大螢幕 UI 佈局。
     * 支援使用者強制指定「tv」或「mobile」，或預設「auto」自動依系統特徵判斷。
     */
    fun isTvModeActive(context: Context, layoutMode: String): Boolean {
        return when (layoutMode) {
            "tv" -> true
            "mobile" -> false
            else -> isRunningOnTvOrLargeScreen(context)
        }
    }

    /**
     * 檢查當前執行環境是否為 Android TV 或橫向大螢幕
     */
    fun isRunningOnTvOrLargeScreen(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTelevision = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasTvFeature = context.packageManager.hasSystemFeature("android.hardware.type.television")

        val config = context.resources.configuration
        val isLandscapeLarge = config.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                config.screenWidthDp >= 600

        return isTelevision || hasLeanback || hasTvFeature || isLandscapeLarge
    }

    /**
     * 檢查是否為原生電視裝置
     */
    fun isTvDevice(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTelevision = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasTvFeature = context.packageManager.hasSystemFeature("android.hardware.type.television")
        return isTelevision || hasLeanback || hasTvFeature
    }
}
