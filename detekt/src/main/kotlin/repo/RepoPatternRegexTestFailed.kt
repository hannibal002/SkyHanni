package repo

import RepoPatternElement.Companion.getRepoPatternTestContext
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternRegexTestFailed(config: Config) : SkyHanniRule(
    config,
    "All repo patterns must be accompanied by one or more passing regex test."
) {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)
        val (repoPatternElement, variableName, rawPattern, passingTests, compiledPattern) =
            delegate.getRepoPatternTestContext() ?: return

        passingTests.forEach { test ->
            val regex = test.test
            val matcher = compiledPattern.matcher(regex)

            if (!matcher.find()) {
                delegate.reportIssue(
                    "Repo pattern `$variableName` failed regex test: `$regex` pattern: `$rawPattern`. " +
                        "[View on Regex101](${repoPatternElement.regex101Url})",
                )
            }
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
}
