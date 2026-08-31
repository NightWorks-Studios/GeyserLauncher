package dev.lisfox.geyserlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class GeyserDistributionTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun readsGeyserVersionFromGitProperties() {
        val jar = temporaryFolder.newFile("Geyser.jar")
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        }
        JarOutputStream(jar.outputStream(), manifest).use { output ->
            output.putNextEntry(JarEntry("git.properties"))
            output.write("git.build.version=2.11.2-b1233 (git-master-test)\n".toByteArray())
            output.closeEntry()
        }

        assertEquals("2.11.2-b1233", GeyserDistribution.readVersion(jar))
    }

    @Test
    fun prefersManifestImplementationVersion() {
        val jar = temporaryFolder.newFile("manifest-version.jar")
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            mainAttributes.putValue("Implementation-Version", "3.0.0")
        }
        JarOutputStream(jar.outputStream(), manifest).use { }

        assertEquals("3.0.0", GeyserDistribution.readVersion(jar))
    }
}
