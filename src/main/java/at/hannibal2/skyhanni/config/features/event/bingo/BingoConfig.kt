package at.hannibal2.skyhanni.config.features.event.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BingoConfig {
    @Expose
    @ConfigOption(name = "Bingo Card", desc = "")
    @Accordion
    val bingoCard: BingoCardConfig = BingoCardConfig()

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

    @ConfigOption(name = "Bingo Boop Party", desc = "Bingo Boop Party has been moved to Misc. Click here to jump straight to it.")
    @ConfigEditorButton(buttonText = "Go")
    val boopPartyJumpButton = Runnable { SkyHanniMod.feature.misc.boopParty::boopPartyBingo.jumpToEditor() }

    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(130, "event.bingo.boopParty", "misc.boopParty.boopPartyBingo")
        }
    }
}
