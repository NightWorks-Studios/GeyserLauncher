package dev.lisfox.geyserlauncher.data

import android.content.Context
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File
import java.util.LinkedHashMap
import java.util.UUID

data class ConfigSnapshot(
    val fileExists: Boolean,
    val values: Map<String, Any?>,
    val raw: String
)

class ConfigRepository internal constructor(
    val configFile: File,
    private val defaultConfig: String? = null
) {
    constructor(context: Context) : this(
        File(context.filesDir, "geyser/server/config.yml"),
        context.assets.open(DEFAULT_CONFIG_ASSET).bufferedReader().use { it.readText() }
    )

    private val yaml = Yaml(
        SafeConstructor(LoaderOptions()),
        org.yaml.snakeyaml.representer.Representer(DumperOptions()),
        DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            width = 120
        }
    )

    fun load(): ConfigSnapshot {
        ensureDefaultConfig()
        migrateIncompleteStandaloneTemplate()
        val raw = if (configFile.isFile) configFile.readText() else ""
        val root = parseMap(raw)
        val values = GEYSER_CONFIG_FIELDS.associate { field ->
            field.id to (valueAt(root, field.path) ?: field.defaultValue)
        }
        return ConfigSnapshot(configFile.isFile, values, raw)
    }

    fun save(values: Map<String, Any?>): ConfigSnapshot {
        val existing = if (configFile.isFile) configFile.readText() else ""
        val root = parseMap(existing)
        GEYSER_CONFIG_FIELDS.forEach { field ->
            if (values.containsKey(field.id)) setValue(root, field.path, normalize(field, values[field.id]))
        }
        val rendered = yaml.dump(root)
        parseMap(rendered)
        writeSafely(rendered)
        return load()
    }

    fun saveRaw(raw: String) {
        parseMap(raw)
        writeSafely(raw)
    }

    fun validate(raw: String) {
        parseMap(raw)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMap(raw: String): MutableMap<String, Any?> {
        if (raw.isBlank()) return LinkedHashMap()
        val loaded = yaml.load<Any?>(raw) ?: return LinkedHashMap()
        require(loaded is Map<*, *>) { "配置文件顶层必须是 YAML 对象" }
        return deepMutableMap(loaded)
    }

    private fun deepMutableMap(source: Map<*, *>): MutableMap<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        source.forEach { (key, value) ->
            result[key.toString()] = when (value) {
                is Map<*, *> -> deepMutableMap(value)
                is List<*> -> value.map { item -> if (item is Map<*, *>) deepMutableMap(item) else item }.toMutableList()
                else -> value
            }
        }
        return result
    }

    private fun valueAt(root: Map<String, Any?>, path: List<String>): Any? {
        var current: Any? = root
        path.forEach { segment -> current = (current as? Map<*, *>)?.get(segment) ?: return null }
        return current
    }

    @Suppress("UNCHECKED_CAST")
    private fun setValue(root: MutableMap<String, Any?>, path: List<String>, value: Any?) {
        var current = root
        path.dropLast(1).forEach { segment ->
            val child = current[segment]
            current = if (child is MutableMap<*, *>) {
                child as MutableMap<String, Any?>
            } else {
                LinkedHashMap<String, Any?>().also { current[segment] = it }
            }
        }
        current[path.last()] = value
    }

    private fun normalize(field: ConfigField, value: Any?): Any = when (field.type) {
        ConfigFieldType.BOOLEAN -> value as? Boolean ?: value.toString().toBooleanStrict()
        ConfigFieldType.NUMBER -> value as? Number ?: value.toString().toIntOrNull()
            ?: throw IllegalArgumentException("${field.title}必须是整数")
        ConfigFieldType.LIST -> when (value) {
            is List<*> -> value.map { it.toString() }.filter { it.isNotBlank() }
            else -> value.toString().lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        }
        ConfigFieldType.CHOICE -> value.toString().also {
            require(it in field.choices) { "${field.title}的值无效" }
        }
        ConfigFieldType.TEXT -> value?.toString().orEmpty()
    }

    private fun writeSafely(content: String) {
        configFile.parentFile?.mkdirs()
        if (configFile.isFile) configFile.copyTo(File(configFile.parentFile, "config.yml.bak"), overwrite = true)
        val temporary = File(configFile.parentFile, "config.yml.tmp")
        temporary.writeText(content)
        if (!temporary.renameTo(configFile)) {
            temporary.copyTo(configFile, overwrite = true)
            temporary.delete()
        }
    }

    private fun ensureDefaultConfig() {
        if (configFile.exists() || defaultConfig.isNullOrBlank()) return
        parseMap(defaultConfig)
        configFile.parentFile?.mkdirs()
        configFile.writeText(withUniqueMetricsId(defaultConfig))
    }

    private fun migrateIncompleteStandaloneTemplate() {
        if (!configFile.isFile) return
        val original = configFile.readText()
        var raw = original
        val incompleteJava = Regex("(?m)^java:\\s*\\r?\\n(\\s{2}port:\\s*)0\\s*$")
        if (incompleteJava.containsMatchIn(raw)) {
            raw = incompleteJava.replace(
                raw,
                "java:\n  # The IP address of the Java Edition server.\n  address: 127.0.0.1\n\n  # The port of the Java Edition server.\n  port: 25565"
            )
        }
        if (OLD_BUNDLED_METRICS_ID in raw) {
            raw = raw.replace(OLD_BUNDLED_METRICS_ID, UUID.randomUUID().toString())
        }
        if (raw != original) configFile.writeText(raw)
    }

    private fun withUniqueMetricsId(template: String): String {
        return METRICS_ID_PATTERN.replace(template) { match ->
            "${match.groupValues[1]}${UUID.randomUUID()}"
        }
    }

    private companion object {
        const val DEFAULT_CONFIG_ASSET = "geyser-default-config.yml"
        const val OLD_BUNDLED_METRICS_ID = "24198a1b-f4f4-4ceb-9890-5dc0278807b9"
        val METRICS_ID_PATTERN = Regex("(?m)^(metrics-uuid:\\s*)[0-9a-fA-F-]+\\s*$")
    }
}
