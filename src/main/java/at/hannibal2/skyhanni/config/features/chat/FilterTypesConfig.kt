package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class FilterTypesConfig {
    @Expose
    @ConfigOption(name = "Powder Mining", desc = "")
    @Accordion
    val powderMining: PowderMiningConfig = PowderMiningConfig()

    @Expose
    @ConfigOption(name = "Crystal Nucleus", desc = "")
    @Accordion
    val crystalNucleus: CrystalNucleusConfig = CrystalNucleusConfig()

    @Expose
    @ConfigOption(name = "Foraging", desc = "")
    @Accordion
    val foraging: ForagingFilterConfig = ForagingFilterConfig()

    @Expose
    @ConfigOption(name = "Hunting", desc = "")
    @Accordion
    val hunting: HuntingFilterConfig = HuntingFilterConfig()

    @Expose
    @ConfigOption(name = "Stash Messages", desc = "")
    @Accordion
    val stashMessages: StashConfig = StashConfig()

    @Expose
    @ConfigOption(
        name = "Hypixel Lobbies",
        desc = "Hide announcements in Hypixel lobbies " +
            "(player joins, loot boxes, prototype lobby messages, radiating generosity, Hypixel tournaments)",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val hypixelHub: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Empty", desc = "Hide all empty messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val empty: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Warping", desc = "Hide 'Sending request to join...' and 'Warping...' messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val warping: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Welcome", desc = "Hide the 'Welcome to SkyBlock' message.")
    @ConfigEditorBoolean
    @FeatureToggle
    val welcome: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Guild/Event EXP", desc = "Hide Guild and Event EXP messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val guildEventExp: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Friend Join/Left", desc = "Hide friend join/left messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val friendJoinLeft: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Winter Gifts", desc = "Hide pointless Winter Gift messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val winterGift: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Kill Combo", desc = "Hide messages about your Kill Combo from the Grandma Wolf pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    val killCombo: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Watchdog",
        desc = "Hide the message where Hypixel flexes about how many players they have banned over the last week.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val watchDog: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Profile Join", desc = "Hide 'You are playing on profile' and 'Profile ID' chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val profileJoin: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Fire Sale", desc = "Hide the repeating fire sale reminder chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val fireSale: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Reward Bundles", desc = "Hide the reminders to claim seasonal reward bundles.")
    @ConfigEditorBoolean
    @FeatureToggle
    val rewardBundles: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Event Level Up", desc = "Hide event level up messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val eventLevelUp: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Diana",
        desc = "Hide chat messages around griffin burrow chains, griffin feather drops, and coin drops.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val diana: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Factory Upgrade",
        desc = "Hide §nHypixel's§r Chocolate Factory upgrade and employee promotion messages.\n" +
            "§eTo turn off SkyHanni's upgrade messages, search §lUpgrade Warning",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val factoryUpgrade: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hoppity's Hunt Begin", desc = "Hide \"Hoppity's Hunt has begun\" messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val hoppityBegun: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hoppity's Hunt Eggs", desc = "Hide \"An egg has appeared!\" messages during hoppity's hunt.")
    @ConfigEditorBoolean
    @FeatureToggle
    val hoppityEggs: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Sacrifice", desc = "Hide other players' sacrifice messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val sacrifice: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Garden Pest", desc = "Hide the message of no pests on garden.")
    @ConfigEditorBoolean
    @FeatureToggle
    val gardenNoPest: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Legacy Items Warning", desc = "Hide the legacy items in sacks/storage warning.")
    @ConfigEditorBoolean
    @FeatureToggle
    val legacyItemsWarning: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Block Alpha Achievements", desc = "Hide achievement messages while on the Alpha network.")
    @ConfigEditorBoolean
    @FeatureToggle
    val hideAlphaAchievements: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Parkour Messages",
        desc = "Hide parkour messages (starting, stopping, reaching a checkpoint).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val parkour: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Teleport Pad Messages", desc = "Hide annoying messages when using teleport pads.")
    @ConfigEditorBoolean
    @FeatureToggle
    val teleportPads: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Feast Chef Ted", desc = "Hide annoying messages about Kernels getting added to your purse while farming.")
    @ConfigEditorBoolean
    @FeatureToggle
    val masterChef: Property<Boolean> = Property.of(false)

    // TODO remove
    @Expose
    @ConfigOption(name = "Others", desc = "Hide other annoying messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    val others: Property<Boolean> = Property.of(false)
}
