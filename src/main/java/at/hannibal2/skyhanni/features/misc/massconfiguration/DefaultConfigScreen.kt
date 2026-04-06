package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.utils.compat.SkyHanniChromeScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue

class DefaultConfigScreen(
    val orderedOptions: Map<Category, List<FeatureToggleableOption>>,
    old: String,
    new: String,
) : SkyHanniChromeScreen() {

    override val screenTitle: String = when {
        old == "null" && new == "null" -> "SkyHanni Default Options"
        old == "null" -> "SkyHanni Options In Version $new"
        new == "null" -> "SkyHanni Options since $old"
        else -> "SkyHanni Options $old → $new"
    }

    val scrollValue = ScrollValue()
    val resetSuggestionState: MutableMap<Category, ResetSuggestionState> =
        orderedOptions.keys.associateWith { ResetSuggestionState.LEAVE_DEFAULTS }.toMutableMap()

    override fun buildContent(): Renderable = DefaultConfigGui.buildContent(this)
}
