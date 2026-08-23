package com.example.myapplication.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ImportExportDialog(
    projectCount: Int,
    onDismiss: () -> Unit,
    onExportFile: () -> Unit,
    onGetExportJson: suspend () -> String,
    onImportFile: (overwrite: Boolean, autoCheck: Boolean) -> Unit,
    onImportText: (text: String, overwrite: Boolean, autoCheck: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 匯出, 1: 匯入

    // 匯入設定
    var isOverwrite by remember { mutableStateOf(false) }
    var autoCheckUpdates by remember { mutableStateOf(true) }
    var importTextContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "追蹤清單 匯出與匯入",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                androidx.compose.material3.PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("匯出備份") },
                        icon = { Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("匯入清單") },
                        icon = { Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // ================= 匯出分頁 =================
                    Text(
                        text = "目前共有 $projectCount 個追蹤中的專案。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 匯出至檔案
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📁 匯出為 JSON 備份檔案",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "將追蹤清單儲存至手機儲存空間或雲端硬碟，以供隨時還原。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onExportFile()
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = projectCount > 0
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("選擇位置儲存檔案")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 複製至剪貼簿
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📋 複製到剪貼簿",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "將追蹤清單 JSON 複製至剪貼簿，方便快速轉貼分享。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val json = onGetExportJson()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("GitHub ApkTool Projects", json)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "追蹤清單 JSON 已複製到剪貼簿", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = projectCount > 0
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("複製 JSON 文字")
                            }
                        }
                    }
                } else {
                    // ================= 匯入分頁 =================
                    Text(
                        text = "支援匯入 JSON 備份檔案，或直接貼上一行一個的 GitHub 網址 / owner/repo 清單。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 匯入模式選擇
                    Text(
                        text = "匯入方式：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isOverwrite,
                            onClick = { isOverwrite = false }
                        )
                        Text(
                            text = "合併至現有清單",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { isOverwrite = false }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        RadioButton(
                            selected = isOverwrite,
                            onClick = { isOverwrite = true }
                        )
                        Text(
                            text = "覆蓋現有清單",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { isOverwrite = true }
                        )
                    }

                    if (isOverwrite) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "注意：現有所有追蹤項目將會被清空！",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoCheckUpdates,
                            onCheckedChange = { autoCheckUpdates = it }
                        )
                        Text(
                            text = "匯入完成後自動檢查最新版本",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { autoCheckUpdates = !autoCheckUpdates }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // 選取檔案匯入
                    Button(
                        onClick = {
                            onImportFile(isOverwrite, autoCheckUpdates)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("選取 JSON 檔案匯入")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 文字貼上匯入
                    OutlinedTextField(
                        value = importTextContent,
                        onValueChange = { importTextContent = it },
                        label = { Text("或直接貼上 JSON / 網址清單") },
                        placeholder = { Text("貼上備份 JSON 或:\nhttps://github.com/owner/repo\nowner2/repo2") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val item = clipboard.primaryClip?.getItemAt(0)
                                val pasted = item?.text?.toString() ?: ""
                                if (pasted.isNotBlank()) {
                                    importTextContent = pasted
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "從剪貼簿貼上")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            if (importTextContent.isNotBlank()) {
                                onImportText(importTextContent, isOverwrite, autoCheckUpdates)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = importTextContent.isNotBlank()
                    ) {
                        Text("匯入上方輸入之文字內容")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉")
            }
        }
    )
}
