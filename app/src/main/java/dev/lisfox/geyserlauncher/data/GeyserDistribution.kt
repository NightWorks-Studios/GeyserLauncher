package dev.lisfox.geyserlauncher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Properties
import java.util.jar.JarFile

enum class DistributionOperation { IDLE, CHECKING, DOWNLOADING }

data class GeyserBuild(val version: String, val build: Int, val sha256: String) {
    val displayVersion: String = "$version-b$build"
}

data class GeyserDistributionState(
    val installed: Boolean = false,
    val installedVersion: String? = null,
    val latest: GeyserBuild? = null,
    val operation: DistributionOperation = DistributionOperation.IDLE,
    val progress: Int? = null,
    val status: String? = null,
    val error: String? = null
)

fun interface DistributionReporter {
    fun report(message: String)
}

object GeyserDistribution {
    private const val BUILDS_URL = "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds"
    private const val DOWNLOAD_URL =
        "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/standalone"
    private const val USER_AGENT = "GeyserLauncher/1.0 (Android)"
    private const val JAR_NAME = "Geyser.jar"
    private const val METADATA_NAME = "Geyser.version.properties"

    private val _state = MutableStateFlow(GeyserDistributionState())
    val state = _state.asStateFlow()

    @JvmStatic
    @Synchronized
    fun refreshInstalled(context: Context) {
        val jar = jarFile(context)
        _state.update {
            it.copy(
                installed = jar.isFile,
                installedVersion = readVersion(jar) ?: readStoredVersion(context).takeIf { jar.isFile },
                error = null
            )
        }
    }

    @JvmStatic
    @Synchronized
    fun ensureInstalled(context: Context, reporter: DistributionReporter): File {
        refreshInstalled(context)
        val jar = jarFile(context)
        if (jar.isFile) return jar
        reporter.report("未检测到 Geyser，正在获取最新版本...")
        val build = fetchLatest(reporter)
        reporter.report("正在从 GeyserMC 下载 ${build.displayVersion}...")
        return download(context, build, reporter)
    }

    @Synchronized
    fun checkLatest(): GeyserBuild {
        return fetchLatest(DistributionReporter { })
    }

    @Synchronized
    fun installLatest(context: Context): File {
        val build = _state.value.latest ?: fetchLatest(DistributionReporter { })
        return download(context, build, DistributionReporter { })
    }

    internal fun readVersion(jar: File): String? {
        if (!jar.isFile) return null
        return runCatching {
            JarFile(jar).use { archive ->
                val attributes = archive.manifest?.mainAttributes
                sequenceOf(
                    attributes?.getValue("Implementation-Version"),
                    attributes?.getValue("Specification-Version")
                ).firstOrNull { !it.isNullOrBlank() }?.trim()
                    ?: archive.getJarEntry("git.properties")?.let { entry ->
                        val properties = Properties()
                        archive.getInputStream(entry).use(properties::load)
                        properties.getProperty("git.build.version")
                            ?.trim()
                            ?.substringBefore(' ')
                            ?.takeIf(String::isNotBlank)
                    }
            }
        }.getOrNull()
    }

    private fun fetchLatest(reporter: DistributionReporter): GeyserBuild {
        _state.update {
            it.copy(operation = DistributionOperation.CHECKING, progress = null, status = "正在检查最新版本", error = null)
        }
        return try {
            val json = requestText(BUILDS_URL)
            val root = JSONObject(json)
            val version = root.getString("version")
            val builds = root.getJSONArray("builds")
            var latest: GeyserBuild? = null
            for (index in 0 until builds.length()) {
                val item = builds.getJSONObject(index)
                val standalone = item.optJSONObject("downloads")?.optJSONObject("standalone") ?: continue
                val candidate = GeyserBuild(
                    version = version,
                    build = item.getInt("build"),
                    sha256 = standalone.getString("sha256").lowercase()
                )
                if (latest == null || candidate.build > latest.build) latest = candidate
            }
            val result = requireNotNull(latest) { "官方接口未返回 standalone 构建" }
            _state.update {
                it.copy(
                    latest = result,
                    operation = DistributionOperation.IDLE,
                    status = "最新版本 ${result.displayVersion}",
                    error = null
                )
            }
            reporter.report("检测到最新版本 ${result.displayVersion}")
            result
        } catch (error: Throwable) {
            _state.update {
                it.copy(operation = DistributionOperation.IDLE, status = null, error = readableError("检查更新失败", error))
            }
            throw error
        }
    }

    private fun download(context: Context, build: GeyserBuild, reporter: DistributionReporter): File {
        val root = geyserRoot(context).apply { mkdirs() }
        val target = File(root, JAR_NAME)
        val temporary = File(root, "$JAR_NAME.download")
        val backup = File(root, "$JAR_NAME.backup")
        temporary.delete()
        _state.update {
            it.copy(
                latest = build,
                operation = DistributionOperation.DOWNLOADING,
                progress = 0,
                status = "正在下载 ${build.displayVersion}",
                error = null
            )
        }
        return try {
            val connection = openConnection(DOWNLOAD_URL)
            val digest = MessageDigest.getInstance("SHA-256")
            try {
                val total = connection.contentLengthLong
                var received = 0L
                var reportedBucket = -1
                connection.inputStream.use { input ->
                    temporary.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count == 0) continue
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            received += count
                            if (total > 0) {
                                val progress = ((received * 100) / total).toInt().coerceIn(0, 100)
                                _state.update { it.copy(progress = progress) }
                                val bucket = progress / 10
                                if (bucket > reportedBucket) {
                                    reportedBucket = bucket
                                    reporter.report("下载 Geyser：$progress%")
                                }
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            require(actualHash == build.sha256) { "下载文件 SHA-256 校验失败" }
            validateStandaloneJar(temporary)

            backup.delete()
            if (target.exists()) require(target.renameTo(backup)) { "无法备份当前 Geyser" }
            if (!temporary.renameTo(target)) {
                if (backup.exists()) backup.renameTo(target)
                error("无法安装下载的 Geyser")
            }
            backup.delete()
            storeVersion(context, build)
            val installedVersion = readVersion(target) ?: build.displayVersion
            _state.update {
                it.copy(
                    installed = true,
                    installedVersion = installedVersion,
                    latest = build,
                    operation = DistributionOperation.IDLE,
                    progress = null,
                    status = "已安装 $installedVersion",
                    error = null
                )
            }
            reporter.report("Geyser $installedVersion 下载完成")
            target
        } catch (error: Throwable) {
            temporary.delete()
            if (!target.exists() && backup.exists()) backup.renameTo(target)
            _state.update {
                it.copy(
                    operation = DistributionOperation.IDLE,
                    progress = null,
                    status = null,
                    error = readableError("下载 Geyser 失败", error)
                )
            }
            throw error
        }
    }

    private fun requestText(url: String): String {
        val connection = openConnection(url)
        return try {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json, application/octet-stream")
            setRequestProperty("User-Agent", USER_AGENT)
            connect()
            require(responseCode in 200..299) { "服务器返回 HTTP $responseCode" }
        }

    private fun validateStandaloneJar(file: File) {
        JarFile(file).use { archive ->
            require(archive.manifest?.mainAttributes?.getValue("Main-Class")?.isNotBlank() == true) {
                "下载文件不是有效的 Geyser Standalone JAR"
            }
        }
    }

    private fun storeVersion(context: Context, build: GeyserBuild) {
        val properties = Properties().apply {
            setProperty("version", build.displayVersion)
            setProperty("sha256", build.sha256)
        }
        File(geyserRoot(context), METADATA_NAME).outputStream().use {
            properties.store(it, "Downloaded from GeyserMC official API")
        }
    }

    private fun readStoredVersion(context: Context): String? = runCatching {
        val properties = Properties()
        File(geyserRoot(context), METADATA_NAME).inputStream().use(properties::load)
        properties.getProperty("version")
    }.getOrNull()

    private fun geyserRoot(context: Context) = File(context.filesDir, "geyser")
    private fun jarFile(context: Context) = File(geyserRoot(context), JAR_NAME)

    private fun readableError(prefix: String, error: Throwable): String =
        "$prefix：${error.message ?: error.javaClass.simpleName}"
}
