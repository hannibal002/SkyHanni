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
        val element = ctx.getRepoPatternElement(delegate) ?: return
        if (!element.needsRegexTest) return

        if (element.regexTests.isEmpty()) {
            delegate.reportIssue("Repo pattern `${element.variableName}` must have a regex test.")
            return
        }
    }
}
