package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

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
    var hypixelHub: Boolean = false

    @Expose
    @ConfigOption(name = "Empty", desc = "Hide all empty messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var empty: Boolean = false

    @Expose
    @ConfigOption(name = "Warping", desc = "Hide 'Sending request to join...' and 'Warping...' messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var warping: Boolean = false

    @Expose
    @ConfigOption(name = "Welcome", desc = "Hide the 'Welcome to SkyBlock' message.")
    @ConfigEditorBoolean
    @FeatureToggle
    var welcome: Boolean = false

    @Expose
    @ConfigOption(name = "Guild/Event EXP", desc = "Hide Guild and Event EXP messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var guildEventExp: Boolean = false

    @Expose
    @ConfigOption(name = "Friend Join/Left", desc = "Hide friend join/left messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var friendJoinLeft: Boolean = false

    @Expose
    @ConfigOption(name = "Winter Gifts", desc = "Hide pointless Winter Gift messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var winterGift: Boolean = false

    @Expose
    @ConfigOption(name = "Kill Combo", desc = "Hide messages about your Kill Combo from the Grandma Wolf pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    var killCombo: Boolean = false

    @Expose
    @ConfigOption(
        name = "Watchdog",
        desc = "Hide the message where Hypixel flexes about how many players they have banned over the last week.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var watchDog: Boolean = false

    @Expose
    @ConfigOption(name = "Profile Join", desc = "Hide 'You are playing on profile' and 'Profile ID' chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var profileJoin: Boolean = false

    @Expose
    @ConfigOption(name = "Fire Sale", desc = "Hide the repeating fire sale reminder chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var fireSale: Boolean = false

    @Expose
    @ConfigOption(name = "Reward Bundles", desc = "Hide the reminders to claim seasonal reward bundles.")
    @ConfigEditorBoolean
    @FeatureToggle
    var rewardBundles: Boolean = false

    @Expose
    @ConfigOption(name = "Event Level Up", desc = "Hide event level up messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var eventLevelUp: Boolean = false

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
    @ConfigOption(name = "Hoppity's Hunt Begin", desc = "Hide \"Hoppity's Hunt has begun\" messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hoppityBegun: Boolean = false

    @Expose
    @ConfigOption(name = "Hoppity's Hunt Eggs", desc = "Hide \"An egg has appeared!\" messages during hoppity's hunt.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hoppityEggs: Boolean = false

    @Expose
    @ConfigOption(name = "Sacrifice", desc = "Hide other players' sacrifice messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var sacrifice: Boolean = false

    @Expose
    @ConfigOption(name = "Garden Pest", desc = "Hide the message of no pests on garden.")
    @ConfigEditorBoolean
    @FeatureToggle
    var gardenNoPest: Boolean = false

    @Expose
    @ConfigOption(name = "Legacy Items Warning", desc = "Hide the legacy items in sacks/storage warning.")
    @ConfigEditorBoolean
    @FeatureToggle
    var legacyItemsWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Block Alpha Achievements", desc = "Hide achievement messages while on the Alpha network.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideAlphaAchievements: Boolean = false

    @Expose
    @ConfigOption(
        name = "Parkour Messages",
        desc = "Hide parkour messages (starting, stopping, reaching a checkpoint).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var parkour: Boolean = false

    @Expose
    @ConfigOption(name = "Teleport Pad Messages", desc = "Hide annoying messages when using teleport pads.")
    @ConfigEditorBoolean
    @FeatureToggle
    var teleportPads: Boolean = false

    @Expose
    @ConfigOption(name = "Feast Chef Ted", desc = "Hide annoying messages about Kernels getting added to your purse while farming.")
    @ConfigEditorBoolean
    @FeatureToggle
    var masterChef: Boolean = false

    @Expose
    @ConfigOption(
        name = "Transaction Setup",
        desc = "Hide the progress messages while using the Bazaar, Auction House, or Bank (e.g. 'Putting item in escrow...').",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var transactionSetup: Boolean = false

    @Expose
    @ConfigOption(
        name = "Transaction Orders",
        desc = "Hide Bazaar order setup/cancellation messages and the Auction House collection reminder.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var auctionBazaarSetup: Boolean = false

    @Expose
    @ConfigOption(name = "Slayer Quests", desc = "Hide Slayer Quest start and complete announcements.")
    @ConfigEditorBoolean
    @FeatureToggle
    var slayer: Boolean = false

    @Expose
    @ConfigOption(name = "Slayer Drops", desc = "Hide Slayer rare and very rare drop announcements.")
    @ConfigEditorBoolean
    @FeatureToggle
    var slayerDrop: Boolean = false

    @Expose
    @ConfigOption(
        name = "Low Value Drops",
        desc = "Hide rare drop announcements for low value items, e.g. Carrot, Potato, Enchanted Ender Pearl.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var uselessDrop: Boolean = false

    @Expose
    @ConfigOption(
        name = "Useless Notifications",
        desc = "Hide player tipped and bank interest payout messages.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var uselessNotifications: Boolean = false

    @Expose
    @ConfigOption(
        name = "Mining Abilities",
        desc = "Hide Heart of the Mountain pickaxe ability used/expired messages for Mining Speed Boost, " +
            "Maniac Miner, Tunnel Vision, Gemstone Infusion, Sheer Force, and Pickobulus.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var miningAbilities: Boolean = false

    @Expose
    @ConfigOption(
        name = "Deployables",
        desc = "Hide the message when a previously placed deployable (Power Orb, Lantern, Flare, " +
            "Black Hole, etc.) gets replaced by a new one.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var deployables: Boolean = false

    @Expose
    @ConfigOption(name = "Snow Cannons", desc = "Hide Jerry's Workshop Snow Cannon mount messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var winterIsland: Boolean = false

    @Expose
    @ConfigOption(name = "Party Separator", desc = "Hide the party message separator line.")
    @ConfigEditorBoolean
    @FeatureToggle
    var party: Boolean = false

    @Expose
    @ConfigOption(
        name = "Annoying Warnings",
        desc = "Hide annoying warning messages, e.g. rate limit warnings, combat restrictions.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var uselessWarning: Boolean = false

    @Expose
    @ConfigOption(
        name = "Ability Damage",
        desc = "Hide ability damage notifications for abilities like Wither Impact, Guided Bat, etc.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var abilityDamage: Boolean = false

    @Expose
    @ConfigOption(
        name = "Blocked Actions",
        desc = "Hide messages when an action is blocked, e.g. Instant Transmission having blocks in the way, insufficient Breaking Power.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var blockedActions: Boolean = false

    @Expose
    @ConfigOption(
        name = "NPC Announcements",
        desc = "Hide NPC announcement messages, e.g. Jacob's contest starting, Booster cookie required for action.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var npcAnnouncements: Boolean = false

    @Expose
    @ConfigOption(
        name = "System/Dev Noise",
        desc = "Hide internal system and debug messages that sometimes leak into the chat.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var systemNoise: Boolean = false
}
