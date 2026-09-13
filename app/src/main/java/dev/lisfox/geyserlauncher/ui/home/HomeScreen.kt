package dev.lisfox.geyserlauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lisfox.geyserlauncher.runtime.LogLevel
import dev.lisfox.geyserlauncher.runtime.RuntimeLog
import dev.lisfox.geyserlauncher.runtime.RuntimePhase
import dev.lisfox.geyserlauncher.ui.theme.Danger
import dev.lisfox.geyserlauncher.ui.theme.Primary
import dev.lisfox.geyserlauncher.ui.theme.PrimarySoft
import dev.lisfox.geyserlauncher.ui.theme.Success
import dev.lisfox.geyserlauncher.ui.theme.TextSecondary

private val Starting = Color(0xFFC58B00)
private val StartingSoft = Color(0xFFFFF1C7)
private val RunningSoft = Color(0xFFDDF4E6)
private val ErrorSoft = Color(0xFFFFE7E9)

private data class HomeVisualState(
    val accent: Color,
    val halo: Color,
    val icon: ImageVector,
    val action: String,
    val description: String
)

@Composable
fun HomeScreen(
    phase: RuntimePhase,
    status: String,
    logs: List<RuntimeLog>,
    geyserVersion: String?,
    onToggle: () -> Unit
) {
    val visual = when (phase) {
        RuntimePhase.STOPPED -> HomeVisualState(Primary, PrimarySoft, Icons.Rounded.PowerSettingsNew, "点击启动", "准备就绪")
        RuntimePhase.STARTING -> HomeVisualState(Starting, StartingSoft, Icons.Rounded.HourglassTop, "启动中", status)
        RuntimePhase.RUNNING -> HomeVisualState(Success, RunningSoft, Icons.Rounded.PowerSettingsNew, "点击停止", "Geyser 正在运行")
        RuntimePhase.ERROR -> HomeVisualState(Danger, ErrorSoft, Icons.Rounded.PowerSettingsNew, "重新启动", "启动失败")
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.align(Alignment.Start), verticalAlignment = Alignment.CenterVertically) {
            Text("Geyser", fontSize = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
            Spacer(Modifier.width(10.dp))
            Surface(color = PrimarySoft, shape = RoundedCornerShape(7.dp)) {
                Text(
                    geyserVersion ?: "未安装",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    color = Primary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(250.dp).background(visual.halo, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(205.dp)
                    .shadow(
                        18.dp,
                        CircleShape,
                        ambientColor = visual.accent.copy(alpha = 0.24f),
                        spotColor = visual.accent.copy(alpha = 0.22f)
                    )
                    .background(Color.White, CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    visual.icon,
                    contentDescription = visual.action,
                    tint = visual.accent,
                    modifier = Modifier.size(78.dp)
                )
            }
        }
        Spacer(Modifier.size(28.dp))
        Text(
            visual.action,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.size(8.dp))
        if (phase != RuntimePhase.STARTING) {
            Text(
                visual.description,
                color = if (phase == RuntimePhase.STOPPED) TextSecondary else visual.accent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }
        AnimatedVisibility(
            visible = phase == RuntimePhase.STARTING,
            enter = fadeIn(tween(220)) + expandVertically(expandFrom = Alignment.Top, animationSpec = tween(320)),
            exit = fadeOut(tween(160)) + shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(260))
        ) {
            StartupLogPanel(logs)
        }
        Spacer(Modifier.weight(1f))
    }

}

@Composable
private fun StartupLogPanel(logs: List<RuntimeLog>) {
    val listState = rememberLazyListState()
    val visibleLogs = remember(logs) { logs.takeLast(80) }

    LaunchedEffect(visibleLogs.lastOrNull()?.id) {
        if (visibleLogs.isNotEmpty()) listState.animateScrollToItem(visibleLogs.lastIndex)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(166.dp)
            .padding(top = 18.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF171A22)
    ) {
        if (visibleLogs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "正在等待启动日志...",
                    modifier = Modifier.padding(14.dp),
                    color = Color(0xFF8D96A8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                state = listState
            ) {
                items(visibleLogs, key = RuntimeLog::id) { log ->
                    Text(
                        text = log.text,
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = startupLogColor(log.level),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

private fun startupLogColor(level: LogLevel): Color = when (level) {
    LogLevel.INFO -> Color(0xFFD6DCE8)
    LogLevel.SUCCESS -> Color(0xFF83D9A5)
    LogLevel.WARNING -> Color(0xFFFFCF78)
    LogLevel.ERROR -> Color(0xFFFF8E96)
    LogLevel.DEBUG -> Color(0xFFAFA7D8)
}
