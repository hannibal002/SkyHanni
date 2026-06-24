package repo

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternRegexTestGroupMissing(config: Config, private val ctx: RepoPatternContext) : SkyHanniRule(
    config,
    "All repo pattern regex groups must be tested.",
) {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)
        val element = ctx.getRepoPatternElement(delegate) ?: return
        if (!element.needsRegexTest) return
        val compiledPattern = element.pattern

        val internalRegexGroups: Set<String> = compiledPattern.namedGroups().keys
        val exercisedGroups = mutableSetOf<String>()

        element.regexTests.forEach { test ->
            val matcher = compiledPattern.matcher(test)

            if (!matcher.find()) return@forEach

            internalRegexGroups.forEach { groupName ->
                val capturedValue = matcher.group(groupName) ?: ""

                if (capturedValue.isNotEmpty()) {
                    exercisedGroups.add(groupName)
                }
            }
        }

        if (exercisedGroups.size < internalRegexGroups.size) {
            val unexercisedGroups = internalRegexGroups - exercisedGroups

            delegate.reportIssue(
                "Repo pattern `${element.variableName}` defines internal named groups $internalRegexGroups, " +
                    "but the following groups never captured a non-empty value in any test: $unexercisedGroups. " +
                    "Every group must capture non-empty text at least once.",
            )
        }
    }

}
