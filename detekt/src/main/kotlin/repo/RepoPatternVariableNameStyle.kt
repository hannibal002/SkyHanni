package repo

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternVariableNameStyle(config: Config, private val ctx: RepoPatternContext) :
    SkyHanniRule(config, "All repo pattern variable names must be camelCase and end with 'Pattern'.") {

    private val camelCasePattern = Regex("^[a-z][a-zA-Z0-9]*$")

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)

        val repoPatternElement = ctx.getRepoPatternElement(delegate) ?: return
        val variableName = repoPatternElement.variableName

        if (!variableName.endsWith("Pattern")) {
            delegate.reportIssue("Repo pattern variable `${variableName}` must end with 'Pattern'.")
        }

        if (!camelCasePattern.matches(variableName)) {
            delegate.reportIssue("Repo pattern variable `${variableName}` must be camelCase.")
        }
    }
}
