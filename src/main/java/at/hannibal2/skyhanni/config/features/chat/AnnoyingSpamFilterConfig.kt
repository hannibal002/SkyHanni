package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class AnnoyingSpamFilterConfig {

    @Expose
    @ConfigOption(name = "Bait Catches", desc = "Hides 'You found a X bait' messages.")
    @ConfigEditorBoolean
    var bait: Boolean = false

    @Expose
    @ConfigOption(name = "Blessing Enchant", desc = "Hides 'Your Blessing enchant got you double drops!' messages.")
    @ConfigEditorBoolean
    var blessing: Boolean = false

    @Expose
    @ConfigOption(name = "Blocks In The Way", desc = "Hides 'There are blocks in the way!' messages.")
    @SearchTag("teleport")
    @ConfigEditorBoolean
    var blockWay: Boolean = false

    @Expose
    @ConfigOption(
        name = "Breaking Power",
        desc = "Hides messages about breaking power when attempting to mine a block with unsuitable pickaxe.",
    )
    @ConfigEditorBoolean
    var breakingPower: Boolean = false

    @Expose
    @ConfigOption(name = "Obtain Cookie", desc = "Hides 'Obtain a Booster Cookie from the community shop in the hub!' message")
    @ConfigEditorBoolean
    var cookie: Boolean = false

    @Expose
    @ConfigOption(name = "SBE Bin Data", desc = "Hides SBE 'Unable to download bin data.' message.")
    @ConfigEditorBoolean
    var sbe: Boolean = false

    @Expose
    @ConfigOption(name = "Sacrifice", desc = "Hide other players' sacrifice messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var sacrifice: Boolean = false
}
