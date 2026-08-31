package dev.lisfox.geyserlauncher.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lisfox.geyserlauncher.runtime.JvmArguments
import dev.lisfox.geyserlauncher.ui.components.PageHeader
import dev.lisfox.geyserlauncher.ui.theme.TextSecondary

@Composable
fun JvmSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var arguments by rememberSaveable { mutableStateOf(JvmArguments.loadText(context)) }

    Column(Modifier.fillMaxSize()) {
        PageHeader("JVM 启动参数", onBack) {
            IconButton(onClick = {
                runCatching { JvmArguments.saveText(context, arguments) }
                    .onSuccess {
                        arguments = JvmArguments.loadText(context)
                        Toast.makeText(context, "JVM 参数已保存", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { Toast.makeText(context, it.message ?: "保存失败", Toast.LENGTH_LONG).show() }
            }) {
                Icon(Icons.Rounded.Save, contentDescription = "保存 JVM 参数")
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
        ) {
            Text("自定义参数", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "每行一个参数，将在下次启动 Geyser 时生效",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp)
            )
            OutlinedTextField(
                value = arguments,
                onValueChange = { arguments = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                placeholder = { Text("-Xms256M\n-Xmx1G", fontFamily = FontFamily.Monospace) }
            )
            Text(
                "内置参数",
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                (JvmArguments.REQUIRED + "-Djava.io.tmpdir=<应用临时目录>" + "-Djna.tmpdir=<应用临时目录>")
                    .joinToString("\n"),
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
