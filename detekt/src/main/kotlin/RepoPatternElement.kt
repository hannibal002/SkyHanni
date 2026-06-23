import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import java.net.URLEncoder
import java.util.regex.Pattern

data class RegexTest(
    val test: String,
    val groups: Map<String, String>,
)

class RepoPatternElement private constructor(
    val variableName: String,
    val rawPattern: String,
    val regexTests: List<RegexTest>,
    val failingRegexTests: List<String>,
) {

    val pattern by lazy { rawPattern.toPattern() }

    val regex101Url: String by lazy {
        val encodedPattern = URLEncoder.encode(rawPattern.replace("\"", "\\\""), "UTF-8")
        val urlEncodedNewLine = URLEncoder.encode("\n", "UTF-8")
        val encodedTests = regexTests.joinToString(urlEncodedNewLine) {
            URLEncoder.encode(it.test, "UTF-8")
        }
        "https://regex101.com/?regex=$encodedPattern&testString=$encodedTests&flavor=java"
    }

    companion object {
        private val wrappedRegexTestPattern =
            "WRAPPED-REGEX-TEST: \"(?<test>(?:\\\\\"|[^\"])*)\"(?:\\s*;(?<rest>.*))?".toPattern()
        private val wrappedRegexFailPattern =
            "WRAPPED-REGEX-FAIL: \"(?<test>(?:\\\\\"|[^\"])*)\"".toPattern()

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

            return RepoPatternElement(
                variableName,
                rawPattern,
                regexTests,
                failingRegexTests,
            )
        }

        private fun findRegexTestInKDoc(property: KtProperty): Pair<List<RegexTest>, List<String>> {
            val kDoc = property.docComment ?: return emptyList<RegexTest>() to emptyList()

            val regexTests = mutableListOf<RegexTest>()
            val failingRegexTests = mutableListOf<String>()

            kDoc.getDefaultSection()
                .getContent()
                .lines()
                .forEach { origLine ->
                    val line = origLine.trim()
                    wrappedRegexTestPattern.matcher(line).let { matcher ->
                        if (!matcher.find()) return@let

                        val test = matcher.group("test")
                            ?.replace("\\\"", "\"")
                            ?: return@let

                        val rest = matcher.group("rest") ?: ""

                        regexTests.add(
                            RegexTest(
                                test,
                                parseGroups(rest),
                            ),
                        )
                        return@forEach
                    }

                    wrappedRegexFailPattern.matcher(line).let { matcher ->
                        if (!matcher.find()) return@let

                        val test = matcher.group("test")
                            ?.replace("\\\"", "\"")
                            ?: return@let

                        failingRegexTests.add(test)
                        return@forEach
                    }

                    if (line.startsWith("REGEX-TEST: ")) {
                        val fullContent = line.substringAfter("REGEX-TEST: ")

                        require(fullContent.trim() == fullContent) {
                            "Plain REGEX-TEST must not contain leading or trailing whitespace. " +
                                "If the whitespace is intentional, use WRAPPED-REGEX-TEST instead."
                        }

                        // Parse the plain line cleanly using the new semicolon boundary layout
                        val (testString, groupText) = splitAtFirstSemicolonOutsideQuotes(fullContent)

                        regexTests.add(
                            RegexTest(testString, parseGroups(groupText)),
                        )
                        return@forEach
                    }

                    if (line.startsWith("REGEX-FAIL: ")) {
                        val test = line.substringAfter("REGEX-FAIL: ")

                        require(test.trim() == test) {
                            "Plain REGEX-FAIL must not contain leading or trailing whitespace. " +
                                "If the whitespace is intentional, use WRAPPED-REGEX-FAIL instead."
                        }

                        failingRegexTests.add(test)
                        return@forEach
                    }
                }

            return regexTests to failingRegexTests
        }

        private fun splitAtFirstSemicolonOutsideQuotes(content: String): Pair<String, String> {
            var insideQuotes = false
            for (i in content.indices) {
                val char = content[i]
                if (char == '"') {
                    insideQuotes = !insideQuotes
                } else if (char == ';' && !insideQuotes) {
                    val testPart = content.substring(0, i).trim()
                    val groupsPart = content.substring(i + 1).trim()
                    return testPart to groupsPart
                }
            }
            return content.trim() to ""
        }

        private fun parseGroups(content: String): Map<String, String> {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) return emptyMap()

            val pairs = mutableListOf<String>()
            var currentPair = StringBuilder()
            var insideQuotes = false

            for (char in trimmed) {
                when (char) {
                    '"' -> {
                        insideQuotes = !insideQuotes
                        currentPair.append(char)
                    }

                    ',' -> {
                        if (!insideQuotes) {
                            val token = currentPair.toString().trim()
                            if (token.isNotEmpty()) {
                                pairs.add(token)
                            }
                            currentPair = StringBuilder()
                        } else {
                            currentPair.append(char)
                        }
                    }

                    else -> {
                        currentPair.append(char)
                    }
                }
            }

            val lastToken = currentPair.toString().trim()
            if (lastToken.isNotEmpty()) {
                pairs.add(lastToken)
            }

            return pairs.associate { pair ->
                val split = pair.split("=", limit = 2)
                require(split.size == 2) {
                    "Group values must be in the format `key=\"value\"`. Invalid group assertion: `$pair`"
                }

                val key = split[0].trim()
                val value = split[1].trim()

                require(value.startsWith("\"") && value.endsWith("\"")) {
                    "Group values must be quoted. Invalid group assertion: `$pair`"
                }

                key to value.removeSurrounding("\"")
            }
        }

        data class RepoPatternTestContext(
            val element: RepoPatternElement,
            val variableName: String,
            val rawPattern: String,
            val passingTests: List<RegexTest>,
            val compiledPattern: Pattern,
        )

        // This is since IntelliJ keep complaining about duplicated code
        fun KtPropertyDelegate.getRepoPatternTestContext(): RepoPatternTestContext? {
            val element = asRepoPatternElement() ?: return null
            val rawPattern = element.rawPattern

            if (!rawPattern.needsRegexTest()) return null

            val tests = element.regexTests
            if (tests.isEmpty()) return null

            return RepoPatternTestContext(
                element = element,
                variableName = element.variableName,
                rawPattern = rawPattern,
                compiledPattern = element.pattern,
                passingTests = tests,
            )
        }


        private fun String.needsRegexTest(): Boolean {
            return regexConstructs.containsMatchIn(this)
        }

        private val regexConstructs = Regex("""(?<!\\)[.*+(){}\[|?]""")
    }
}
