package dev.lisfox.geyserlauncher.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class JvmArgumentsTest {
    @Test
    fun parsesOneArgumentPerNonBlankLine() {
        assertEquals(
            listOf("-Xms256M", "-Dname=value with spaces", "-Xmx1G"),
            JvmArguments.parse("  -Xms256M  \n\n-Dname=value with spaces\n-Xmx1G")
        )
    }

    @Test
    fun shellQuotesMetacharactersAndSingleQuotes() {
        assertEquals("'-Dvalue=\$HOME; `id`'", JvmArguments.shellQuote("-Dvalue=\$HOME; `id`"))
        assertEquals("'-Dname=it'\"'\"'s safe'", JvmArguments.shellQuote("-Dname=it's safe"))
    }
}
