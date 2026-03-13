package at.hannibal2.skyhanni.detektrules

import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import java.net.URLEncoder

class RepoPatternElement private constructor(
    val variableName: String,
    val rawPattern: String,
    val regexTests: List<String>,
    val failingRegexTests: List<String>,
) {

    val pattern by lazy { rawPattern.toPattern() }

    val regex101Url: String by lazy {
        val encodedPattern = URLEncoder.encode(rawPattern.replace("\"", "\\\""), "UTF-8")
        val urlEncodedNewLine = URLEncoder.encode("\n", "UTF-8")
        val encodedTests = regexTests.joinToString(urlEncodedNewLine) { URLEncoder.encode(it, "UTF-8") }
        "https://regex101.com/?regex=$encodedPattern&testString=$encodedTests&flavor=java"
    }

    companion object {
        private val wrappedRegexTestPattern = "\"(?<test>.*)\"".toPattern()

        fun KtPropertyDelegate.asRepoPatternElement(): RepoPatternElement? {
            val expression = this.expression as? KtDotQualifiedExpression ?: return null
            val callExpression = expression.selectorExpression as? KtCallExpression ?: return null
            if (callExpression.valueArguments.size != 2) return null

            val patternArg = callExpression.valueArguments[1].getArgumentExpression() ?: return null

            // We only want to match on plain strings, not string templates
            if (patternArg !is KtStringTemplateExpression) return null
            if (patternArg.entries.any { it is KtStringTemplateEntryWithExpression }) return null

            val rawPattern = patternArg.entries.joinToString("") { entry ->
                when (entry) {
                    is KtLiteralStringTemplateEntry -> entry.text
                    is KtEscapeStringTemplateEntry -> entry.unescapedValue
                    else -> "" // Skip any other types of entries
                }
            }.removeSurrounding("\"").replace("\n", "")

            val parent = parent as? KtProperty ?: return null
            val variableName = parent.name ?: "unknownPattern"

            val (regexTests, failingRegexTests) = findRegexTestInKDoc(parent.docComment)
            return RepoPatternElement(variableName, rawPattern, regexTests, failingRegexTests)
        }

        // NOTE: If you update this code, remember to also update .live-plugins/regexr/plugin.kts
        fun findRegexTestInKDoc(kDoc: KDoc?): Pair<List<String>, List<String>> {
            val regexTests = mutableListOf<String>()
            val failingRegexTests = mutableListOf<String>()

            kDoc?.getAllSections()?.forEach { section ->
                for (tag in section.findTagsByName("regexTest")) {
                    regexTests.add(tag.getContent().trim())
                }
                for (tag in section.findTagsByName("regexFail")) {
                    failingRegexTests.add(tag.getContent().trim())
                }
                for (tag in section.findTagsByName("regexTestWrapped")) {
                    val matcher = wrappedRegexTestPattern.matcher(tag.getContent().trim())
                    if (!matcher.find()) continue
                    matcher.group("test")?.let(regexTests::add)
                }
                for (tag in section.findTagsByName("regexFailWrapped")) {
                    val matcher = wrappedRegexTestPattern.matcher(tag.getContent().trim())
                    if (!matcher.find()) continue
                    matcher.group("test")?.let(failingRegexTests::add)
                }
            }

            return regexTests to failingRegexTests
        }
    }
}
