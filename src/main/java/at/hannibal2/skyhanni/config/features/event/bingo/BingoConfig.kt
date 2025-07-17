package at.hannibal2.skyhanni.config.features.event.bingo

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.input.Keyboard

class BingoConfig {
    @Expose
    @ConfigOption(name = "Bingo Card", desc = "")
    @Accordion
    val bingoCard: BingoCardConfig = BingoCardConfig()

    @Expose
    @ConfigOption(name = "Bingo Net", desc = "")
    @Accordion
    val bingoNet: BingoNetConfig = BingoNetConfig()

    @Expose
    @ConfigOption(name = "Compact Chat Messages", desc = "")
    @Accordion
    val compactChat: CompactChatConfig = CompactChatConfig()

    // TODO move into own category
    @Expose
    @ConfigOption(
        name = "Minion Craft Helper",
        desc = "Show how many more items you need to upgrade the minion in your inventory. Especially useful for Bingo."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var minionCraftHelperEnabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Progress to T1",
        desc = "Show tier 1 Minion Crafts in the Helper display even if needed items are not fully collected."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var minionCraftHelperProgressFirst: Boolean = false

    @Expose
    @ConfigLink(owner = BingoConfig::class, field = "minionCraftHelperEnabled")
    val minionCraftHelperPos: Position = Position(10, 10)

    @Expose
    @ConfigOption(
        name = "Boop Party",
        desc = "Send party invite to players that boop you while you are on a Bingo profile."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var boopParty: Boolean = false

    @Expose
    @FeatureToggle
    @ConfigOption(
        name = "Splasher Overlay",
        desc = "Show Data that is useful for a Splasher in an Overlay after you announced a Splash."
    )
    val useSplasherOverlay: Boolean = true

    @FeatureToggle
    @Expose
    @ConfigOption(
        name = "Show Splash Status Updates",
        desc = "Will inform you about Splash Status Updates in the Chat."
    )
    val showSplashStatusUpdates: Boolean = true

    @Expose
    @ConfigOption(name = "Bingo Multipurpose", desc = "Used for various Features in regards to Bingo such as Warp to Hub Selector (Splash) or Minion crafting.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_R)
    val bingoKeybind: Property<Int> = Property.of(Keyboard.KEY_R)
}
