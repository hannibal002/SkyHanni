package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EventsFilterConfig {

    @Expose
    @ConfigOption(name = "Cannon Mount", desc = "Hide messages when players mount a cannon on Jerry's workshop")
    @ConfigEditorBoolean
    var cannon: Boolean = false

    @Expose
    @ConfigOption(
        name = "Diana",
        desc = "Hide chat messages around griffin burrow chains, griffin feather drops, and coin drops.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var diana: Boolean = false

    @Expose
    @ConfigOption(
        name = "Factory Upgrade",
        desc = "Hide §nHypixel's§r Chocolate Factory upgrade and employee promotion messages.\n" +
            "§eTo turn off SkyHanni's upgrade messages, search §lUpgrade Warning",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var factoryUpgrade: Boolean = false

    @Expose
    @ConfigOption(name = "Winter Gifts", desc = "Hide pointless Winter Gift messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var winterGift: Boolean = false

}
