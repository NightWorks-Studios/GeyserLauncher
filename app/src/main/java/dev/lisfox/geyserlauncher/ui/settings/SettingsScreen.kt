package dev.lisfox.geyserlauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lisfox.geyserlauncher.data.ConfigCategory
import dev.lisfox.geyserlauncher.ui.components.NavigationRow
import dev.lisfox.geyserlauncher.ui.components.PageHeader
import dev.lisfox.geyserlauncher.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onCategory: (ConfigCategory) -> Unit,
    onFiles: () -> Unit,
    onRawConfig: () -> Unit,
    onLogs: () -> Unit,
    onJvm: () -> Unit,
    onGeyserUpdate: () -> Unit,
    onBattery: () -> Unit,
    onAbout: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("设置")
        Text(
            "GEYSER",
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        NavigationRow(Icons.Rounded.Router, "基岩版监听", "地址、端口与 Bedrock 网络参数") { onCategory(ConfigCategory.BEDROCK) }
        NavigationRow(Icons.Rounded.Dns, "Java 服务器", "地址、端口、认证与代理转发") { onCategory(ConfigCategory.JAVA) }
        NavigationRow(Icons.Rounded.Language, "服务器信息", "MOTD、人数与 Ping 转发") { onCategory(ConfigCategory.MOTD) }
        NavigationRow(Icons.Rounded.Gamepad, "游戏体验", "坐标、命令、资源包与兼容选项") { onCategory(ConfigCategory.GAMEPLAY) }
        NavigationRow(Icons.Rounded.SettingsEthernet, "通用", "语言、登录、更新与诊断") { onCategory(ConfigCategory.GENERAL) }
        NavigationRow(Icons.Rounded.Memory, "高级选项", "缓存、压缩、代理与资源包") { onCategory(ConfigCategory.ADVANCED) }

        Text(
            "工具",
            modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 4.dp),
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        NavigationRow(Icons.Rounded.Folder, "文件管理", "浏览、导入、导出、重命名和编辑文件", onClick = onFiles)
        NavigationRow(Icons.Rounded.Code, "完整配置文件", "直接编辑 config.yml 并进行语法校验", onClick = onRawConfig)
        NavigationRow(Icons.AutoMirrored.Rounded.Article, "运行日志", "查看、复制日志并发送控制台指令", onClick = onLogs)
        NavigationRow(Icons.Rounded.SystemUpdate, "Geyser 更新", "检查并安装 Geyser 官方最新版本", onClick = onGeyserUpdate)
        NavigationRow(Icons.Rounded.Memory, "JVM 启动参数", "设置内存与其他 Java 虚拟机参数", onClick = onJvm)
        NavigationRow(Icons.Rounded.Security, "忽略电池优化", "提高后台持续运行稳定性", onClick = onBattery)
        NavigationRow(Icons.Rounded.Info, "关于", "Geyser Launcher 1.0", onClick = onAbout)
    }
}
