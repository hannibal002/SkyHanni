package style

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtNamedFunction

class OnlyOnIslandSpecificity(config: Config) : SkyHanniRule(
    config,
    "`onlyOnSkyblock = true` provides no value when `onlyOnIsland` is present, as it is implicitly true.",
) {

    override fun visitNamedFunction(function: KtNamedFunction) {
        val annotations = function.annotationEntries
        val handleEventAnnotation = annotations.find { it.calleeExpression?.text == "HandleEvent" }

        handleEventAnnotation?.let { annotation ->
            val arguments = annotation.valueArguments
            val hasOnlyOnSkyblock = arguments.any { it.asElement().text.contains("onlyOnSkyblock") }
            val hasOnlyOnIsland = arguments.any { it.asElement().text.contains("onlyOnIsland") }

            if (hasOnlyOnSkyblock && hasOnlyOnIsland) {
                annotation.reportIssue(
                    "This instance of `onlyOnSkyblock = true` should be removed. `onlyOnIsland` implicitly covers this check."
                )
            }
        }

        super.visitNamedFunction(function)
    }
}
