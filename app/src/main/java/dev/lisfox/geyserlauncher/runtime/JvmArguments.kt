package dev.lisfox.geyserlauncher.runtime

import android.content.Context

object JvmArguments {
    private const val PREFERENCES = "launcher_settings"
    private const val KEY_ARGUMENTS = "jvm_arguments"

    @JvmField
    val REQUIRED = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "-Djava.net.preferIPv4Stack=true",
        "-Djava.net.preferIPv6Addresses=false"
    )

    @JvmStatic
    fun load(context: Context): List<String> = parse(loadText(context))

    fun loadText(context: Context): String = context
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .getString(KEY_ARGUMENTS, "")
        .orEmpty()

    fun saveText(context: Context, text: String) {
        val normalized = parse(text).joinToString("\n")
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ARGUMENTS, normalized)
            .apply()
    }

    fun parse(text: String): List<String> {
        require('\u0000' !in text) { "JVM 参数不能包含空字符" }
        val arguments = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        require(arguments.size <= 200) { "JVM 参数不能超过 200 项" }
        require(arguments.all { it.length <= 4_096 }) { "单项 JVM 参数过长" }
        return arguments
    }

    @JvmStatic
    fun shellQuote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"
}
