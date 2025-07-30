package at.hannibal2.skyhanni.config.features.mining.nucleus

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PowderChestTimerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the feature.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Only When Max Great Explorer", desc = "Only enable the feature when your Great Explorer is maxed.")
    @ConfigEditorBoolean
    var onlyMaxGreatExplorer: Boolean = false

    @Expose
    @ConfigOption(
        name = "Highlight Chests",
        desc = "Highlight chests with a color depending on how much time left until they despawn.",
    )
    @ConfigEditorBoolean
    var highlightChests: Boolean = true

    @Expose
    @ConfigOption(
        name = "Use Static Color",
        desc = "Use a single color for the chest highlight instead of changing it depending of the time.",
    )
    @ConfigEditorBoolean
    var useStaticColor: Boolean = false

    @Expose
    @ConfigOption(name = "Static Color", desc = "Static color to use.")
    @ConfigEditorColour
    var staticColor: ChromaColour = ChromaColour.fromStaticRGB(85, 255, 85, 245)

    @Expose
    @ConfigOption(name = "Draw Timer", desc = "Draw time left until the chest despawns.")
    @ConfigEditorBoolean
    var drawTimerOnChest: Boolean = true

    @Expose
    @ConfigOption(name = "Draw Line", desc = "Draw a line starting at your cursor to the chosen chest.")
    @ConfigEditorDropdown
    var lineMode: LineMode = LineMode.OLDEST

    enum class LineMode {
        OLDEST,
        NEAREST,
        NONE;

        private val displayName = toFormattedName()
        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(name = "Line Count", desc = "Specify the number of chests to draw a line between.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 30f, minStep = 1f)
    var drawLineToChestAmount: Int = 5

    @Expose
    @ConfigLink(owner = PowderChestTimerConfig::class, field = "enabled")
    val position: Position = Position(100, 100)
}
