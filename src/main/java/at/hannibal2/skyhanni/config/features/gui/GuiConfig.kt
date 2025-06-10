package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.chroma.ChromaConfig
import at.hannibal2.skyhanni.config.features.gui.customscoreboard.CustomScoreboardConfig
import at.hannibal2.skyhanni.config.features.markedplayer.MarkedPlayerConfig
import at.hannibal2.skyhanni.config.features.misc.DiscordRPCConfig
import at.hannibal2.skyhanni.config.features.misc.compacttablist.CompactTabListConfig
import at.hannibal2.skyhanni.config.features.misc.cosmetic.CosmeticConfig
//#if TODO
import at.hannibal2.skyhanni.data.GuiEditManager.openGuiPositionEditor
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.input.Keyboard

// todo 1.21 impl needed
class GuiConfig {
    @Expose
    @Category(name = "Compact Tab List", desc = "Compact Tab List Settings")
    @Accordion
    var compactTabList: CompactTabListConfig = CompactTabListConfig()

    @Expose
    @Category(name = "Custom Scoreboard", desc = "Custom Scoreboard Settings")
    var customScoreboard: CustomScoreboardConfig = CustomScoreboardConfig()

    @Expose
    @Category(name = "Chroma", desc = "Settings for Chroma text (Credit to SBA).")
    @Accordion
    var chroma: ChromaConfig = ChromaConfig()

    //#if TODO
    @ConfigOption(
        name = "Edit GUI Locations",
        desc = "Opens the Position Editor, allows changing the position of SkyHanni's overlays."
    )
    @ConfigEditorButton(buttonText = "Edit")
    var positions: Runnable = Runnable { openGuiPositionEditor(true) }
    //#endif

    @Expose
    @ConfigOption(name = "Open Hotkey", desc = "Press this key to open the GUI Editor.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var keyBindOpen: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(name = "Global GUI Scale", desc = "Globally scale all SkyHanni GUIs.")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 10f, minStep = 0.05f)
    var globalScale: Float = 1f

    @Expose
    @ConfigOption(name = "Time Format", desc = "Change SkyHanni to use 24h time instead of 12h time.")
    @ConfigEditorBoolean
    @FeatureToggle
    var timeFormat24h: Boolean = false

    @Expose
    @ConfigOption(name = "Discord Rich Presence", desc = "")
    @Accordion
    var discordRPC: DiscordRPCConfig = DiscordRPCConfig()

    @Expose
    @ConfigOption(name = "Hotbar", desc = "Settings for adjusting the hotbar.")
    @Accordion
    var hotbar: HotbarConfig = HotbarConfig()

    @Expose
    @ConfigOption(name = "XP Bar", desc = "Settings for adjusting the XP bar.")
    @Accordion
    var xpBar: XPBarConfig = XPBarConfig()

    @Expose
    @ConfigOption(name = "Mayor Overlay", desc = "Settings for the mayor overlay.")
    @Accordion
    var mayorOverlay = MayorOverlayConfig()

    @Expose
    @ConfigOption(name = "Marked Players", desc = "Players that got marked with §e/shmarkplayer§7.")
    @Accordion
    var markedPlayers: MarkedPlayerConfig = MarkedPlayerConfig()

    @Expose
    @ConfigOption(name = "Modify Visual Words", desc = "")
    @Accordion
    var modifyWords: ModifyWordsConfig = ModifyWordsConfig()

    @Expose
    @ConfigOption(name = "Custom Text Box", desc = "")
    @Accordion
    var customTextBox: TextBoxConfig = TextBoxConfig()

    @Expose
    @ConfigOption(name = "Tab Widget", desc = "")
    @Accordion
    var tabWidget: TabWidgetConfig = TabWidgetConfig()

    @Expose
    @ConfigOption(name = "In-Game Date", desc = "")
    @Accordion
    var inGameDate: InGameDateConfig = InGameDateConfig()

    @Expose
    @ConfigOption(name = "Beacon Power", desc = "Display the current beacon power duration and what stat is boosted.")
    @ConfigEditorBoolean
    @FeatureToggle
    var beaconPower: Boolean = false

    @Expose
    @ConfigOption(name = "Show Beacon Stat", desc = "Show what stat is being boosted by your beacon.")
    @ConfigEditorBoolean
    var beaconPowerStat: Boolean = true

    @Expose
    @ConfigLink(owner = GuiConfig::class, field = "beaconPower")
    var beaconPowerPosition: Position = Position(10, 10)

    @Expose
    @ConfigOption(
        name = "Real Time",
        desc = "Display the current computer time, a handy feature when playing in full-screen mode."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var realTime: Boolean = false

    @Expose
    @ConfigOption(
        name = "Real Time 12h Format",
        desc = "Display the current computer time in 12hr Format rather than 24h Format."
    )
    @ConfigEditorBoolean
    var realTimeFormatToggle: Boolean = false

    @Expose
    @ConfigOption(name = "Real Time Show Seconds", desc = "Include the current seconds in the Real Time display.")
    @ConfigEditorBoolean
    var realTimeShowSeconds: Boolean = true

    @Expose
    @ConfigLink(owner = GuiConfig::class, field = "realTime")
    var realTimePosition: Position = Position(10, 10)

    @Expose
    @Category(name = "Cosmetic", desc = "Cosmetics Settings")
    var cosmetic: CosmeticConfig = CosmeticConfig()

    @Expose
    @ConfigOption(name = "TPS Display", desc = "Show the TPS of the current server, like in Soopy.")
    @ConfigEditorBoolean
    @FeatureToggle
    var tpsDisplay: Boolean = false

    @Expose
    @ConfigLink(owner = GuiConfig::class, field = "tpsDisplay")
    var tpsDisplayPosition: Position = Position(10, 10)

    @Expose
    @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure SkyHanni.")
    @ConfigEditorBoolean
    @FeatureToggle
    var configButtonOnPause: Boolean = true

    @Expose
    @ConfigOption(name = "Widen Config", desc = "Make SkyHanni's config window wider. (~1.5x)")
    @ConfigEditorBoolean
    val widenConfig: Property<Boolean> = Property.of(false)

    @Expose
    var titlePosition: Position = Position(0, 160)
}
