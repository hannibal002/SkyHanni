package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.NoConfigLink
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.ItemPriceSource
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SackItemDisplayConfig {

    @Expose
    @ConfigOption(name = "Note", desc = "These Settings are for /shdisplaysackitem")
    @ConfigEditorInfoText
    val notice = ""

    @Expose
    @ConfigOption(name = "Price Source", desc = "")
    @ConfigEditorDropdown
    val priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_SELL

    @Expose
    @NoConfigLink
    val position: Position = Position(300, 20)
}
