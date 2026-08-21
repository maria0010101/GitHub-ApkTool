package com.example.myapplication.data.repository

import com.example.myapplication.data.local.GitProject
import com.example.myapplication.data.local.GitProjectDao
import com.example.myapplication.data.remote.GitHubApiService
import com.example.myapplication.data.remote.GitHubReleaseDto
import com.example.myapplication.data.remote.ReleaseAssetDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GitProjectRepository(
    private val gitProjectDao: GitProjectDao,
    private val apiService: GitHubApiService
) {
    val allProjects: Flow<List<GitProject>> = gitProjectDao.getAllProjects()

    suspend fun getProjectById(id: Long): GitProject? = withContext(Dispatchers.IO) {
        gitProjectDao.getProjectById(id)
    }

    suspend fun insertProject(project: GitProject): Long = withContext(Dispatchers.IO) {
        gitProjectDao.insertProject(project)
    }

    suspend fun updateProject(project: GitProject) = withContext(Dispatchers.IO) {
        gitProjectDao.updateProject(project)
    }

    suspend fun deleteProject(project: GitProject) = withContext(Dispatchers.IO) {
        gitProjectDao.deleteProject(project)
    }

    suspend fun deleteProjectById(id: Long) = withContext(Dispatchers.IO) {
        gitProjectDao.deleteProjectById(id)
    }

    suspend fun checkAndUpdateProject(project: GitProject, apiToken: String? = null): Result<GitProject> {
        return try {
            val authHeader = if (!apiToken.isNullOrBlank()) "Bearer $apiToken" else null
            val response = apiService.getLatestRelease(project.owner, project.repo, authHeader)

            if (response.isSuccessful && response.body() != null) {
                val release = response.body()!!
                val apkAsset = findBestApkAsset(release.assets)

                val updatedProject = project.copy(
                    latestVersion = release.tagName,
                    latestApkUrl = apkAsset?.browserDownloadUrl ?: project.latestApkUrl,
                    latestApkName = apkAsset?.name ?: project.latestApkName,
                    updatedAt = System.currentTimeMillis()
                )

                updateProject(updatedProject)
                Result.success(updatedProject)
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "找不到此 Repo 或目前無任何 Release"
                    403 -> "API 請求次數達上限，請設定 GitHub Token"
                    else -> "檢查更新失敗: HTTP ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "網路連線異常"))
        }
    }

    suspend fun fetchLatestReleaseInfo(owner: String, repo: String, apiToken: String? = null): Result<Pair<GitHubReleaseDto, ReleaseAssetDto?>> {
        return try {
            val authHeader = if (!apiToken.isNullOrBlank()) "Bearer $apiToken" else null
            val response = apiService.getLatestRelease(owner, repo, authHeader)

            if (response.isSuccessful && response.body() != null) {
                val release = response.body()!!
                val apkAsset = findBestApkAsset(release.assets)
                Result.success(Pair(release, apkAsset))
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "找不到 GitHub 儲存庫或無 Release"
                    403 -> "GitHub API 速率限制，請在設定中填入 Personal Access Token"
                    else -> "獲取失敗 (HTTP ${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "網路連線異常"))
        }
    }

    private fun findBestApkAsset(assets: List<ReleaseAssetDto>): ReleaseAssetDto? {
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        return apkAssets.find { it.name.contains("arm64", ignoreCase = true) }
            ?: apkAssets.find { it.name.contains("universal", ignoreCase = true) }
            ?: apkAssets.first()
    }
}
