package dev.lisfox.geyserlauncher.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class BrowserEntry(
    val relativePath: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long
)

class FileRepository(private val context: Context) {
    val root = File(context.filesDir, "geyser").apply { mkdirs() }

    fun list(relativePath: String): List<BrowserEntry> {
        val directory = resolve(relativePath)
        require(directory.isDirectory) { "目录不存在" }
        return directory.listFiles().orEmpty()
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            .map { file ->
                BrowserEntry(
                    relativePath = file.relativeTo(root).invariantSeparatorsPath,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L
                )
            }
    }

    fun resolve(relativePath: String): File {
        val canonicalRoot = root.canonicalFile
        val candidate = File(canonicalRoot, relativePath).canonicalFile
        require(candidate == canonicalRoot || candidate.path.startsWith(canonicalRoot.path + File.separator)) {
            "不能访问 Geyser 目录之外的文件"
        }
        return candidate
    }

    fun parent(relativePath: String): String {
        val file = resolve(relativePath)
        return if (file == root.canonicalFile) "" else requireNotNull(file.parentFile).relativeTo(root).invariantSeparatorsPath
    }

    fun importFile(relativeDirectory: String, uri: Uri) {
        val directory = resolve(relativeDirectory)
        val name = displayName(uri).sanitizeFileName()
        require(name.isNotBlank()) { "无法获取文件名" }
        val target = File(directory, uniqueName(directory, name))
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use(input::copyTo)
        } ?: error("无法读取所选文件")
    }

    fun rename(relativePath: String, newName: String) {
        val safeName = newName.trim().sanitizeFileName()
        require(safeName.isNotBlank() && safeName != "." && safeName != "..") { "文件名无效" }
        val source = resolve(relativePath)
        val target = File(source.parentFile, safeName)
        require(!target.exists()) { "同名文件已存在" }
        require(source.renameTo(target)) { "重命名失败" }
    }

    fun delete(relativePath: String) {
        val target = resolve(relativePath)
        require(target != root.canonicalFile) { "不能删除根目录" }
        require(target.deleteRecursively()) { "删除失败" }
    }

    fun export(relativePath: String, uri: Uri) {
        val source = resolve(relativePath)
        require(source.exists()) { "文件不存在" }
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            if (source.isDirectory) {
                ZipOutputStream(output.buffered()).use { zip ->
                    writeZip(source, requireNotNull(source.parentFile), zip)
                }
            } else {
                source.inputStream().use { input -> input.copyTo(output) }
            }
        } ?: error("无法写入所选位置")
    }

    fun read(relativePath: String): String = resolve(relativePath).readText()

    fun write(relativePath: String, content: String) {
        resolve(relativePath).writeText(content)
    }

    fun validateYaml(content: String) {
        Yaml(SafeConstructor(LoaderOptions())).load<Any?>(content)
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0).orEmpty()
        }
        return uri.lastPathSegment.orEmpty().substringAfterLast('/')
    }

    private fun String.sanitizeFileName(): String = replace('/', '_').replace('\\', '_').replace('\u0000', '_')

    private fun uniqueName(directory: File, original: String): String {
        if (!File(directory, original).exists()) return original
        val base = original.substringBeforeLast('.', original)
        val extension = original.substringAfterLast('.', "").let { if (it.isBlank()) "" else ".$it" }
        var index = 2
        while (File(directory, "$base ($index)$extension").exists()) index++
        return "$base ($index)$extension"
    }

    private fun writeZip(file: File, base: File, zip: ZipOutputStream) {
        val canonicalBase = base.canonicalFile
        val canonicalFile = file.canonicalFile
        require(canonicalFile == canonicalBase || canonicalFile.path.startsWith(canonicalBase.path + File.separator)) {
            "目录中包含无效路径"
        }
        val entryName = canonicalFile.relativeTo(canonicalBase).invariantSeparatorsPath +
            if (canonicalFile.isDirectory) "/" else ""
        val entry = ZipEntry(entryName).apply { time = canonicalFile.lastModified() }
        zip.putNextEntry(entry)
        if (canonicalFile.isFile) {
            canonicalFile.inputStream().use { input -> input.copyTo(zip) }
        }
        zip.closeEntry()
        if (canonicalFile.isDirectory) {
            canonicalFile.listFiles()?.sortedBy(File::getName)?.forEach { child ->
                if (!child.canonicalPath.startsWith(canonicalFile.canonicalPath + File.separator)) {
                    throw IOException("目录中包含无效路径")
                }
                writeZip(child, canonicalBase, zip)
            }
        }
    }
}
