package com.example.myapplication.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.GitProject
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.data.repository.GitProjectRepository
import com.example.myapplication.downloader.ApkDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class ProjectUiState(
    val isCheckingUpdates: Boolean = false,
    val isExportingOrImporting: Boolean = false,
    val searchQuery: String = "",
    val githubToken: String = "",
    val activeCheckingProjectIds: Set<Long> = emptySet()
)

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GitProjectRepository
    val apkDownloader: ApkDownloader = ApkDownloader(application)
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        ProjectUiState(
            githubToken = prefs.getString("github_token", "") ?: ""
        )
    )
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = GitProjectRepository(database.gitProjectDao(), RetrofitClient.apiService)
    }

    val projects: StateFlow<List<GitProject>> = repository.allProjects
        .combine(_uiState) { list, state ->
            if (state.searchQuery.isBlank()) {
                list
            } else {
                list.filter {
                    it.name.contains(state.searchQuery, ignoreCase = true) ||
                            it.owner.contains(state.searchQuery, ignoreCase = true) ||
                            it.repo.contains(state.searchQuery, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun saveGithubToken(token: String) {
        val trimmed = token.trim()
        prefs.edit().putString("github_token", trimmed).apply()
        _uiState.update { it.copy(githubToken = trimmed) }
        viewModelScope.launch {
            _messageEvent.emit("GitHub Token 設定已儲存")
        }
    }

    fun addOrUpdateProject(
        id: Long = 0,
        customName: String,
        inputOwnerOrUrl: String,
        inputRepo: String
    ) {
        viewModelScope.launch {
            val (owner, repo) = parseOwnerAndRepo(inputOwnerOrUrl, inputRepo)
            if (owner.isBlank() || repo.isBlank()) {
                _messageEvent.emit("請輸入有效的 Owner 與 Repo 名稱或 GitHub 網址")
                return@launch
            }

            val finalName = if (customName.isNotBlank()) customName.trim() else repo

            _uiState.update { it.copy(isCheckingUpdates = true) }
            val token = _uiState.value.githubToken.ifBlank { null }
            val fetchResult = repository.fetchLatestReleaseInfo(owner, repo, token)

            if (fetchResult.isSuccess) {
                val (release, apkAsset) = fetchResult.getOrThrow()
                val project = GitProject(
                    id = id,
                    name = finalName,
                    owner = owner,
                    repo = repo,
                    latestVersion = release.tagName,
                    latestApkUrl = apkAsset?.browserDownloadUrl,
                    latestApkName = apkAsset?.name ?: "${repo}_${release.tagName}.apk",
                    updatedAt = System.currentTimeMillis()
                )
                repository.insertProject(project)
                _messageEvent.emit("已成功儲存專案「$finalName」，版本：${release.tagName}")
            } else {
                // 若連線失敗但用戶仍想加入
                val project = GitProject(
                    id = id,
                    name = finalName,
                    owner = owner,
                    repo = repo,
                    latestVersion = "未取得",
                    latestApkUrl = null,
                    latestApkName = null,
                    updatedAt = System.currentTimeMillis()
                )
                repository.insertProject(project)
                _messageEvent.emit("已儲存專案，但檢查 Release 失敗：${fetchResult.exceptionOrNull()?.message}")
            }
            _uiState.update { it.copy(isCheckingUpdates = false) }
        }
    }

    fun checkProjectUpdate(project: GitProject) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeCheckingProjectIds = it.activeCheckingProjectIds + project.id) }
            val token = _uiState.value.githubToken.ifBlank { null }
            val result = repository.checkAndUpdateProject(project, token)

            result.fold(
                onSuccess = { updated ->
                    if (updated.latestVersion != project.latestVersion) {
                        _messageEvent.emit("「${project.name}」發現新版本：${updated.latestVersion}！")
                    } else {
                        _messageEvent.emit("「${project.name}」已是最新版本 (${updated.latestVersion})")
                    }
                },
                onFailure = { error ->
                    _messageEvent.emit("「${project.name}」檢查失敗: ${error.message}")
                }
            )

            _uiState.update { it.copy(activeCheckingProjectIds = it.activeCheckingProjectIds - project.id) }
        }
    }

    fun checkAllUpdates() {
        val currentList = projects.value
        if (currentList.isEmpty()) {
            viewModelScope.launch {
                _messageEvent.emit("目前尚無追蹤中的專案")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdates = true) }
            val token = _uiState.value.githubToken.ifBlank { null }
            var updatedCount = 0
            var failCount = 0

            for (project in currentList) {
                _uiState.update { it.copy(activeCheckingProjectIds = it.activeCheckingProjectIds + project.id) }
                val result = repository.checkAndUpdateProject(project, token)
                if (result.isSuccess) {
                    if (result.getOrNull()?.latestVersion != project.latestVersion) {
                        updatedCount++
                    }
                } else {
                    failCount++
                }
                _uiState.update { it.copy(activeCheckingProjectIds = it.activeCheckingProjectIds - project.id) }
            }

            _uiState.update { it.copy(isCheckingUpdates = false) }
            val msg = buildString {
                append("檢查完成！")
                if (updatedCount > 0) append(" 發現 $updatedCount 個專案有新版本。")
                if (failCount > 0) append(" $failCount 個專案檢查失敗。")
                if (updatedCount == 0 && failCount == 0) append(" 所有專案皆為最新版本。")
            }
            _messageEvent.emit(msg)
        }
    }

    fun deleteProject(project: GitProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            _messageEvent.emit("已刪除「${project.name}」")
        }
    }

    fun downloadApk(project: GitProject) {
        val url = project.latestApkUrl
        if (url.isNullOrBlank()) {
            viewModelScope.launch {
                _messageEvent.emit("「${project.name}」最新的 Release 中沒有找到可下載的 .apk 檔案")
            }
            return
        }

        val fileName = project.latestApkName ?: "${project.repo}_${project.latestVersion}.apk"
        apkDownloader.downloadApk(url, fileName, "${project.name} ${project.latestVersion}")
    }

    suspend fun getExportJsonString(): String {
        return repository.exportProjectsJson()
    }

    fun exportToFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingOrImporting = true) }
            try {
                val json = repository.exportProjectsJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(json)
                        }
                    }
                }
                _messageEvent.emit("追蹤清單已成功匯出至指定檔案！")
            } catch (e: Exception) {
                _messageEvent.emit("匯出失敗: ${e.message}")
            } finally {
                _uiState.update { it.copy(isExportingOrImporting = false) }
            }
        }
    }

    fun importFromFile(
        uri: Uri,
        context: Context,
        overwrite: Boolean = false,
        autoCheckUpdates: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingOrImporting = true) }
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    }
                }

                if (content.isNullOrBlank()) {
                    _messageEvent.emit("選取的檔案內容為空")
                    _uiState.update { it.copy(isExportingOrImporting = false) }
                    return@launch
                }

                val result = repository.importProjectsFromJson(content, overwrite)
                result.fold(
                    onSuccess = { count ->
                        _messageEvent.emit("成功匯入 $count 個專案！")
                        if (autoCheckUpdates && count > 0) {
                            checkAllUpdates()
                        }
                    },
                    onFailure = { error ->
                        _messageEvent.emit("匯入失敗: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _messageEvent.emit("讀取檔案失敗: ${e.message}")
            } finally {
                _uiState.update { it.copy(isExportingOrImporting = false) }
            }
        }
    }

    fun importFromText(
        text: String,
        overwrite: Boolean = false,
        autoCheckUpdates: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingOrImporting = true) }
            try {
                val result = repository.importProjectsFromJson(text, overwrite)
                result.fold(
                    onSuccess = { count ->
                        _messageEvent.emit("成功匯入 $count 個專案！")
                        if (autoCheckUpdates && count > 0) {
                            checkAllUpdates()
                        }
                    },
                    onFailure = { error ->
                        _messageEvent.emit("匯入失敗: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _messageEvent.emit("解析失敗: ${e.message}")
            } finally {
                _uiState.update { it.copy(isExportingOrImporting = false) }
            }
        }
    }

    private fun parseOwnerAndRepo(input1: String, input2: String): Pair<String, String> {
        var clean1 = input1.trim()
        val clean2 = input2.trim()

        if (clean1.startsWith("http://") || clean1.startsWith("https://") || clean1.contains("github.com")) {
            clean1 = clean1.substringBefore("?").substringBefore("#")
            val path = clean1.substringAfter("github.com/").trim('/')
            val parts = path.split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                return Pair(parts[0], parts[1].removeSuffix(".git"))
            }
        }

        if (clean1.contains("/") && clean2.isBlank()) {
            clean1 = clean1.substringBefore("?").substringBefore("#")
            val parts = clean1.split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                return Pair(parts[0], parts[1].removeSuffix(".git"))
            }
        }

        return Pair(clean1, clean2.removeSuffix(".git"))
    }
}

