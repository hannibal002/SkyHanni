package at.hannibal2.skyhanni.config.features.mining.nucleus

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class CrystalNucleusSpeedrunConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable the Nucleus Speedrun timer.")
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = CrystalNucleusSpeedrunConfig::class, field = "enabled")
    var position: Position = Position(100, 100)

    @Expose
    @ConfigOption(name = "Quick Disable", desc = "You can use §e/shnucspeedrun §rto quickly toggle all functions of this module.")
    @ConfigEditorText
    var ignore: Nothing? = null

    @Expose
    @ConfigOption(name = "Run Start Type", desc = "What should start the initial timer for the run.")
    @ConfigEditorDropdown
    var runStartType: RunStartType = RunStartType.LEAVE_CRYSTAL_NUCLEUS

    enum class RunStartType(private val displayName: String) {
        LEAVE_CRYSTAL_NUCLEUS("Leave Crystal Nucleus"),
        HOTKEY("Hotkey (Manual)"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Split Type", desc = "Whether split times should be set automatically on collecting a crystal, or manually.")
    @ConfigEditorDropdown
    var runSplitType: RunSplitType = RunSplitType.AUTO

    enum class RunSplitType(private val displayName: String) {
        AUTO("Auto-Split (Crystal Collected)"),
        HOTKEY("Hotkey (Manual)")
        ;

        override fun toString() = displayName
    }

    @Transient
    val defaultSplitOrder: MutableList<CrystalNucleusApi.NucleusCrystalType> = mutableListOf(
        CrystalNucleusApi.NucleusCrystalType.AMBER,
        CrystalNucleusApi.NucleusCrystalType.AMETHYST,
        CrystalNucleusApi.NucleusCrystalType.JADE,
        CrystalNucleusApi.NucleusCrystalType.SAPPHIRE,
        CrystalNucleusApi.NucleusCrystalType.TOPAZ
    )

    @Expose
    @ConfigOption(
        name = "Manual Split Order",
        desc = "The default order to use for splits. Splits will still auto-detect correctly if you collect a crystal out of this " +
            "order, however the display may be incorrect in real-time."
    )
    @ConfigEditorDraggableList
    var manualSplitOrder: MutableList<CrystalNucleusApi.NucleusCrystalType> = defaultSplitOrder.toMutableList()

    @Expose
    @ConfigOption(name = "Run Stop Type", desc = "What should stop the timer for the run.")
    @ConfigEditorDropdown
    var runStopType: RunStopType = RunStopType.ALL_CRYSTALS_PLACED

    enum class RunStopType(private val displayName: String) {
        LAST_CRYSTAL_COLLECTED("Last Crystal Collected"),
        ALL_CRYSTALS_PLACED("All Crystals Placed"),
        HOTKEY("Hotkey (Manual)"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Hotkey Assignment", desc = "The hotkey to both start the timer, and perform a split.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var hotkey: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Wait for Warp + Move",
        desc = "If enabled, after a split is performed, the timer will wait for the player to §e/warp nucleus §rand move before starting" +
            "the next split's timer.\n" +
            "§cShould only be used for those with §bMVP§f+ §cwho can unlock the travel scroll§r."
    )
    @ConfigEditorBoolean
    var waitForWarpAndMove: Boolean = true

    @Expose
    @ConfigOption(
        name = "Split Cache Count",
        desc = "How many split times to keep in storage to calculate a rolling average.\n" +
            "§eLarger numbers will be more accurate, but will use more storage space, and may cause memory allocation issues§r."
    )
    @ConfigEditorSlider(minValue = 10f, maxValue = 1000f, minStep = 1f)
    var splitCacheCount: Int = 25
}
