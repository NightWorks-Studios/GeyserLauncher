package dev.lisfox.geyserlauncher.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lisfox.geyserlauncher.data.FileRepository
import dev.lisfox.geyserlauncher.ui.components.PageHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

@Composable
fun YamlEditorScreen(relativePath: String, repository: FileRepository, onBack: () -> Unit) {
    var value by remember(relativePath) { mutableStateOf("") }
    var savedValue by remember(relativePath) { mutableStateOf("") }
    var loading by remember(relativePath) { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var confirmBack by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val leave = { if (value != savedValue) confirmBack = true else onBack() }

    LaunchedEffect(relativePath) {
        runCatching { withContext(Dispatchers.IO) { repository.read(relativePath) } }
            .onSuccess { text -> value = text; savedValue = text }
            .onFailure { snackbar.showSnackbar(it.message ?: "文件读取失败") }
        loading = false
    }
    BackHandler(onBack = leave)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PageHeader(relativePath.substringAfterLast('/'), leave) {
                IconButton(
                    enabled = !loading && !saving && value != savedValue,
                    onClick = {
                        saving = true
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    repository.validateYaml(value)
                                    repository.write(relativePath, value)
                                }
                            }.onSuccess {
                                savedValue = value
                                snackbar.showSnackbar("已保存")
                            }.onFailure {
                                snackbar.showSnackbar("YAML 无效：${it.message ?: "无法解析"}")
                            }
                            saving = false
                        }
                    }
                ) {
                    if (saving) CircularProgressIndicator(Modifier.padding(10.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.Save, contentDescription = "保存文件")
                }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val vertical = rememberScrollState()
                val horizontal = rememberScrollState()
                Box(
                    Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)
                        .background(Color(0xFF171A22), RoundedCornerShape(8.dp)).padding(14.dp)
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.fillMaxSize().verticalScroll(vertical).horizontalScroll(horizontal),
                        textStyle = TextStyle(
                            color = Color(0xFFD9DEE9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        ),
                        visualTransformation = YamlSyntaxTransformation(),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF8EAAFF))
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).widthIn(max = 520.dp))
    }

    if (confirmBack) {
        AlertDialog(
            onDismissRequest = { confirmBack = false },
            title = { Text("放弃未保存的更改？") },
            confirmButton = { TextButton(onClick = onBack) { Text("放弃") } },
            dismissButton = { TextButton(onClick = { confirmBack = false }) { Text("继续编辑") } }
        )
    }
}

private class YamlSyntaxTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        style(builder, text.text, "(?m)^[ \\t-]*[A-Za-z][A-Za-z0-9_.-]*(?=\\s*:)", Color(0xFF7EB6FF))
        style(builder, text.text, "\\\"[^\\\"]*\\\"|'[^']*'", Color(0xFF99D69C))
        style(builder, text.text, "(?i)\\b(true|false|null|yes|no)\\b", Color(0xFFC9A7FF))
        style(builder, text.text, "(?<![A-Za-z0-9])-?[0-9]+(?:\\.[0-9]+)?(?![A-Za-z0-9])", Color(0xFFFFC27A))
        style(builder, text.text, "#[^\\n]*", Color(0xFF7F899B))
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun style(builder: AnnotatedString.Builder, text: String, regex: String, color: Color) {
        Pattern.compile(regex, Pattern.MULTILINE).matcher(text).run {
            while (find()) builder.addStyle(SpanStyle(color = color), start(), end())
        }
    }
}
