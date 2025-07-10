package at.hannibal2.skyhanni.config.features.garden.visitor

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class RewardWarningConfig {
    @Expose
    @ConfigOption(name = "Notify in Chat", desc = "Send a chat message once you talk to a visitor with a reward.")
    @ConfigEditorBoolean
    @FeatureToggle
    var notifyInChat: Boolean = true

    @Expose
    @ConfigOption(name = "Show over Name", desc = "Show the reward name above the visitor name.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showOverName: Boolean = true

    @Expose
    @ConfigOption(name = "Block Refusing Reward", desc = "Prevent refusing visitors with a reward.")
    @ConfigEditorBoolean
    @FeatureToggle
    var preventRefusing: Boolean = true

    @Expose
    @ConfigOption(name = "Bypass Key", desc = "Hold this key to bypass the Prevent Refusing feature.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LCONTROL)
    var bypassKey: Int = Keyboard.KEY_LCONTROL


    @Expose
    @ConfigOption(name = "Items", desc = "Warn for these reward item visitor drops.")
    @ConfigEditorDraggableList
    val drops: MutableList<VisitorReward> = mutableListOf(
        VisitorReward.OVERGROWN_GRASS,
        VisitorReward.GREEN_BANDANA,
        VisitorReward.DEDICATION,
        VisitorReward.MUSIC_RUNE,
        VisitorReward.SPACE_HELMET,
        VisitorReward.CULTIVATING,
        VisitorReward.REPLENISH,
        VisitorReward.COPPER_DYE
    )

    @Expose
    @ConfigOption(
        name = "Coins per Copper",
        desc = "The price to use for the options below.\n" +
            "Requires at least one of them to be on."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 50_000f, minStep = 250f)
    var coinsPerCopperPrice: Int = 6_000

    @Expose
    @ConfigOption(
        name = "Block Refusing Copper",
        desc = "Prevent refusing visitors with a coins per copper lower than the set value."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var preventRefusingCopper: Boolean = false

    @Expose
    @ConfigOption(
        name = "Block Accepting Copper",
        desc = "Prevent accepting visitors with a coins per copper higher than the set value."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var preventAcceptingCopper: Boolean = false

    @Expose
    @ConfigOption(
        name = "Acceptable Coin Loss",
        desc = "The price to use for the below options.\n" +
            "Requires one of the below options to be on.\n" +
            "Above options take precedence."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 500_000f, minStep = 1_000f)
    var coinsLossThreshold: Int = 150_000

    @Expose
    @ConfigOption(
        name = "Block Refusing Low Loss",
        desc = "Prevent refusing a visitor with a net loss lower than a certain value."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var preventRefusingLowLoss: Boolean = false

    @Expose
    @ConfigOption(
        name = "Block Accepting High Loss",
        desc = "Prevent accepting a visitor with a net loss higher than a certain value."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var preventAcceptingHighLoss: Boolean = false

    @Expose
    @ConfigOption(
        name = "Block Refusing New Visitors",
        desc = "Prevents refusing a visitor you've never completed an offer with.\n" +
            "§eDisabled while on bingo."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var preventRefusingNew: Boolean = true

    @Expose
    @ConfigOption(name = "Opacity", desc = "How strong the offer buttons should be grayed out when blocked.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 255f, minStep = 5f)
    var opacity: Int = 180

    @Expose
    @ConfigOption(name = "Outline", desc = "Add a red/green line around the best offer buttons.")
    @ConfigEditorBoolean
    var optionOutline: Boolean = true
}
