package dev.lisfox.geyserlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lisfox.geyserlauncher.data.DistributionOperation
import dev.lisfox.geyserlauncher.data.GeyserDistributionState
import dev.lisfox.geyserlauncher.runtime.RuntimePhase
import dev.lisfox.geyserlauncher.ui.components.PageHeader
import dev.lisfox.geyserlauncher.ui.theme.Danger
import dev.lisfox.geyserlauncher.ui.theme.Success
import dev.lisfox.geyserlauncher.ui.theme.TextSecondary

@Composable
fun GeyserUpdateScreen(
    state: GeyserDistributionState,
    runtimePhase: RuntimePhase,
    onBack: () -> Unit,
    onCheck: () -> Unit,
    onUpdate: () -> Unit
) {
    val busy = state.operation != DistributionOperation.IDLE
    val running = runtimePhase == RuntimePhase.STARTING || runtimePhase == RuntimePhase.RUNNING
    val updateAvailable = state.latest != null &&
        (!state.installed || state.installedVersion != state.latest.displayVersion)

    Column(Modifier.fillMaxSize()) {
        PageHeader("Geyser 更新", onBack)
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            VersionRow("已安装版本", if (state.installed) state.installedVersion ?: "未知版本" else "未安装")
            HorizontalDivider(Modifier.padding(vertical = 15.dp))
            VersionRow("官方最新版本", state.latest?.displayVersion ?: "尚未检查")

            Spacer(Modifier.height(24.dp))
            when (state.operation) {
                DistributionOperation.CHECKING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp)
                    Text("正在检查最新版本", Modifier.padding(start = 12.dp), color = TextSecondary)
                }
                DistributionOperation.DOWNLOADING -> {
                    Text("正在下载 ${state.latest?.displayVersion.orEmpty()}", color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    if (state.progress != null) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${state.progress}%", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    } else {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
                DistributionOperation.IDLE -> {
                    state.error?.let { Text(it, color = Danger) }
                    if (state.error == null && state.status != null) {
                        Text(state.status, color = if (updateAvailable) TextSecondary else Success)
                    }
                    if (running) {
                        Text("停止 Geyser 后才能安装更新", color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCheck, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("检查更新", Modifier.padding(start = 8.dp))
                }
                Button(
                    onClick = onUpdate,
                    enabled = !busy && !running && updateAvailable,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Text(if (state.installed) "安装更新" else "安装", Modifier.padding(start = 8.dp))
                }
            }
            Text(
                "安装包从 GeyserMC 官方下载并使用 SHA-256 校验",
                modifier = Modifier.padding(top = 18.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun VersionRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = TextSecondary)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
