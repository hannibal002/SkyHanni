package repo

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class RepoPatternPassesShapeRequirement(config: Config, private val ctx: RepoPatternContext) :
    SkyHanniRule(config, "All repo pattern keys must pass the shape requirements.") {

    override fun visitPropertyDelegate(delegate: KtPropertyDelegate) {
        super.visitPropertyDelegate(delegate)

        val repoPatternElement = ctx.getRepoPatternElement(delegate) ?: return
        val variableName = repoPatternElement.variableName
        val rawKey = repoPatternElement.rawKey ?: return

        if (!keyShape.containsMatchIn(rawKey)) {
            delegate.reportIssue(
                "Repo pattern `$variableName` did not meet shape requirements. " +
                    "The patten key: \"$rawKey\" must match \"${keyShape.pattern}\""
            )
        }
    }

    companion object {
        private val keyShape = Regex("^(?:[a-z0-9]+[.-])*[a-z0-9]+$")
    }

}
