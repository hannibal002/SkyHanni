package repo

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternRegexTestFailed(config: Config, private val ctx: RepoPatternContext) : SkyHanniRule(
    config,
    "All repo patterns must be accompanied by one or more passing regex test.",
) {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)
        val element = ctx.getRepoPatternElement(delegate) ?: return
        if (!element.needsRegexTest()) return

        element.regexTests.forEach { test ->
            val regex = test.test
            val pattern = element.pattern
            val matcher = pattern.matcher(regex)

            if (!matcher.find()) {
                delegate.reportIssue(
                    "Repo pattern `${element.variableName}` failed regex test: `$regex` pattern: `${element.rawPattern}`. " +
                        "[View on Regex101](${element.regex101Url})",
                )
            }
        }

        element.failingRegexTests.forEach { test ->
            if (element.pattern.matcher(test).find()) {
                delegate.reportIssue(
                    "Repo pattern `${element.variableName}` passed regex test: `$test` pattern: `${element.rawPattern}` " +
                        "even though it was set to fail. [View on Regex101](${element.regex101Url})",
                )
            }
        }
    }
}
