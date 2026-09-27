package com.example.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.GitProject
import com.example.myapplication.ui.viewmodel.ProjectViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvProjectListScreen(viewModel: ProjectViewModel) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<GitProject?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    var pendingImportOverwrite by remember { mutableStateOf(false) }
    var pendingImportAutoCheck by remember { mutableStateOf(true) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportToFile(it, context) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importFromFile(
                uri = it,
                context = context,
                overwrite = pendingImportOverwrite,
                autoCheckUpdates = pendingImportAutoCheck
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messageEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // 電視遙控器返回鍵行為：若搜尋展開則關閉搜尋
    BackHandler(enabled = isSearchExpanded) {
        isSearchExpanded = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // ================= 電視專用頂部導航欄 =================
            TvHeaderBar(
                projectCount = projects.size,
                isCheckingUpdates = uiState.isCheckingUpdates,
                isExporting = uiState.isExportingOrImporting,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                onAddClick = {
                    projectToEdit = null
                    showAddDialog = true
                },
                onCheckAll = { viewModel.checkAllUpdates() },
                onImportExportClick = { showImportExportDialog = true },
                onSwitchToMobile = { viewModel.saveLayoutMode("mobile") },
                onSettingsClick = { showSettingsDialog = true }
            )

            // 搜尋列 (TV 遙控器友善展開)
            AnimatedVisibility(visible = isSearchExpanded) {
                var searchFieldFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("搜尋專案名稱、Owner 或 Repo...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .onFocusChanged { searchFieldFocused = it.isFocused }
                        .border(
                            width = if (searchFieldFocused) 2.dp else 0.dp,
                            color = if (searchFieldFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "清除搜尋")
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 專案網格清單或空狀態 =================
            if (projects.isEmpty()) {
                TvEmptyStateView(
                    onAddClick = {
                        projectToEdit = null
                        showAddDialog = true
                    },
                    onQuickAdd = { name, repo ->
                        viewModel.addOrUpdateProject(
                            id = 0,
                            customName = name,
                            inputOwnerOrUrl = repo,
                            inputRepo = ""
                        )
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 380.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(projects, key = { it.id }) { project ->
                        val isChecking = uiState.activeCheckingProjectIds.contains(project.id)
                        TvProjectCard(
                            project = project,
                            isChecking = isChecking,
                            onDownload = { viewModel.downloadApk(project) },
                            onCheckUpdate = { viewModel.checkProjectUpdate(project) },
                            onEdit = {
                                projectToEdit = project
                                showAddDialog = true
                            },
                            onDelete = { viewModel.deleteProject(project) },
                            onOpenGitHub = {
                                val url = "https://github.com/${project.owner}/${project.repo}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            onInstallApk = {
                                project.latestApkName?.let { fileName ->
                                    viewModel.apkDownloader.installDownloadedApk(fileName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditProjectDialog(
            projectToEdit = projectToEdit,
            onDismiss = {
                showAddDialog = false
                projectToEdit = null
            },
            onConfirm = { id, name, ownerOrUrl, repo ->
                viewModel.addOrUpdateProject(id, name, ownerOrUrl, repo)
                showAddDialog = false
                projectToEdit = null
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentToken = uiState.githubToken,
            currentLayoutMode = uiState.layoutMode,
            onDismiss = { showSettingsDialog = false },
            onSaveToken = { token ->
                viewModel.saveGithubToken(token)
            },
            onSaveLayoutMode = { mode ->
                viewModel.saveLayoutMode(mode)
            }
        )
    }

    if (showImportExportDialog) {
        ImportExportDialog(
            projectCount = projects.size,
            onDismiss = { showImportExportDialog = false },
            onExportFile = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportLauncher.launch("github_apktool_backup_$timestamp.json")
            },
            onGetExportJson = {
                viewModel.getExportJsonString()
            },
            onImportFile = { overwrite, autoCheck ->
                pendingImportOverwrite = overwrite
                pendingImportAutoCheck = autoCheck
                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            onImportText = { text, overwrite, autoCheck ->
                viewModel.importFromText(text, overwrite, autoCheck)
            }
        )
    }
}

/**
 * Android TV 頂部寬版控制導航欄
 */
@Composable
fun TvHeaderBar(
    projectCount: Int,
    isCheckingUpdates: Boolean,
    isExporting: Boolean,
    isSearchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    onAddClick: () -> Unit,
    onCheckAll: () -> Unit,
    onImportExportClick: () -> Unit,
    onSwitchToMobile: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App 標題與電視專屬徽章
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Android TV",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GitHub ApkTool",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Android TV",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = "已追蹤 $projectCount 個開源專案",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 遙控器導航按鈕群
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvNavButton(
                icon = Icons.Default.Add,
                label = "新增專案",
                onClick = onAddClick,
                isPrimary = true
            )

            TvNavButton(
                icon = Icons.Default.Refresh,
                label = if (isCheckingUpdates) "檢查中..." else "檢查全部",
                onClick = onCheckAll,
                isLoading = isCheckingUpdates
            )

            TvNavButton(
                icon = Icons.Default.ImportExport,
                label = "備份 / 匯入",
                onClick = onImportExportClick,
                isLoading = isExporting
            )

            TvNavButton(
                icon = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                label = if (isSearchExpanded) "關閉搜尋" else "搜尋",
                onClick = onToggleSearch
            )

            TvNavButton(
                icon = Icons.Default.PhoneAndroid,
                label = "手機版",
                onClick = onSwitchToMobile
            )

            TvNavButton(
                icon = Icons.Default.Settings,
                label = "設定",
                onClick = onSettingsClick
            )
        }
    }
}

/**
 * 具備 D-pad 焦點動畫與發光效果的 TV 導航按鈕
 */
@Composable
fun TvNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    isLoading: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(120),
        label = "navScale"
    )

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        color = when {
            isFocused -> if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shadowElevation = if (isFocused) 8.dp else 1.dp,
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = if (isFocused && isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isFocused && isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 專為電視 10 呎觀看距離設計的專案卡片，具備遙控器 D-pad 焦點反饋
 */
@Composable
fun TvProjectCard(
    project: GitProject,
    isChecking: Boolean,
    onDownload: () -> Unit,
    onCheckUpdate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenGitHub: () -> Unit,
    onInstallApk: () -> Unit
) {
    var isCardFocused by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val formattedCheckTime = remember(project.updatedAt) {
        if (project.updatedAt > 0) dateFormat.format(Date(project.updatedAt)) else "未檢測"
    }
    val formattedReleaseTime = remember(project.releaseTime) {
        if (project.releaseTime > 0) dateFormat.format(Date(project.releaseTime)) else "未知"
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isCardFocused) 1.02f else 1.0f,
        animationSpec = tween(120),
        label = "cardScale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCardFocused) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCardFocused) 10.dp else 2.dp
        ),
        border = BorderStroke(
            width = if (isCardFocused) 2.5.dp else 1.dp,
            color = if (isCardFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .onFocusChanged { isCardFocused = it.isFocused }
            .focusable()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 頂部：專案名稱與選單
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${project.owner}/${project.repo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    TvFocusableIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "更多選項",
                        onClick = { menuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("在 GitHub 開啟") },
                            leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenGitHub()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("安裝已下載 APK") },
                            leadingIcon = { Icon(Icons.Outlined.InstallMobile, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onInstallApk()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("編輯專案") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("刪除專案", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 版本與時間標籤欄位
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = project.latestVersion.ifBlank { "未知版本" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "更新: $formattedReleaseTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "檢測: $formattedCheckTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // APK 檔名
            if (project.latestApkName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📦 ${project.latestApkName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 卡片底部動作按鈕列
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TvFocusableIconButton(
                        icon = Icons.Default.Refresh,
                        contentDescription = "檢查更新",
                        onClick = onCheckUpdate,
                        isLoading = isChecking
                    )

                    TvFocusableIconButton(
                        icon = Icons.Outlined.InstallMobile,
                        contentDescription = "安裝已下載 APK",
                        onClick = onInstallApk
                    )
                }

                val hasApk = !project.latestApkUrl.isNullOrBlank()
                var downloadBtnFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onDownload,
                    enabled = hasApk,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (downloadBtnFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    ),
                    border = BorderStroke(
                        width = if (downloadBtnFocused) 2.5.dp else 0.dp,
                        color = if (downloadBtnFocused) Color.White else Color.Transparent
                    ),
                    modifier = Modifier
                        .onFocusChanged { downloadBtnFocused = it.isFocused }
                        .focusable()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasApk) "下載 APK" else "無 APK",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 遙控器導航友善的圖示按鈕
 */
@Composable
fun TvFocusableIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = Modifier
            .size(38.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Android TV 專屬空狀態畫面：支援一鍵點選加入熱門開源 TV 應用程式
 */
@Composable
fun TvEmptyStateView(
    onAddClick: () -> Unit,
    onQuickAdd: (name: String, repo: String) -> Unit
) {
    val recommendations = listOf(
        Pair("SmartTube", "yuliskov/SmartTube"),
        Pair("ReSukiSU", "ReSukiSU/ReSukiSU"),
        Pair("Seal", "JunkFood02/Seal"),
        Pair("NewPipe", "TeamNewPipe/NewPipe"),
        Pair("Kodi", "xbmc/xbmc"),
        Pair("GitHub-ApkTool", "maria0010101/GitHub-ApkTool")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "歡迎使用 GitHub ApkTool 電視版",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "電視遙控器打字不易？點擊下方推薦的熱門開源 APK 專案，即刻一鍵加入追蹤！",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 熱門推薦快速加入晶片列表
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                recommendations.forEach { (name, repo) ->
                    var chipFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { onQuickAdd(name, repo) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (chipFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            width = if (chipFocused) 2.5.dp else 1.dp,
                            color = if (chipFocused) Color.White else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shadowElevation = if (chipFocused) 6.dp else 1.dp,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .onFocusChanged { chipFocused = it.isFocused }
                            .focusable()
                    ) {
                        Text(
                            text = "+ $name",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (chipFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            var addBtnFocused by remember { mutableStateOf(false) }
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (addBtnFocused) 2.5.dp else 0.dp,
                    color = if (addBtnFocused) Color.White else Color.Transparent
                ),
                modifier = Modifier
                    .onFocusChanged { addBtnFocused = it.isFocused }
                    .focusable()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("手動輸入 GitHub 專案網址", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
