package dev.lisfox.geyserlauncher.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lisfox.geyserlauncher.runtime.LogLevel
import dev.lisfox.geyserlauncher.runtime.RuntimeLog
import dev.lisfox.geyserlauncher.runtime.RuntimePhase
import dev.lisfox.geyserlauncher.ui.components.PageHeader
import android.widget.Toast

@Composable
fun LogsScreen(
    logs: List<RuntimeLog>,
    phase: RuntimePhase,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val allLogs = remember(logs) { logs.joinToString("\n", transform = RuntimeLog::text) }
    var command by remember { mutableStateOf("") }
    val commandEnabled = phase == RuntimePhase.STARTING || phase == RuntimePhase.RUNNING
    val submitCommand = {
        val value = command.trim()
        if (value.isNotEmpty() && commandEnabled) {
            onSendCommand(value)
            command = ""
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.scrollToItem(logs.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader("运行日志", onBack) {
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(allLogs))
                Toast.makeText(context, "日志已复制", Toast.LENGTH_SHORT).show()
            }, enabled = logs.isNotEmpty()) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "复制全部日志")
            }
            IconButton(onClick = onClear, enabled = logs.isNotEmpty()) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "清空日志")
            }
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)
                .background(Color(0xFF171A22), RoundedCornerShape(8.dp))
        ) {
            if (logs.isEmpty()) {
                Text(
                    "暂无日志",
                    modifier = Modifier.padding(18.dp),
                    color = Color(0xFF8D96A8),
                    fontFamily = FontFamily.Monospace
                )
            } else {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                        state = listState
                    ) {
                        items(logs, key = RuntimeLog::id) { log -> LogRow(log) }
                    }
                }
            }
        }
        OutlinedTextField(
            value = command,
            onValueChange = { command = it.replace("\n", "") },
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            enabled = commandEnabled,
            singleLine = true,
            placeholder = { Text(if (commandEnabled) "输入控制台指令" else "Geyser 未运行") },
            trailingIcon = {
                IconButton(onClick = submitCommand, enabled = commandEnabled && command.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "发送指令")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submitCommand() })
        )
    }
}

@Composable
private fun LogRow(log: RuntimeLog) {
    val color = when (log.level) {
        LogLevel.INFO -> Color(0xFFD6DCE8)
        LogLevel.SUCCESS -> Color(0xFF83D9A5)
        LogLevel.WARNING -> Color(0xFFFFCF78)
        LogLevel.ERROR -> Color(0xFFFF8E96)
        LogLevel.DEBUG -> Color(0xFFAFA7D8)
    }
    val annotated = buildAnnotatedString {
        append(log.text)
        addStyle(SpanStyle(color = color), 0, length)
        Regex("https?://\\S+").findAll(log.text).forEach { match ->
            addStyle(SpanStyle(color = Color(0xFF79B8FF)), match.range.first, match.range.last + 1)
        }
        Regex("(?i)\\b(error|warn(?:ing)?|debug|info|done|started)\\b").findAll(log.text).forEach { match ->
            addStyle(SpanStyle(color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), match.range.first, match.range.last + 1)
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            annotated,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}
