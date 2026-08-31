package dev.lisfox.geyserlauncher.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lisfox.geyserlauncher.data.ConfigCategory
import dev.lisfox.geyserlauncher.data.ConfigField
import dev.lisfox.geyserlauncher.data.ConfigFieldType
import dev.lisfox.geyserlauncher.data.GEYSER_CONFIG_FIELDS
import dev.lisfox.geyserlauncher.ui.components.PageHeader
import dev.lisfox.geyserlauncher.ui.theme.Divider
import dev.lisfox.geyserlauncher.ui.theme.TextSecondary
import dev.lisfox.geyserlauncher.viewmodel.ConfigViewModel

@Composable
fun ConfigCategoryScreen(category: ConfigCategory, viewModel: ConfigViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val notice = state.error ?: state.message

    LaunchedEffect(notice) {
        if (notice != null) {
            snackbar.showSnackbar(notice)
            viewModel.consumeNotice()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PageHeader(category.title, onBack = onBack) {
                IconButton(onClick = viewModel::save, enabled = !state.loading && !state.saving) {
                    if (state.saving) {
                        CircularProgressIndicator(Modifier.padding(10.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = "保存配置")
                    }
                }
            }
            Text(
                category.subtitle,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!state.fileExists && !state.loading) {
                Text(
                    "配置尚未生成，保存后将创建 config.yml。",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val fields = GEYSER_CONFIG_FIELDS.filter { it.category == category }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(fields, key = ConfigField::id) { field ->
                        ConfigFieldRow(field, state.values[field.id] ?: field.defaultValue) {
                            viewModel.update(field.id, it)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ConfigFieldRow(field: ConfigField, value: Any, onValueChange: (Any) -> Unit) {
    when (field.type) {
        ConfigFieldType.BOOLEAN -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldLabel(field, Modifier.weight(1f))
                Switch(checked = value as? Boolean ?: value.toString().toBoolean(), onCheckedChange = onValueChange)
            }
        }

        ConfigFieldType.CHOICE -> {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
                FieldLabel(field)
                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    field.choices.forEach { choice ->
                        FilterChip(
                            selected = value.toString() == choice,
                            onClick = { onValueChange(choice) },
                            label = { Text(choice) }
                        )
                    }
                }
            }
        }

        else -> {
            val rendered = when (value) {
                is List<*> -> value.joinToString("\n")
                else -> value.toString()
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
                FieldLabel(field)
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = rendered,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = field.type != ConfigFieldType.LIST,
                    minLines = if (field.type == ConfigFieldType.LIST) 3 else 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (field.type == ConfigFieldType.NUMBER) KeyboardType.Number else KeyboardType.Text
                    )
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 24.dp), color = Divider)
}

@Composable
private fun FieldLabel(field: ConfigField, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(field.title, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Text(field.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
