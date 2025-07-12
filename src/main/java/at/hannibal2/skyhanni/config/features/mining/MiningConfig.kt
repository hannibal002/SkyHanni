package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.features.mining.caverns.DeepCavernsGuideConfig
import at.hannibal2.skyhanni.config.features.mining.dwarves.KingTalismanConfig
import at.hannibal2.skyhanni.config.features.mining.glacite.ColdOverlayConfig
import at.hannibal2.skyhanni.config.features.mining.glacite.FossilExcavatorConfig
import at.hannibal2.skyhanni.config.features.mining.glacite.GlaciteMineshaftConfig
import at.hannibal2.skyhanni.config.features.mining.glacite.MineshaftConfig
import at.hannibal2.skyhanni.config.features.mining.glacite.MineshaftPityDisplayConfig
import at.hannibal2.skyhanni.config.features.mining.glacite.TunnelMapsConfig
import at.hannibal2.skyhanni.config.features.mining.nucleus.AreaWallsConfig
import at.hannibal2.skyhanni.config.features.mining.nucleus.CrystalHighlighterConfig
import at.hannibal2.skyhanni.config.features.mining.nucleus.CrystalNucleusTrackerConfig
import at.hannibal2.skyhanni.config.features.mining.nucleus.MetalDetectorConfig
import at.hannibal2.skyhanni.config.features.mining.nucleus.PowderChestTimerConfig
import at.hannibal2.skyhanni.config.features.mining.nucleus.PowderTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class MiningConfig {
    @Expose
    @Category(name = "Mining Event Tracker", desc = "Settings for the Mining Event Tracker.")
    val miningEvent: MiningEventConfig = MiningEventConfig()

    @Expose
    @Category(name = "HotM", desc = "Settings for Heart of the Mountain.")
    val hotm: HotmConfig = HotmConfig()

    @Expose
    @ConfigOption(name = "Powder Tracker", desc = "")
    @Accordion
    val powderTracker: PowderTrackerConfig = PowderTrackerConfig()

    @Expose
    @ConfigOption(name = "Powder Chest Timer", desc = "")
    @Accordion
    val powderChestTimer: PowderChestTimerConfig = PowderChestTimerConfig()

    @Expose
    @ConfigOption(name = "King Talisman", desc = "")
    @Accordion
    val kingTalisman: KingTalismanConfig = KingTalismanConfig()

    @Expose
    @ConfigOption(name = "Deep Caverns Guide", desc = "")
    @Accordion
    val deepCavernsGuide: DeepCavernsGuideConfig = DeepCavernsGuideConfig()

    @Expose
    @ConfigOption(name = "Area Walls", desc = "")
    @Accordion
    val crystalHollowsAreaWalls: AreaWallsConfig = AreaWallsConfig()

    @Expose
    @ConfigOption(name = "Crystal Nucleus Tracker", desc = "")
    @Accordion
    val crystalNucleusTracker: CrystalNucleusTrackerConfig = CrystalNucleusTrackerConfig()

    @Expose
    @ConfigOption(name = "Metal Detector", desc = "")
    @Accordion
    val metalDetector: MetalDetectorConfig = MetalDetectorConfig()

    @Expose
    @ConfigOption(name = "Cold Overlay", desc = "")
    @Accordion
    val coldOverlay: ColdOverlayConfig = ColdOverlayConfig()

    @Expose
    @Category(name = "Fossil Excavator", desc = "Settings for the Fossil Excavator Features.")
    val fossilExcavator: FossilExcavatorConfig = FossilExcavatorConfig()

    @Expose
    @Category(name = "Glacite Mineshaft", desc = "Settings for the Glacite Mineshaft.")
    val glaciteMineshaft: GlaciteMineshaftConfig = GlaciteMineshaftConfig()

    @Expose
    @ConfigOption(name = "Notifications", desc = "")
    @Accordion
    val notifications: MiningNotificationsConfig = MiningNotificationsConfig()

    @Expose
    @Category(name = "Tunnel Maps", desc = "Settings for the Tunnel Maps.")
    val tunnelMaps: TunnelMapsConfig = TunnelMapsConfig()

    @Expose
    @ConfigOption(name = "Commissions Blocks Color", desc = "")
    @Accordion
    val commissionsBlocksColor: CommissionsBlocksColorConfig = CommissionsBlocksColorConfig()

    @Expose
    @ConfigOption(name = "Mineshaft", desc = "")
    @Accordion
    val mineshaft: MineshaftConfig = MineshaftConfig()

    @Expose
    @ConfigOption(name = "Mineshaft Pity Display", desc = "")
    @Accordion
    val mineshaftPityDisplay: MineshaftPityDisplayConfig = MineshaftPityDisplayConfig()

    @Expose
    @ConfigOption(name = "Crystal Nucleus Crystal Highlights", desc = "")
    @Accordion
    val crystalHighlighter: CrystalHighlighterConfig = CrystalHighlighterConfig()

    @Expose
    @ConfigOption(name = "Flowstate Helper", desc = "")
    @Accordion
    val flowstateHelper: FlowstateHelperConfig = FlowstateHelperConfig()

    @Expose
    @ConfigOption(name = "Highlight Commission Mobs", desc = "Highlight mobs that are part of active commissions.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightCommissionMobs: Boolean = false

    @Expose
    @ConfigOption(
        name = "Names in Core",
        desc = "Show the names of the 4 areas while in the center of the Crystal Hollows.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var crystalHollowsNamesInCore: Boolean = false

    @Expose
    @ConfigOption(name = "Private Island Ability Block", desc = "Block the mining ability when on private island.")
    @SearchTag("Pickaxe Pickobulus")
    @ConfigEditorBoolean
    @FeatureToggle
    var privateIslandNoPickaxeAbility: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight your Golden Goblin", desc = "Highlight golden goblins you have spawned in green.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightYourGoldenGoblin: Boolean = true

    @Expose
    @ConfigOption(
        name = "Line to your Golden Goblin",
        desc = "Also makes a line to your goblin. §eNeeds the option above to work.",
    )
    @ConfigEditorBoolean
    var lineToYourGoldenGoblin: Boolean = false

    @Expose
    @ConfigOption(name = "Precision Mining Helper", desc = "Draws a box over the Precision Mining particles.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightPrecisionMiningParticles: Boolean = false

    @Expose
    @ConfigOption(name = "Mute High Heat", desc = "Mutes the panting sound when above 90 heat in the Magma Fields.")
    @ConfigEditorBoolean
    @FeatureToggle
    var muteHighHeat: Boolean = true
}
