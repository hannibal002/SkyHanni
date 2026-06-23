package repo

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternRegexTestMissing(config: Config, private val ctx: RepoPatternContext) : SkyHanniRule(
    config,
    "All repo patterns must be accompanied by one or more regex test.",
) {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)
        val (repoPatternElement, variableName, _) =
            ctx.getRepoPatternElementSplat(delegate) ?: return

        if (repoPatternElement.regexTests.isEmpty()) {
            delegate.reportIssue("Repo pattern `${variableName}` must have a regex test.")
            return
        }
    }
}
