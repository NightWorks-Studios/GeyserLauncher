package dev.lisfox.geyserlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lisfox.geyserlauncher.ui.components.PageHeader
import dev.lisfox.geyserlauncher.ui.theme.Primary
import dev.lisfox.geyserlauncher.ui.theme.TextSecondary

@Composable
fun AboutScreen(geyserVersion: String?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("关于", onBack)
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = Primary, modifier = Modifier.size(76.dp))
            Spacer(Modifier.size(18.dp))
            Text("Geyser Launcher", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("版本 1.0", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(10.dp))
            Text("Geyser ${geyserVersion ?: "未安装"}", color = TextSecondary)
            Text("JRE 25 · ARM64", color = TextSecondary)
        }
    }
}
