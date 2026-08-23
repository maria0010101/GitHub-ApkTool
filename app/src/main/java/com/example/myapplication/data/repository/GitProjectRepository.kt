package com.example.myapplication.data.repository

import com.example.myapplication.data.local.GitProject
import com.example.myapplication.data.local.GitProjectDao
import com.example.myapplication.data.remote.GitHubApiService
import com.example.myapplication.data.remote.GitHubReleaseDto
import com.example.myapplication.data.remote.ReleaseAssetDto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class ProjectBackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val projects: List<ProjectExportItem> = emptyList()
)

data class ProjectExportItem(
    val name: String,
    val owner: String,
    val repo: String,
    val latestVersion: String? = null,
    val latestApkUrl: String? = null,
    val latestApkName: String? = null,
    val updatedAt: Long = 0
)

class GitProjectRepository(
    private val gitProjectDao: GitProjectDao,
    private val apiService: GitHubApiService
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

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

    suspend fun getAllProjectsList(): List<GitProject> = withContext(Dispatchers.IO) {
        gitProjectDao.getAllProjectsList()
    }

    suspend fun deleteAllProjects() = withContext(Dispatchers.IO) {
        gitProjectDao.deleteAllProjects()
    }

    suspend fun checkAndUpdateProject(project: GitProject, apiToken: String? = null): Result<GitProject> {
        return try {
            val fetchResult = fetchLatestReleaseInfo(project.owner, project.repo, apiToken)
            if (fetchResult.isSuccess) {
                val (release, apkAsset) = fetchResult.getOrThrow()
                val updatedProject = project.copy(
                    latestVersion = release.tagName,
                    latestApkUrl = apkAsset?.browserDownloadUrl ?: project.latestApkUrl,
                    latestApkName = apkAsset?.name ?: project.latestApkName,
                    updatedAt = System.currentTimeMillis()
                )
                updateProject(updatedProject)
                Result.success(updatedProject)
            } else {
                Result.failure(fetchResult.exceptionOrNull() ?: Exception("檢查更新失敗"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "網路連線異常"))
        }
    }

    suspend fun fetchLatestReleaseInfo(
        owner: String,
        repo: String,
        apiToken: String? = null
    ): Result<Pair<GitHubReleaseDto, ReleaseAssetDto?>> = withContext(Dispatchers.IO) {
        try {
            val authHeader = if (!apiToken.isNullOrBlank()) "Bearer $apiToken" else null

            // 1. 優先嘗試獲取最新正式發布版本 (/releases/latest)
            val latestResponse = try {
                apiService.getLatestRelease(owner, repo, authHeader)
            } catch (e: Exception) {
                null
            }

            if (latestResponse != null && latestResponse.isSuccessful && latestResponse.body() != null) {
                val release = latestResponse.body()!!
                val apkAsset = findBestApkAsset(release.assets)
                return@withContext Result.success(Pair(release, apkAsset))
            }

            // 2. 若 /releases/latest 返回 404（例如專案僅有 Pre-release 或尚未發布正式版），回退至查詢 Releases 列表 (/releases)
            val releasesResponse = apiService.getReleases(owner, repo, authHeader)
            if (releasesResponse.isSuccessful && !releasesResponse.body().isNullOrEmpty()) {
                val releases = releasesResponse.body()!!
                // 優先選擇非草稿的最新發布（包含 Pre-release，例如 v4.2.0-rc1）
                val release = releases.firstOrNull { !it.isDraft } ?: releases.first()
                val apkAsset = findBestApkAsset(release.assets)
                return@withContext Result.success(Pair(release, apkAsset))
            }

            // 3. 處理錯誤回應代碼
            val errCode = if (releasesResponse.code() != 200) releasesResponse.code() else (latestResponse?.code() ?: 404)
            val errorMsg = when (errCode) {
                404 -> "找不到 GitHub 儲存庫「$owner/$repo」或該專案尚無任何 Release 發佈"
                403 -> "GitHub API 速率限制，請在設定中填入 Personal Access Token"
                else -> "獲取 Release 失敗 (HTTP $errCode)"
            }
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "網路連線異常"))
        }
    }

    suspend fun exportProjectsJson(): String = withContext(Dispatchers.IO) {
        val projects = gitProjectDao.getAllProjectsList()
        val exportItems = projects.map {
            ProjectExportItem(
                name = it.name,
                owner = it.owner,
                repo = it.repo,
                latestVersion = it.latestVersion,
                latestApkUrl = it.latestApkUrl,
                latestApkName = it.latestApkName,
                updatedAt = it.updatedAt
            )
        }
        val backupData = ProjectBackupData(projects = exportItems)
        gson.toJson(backupData)
    }

    suspend fun importProjectsFromJson(jsonContent: String, overwrite: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = jsonContent.trim()
            if (content.isBlank()) {
                return@withContext Result.failure(Exception("匯入內容為空"))
            }

            val importItems = mutableListOf<ProjectExportItem>()

            // 嘗試解析 JSON 格式
            if (content.startsWith("{")) {
                val backupData = try {
                    gson.fromJson(content, ProjectBackupData::class.java)
                } catch (e: Exception) {
                    null
                }
                if (backupData != null && backupData.projects.isNotEmpty()) {
                    importItems.addAll(backupData.projects)
                }
            } else if (content.startsWith("[")) {
                val listType = object : TypeToken<List<ProjectExportItem>>() {}.type
                val list = try {
                    gson.fromJson<List<ProjectExportItem>>(content, listType)
                } catch (e: Exception) {
                    null
                }
                if (list != null && list.isNotEmpty()) {
                    importItems.addAll(list)
                }
            }

            // 若不是 JSON，則嘗試以行分隔解析 (支援 URL 或 owner/repo 清單)
            if (importItems.isEmpty()) {
                val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }
                for (line in lines) {
                    val (owner, repo) = parseLineOwnerAndRepo(line)
                    if (owner.isNotBlank() && repo.isNotBlank()) {
                        importItems.add(
                            ProjectExportItem(
                                name = repo,
                                owner = owner,
                                repo = repo,
                                latestVersion = "未取得",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            if (importItems.isEmpty()) {
                return@withContext Result.failure(Exception("未能識別出任何有效的 GitHub 專案資料"))
            }

            if (overwrite) {
                gitProjectDao.deleteAllProjects()
            }

            var count = 0
            for (item in importItems) {
                val owner = item.owner.trim()
                val repo = item.repo.trim().removeSuffix(".git")
                if (owner.isBlank() || repo.isBlank()) continue

                val existing = gitProjectDao.findProject(owner, repo)
                val project = GitProject(
                    id = existing?.id ?: 0,
                    name = item.name.ifBlank { repo },
                    owner = owner,
                    repo = repo,
                    latestVersion = item.latestVersion ?: existing?.latestVersion ?: "未取得",
                    latestApkUrl = item.latestApkUrl ?: existing?.latestApkUrl,
                    latestApkName = item.latestApkName ?: existing?.latestApkName,
                    updatedAt = if (item.updatedAt > 0) item.updatedAt else System.currentTimeMillis()
                )
                gitProjectDao.insertProject(project)
                count++
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(Exception("匯入解析失敗: ${e.message}"))
        }
    }

    private fun parseLineOwnerAndRepo(line: String): Pair<String, String> {
        var clean = line.trim().substringBefore("?").substringBefore("#")
        if (clean.contains("github.com/")) {
            clean = clean.substringAfter("github.com/").trim('/')
        }
        val parts = clean.split("/").filter { it.isNotBlank() }
        return if (parts.size >= 2) {
            Pair(parts[0], parts[1].removeSuffix(".git"))
        } else {
            Pair("", "")
        }
    }

    private fun findBestApkAsset(assets: List<ReleaseAssetDto>): ReleaseAssetDto? {
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        // 排除除錯版本 (優先考慮 release 版)
        val releaseApks = apkAssets.filter { !it.name.contains("debug", ignoreCase = true) }
        val pool = if (releaseApks.isNotEmpty()) releaseApks else apkAssets

        return pool.find { it.name.contains("arm64", ignoreCase = true) || it.name.contains("aarch64", ignoreCase = true) }
            ?: pool.find { it.name.contains("universal", ignoreCase = true) }
            ?: pool.find { it.name.contains("v7a", ignoreCase = true) || it.name.contains("armv7", ignoreCase = true) }
            ?: pool.firstOrNull()
            ?: apkAssets.first()
    }
}

