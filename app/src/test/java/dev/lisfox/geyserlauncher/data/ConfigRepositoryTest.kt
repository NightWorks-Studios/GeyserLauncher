package dev.lisfox.geyserlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File

class ConfigRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun saveUpdatesTypedFieldsAndPreservesUnknownValues() {
        val config = File(temporaryFolder.newFolder("server"), "config.yml")
        val original = """
            unknown:
              nested: keep
            java:
              address: old.example.com
              port: 25565
            config-version: 4
        """.trimIndent() + "\n"
        config.writeText(original)
        val repository = ConfigRepository(config)
        val values = repository.load().values.toMutableMap().apply {
            this["java_port"] = "25570"
            this["java_address"] = "mc.example.com"
            this["game_coordinates"] = true
            this["advanced_packs"] = "https://example.com/a.mcpack\nhttps://example.com/b.mcpack"
        }

        repository.save(values)

        val root = parse(config.readText())
        assertEquals("keep", mapAt(root, "unknown")["nested"])
        assertEquals(25570, mapAt(root, "java")["port"])
        assertEquals("mc.example.com", mapAt(root, "java")["address"])
        assertEquals(true, mapAt(root, "gameplay")["show-coordinates"])
        assertEquals(2, (mapAt(root, "advanced")["resource-pack-urls"] as List<*>).size)
        assertEquals(original, File(config.parentFile, "config.yml.bak").readText())
    }

    @Test
    fun invalidYamlIsRejectedBeforeWrite() {
        val config = File(temporaryFolder.newFolder("invalid"), "config.yml")
        val repository = ConfigRepository(config)

        val result = runCatching { repository.saveRaw("bedrock: [not closed") }

        assertTrue(result.isFailure)
        assertTrue(!config.exists())
    }

    @Test
    fun migratesIncompleteTemplateGeneratedByPreviousLauncherBuild() {
        val config = File(temporaryFolder.newFolder("migration"), "config.yml")
        config.writeText(
            """
                java:
                  port: 0
                  auth-type: online
                metrics-uuid: 24198a1b-f4f4-4ceb-9890-5dc0278807b9
            """.trimIndent()
        )

        ConfigRepository(config).load()

        val migrated = parse(config.readText())
        assertEquals("127.0.0.1", mapAt(migrated, "java")["address"])
        assertEquals(25565, mapAt(migrated, "java")["port"])
        assertTrue(migrated["metrics-uuid"] != "24198a1b-f4f4-4ceb-9890-5dc0278807b9")
    }

    @Test
    fun schemaHasUniqueIdsAndPaths() {
        assertEquals(GEYSER_CONFIG_FIELDS.size, GEYSER_CONFIG_FIELDS.map(ConfigField::id).distinct().size)
        assertEquals(GEYSER_CONFIG_FIELDS.size, GEYSER_CONFIG_FIELDS.map(ConfigField::path).distinct().size)
        assertTrue(ConfigCategory.entries.all { category -> GEYSER_CONFIG_FIELDS.any { it.category == category } })
    }

    @Test
    fun everySchemaFieldExistsInGeneratedStandaloneTemplate() {
        val templateFile = sequenceOf(
            File("app/src/main/assets/geyser-default-config.yml"),
            File("src/main/assets/geyser-default-config.yml")
        ).first(File::isFile)
        val root = parse(templateFile.readText())

        GEYSER_CONFIG_FIELDS.forEach { field ->
            assertTrue("Missing generated config path: ${field.path.joinToString(".")}", hasPath(root, field.path))
            assertEquals(
                "Wrong default for: ${field.path.joinToString(".")}",
                field.defaultValue,
                valueAt(root, field.path)
            )
        }
        val editablePaths = GEYSER_CONFIG_FIELDS.map { it.path.joinToString(".") }.toSet()
        val protectedPaths = setOf("metrics-uuid", "config-version")
        assertEquals(protectedPaths, flattenLeafPaths(root) - editablePaths)
        assertEquals(7, root["config-version"])
    }

    @Suppress("UNCHECKED_CAST")
    private fun parse(text: String): Map<String, Any?> =
        Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(text)

    @Suppress("UNCHECKED_CAST")
    private fun mapAt(root: Map<String, Any?>, key: String): Map<String, Any?> = root[key] as Map<String, Any?>

    private fun hasPath(root: Map<String, Any?>, path: List<String>): Boolean {
        var current: Any? = root
        path.forEach { segment ->
            val map = current as? Map<*, *> ?: return false
            if (!map.containsKey(segment)) return false
            current = map[segment]
        }
        return true
    }

    private fun valueAt(root: Map<String, Any?>, path: List<String>): Any? {
        var current: Any? = root
        path.forEach { segment -> current = (current as Map<*, *>)[segment] }
        return current
    }

    private fun flattenLeafPaths(root: Map<String, Any?>, prefix: String = ""): Set<String> = buildSet {
        root.forEach { (key, value) ->
            val path = if (prefix.isBlank()) key else "$prefix.$key"
            if (value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                addAll(flattenLeafPaths(value as Map<String, Any?>, path))
            } else {
                add(path)
            }
        }
    }
}
