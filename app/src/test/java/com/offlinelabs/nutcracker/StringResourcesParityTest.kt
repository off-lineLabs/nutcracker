package com.offlinelabs.nutcracker

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards against missing translations: every locale's strings.xml must define exactly the same
 * keys as the default (English) file, and each string must use the same format placeholders
 * (e.g. `%1$s`, `%1$d`) in every locale so formatting can't crash or drop values at runtime.
 */
class StringResourcesParityTest {

    private val locales = listOf("values", "values-es-rES", "values-pt-rBR")

    /** One parsed `<string>` entry. */
    private data class Entry(val text: String, val formatted: Boolean)

    @Test
    fun allLocales_haveTheSameKeys() {
        val parsed = locales.associateWith { parseStrings(stringsFile(it)) }
        val baseKeys = parsed.getValue("values").keys

        val problems = mutableListOf<String>()
        for ((locale, entries) in parsed) {
            if (locale == "values") continue
            val missing = (baseKeys - entries.keys).sorted()
            val extra = (entries.keys - baseKeys).sorted()
            if (missing.isNotEmpty()) problems += "$locale is missing ${missing.size} key(s): $missing"
            if (extra.isNotEmpty()) problems += "$locale has ${extra.size} key(s) not in values/: $extra"
        }

        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun allLocales_useTheSamePlaceholders() {
        val parsed = locales.associateWith { parseStrings(stringsFile(it)) }
        val base = parsed.getValue("values")

        val problems = mutableListOf<String>()
        for ((key, baseEntry) in base) {
            val expected = placeholders(baseEntry)
            for (locale in locales) {
                if (locale == "values") continue
                val entry = parsed.getValue(locale)[key] ?: continue // reported by the key test
                val actual = placeholders(entry)
                if (actual != expected) {
                    problems += "$key: values/ has $expected but $locale has $actual"
                }
            }
        }

        if (problems.isNotEmpty()) fail("Placeholder mismatch in ${problems.size} string(s):\n" + problems.joinToString("\n"))
    }

    @Test
    fun parser_findsTheResources() {
        // Sanity check so the tests above can't pass vacuously on an empty parse.
        val base = parseStrings(stringsFile("values"))
        assertTrue("Expected values/strings.xml to define many strings, found ${base.size}", base.size > 100)
    }

    private fun placeholders(entry: Entry): List<String> {
        if (!entry.formatted) return emptyList()
        val withoutEscapedPercents = entry.text.replace("%%", "")
        return PLACEHOLDER.findAll(withoutEscapedPercents).map { it.value }.sorted().toList()
    }

    private fun parseStrings(file: File): Map<String, Entry> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.documentElement.getElementsByTagName("string")
        val result = linkedMapOf<String, Entry>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            if (element.getAttribute("translatable") == "false") continue
            val name = element.getAttribute("name")
            val formatted = element.getAttribute("formatted") != "false"
            result[name] = Entry(element.textContent, formatted)
        }
        return result
    }

    private fun stringsFile(valuesDir: String): File {
        val relative = "src/main/res/$valuesDir/strings.xml"
        // Gradle runs unit tests with the module dir as working dir; the IDE may use the project root.
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            listOf(File(dir, relative), File(dir, "app/$relative")).firstOrNull { it.isFile }?.let { return it }
            dir = dir.parentFile
        }
        error("Could not locate $relative from ${System.getProperty("user.dir")}")
    }

    private companion object {
        /** Java/Android format specifiers, e.g. %s, %d, %1$s, %2$d, %1$.1f. `%%` is stripped beforehand. */
        // The space flag is deliberately not supported so prose like "50% of" isn't mistaken for a specifier.
        val PLACEHOLDER = Regex("""%(\d+\$)?[-#+0,(]*\d*(\.\d+)?[a-zA-Z]""")
    }
}
