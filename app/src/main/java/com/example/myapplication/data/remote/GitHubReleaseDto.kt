package com.example.myapplication.data.remote

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("html_url") val htmlUrl: String? = null,
    @SerializedName("prerelease") val isPrerelease: Boolean = false,
    @SerializedName("draft") val isDraft: Boolean = false,
    @SerializedName("assets") val assets: List<ReleaseAssetDto> = emptyList()
)

data class ReleaseAssetDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val size: Long = 0,
    @SerializedName("download_count") val downloadCount: Long = 0,
    @SerializedName("browser_download_url") val browserDownloadUrl: String = "",
    @SerializedName("content_type") val contentType: String? = null
)

