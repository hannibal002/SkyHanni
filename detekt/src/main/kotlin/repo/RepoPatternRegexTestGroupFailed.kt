package repo

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternRegexTestGroupFailed(config: Config, private val ctx: RepoPatternContext) : SkyHanniRule(
    config,
    "All repo pattern regex groups must be correctly tested.",
) {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)
        val element = ctx.getRepoPatternElement(delegate) ?: return
        if (!element.needsRegexTest()) return
        val compiledPattern = element.pattern

        val internalRegexGroups: Set<String> = compiledPattern.namedGroups().keys
        val exercisedGroups = mutableSetOf<String>()

        element.regexTests.forEach { test ->
            val matcher = compiledPattern.matcher(test.test)

            if (!matcher.find()) return@forEach

            test.groups.keys.forEach { specifiedGroupName ->
                if (!internalRegexGroups.contains(specifiedGroupName)) {
                    delegate.reportIssue(
                        "Repo pattern `${element.variableName}` specifies a test value for group `$specifiedGroupName`, " +
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
                        "Repo pattern `${element.variableName}` failed regex test: `${test.test}` pattern: `${element.rawPattern}`. " +
                            "Group `$groupName` expected `$expectedValue` got `$capturedValue`. " +
                                "[View on Regex101](${element.regex101Url})",
                    )
                }
            }
        }

        if (exercisedGroups.size < internalRegexGroups.size) {
            val unexercisedGroups = internalRegexGroups - exercisedGroups

            delegate.reportIssue(
                "Repo pattern `${element.variableName}` defines internal named groups $internalRegexGroups, " +
                    "but the following groups never captured a non-empty value in any test: $unexercisedGroups. " +
                    "Every group must capture non-empty text at least once."
            )
        }
    }

}
