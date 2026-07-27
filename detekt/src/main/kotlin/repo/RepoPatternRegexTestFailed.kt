package repo

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternRegexTestFailed(config: Config, private val ctx: RepoPatternContext) :
    SkyHanniRule(config, "All repo patterns must be accompanied by one or more passing regex test.") {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)

        val repoPatternElement = ctx.getRepoPatternElement(delegate) ?: return
        val variableName = repoPatternElement.variableName
        val rawPattern = repoPatternElement.rawPattern

        repoPatternElement.kDocErrors.forEach { error ->
            delegate.reportIssue("Repo pattern `$variableName`: $error")
        }

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
