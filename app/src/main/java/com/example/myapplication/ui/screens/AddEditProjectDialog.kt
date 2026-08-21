package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.GitProject

@Composable
fun AddEditProjectDialog(
    projectToEdit: GitProject? = null,
    onDismiss: () -> Unit,
    onConfirm: (id: Long, name: String, ownerOrUrl: String, repo: String) -> Unit
) {
    var ownerOrUrl by remember {
        mutableStateOf(
            if (projectToEdit != null) "${projectToEdit.owner}/${projectToEdit.repo}" else ""
        )
    }
    var customName by remember { mutableStateOf(projectToEdit?.name ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (projectToEdit == null) "新增 GitHub 專案追蹤" else "編輯專案")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "支援直接貼上 GitHub 網址 (如 https://github.com/owner/repo) 或輸入「擁有者/專案名」",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ownerOrUrl,
                    onValueChange = {
                        ownerOrUrl = it
                        errorMessage = null
                    },
                    label = { Text("GitHub 網址或 owner/repo") },
                    placeholder = { Text("例如: JunkFood02/Seal") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("自訂顯示名稱 (選填)") },
                    placeholder = { Text("預設使用 Repo 名稱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (projectToEdit == null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "常用熱門範例 (點擊代入):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SuggestionChip(
                            onClick = {
                                ownerOrUrl = "JunkFood02/Seal"
                                customName = "Seal (影片下載器)"
                            },
                            label = { Text("Seal") }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SuggestionChip(
                            onClick = {
                                ownerOrUrl = "TeamNewPipe/NewPipe"
                                customName = "NewPipe"
                            },
                            label = { Text("NewPipe") }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SuggestionChip(
                            onClick = {
                                ownerOrUrl = "LawnchairLauncher/lawnchair"
                                customName = "Lawnchair"
                            },
                            label = { Text("Lawnchair") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ownerOrUrl.isBlank()) {
                        errorMessage = "請輸入 GitHub 網址或 owner/repo"
                        return@Button
                    }
                    onConfirm(
                        projectToEdit?.id ?: 0,
                        customName,
                        ownerOrUrl,
                        ""
                    )
                }
            ) {
                Text("儲存並檢查")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
