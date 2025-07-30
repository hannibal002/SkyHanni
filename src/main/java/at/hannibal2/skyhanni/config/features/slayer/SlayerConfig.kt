package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.features.slayer.blaze.BlazeConfig
import at.hannibal2.skyhanni.config.features.slayer.endermen.EndermanConfig
import at.hannibal2.skyhanni.config.features.slayer.spider.SpiderConfig
import at.hannibal2.skyhanni.config.features.slayer.vampire.VampireConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class SlayerConfig {

    @Expose
    @ConfigOption(name = "Spider", desc = "")
    @Accordion
    val spider: SpiderConfig = SpiderConfig()

    // TODO rename to "enderman"
    @Expose
    @Category(name = "Enderman", desc = "Enderman Slayer Feature")
    @Accordion
    val endermen: EndermanConfig = EndermanConfig()

    // TODO rename to "blaze"
    @Expose
    @Category(name = "Blaze", desc = "Blaze Slayer Features")
    val blazes: BlazeConfig = BlazeConfig()

    @Expose
    @Category(name = "Vampire", desc = "Vampire Slayer Features")
    val vampire: VampireConfig = VampireConfig()

    @Expose
    @ConfigOption(name = "Item Profit Tracker", desc = "")
    @Accordion
    val itemProfitTracker: SlayerProfitTrackerConfig = SlayerProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Items on Ground", desc = "")
    @Accordion
    val itemsOnGround: ItemsOnGroundConfig = ItemsOnGroundConfig()

    @Expose
    @ConfigOption(name = "RNG Meter Display", desc = "")
    @Accordion
    val rngMeterDisplay: RngMeterDisplayConfig = RngMeterDisplayConfig()

    @Expose
    @ConfigOption(name = "Boss Spawn Warning", desc = "")
    @Accordion
    val slayerBossWarning: SlayerBossWarningConfig = SlayerBossWarningConfig()

    @Expose
    @ConfigOption(
        name = "Block Not Spawnable",
        desc = "Prevent clicking slayer bosses that cannot be spawned in the current dimension in Maddox's menu.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var blockNotSpawnable: Boolean = true

    @Expose
    @ConfigOption(name = "Miniboss Highlight", desc = "Highlight Slayer Mini-Boss in blue color.")
    @ConfigEditorBoolean
    @FeatureToggle
    var slayerMinibossHighlight: Boolean = false

    @Expose
    @ConfigOption(name = "Line to Miniboss", desc = "Add a line to every Slayer Mini-Boss around you.")
    @ConfigEditorBoolean
    @FeatureToggle
    var slayerMinibossLine: Boolean = false

    @Expose
    @ConfigOption(
        name = "Line to Miniboss Width",
        desc = "The width of the line pointing to every Slayer Mini-Boss around you.",
    )
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 10f)
    var slayerMinibossLineWidth: Int = 3

    @Expose
    @ConfigOption(
        name = "Hide Mob Names",
        desc = "Hide the name of the mobs you need to kill in order for the Slayer boss to spawn. " +
            "Exclude mobs that are damaged, corrupted, runic or semi rare.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideMobNames: Boolean = false

    @Expose
    @ConfigOption(
        name = "Quest Warning",
        desc = "Warn when wrong Slayer quest is selected, or killing mobs for the wrong Slayer.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var questWarning: Boolean = true

    @Expose
    @ConfigOption(name = "Quest Warning Title", desc = "Send a title when warning.")
    @ConfigEditorBoolean
    @FeatureToggle
    var questWarningTitle: Boolean = true

    @Expose
    @ConfigOption(
        name = "Hide Irrelevant Mobs",
        desc = "Makes mobs partially transparent so that they dont annoy while having an active slayer quest. " +
            "Useful for e.g. Magma Cubes in Burning Desert for Tara Slayer.",
    )
    @SearchTag("tarantula spider opacity")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideIrrelevantMobs: Boolean = false

    @Expose
    @ConfigOption(
        name = "Adjust Irrelevant Opacity",
        desc = "Adjust the opacity of irrelevant mobs. (in %)",
    )
    @SearchTag("magma cube tarantula tara spider slayer quest")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var hideIrrelevantMobsOpacity: Int = 40

    @Expose
    @ConfigOption(name = "Time to Kill Message", desc = "Sends time to kill a slayer in chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var timeToKillMessage: Boolean = true

    @Expose
    @ConfigOption(name = "Quest Complete Message", desc = "Sends time to complete (Spawn & Kill) a slayer quest in chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var questCompleteMessage: Boolean = true

    @Expose
    @ConfigOption(name = "Compact Time Messages", desc = "Shorter Time to Kill and Quest Complete messages.")
    @ConfigEditorBoolean
    var compactTimeMessage: Boolean = false
}
