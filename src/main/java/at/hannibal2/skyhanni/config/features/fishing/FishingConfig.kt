package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.TrophyFishingConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FishingConfig {
    @Expose
    @Category(name = "Trophy Fishing", desc = "Trophy Fishing Settings")
    val trophyFishing: TrophyFishingConfig = TrophyFishingConfig()

    @Expose
    @ConfigOption(name = "Thunder Spark", desc = "")
    @Accordion
    val thunderSpark: ThunderSparkConfig = ThunderSparkConfig()

    @Expose
    @ConfigOption(name = "Barn Fishing Timer", desc = "")
    @Accordion
    val barnTimer: BarnTimerConfig = BarnTimerConfig()

    @Expose
    @ConfigOption(name = "Chum/Chumcap Bucket Hider", desc = "")
    @Accordion
    val chumBucketHider: ChumBucketHiderConfig = ChumBucketHiderConfig()

    @Expose
    @ConfigOption(name = "Fished Item Name", desc = "")
    @Accordion
    val fishedItemName: FishedItemNameConfig = FishedItemNameConfig()

    @Expose
    @ConfigOption(name = "Fishing Hook Display", desc = "")
    @Accordion
    val fishingHookDisplay: FishingHookDisplayConfig = FishingHookDisplayConfig()

    @Expose
    @ConfigOption(name = "Bait Warnings", desc = "")
    @Accordion
    val fishingBaitWarnings: FishingBaitWarningsConfig = FishingBaitWarningsConfig()

    @Expose
    @ConfigOption(name = "Rare Sea Creatures", desc = "")
    @Accordion
    val rareCatches: RareCatchesConfig = RareCatchesConfig()

    @Expose
    @ConfigOption(name = "Fishing Profit Tracker", desc = "")
    @Accordion
    val fishingProfitTracker: FishingProfitTrackerConfig = FishingProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Totem of Corruption", desc = "")
    @Accordion
    val totemOfCorruption: TotemOfCorruptionConfig = TotemOfCorruptionConfig()

    @Expose
    @ConfigOption(name = "Sea Creature Tracker", desc = "")
    @Accordion
    val seaCreatureTracker: SeaCreatureTrackerConfig = SeaCreatureTrackerConfig()

    @Expose
    @ConfigOption(name = "Lava Replacement", desc = "")
    @Accordion
    val lavaReplacement: LavaReplacementConfig = LavaReplacementConfig()

    @Expose
    @ConfigOption(name = "Shark Fish Counter", desc = "Counts how many Sharks have been caught.")
    @ConfigEditorBoolean
    @FeatureToggle
    var sharkFishCounter: Boolean = false

    @Expose
    @ConfigLink(owner = FishingConfig::class, field = "sharkFishCounter")
    val sharkFishCounterPos: Position = Position(10, 10)

    @Expose
    @ConfigOption(
        name = "Shorten Fishing Message",
        desc = "Shorten the chat message that says what type of Sea Creature you have fished.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var shortenFishingMessage: Boolean = false

    @Expose
    @ConfigOption(
        name = "Compact Double Hook",
        desc = "Add Double Hook to the Sea Creature chat message instead of in a previous line.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var compactDoubleHook: Boolean = true

    @Expose
    @ConfigOption(name = "Hotspot Radar Guesser", desc = "Shows where the closest Fishing Hotspot is when using the §9Hotspot Radar§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    var guessHotspotRadar: Boolean = true

    @Expose
    @ConfigOption(
        name = "Pathfind to Hotspots",
        desc = "When the Hotspot Radar Guesser feature finds a target, shows a pathfind to that Fishing Hotspot.",
    )
    @ConfigEditorBoolean
    var guessHotspotRadarPathFind: Boolean = true

    @Expose
    @ConfigOption(name = "Line to Hotspot", desc = "Draws a line towards the Fishing Hotspot.")
    @ConfigEditorBoolean
    @FeatureToggle
    var lineToHotspot: Boolean = false
}
