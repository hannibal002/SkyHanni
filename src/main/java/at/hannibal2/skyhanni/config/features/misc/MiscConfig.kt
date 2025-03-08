package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.config.features.commands.CommandsConfig
import at.hannibal2.skyhanni.config.features.garden.NextJacobContestConfig
import at.hannibal2.skyhanni.config.features.minion.MinionsConfig
import at.hannibal2.skyhanni.config.features.misc.pets.PetConfig
import at.hannibal2.skyhanni.config.features.stranded.StrandedConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

class MiscConfig {
    @Expose
    @Category(name = "Pets", desc = "Pets Settings")
    var pets: PetConfig = PetConfig()

    @Expose
    @Category(name = "Commands", desc = "Enable or disable commands.")
    var commands: CommandsConfig = CommandsConfig()

    @Expose
    @Category(name = "Party Commands", desc = "Enable or disable party commands.")
    var partyCommands: PartyCommandsConfig = PartyCommandsConfig()

    @Expose
    @Category(name = "Minions", desc = "The minions on your private island.")
    var minions: MinionsConfig = MinionsConfig()

    @Expose
    @Category(name = "Stranded", desc = "Features for the Stranded game mode.")
    var stranded: StrandedConfig = StrandedConfig()

    @Expose
    @Category(name = "Area Navigation", desc = "Helps navigate to different areas on the current island.")
    var areaNavigation: AreaNavigationConfig = AreaNavigationConfig()

    // TODO maybe we can migrate this already
    @Expose
    @ConfigOption(name = "Hide Armor", desc = "")
    @Accordion
    var hideArmor2: HideArmorConfig = HideArmorConfig()

    // TODO rename nonGodPotEffect
    @Expose
    @ConfigOption(name = "Non-God Pot Effects", desc = "")
    @Accordion
    var potionEffect: PotionEffectsConfig = PotionEffectsConfig()

    @JvmField
    @Expose
    @ConfigOption(name = "Particle Hider", desc = "")
    @Accordion
    var particleHiders: ParticleHiderConfig = ParticleHiderConfig()

    @Expose
    @ConfigOption(name = "Trevor The Trapper", desc = "")
    @Accordion
    var trevorTheTrapper: TrevorTheTrapperConfig = TrevorTheTrapperConfig()

    @Expose
    @ConfigOption(name = "Teleport Pads On Private Island", desc = "")
    @Accordion
    var teleportPad: TeleportPadConfig = TeleportPadConfig()

    @Expose
    @ConfigOption(name = "Quick Mod Menu Switch", desc = "")
    @Accordion
    var quickModMenuSwitch: QuickModMenuSwitchConfig = QuickModMenuSwitchConfig()

    @Expose
    @ConfigOption(name = "Glowing Dropped Items", desc = "")
    @Accordion
    var glowingDroppedItems: GlowingDroppedItemsConfig = GlowingDroppedItemsConfig()

    @Expose
    @ConfigOption(name = "Highlight Party Members", desc = "")
    @Accordion
    var highlightPartyMembers: HighlightPartyMembersConfig = HighlightPartyMembersConfig()

    @Expose
    @ConfigOption(name = "Kick Duration", desc = "")
    @Accordion
    var kickDuration: KickDurationConfig = KickDurationConfig()

    @Expose
    @ConfigOption(name = "Tracker", desc = "Tracker Config")
    @Accordion
    var tracker: TrackerConfig = TrackerConfig()

    @Expose
    @ConfigOption(name = "Pet Candy Display", desc = "")
    @Accordion
    var petCandy: PetCandyDisplayConfig = PetCandyDisplayConfig()

    @Expose
    @ConfigOption(name = "Bits Features", desc = "")
    @Accordion
    var bits: BitsConfig = BitsConfig()

    @Expose
    @ConfigOption(name = "Patcher Coords Waypoints", desc = "")
    @Accordion
    var patcherCoordsWaypoint: PatcherCoordsWaypointConfig = PatcherCoordsWaypointConfig()

    @Expose
    @ConfigOption(name = "Reminders", desc = "")
    @Accordion
    var reminders: RemindersConfig = RemindersConfig()

    @Expose
    @ConfigOption(name = "Last Servers", desc = "")
    @Accordion
    var lastServers: LastServersConfig = LastServersConfig()

    @Expose
    @ConfigOption(name = "Enchanted Clock", desc = "")
    @Accordion
    var enchantedClock: EnchantedClockConfig = EnchantedClockConfig()

    @Expose
    @ConfigOption(name = "Century Party Invitation", desc = "Features for the Century Party Invitation")
    @Accordion
    var centuryPartyInvitation: CenturyPartyInvitationConfig = CenturyPartyInvitationConfig()

    @Expose
    @ConfigOption(name = "Reset Search on Close", desc = "Reset the search in GUIs after closing the inventory.")
    @ConfigEditorBoolean
    var resetSearchGuiOnClose: Boolean = true

    @Expose
    @ConfigOption(name = "Show Outside SkyBlock", desc = "Show these features outside of SkyBlock.")
    @ConfigEditorDraggableList
    var showOutsideSB: Property<MutableList<OutsideSBFeature>> = Property.of(mutableListOf())

    @Expose
    @ConfigOption(name = "Auto Join Skyblock", desc = "Automatically join Skyblock when you join Hypixel.")
    @ConfigEditorBoolean
    @FeatureToggle
    var autoJoinSkyblock: Boolean = false

    @Expose
    @ConfigOption(name = "Exp Bottles", desc = "Hide all the experience orbs lying on the ground.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideExpBottles: Boolean = false

    // TODO rename to hideTemporaryArmorStands
    @Expose
    @ConfigOption(
        name = "Armor Stands",
        desc = "Hide armor stands that are sometimes visible for a fraction of a second.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideTemporaryArmorstands: Boolean = true

    @Expose
    var collectionCounterPos: Position = Position(10, 10, false, true)

    @Expose
    var carryPosition: Position = Position(10, 10, false, true)

    @Expose
    @ConfigOption(name = "Brewing Stand Overlay", desc = "Display the item names directly inside the Brewing Stand.")
    @ConfigEditorBoolean
    @FeatureToggle
    var brewingStandOverlay: Boolean = true

    @Expose
    @ConfigOption(name = "Crash On Death", desc = "Crashes your game every time you die in Skyblock")
    @ConfigEditorBoolean
    var crashOnDeath: Boolean = false

    // TODO rename to skyblockXPBar
    @Expose
    @ConfigOption(
        name = "SkyBlock XP Bar",
        desc = "Replaces the vanilla XP bar with a SkyBlock XP bar.\nExcept in Catacombs & Rift.\nBest used with the option below.",
    )
    @SearchTag("skyblockxp")
    @ConfigEditorBoolean
    @FeatureToggle
    var skyblockXpBar: Boolean = false

    @Expose
    @ConfigOption(name = "XP in Inventory", desc = "Show your current XP in inventories that would use your XP.")
    @ConfigEditorBoolean
    @FeatureToggle
    var xpInInventory: Boolean = true

    // TODO move into scoreboard accordion
    @Expose
    @ConfigOption(
        name = "Red Scoreboard Numbers",
        desc = "Hide the red scoreboard numbers on the right side of the screen.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideScoreboardNumbers: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Piggy", desc = "Replace 'Piggy' with 'Purse' in the Scoreboard.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hidePiggyScoreboard: Boolean = true

    @Expose
    @ConfigOption(
        name = "Color Month Names",
        desc = "Color the month names in the Scoreboard.\nAlso applies to the Custom Scoreboard.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var colorMonthNames: Boolean = false

    @Expose
    @ConfigOption(name = "Explosions Hider", desc = "Hide explosions.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideExplosions: Boolean = false

    @Expose
    @ConfigOption(name = "CH Join", desc = "Help buy a pass for accessing the Crystal Hollows if needed.")
    @ConfigEditorBoolean
    @FeatureToggle
    var crystalHollowsJoin: Boolean = true

    @Expose
    @ConfigOption(name = "Fire Overlay Hider", desc = "Hide the fire overlay (Like in Skytils).")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideFireOverlay: Boolean = false

    @Expose
    @ConfigOption(
        name = "Better Sign Editing",
        desc = "Allow pasting (Ctrl+V), copying (Ctrl+C), and deleting whole words/lines (Ctrl+Backspace/Ctrl+Shift+Backspace) in signs.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var betterSignEditing: Boolean = true

    @Expose
    @ConfigOption(name = "Movement Speed", desc = "Show the player movement speed in blocks per second.")
    @ConfigEditorBoolean
    @FeatureToggle
    var playerMovementSpeed: Boolean = false

    @Expose
    @ConfigLink(owner = MiscConfig::class, field = "playerMovementSpeed")
    var playerMovementSpeedPos: Position = Position(394, 124, false, true)


    @Expose
    @ConfigOption(
        name = "Frog Mask Display",
        desc = "Displays information about the active §2Frog Mask§7 region. §eRequires a Frog Mask in inventory."
    )
    @ConfigEditorDropdown
    var frogMaskDisplay: FrogMaskCondition = FrogMaskCondition.DISABLED

    enum class FrogMaskCondition(private val displayName: String) {
        DISABLED("Off"),
        ALWAYS("Always"),
        PARK("In The Park"),
        WORN("While Worn"),
        WORN_IN_PARK("While Worn in The Park");

        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = MiscConfig::class, field = "frogMaskDisplay")
    var frogMaskDisplayPosition: Position = Position(25, 25, false, true)

    @Expose
    @ConfigOption(
        name = "Server Restart Title",
        desc = "Show a title with seconds remaining until the server restarts after a Game Update or Scheduled Restart.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var serverRestartTitle: Boolean = true

    @Expose
    @ConfigOption(
        name = "Piece Of Wizard Portal",
        desc = "Restore the Earned By lore line on bought Piece Of Wizard Portal.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var restorePieceOfWizardPortalLore: Boolean = true

    @Expose
    @ConfigOption(
        name = "Account Upgrade Reminder",
        desc = "Remind you to claim community shop account and profile upgrades when complete.",
    )
    @ConfigEditorBoolean
    @SearchTag("Elizabeth Community Center")
    @FeatureToggle
    var accountUpgradeReminder: Boolean = true

    @Expose
    @ConfigOption(name = "NEU Heavy Pearls", desc = "Fix NEU's Heavy Pearl detection.")
    @ConfigEditorBoolean
    @FeatureToggle
    var fixNeuHeavyPearls: Boolean = true

    @Expose
    @ConfigOption(
        name = "Fix Patcher Lines",
        desc = "Suggest in chat to disable Patcher's `parallax fix` that breaks SkyHanni's line from middle of player to somewhere else.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var fixPatcherLines: Boolean = true

    @Expose
    @ConfigOption(name = "Time In Limbo", desc = "Show the time since you entered the limbo.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showTimeInLimbo: Boolean = true

    @Expose
    @ConfigLink(owner = MiscConfig::class, field = "showTimeInLimbo")
    var showTimeInLimboPosition: Position = Position(400, 200, 1.3f)

    @Expose
    @ConfigOption(name = "Limbo Playtime Detailed", desc = "Show your total time in limbo in the detailed /playtime.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showLimboTimeInPlaytimeDetailed: Boolean = true

    @Expose
    @ConfigOption(name = "Lesser Orb of Healing Hider", desc = "Hide the Lesser Orb of Healing.")
    @ConfigEditorBoolean
    @FeatureToggle
    var lesserOrbHider: Boolean = false

    @Expose
    @ConfigOption(name = "Lock Mouse Message", desc = "Show a message in chat when toggling §e/shmouselock§7.")
    @ConfigEditorBoolean
    var lockMouseLookChatMessage: Boolean = true

    // Does not have a config element!
    @Expose
    var lockedMouseDisplay: Position = Position(400, 200, 0.8f)

    @Expose
    @ConfigLink(owner = NextJacobContestConfig::class, field = "display")
    var inventoryLoadPos: Position = Position(394, 124, false, true)

    @Expose
    @ConfigOption(
        name = "Fix Ghost Entities",
        desc = "Remove ghost entities caused by a Hypixel bug.\n" + "This includes Diana, Dungeon and Crimson Isle mobs and nametags.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var fixGhostEntities: Boolean = true

    @Expose
    @ConfigOption(name = "Replace Roman Numerals", desc = "Replace Roman Numerals with Arabic Numerals on any item.")
    @ConfigEditorBoolean
    @FeatureToggle
    var replaceRomanNumerals: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Thunder Bottle", desc = "Show a notification when your Thunder Bottle is fully charged.")
    @ConfigEditorBoolean
    @FeatureToggle
    var thunderBottleNotification: Boolean = true

    @Expose
    @ConfigOption(
        name = "Unknown Perkpocalypse Mayor Warning",
        desc = "Show a warning when the Unknown Perkpocalypse Mayor is unknown.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var unknownPerkpocalypseMayorWarning: Boolean = true

    @ConfigOption(name = "Hide Far Entities", desc = "")
    @Accordion
    @Expose
    var hideFarEntities: HideFarEntitiesConfig = HideFarEntitiesConfig()

    @Expose
    @ConfigOption(name = "Last Storage", desc = "")
    @Accordion
    var lastStorage: LastStorageConfig = LastStorageConfig()

    @Expose
    @ConfigOption(
        name = "Maintain Volume During Warnings",
        desc = "Do not change game volume levels when warning sounds are played.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var maintainGameVolume: Boolean = false

    @Expose
    @ConfigOption(
        name = "NEU Soul Path Find",
        desc = "When showing §e/neusouls on§7, show a pathfind to the Fairy Souls missing and a percentage of souls done in chat.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var neuSoulsPathFind: Boolean = true

    @Expose
    @ConfigOption(
        name = "GFS Piggy Bank",
        desc = "When your Piggy Bank breaks, send a chat warning to get enchanted pork from sacks.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var gfsPiggyBank: Boolean = true

    @Expose
    @ConfigOption(name = "SkyHanni User Luck", desc = "Shows SkyHanni User Luck in the SkyBlock Stats.")
    @ConfigEditorBoolean
    @FeatureToggle
    var userluckEnabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Computer Time Offset Warning",
        desc = "Sends a Chat Warning if your computer time is not synchronized with the actual time.\n" +
            "§cMaking sure your computer time is correct is important for SkyHanni to display times correctly.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var warnAboutPcTimeOffset: Boolean = true
}
