package dev.lisfox.geyserlauncher.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR, DEBUG }
enum class RuntimePhase { STOPPED, STARTING, RUNNING, ERROR }

data class RuntimeLog(val id: Long, val text: String, val level: LogLevel)

object GeyserRuntime {
    private const val MAX_LOG_LINES = 1_500
    private val nextLogId = AtomicLong()
    private val ansiPattern = Regex("\\u001B(?:\\[[0-?]*[ -/]*[@-~]|\\][^\\u0007]*(?:\\u0007|\\u001B\\\\))")
    private val donePattern = Regex("(?i)(?:^|\\]\\s*|\\s)Done(?:\\s*\\(|[.!]|\\s|$)")
    private val _phase = MutableStateFlow(RuntimePhase.STOPPED)
    private val _logs = MutableStateFlow<List<RuntimeLog>>(emptyList())
    private val _lastMessage = MutableStateFlow("等待启动")
    private val _error = MutableStateFlow<String?>(null)

    val phase = _phase.asStateFlow()
    val logs = _logs.asStateFlow()
    val lastMessage = _lastMessage.asStateFlow()
    val error = _error.asStateFlow()

    @JvmStatic
    fun emit(line: String, processActive: Boolean) {
        val clean = ansiPattern.replace(line, "").trimEnd()
        if (clean.isNotBlank()) {
            _lastMessage.value = clean
            _logs.update { previous ->
                (previous + RuntimeLog(nextLogId.incrementAndGet(), clean, levelOf(clean)))
                    .takeLast(MAX_LOG_LINES)
            }
            when {
                isFailure(clean) -> {
                    _phase.value = RuntimePhase.ERROR
                    _error.value = clean
                }
                isStopped(clean) -> _phase.value = RuntimePhase.STOPPED
                processActive && donePattern.containsMatchIn(clean) -> _phase.value = RuntimePhase.RUNNING
                processActive && _phase.value != RuntimePhase.RUNNING -> _phase.value = RuntimePhase.STARTING
            }
        }
    }

    @JvmStatic
    fun beginStart() {
        _phase.value = RuntimePhase.STARTING
        _lastMessage.value = "正在启动 Geyser"
        _error.value = null
    }

    @JvmStatic
    fun markServiceStopped() {
        if (_phase.value != RuntimePhase.ERROR) _phase.value = RuntimePhase.STOPPED
    }

    fun consumeError() {
        _error.value = null
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun levelOf(line: String): LogLevel {
        val normalized = line.uppercase()
        return when {
            "ERROR" in normalized || "SEVERE" in normalized || "失败" in line -> LogLevel.ERROR
            "WARN" in normalized || "警告" in line -> LogLevel.WARNING
            "DEBUG" in normalized || "TRACE" in normalized -> LogLevel.DEBUG
            "STARTED" in normalized || "DONE" in normalized || "启动成功" in line || "已启动" in line -> LogLevel.SUCCESS
            else -> LogLevel.INFO
        }
    }

    private fun isFailure(line: String): Boolean =
        line.startsWith("启动失败:") || line.startsWith("Geyser 已退出，代码:")

    private fun isStopped(line: String): Boolean =
        line == "Geyser 已停止" || line == "Geyser 未运行"
}
