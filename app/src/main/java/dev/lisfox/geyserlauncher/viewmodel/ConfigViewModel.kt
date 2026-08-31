package dev.lisfox.geyserlauncher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.lisfox.geyserlauncher.data.ConfigRepository
import dev.lisfox.geyserlauncher.data.GEYSER_CONFIG_FIELDS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfigUiState(
    val loading: Boolean = true,
    val fileExists: Boolean = false,
    val values: Map<String, Any?> = GEYSER_CONFIG_FIELDS.associate { it.id to it.defaultValue },
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ConfigRepository(application)
    private val _state = MutableStateFlow(ConfigUiState())
    val state = _state.asStateFlow()

    init {
        reload()
    }

    fun update(fieldId: String, value: Any?) {
        _state.update { current -> current.copy(values = current.values + (fieldId to value), message = null, error = null) }
    }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repository.load() }
                .onSuccess { snapshot ->
                    _state.update { it.copy(loading = false, fileExists = snapshot.fileExists, values = snapshot.values) }
                }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, error = failure.message ?: "配置读取失败") }
                }
        }
    }

    fun save() {
        val values = state.value.values
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(saving = true, message = null, error = null) }
            runCatching { repository.save(values) }
                .onSuccess { snapshot ->
                    _state.update {
                        it.copy(
                            saving = false,
                            fileExists = true,
                            values = snapshot.values,
                            message = "配置已保存，重启 Geyser 后生效"
                        )
                    }
                }
                .onFailure { failure ->
                    _state.update { it.copy(saving = false, error = failure.message ?: "配置保存失败") }
                }
        }
    }

    fun consumeNotice() {
        _state.update { it.copy(message = null, error = null) }
    }
}
