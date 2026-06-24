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

    val needsRegexTest: Boolean by lazy {
        regexConstructs.containsMatchIn(rawPattern)
    }

    companion object {
        private val regexConstructs = Regex("""(?<!\\)[.*+(){}\[|?]""")

        fun KtPropertyDelegate.asRepoPatternElement(): RepoPatternElement? {
            val expression = this.expression as? KtDotQualifiedExpression ?: return null
            val callExpression = expression.selectorExpression as? KtCallExpression ?: return null
            if (callExpression.valueArguments.size != 2) return null

            val patternArg = callExpression.valueArguments[1].getArgumentExpression() ?: return null

            // We only want to match on plain strings, not string templates
            if (patternArg !is KtStringTemplateExpression) return null

            val rawPattern = buildRawPattern(patternArg) ?: return null

            val parent = parent as? KtProperty ?: return null
            val variableName = parent.name ?: "unknownPattern"

            val (regexTests, failingRegexTests) = findRegexTestInKDoc(parent)
            return RepoPatternElement(variableName, rawPattern, regexTests, failingRegexTests)
        }

        private fun findRegexTestInKDoc(property: KtProperty): Pair<List<String>, List<String>> {
            val kDoc = property.docComment ?: return emptyList<String>() to emptyList()

            val regexTests = mutableListOf<String>()
            val failingRegexTests = mutableListOf<String>()

            kDoc.getDefaultSection().getContent().lines().forEach { line ->
                when {
                    line.startsWith("REGEX-TEST: ") -> {
                        val test = line.substring("REGEX-TEST: ".length)

                        require(test.trim() == test) {
                            "Plain REGEX-TEST must not contain leading or trailing whitespace. If the whitespace is " +
                                "intentional, use WRAPPED-REGEX-TEST instead."
                        }

                        regexTests.add(test)
                    }

                    line.startsWith("REGEX-FAIL: ") -> {
                        val test = line.substring("REGEX-FAIL: ".length)

                        require(test.trim() == test) {
                            "Plain REGEX-FAIL must not contain leading or trailing whitespace. If the whitespace is " +
                                "intentional, use WRAPPED-REGEX-FAIL instead."
                        }

                        failingRegexTests.add(test)
                    }

                    line.startsWith("WRAPPED-REGEX-TEST: ") -> {
                        extractWrappedValue(line)?.let(regexTests::add)
                    }

                    line.startsWith("WRAPPED-REGEX-FAIL: ") -> {
                        extractWrappedValue(line)?.let(failingRegexTests::add)
                    }
                }
            }
            return regexTests to failingRegexTests
        }

        private fun extractWrappedValue(line: String): String? {
            val firstQuote = line.indexOf('"')
            if (firstQuote == -1) return null

            val lastQuote = line.lastIndexOf('"')
            if (lastQuote <= firstQuote) return null

            return line.substring(firstQuote + 1, lastQuote)
        }

        private fun buildRawPattern(expression: KtStringTemplateExpression): String? {
            val builder = StringBuilder()

            for (entry in expression.entries) {
                when (entry) {
                    is KtStringTemplateEntryWithExpression -> return null
                    is KtLiteralStringTemplateEntry -> builder.append(entry.text)
                    is KtEscapeStringTemplateEntry -> builder.append(entry.unescapedValue)
                }
            }

            return builder
                .toString()
                .removeSurrounding("\"")
                .replace("\n", "")
        }
    }
}
