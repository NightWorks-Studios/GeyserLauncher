package dev.lisfox.geyserlauncher.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.lisfox.geyserlauncher.data.BrowserEntry
import dev.lisfox.geyserlauncher.data.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilesUiState(
    val currentPath: String = "",
    val entries: List<BrowserEntry> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

class FilesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application)
    private val _state = MutableStateFlow(FilesUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun openDirectory(relativePath: String) {
        _state.update { it.copy(currentPath = relativePath) }
        refresh()
    }

    fun navigateUp(): Boolean {
        if (state.value.currentPath.isBlank()) return false
        openDirectory(repository.parent(state.value.currentPath))
        return true
    }

    fun import(uri: Uri) = mutate("文件已导入") { repository.importFile(state.value.currentPath, uri) }

    fun rename(entry: BrowserEntry, newName: String) = mutate("已重命名") {
        repository.rename(entry.relativePath, newName)
    }

    fun delete(entry: BrowserEntry) = mutate("已删除 ${entry.name}") {
        repository.delete(entry.relativePath)
    }

    fun export(entry: BrowserEntry, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.export(entry.relativePath, uri) }
                .onSuccess { _state.update { it.copy(message = "已导出 ${entry.name}", error = null) } }
                .onFailure { failure -> _state.update { it.copy(error = failure.message ?: "导出失败") } }
        }
    }

    fun refresh() {
        val path = state.value.currentPath
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repository.list(path) }
                .onSuccess { entries -> _state.update { it.copy(loading = false, entries = entries) } }
                .onFailure { failure -> _state.update { it.copy(loading = false, error = failure.message ?: "目录读取失败") } }
        }
    }

    fun consumeNotice() {
        _state.update { it.copy(message = null, error = null) }
    }

    fun repository(): FileRepository = repository

    private fun mutate(message: String, action: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(action)
                .onSuccess {
                    val entries = repository.list(state.value.currentPath)
                    _state.update { it.copy(entries = entries, message = message, error = null) }
                }
                .onFailure { failure -> _state.update { it.copy(error = failure.message ?: "操作失败") } }
        }
    }
}
