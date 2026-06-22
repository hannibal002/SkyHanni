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
        private val wrappedRegexTestPattern = "WRAPPED-REGEX-TEST: \"(?<test>.*)\"".toPattern()
        private val wrappedRegexFailPattern = "WRAPPED-REGEX-FAIL: \"(?<test>.*)\"".toPattern()

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

            val (regexTests, failingRegexTests) = findRegexTestInKDoc(parent)
            return RepoPatternElement(variableName, rawPattern, regexTests, failingRegexTests)
        }

        private fun findRegexTestInKDoc(property: KtProperty): Pair<List<String>, List<String>> {
            val kDoc = property.docComment ?: return listOf<String>() to listOf()

            val regexTests = mutableListOf<String>()
            val failingRegexTests = mutableListOf<String>()

            kDoc.getDefaultSection().getContent().lines().forEach { line ->
                wrappedRegexTestPattern.matcher(line).let { matcher ->
                    if (!matcher.find()) return@let
                    val test = matcher.group("test") ?: return@let
                    regexTests.add(test)
                    return@forEach
                }
                wrappedRegexFailPattern.matcher(line).let { matcher ->
                    if (!matcher.find()) return@let
                    val test = matcher.group("test") ?: return@let
                    failingRegexTests.add(test)
                    return@forEach
                }
                if (line.contains("REGEX-TEST: ")) {
                    val test = line.substringAfter("REGEX-TEST: ")
                    require(test.trim() == test) {
                        "Plain REGEX-TEST must not contain leading or trailing whitespace. If the whitespace is " +
                            "intentional, use WRAPPED-REGEX-TEST instead."
                    }
                    regexTests.add(test)
                    return@forEach
                }
                if (line.contains("REGEX-FAIL: ")) {
                    val test = line.substringAfter("REGEX-FAIL: ")
                    require(test.trim() == test) {
                        "Plain REGEX-FAIL must not contain leading or trailing whitespace. If the whitespace is " +
                            "intentional, use WRAPPED-REGEX-FAIL instead."
                    }
                    failingRegexTests.add(test)
                    return@forEach
                }
            }
            return regexTests to failingRegexTests
        }
    }
}
