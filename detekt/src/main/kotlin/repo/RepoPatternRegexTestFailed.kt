package repo

import RepoPatternElement.Companion.asRepoPatternElement
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternRegexTestFailed(config: Config) : SkyHanniRule(
    config,
    "All repo patterns must be accompanied by one or more passing regex test."
) {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)

        val repoPatternElement = delegate.asRepoPatternElement() ?: return
        val variableName = repoPatternElement.variableName
        val rawPattern = repoPatternElement.rawPattern

        if (!rawPattern.needsRegexTest()) return

        val passingTests = repoPatternElement.regexTests
        if (passingTests.isEmpty()) return

        val compiledPattern = repoPatternElement.pattern
        val internalRegexGroups: Set<String> = compiledPattern.namedGroups().keys
        var foundAnyNonEmptyInternalCapture = false

        passingTests.forEach { test ->
            val regex = test.test
            val matcher = compiledPattern.matcher(regex)

            if (!matcher.find()) {
                delegate.reportIssue(
                    "Repo pattern `$variableName` failed regex test: `$regex` pattern: `$rawPattern`. " +
                        "[View on Regex101](${repoPatternElement.regex101Url})",
                )
                return@forEach
            }

            test.groups.keys.forEach { specifiedGroupName ->
                if (!internalRegexGroups.contains(specifiedGroupName)) {
                    delegate.reportIssue(
                        "Repo pattern `$variableName` specifies a test value for group `$specifiedGroupName`, " +
                            "but no group named `$specifiedGroupName` exists inside the regular expression."
                    )
                }
            }

            internalRegexGroups.forEach { groupName ->
                val capturedValue = matcher.group(groupName) ?: ""

                if (capturedValue.isNotEmpty()) {
                    foundAnyNonEmptyInternalCapture = true
                }

                val expectedValue = test.groups[groupName] ?: return@forEach

                if (expectedValue != capturedValue) {
                    delegate.reportIssue(
                        "Repo pattern `$variableName` failed regex test: `$regex` pattern: `$rawPattern`. " +
                            "Group `$groupName` expected `$expectedValue` got `$capturedValue`. " +
                            "[View on Regex101](${repoPatternElement.regex101Url})",
                    )
                }
            }
        }

        if (internalRegexGroups.isNotEmpty() && !foundAnyNonEmptyInternalCapture) {
            delegate.reportIssue(
                "Repo pattern `$variableName` defines internal named groups $internalRegexGroups, " +
                    "but all test strings capture nothing but empty strings (\"\"). Provide a test string " +
                    "that actually exercises and populates at least one group."
            )
        }

        repoPatternElement.failingRegexTests.forEach { test ->
            if (compiledPattern.matcher(test).find()) {
                delegate.reportIssue(
                    "Repo pattern `$variableName` passed regex test: `$test` pattern: `$rawPattern` " +
                        "even though it was set to fail. [View on Regex101](${repoPatternElement.regex101Url})"
                )
            }
        }
    }

    private fun String.needsRegexTest(): Boolean {
        return regexConstructs.containsMatchIn(this)
    }

    companion object {
        val regexConstructs = Regex("""(?<!\\)[.*+(){}\[|?]""")
    }
}
