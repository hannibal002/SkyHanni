package repo

import RepoPatternElement.Companion.asRepoPatternElement
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternUnnamedGroup(config: Config) : SkyHanniRule(config, "All repo patterns must not contain unnamed groups.") {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)

        val repoPatternElement = delegate.asRepoPatternElement() ?: return

        if (repoPatternElement.rawPattern.hasUnnamedGroup()) {
            delegate.reportIssue("Repo pattern `${repoPatternElement.variableName}` must not contain unnamed capture groups.")
        }
    }

    private fun String.hasUnnamedGroup(): Boolean {
        // Remove content inside square brackets
        val withoutSquareBrackets = squareBracketRegex.replace(this, "")
        // Check if simplified string contains unnamed groups
        return unnamedGroupRegex.containsMatchIn(withoutSquareBrackets)
    }

    companion object {
        // Regex to find content inside square brackets, including nested brackets
        private val squareBracketRegex = Regex("""(?<!\\)\[(?:\^])?(?:\\.|[^]])*]""")
        // Regex to find unescaped '(' not followed by '?'
        private val unnamedGroupRegex = Regex("""(?<!\\)\((?!\?)""")
    }
}
