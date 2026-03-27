package repo

import RepoPatternElement.Companion.asRepoPatternElement
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternRegexTestFailed(config: Config) : SkyHanniRule(config, "All repo patterns must be accompanied by one or more passing regex test.") {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)

        val repoPatternElement = delegate.asRepoPatternElement() ?: return
        val variableName = repoPatternElement.variableName
        val rawPattern = repoPatternElement.rawPattern

        if (!rawPattern.needsRegexTest()) return

        if (repoPatternElement.regexTests.isEmpty()) return

        repoPatternElement.regexTests.forEach { test ->
            if (!repoPatternElement.pattern.matcher(test).find()) {
                delegate.reportIssue(
                    "Repo pattern `$variableName` failed regex test: `$test` pattern: `$rawPattern`. " +
                        "[View on Regex101](${repoPatternElement.regex101Url})",
                )
            }
        }

        repoPatternElement.failingRegexTests.forEach { test ->
            if (repoPatternElement.pattern.matcher(test).find()) {
                delegate.reportIssue("Repo pattern `$variableName` passed regex test: `$test` pattern: `$rawPattern` " +
                    "even though it was set to fail. [View on Regex101](${repoPatternElement.regex101Url})")
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
