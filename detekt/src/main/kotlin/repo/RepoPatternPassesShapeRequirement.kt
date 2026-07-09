package repo

import RepoPatternElement.Companion.asRepoPatternElement
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternPassesShapeRequirement(config: Config): SkyHanniRule(config, "All repo pattern keys must pass the shape requirements.") {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)

        val repoPatternElement = delegate.asRepoPatternElement() ?: return
        val variableName = repoPatternElement.variableName
        val rawKey = repoPatternElement.rawKey ?: return

        if (!keyShape.containsMatchIn(rawKey)) {
            delegate.reportIssue(
                "Repo pattern `$variableName` did not meet shape requirements. The patten key: \"$rawKey\" must match \"${keyShape.pattern}\""
            )
        }
    }

    companion object {
        private val keyShape = Regex("^(?:[a-z0-9]+[.-])*[a-z0-9]+$")
    }

}
