package repo

import RepoPatternElement.Companion.getRepoPatternTestContext
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternRegexTestGroupFailed(config: Config) : SkyHanniRule(
    config,
    "All repo pattern regex groups must be correctly tested."
) {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)
        val (repoPatternElement, variableName, rawPattern, passingTests, compiledPattern) =
            delegate.getRepoPatternTestContext() ?: return

        val internalRegexGroups: Set<String> = compiledPattern.namedGroups().keys
        val exercisedGroups = mutableSetOf<String>()

        passingTests.forEach { test ->
            val matcher = compiledPattern.matcher(test.test)

            if (!matcher.find()) return@forEach

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
                    exercisedGroups.add(groupName)
                }

                val expectedValue = test.groups[groupName] ?: return@forEach

                if (expectedValue != capturedValue) {
                    delegate.reportIssue(
                        "Repo pattern `$variableName` failed regex test: `${test.test}` pattern: `$rawPattern`. " +
                            "Group `$groupName` expected `$expectedValue` got `$capturedValue`. " +
                            "[View on Regex101](${repoPatternElement.regex101Url})",
                    )
                }
            }
        }

        if (exercisedGroups.size < internalRegexGroups.size) {
            val unexercisedGroups = internalRegexGroups - exercisedGroups

            delegate.reportIssue(
                "Repo pattern `$variableName` defines internal named groups $internalRegexGroups, " +
                    "but the following groups never captured a non-empty value in any test: $unexercisedGroups. " +
                    "Every group must capture non-empty text at least once."
            )
        }
    }

}
