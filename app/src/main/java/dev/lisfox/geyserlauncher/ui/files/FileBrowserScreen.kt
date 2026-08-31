package dev.lisfox.geyserlauncher.ui.files

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lisfox.geyserlauncher.data.BrowserEntry
import dev.lisfox.geyserlauncher.ui.components.PageHeader
import dev.lisfox.geyserlauncher.ui.theme.Danger
import dev.lisfox.geyserlauncher.ui.theme.Divider
import dev.lisfox.geyserlauncher.ui.theme.Primary
import dev.lisfox.geyserlauncher.ui.theme.TextSecondary
import dev.lisfox.geyserlauncher.viewmodel.FilesViewModel

@Composable
fun FileBrowserScreen(viewModel: FilesViewModel, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var renameEntry by remember { mutableStateOf<BrowserEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<BrowserEntry?>(null) }
    var exportEntry by remember { mutableStateOf<BrowserEntry?>(null) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }
    val exporter = rememberLauncherForActivityResult(ExportDocumentContract()) { uri ->
        val entry = exportEntry
        exportEntry = null
        if (uri != null && entry != null) viewModel.export(entry, uri)
    }
    val navigateBack = {
        if (!viewModel.navigateUp()) onBack()
    }

    BackHandler(enabled = state.currentPath.isNotBlank()) { viewModel.navigateUp() }
    LaunchedEffect(state.message, state.error) {
        val notice = state.error ?: state.message
        if (notice != null) {
            snackbar.showSnackbar(notice)
            viewModel.consumeNotice()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PageHeader(if (state.currentPath.isBlank()) "文件" else state.currentPath.substringAfterLast('/'), navigateBack) {
                IconButton(onClick = { importer.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = "导入文件")
                }
            }
            Text(
                text = if (state.currentPath.isBlank()) "/geyser" else "/geyser/${state.currentPath}",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (state.entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("目录为空", color = TextSecondary)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(top = 10.dp)) {
                    items(state.entries, key = BrowserEntry::relativePath) { entry ->
                        BrowserEntryRow(
                            entry = entry,
                            onOpen = {
                                if (entry.isDirectory) viewModel.openDirectory(entry.relativePath)
                                else if (entry.name.substringAfterLast('.', "").lowercase() in setOf("yml", "yaml")) onEdit(entry.relativePath)
                            },
                            onRename = { renameEntry = entry },
                            onExport = {
                                exportEntry = entry
                                exporter.launch(
                                    ExportDocumentRequest(
                                        fileName = if (entry.isDirectory) "${entry.name}.zip" else entry.name,
                                        mimeType = if (entry.isDirectory) "application/zip" else mimeType(entry.name)
                                    )
                                )
                            },
                            onDelete = { deleteEntry = entry }
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    renameEntry?.let { entry ->
        RenameDialog(entry.name, onDismiss = { renameEntry = null }) { name ->
            viewModel.rename(entry, name)
            renameEntry = null
        }
    }
    deleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteEntry = null },
            title = { Text(if (entry.isDirectory) "删除目录？" else "删除文件？") },
            text = { Text(entry.name) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(entry); deleteEntry = null }) {
                    Text("删除", color = Danger)
                }
            },
            dismissButton = { TextButton(onClick = { deleteEntry = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun BrowserEntryRow(
    entry: BrowserEntry,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val editable = entry.name.substringAfterLast('.', "").lowercase() in setOf("yml", "yaml")
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = entry.isDirectory || editable, onClick = onOpen)
            .padding(start = 24.dp, end = 12.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                entry.isDirectory -> Icons.Rounded.Folder
                editable -> Icons.Rounded.Code
                else -> Icons.AutoMirrored.Rounded.InsertDriveFile
            },
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = if (entry.isDirectory || editable) Primary else TextSecondary
        )
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                if (entry.isDirectory) "目录" else formatBytes(entry.size),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "文件操作")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("重命名") }, onClick = { menuOpen = false; onRename() })
                DropdownMenuItem(text = { Text("导出") }, onClick = { menuOpen = false; onExport() })
                DropdownMenuItem(text = { Text("删除", color = Danger) }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 70.dp), color = Divider)
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("文件名") }) },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
}

private data class ExportDocumentRequest(val fileName: String, val mimeType: String)

private class ExportDocumentContract : ActivityResultContract<ExportDocumentRequest, Uri?>() {
    override fun createIntent(context: Context, input: ExportDocumentRequest): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

private fun mimeType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "yml", "yaml" -> "application/yaml"
    "json" -> "application/json"
    "txt", "log", "properties" -> "text/plain"
    "jar" -> "application/java-archive"
    "zip" -> "application/zip"
    else -> "application/octet-stream"
}
