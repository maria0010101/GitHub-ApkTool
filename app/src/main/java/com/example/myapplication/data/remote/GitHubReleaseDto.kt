package com.example.myapplication.data.remote

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("html_url") val htmlUrl: String?,
    @SerializedName("prerelease") val isPrerelease: Boolean = false,
    @SerializedName("assets") val assets: List<ReleaseAssetDto> = emptyList()
)

data class ReleaseAssetDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long,
    @SerializedName("download_count") val downloadCount: Long,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
    @SerializedName("content_type") val contentType: String?
)
