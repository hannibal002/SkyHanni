package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.config.features.About.UpdateStream
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.data.GardenCropMilestonesCommunityFix
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.data.TrackerManager
import at.hannibal2.skyhanni.data.bazaar.HypixelBazaarFetcher
import at.hannibal2.skyhanni.features.bingo.card.BingoCardDisplay
import at.hannibal2.skyhanni.features.bingo.card.nextstephelper.BingoNextStepHelper
import at.hannibal2.skyhanni.features.chat.ColorFormattingHelper
import at.hannibal2.skyhanni.features.chat.translation.Translator
import at.hannibal2.skyhanni.features.combat.endernodetracker.EnderNodeTracker
import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostUtil
import at.hannibal2.skyhanni.features.commands.HelpCommand
import at.hannibal2.skyhanni.features.commands.PartyChatCommands
import at.hannibal2.skyhanni.features.commands.PartyCommands
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.features.dungeon.CroesusChestTracker
import at.hannibal2.skyhanni.features.event.diana.AllBurrowsList
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper
import at.hannibal2.skyhanni.features.event.diana.DianaProfitTracker
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper
import at.hannibal2.skyhanni.features.event.diana.InquisitorWaypointShare
import at.hannibal2.skyhanni.features.event.diana.MythologicalCreatureTracker
import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionStats
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggLocations
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggLocator
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEventSummary
import at.hannibal2.skyhanni.features.event.jerry.frozentreasure.FrozenTreasureTracker
import at.hannibal2.skyhanni.features.fishing.tracker.FishingProfitTracker
import at.hannibal2.skyhanni.features.fishing.tracker.SeaCreatureTracker
import at.hannibal2.skyhanni.features.garden.FarmingMilestoneCommand
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenCropTimeCommand
import at.hannibal2.skyhanni.features.garden.GardenCropsInCommand
import at.hannibal2.skyhanni.features.garden.SensitivityReducer
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.farming.ArmorDropTracker
import at.hannibal2.skyhanni.features.garden.farming.CropMoneyDisplay
import at.hannibal2.skyhanni.features.garden.farming.CropSpeedMeter
import at.hannibal2.skyhanni.features.garden.farming.DicerRngDropTracker
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay
import at.hannibal2.skyhanni.features.garden.farming.GardenStartLocation
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLaneCreator
import at.hannibal2.skyhanni.features.garden.fortuneguide.CaptureFarmingGear
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.pests.PestFinder
import at.hannibal2.skyhanni.features.garden.pests.PestProfitTracker
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorDropStatistics
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryStrayTracker
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentsProfitTracker
import at.hannibal2.skyhanni.features.mining.KingTalismanHelper
import at.hannibal2.skyhanni.features.mining.MineshaftPityDisplay
import at.hannibal2.skyhanni.features.mining.fossilexcavator.ExcavatorProfitTracker
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.CorpseTracker
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderTracker
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.misc.CarryTracker
import at.hannibal2.skyhanni.features.misc.CollectionTracker
import at.hannibal2.skyhanni.features.misc.LockMouseLook
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.misc.TpsCounter
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager
import at.hannibal2.skyhanni.features.misc.limbo.LimboTimeTracker
import at.hannibal2.skyhanni.features.misc.massconfiguration.DefaultConfigFeatures
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationHelper
import at.hannibal2.skyhanni.features.misc.reminders.ReminderManager
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
import at.hannibal2.skyhanni.features.rift.area.westvillage.VerminTracker
import at.hannibal2.skyhanni.features.rift.everywhere.PunchcardHighlight
import at.hannibal2.skyhanni.features.slayer.SlayerProfitTracker
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.DebugCommand
import at.hannibal2.skyhanni.test.PacketTest
import at.hannibal2.skyhanni.test.SkyBlockIslandTest
import at.hannibal2.skyhanni.test.SkyHanniConfigSearchResetCommand
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.test.WorldEdit
import at.hannibal2.skyhanni.test.command.CopyActionBarCommand
import at.hannibal2.skyhanni.test.command.CopyBossbarCommand
import at.hannibal2.skyhanni.test.command.CopyItemCommand
import at.hannibal2.skyhanni.test.command.CopyNearbyEntitiesCommand
import at.hannibal2.skyhanni.test.command.CopyScoreboardCommand
import at.hannibal2.skyhanni.test.command.TestChatCommand
import at.hannibal2.skyhanni.test.command.TrackParticlesCommand
import at.hannibal2.skyhanni.test.command.TrackSoundsCommand
import at.hannibal2.skyhanni.test.graph.GraphEditor
import at.hannibal2.skyhanni.utils.APIUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ExtendedChatColor
import at.hannibal2.skyhanni.utils.ItemPriceUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.chat.ChatClickActionManager
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternGui

@SkyHanniModule
object Commands {

    private val openMainMenu: (Array<String>) -> Unit = {
        if (it.isNotEmpty()) {
            if (it[0].lowercase() == "gui") {
                GuiEditManager.openGuiPositionEditor(hotkeyReminder = true)
            } else {
                ConfigGuiManager.openConfigGui(it.joinToString(" "))
            }
        } else {
            ConfigGuiManager.openConfigGui()
        }
    }

    val commands = mutableListOf<CommandBuilder>()

    @HandleEvent
    fun registerCommands(event: RegisterCommandsEvent) {
        usersMain(event)
        usersNormal(event)
        //TODO: usersNormalClearTracker(event)
        usersBugFix(event)
        developersCodingHelp(event)
        developersDebugFeatures(event)
        internalCommands(event)
        shortenedCommands(event)
    }

    private fun usersMain(event: RegisterCommandsEvent) {
        event.register("sh") {
            aliases = listOf("skyhanni")
            description = "Opens the main SkyHanni config"
            callback = openMainMenu
        }
        event.register("ff") {
            description = "Opens the Farming Fortune Guide"
            callback = { openFortuneGuide() }
        }
        event.register("shcommands") {
            description = "Shows this list"
            callback = { HelpCommand.onCommand(it, commands) }
        }
        event.register("shdefaultoptions") {
            description = "Select default options"
            callback = { DefaultConfigFeatures.onCommand(it) }
            autoComplete = { DefaultConfigFeatures.onComplete(it) }
        }
        event.register("shremind") {
            description = "Set a reminder for yourself"
            callback = { ReminderManager.command(it) }
        }
        event.register("shwords") {
            description = "Opens the config list for modifying visual words"
            callback = { openVisualWords() }
        }
        event.register("shnavigate") {
            description = "Using path finder to go to locatons"
            callback = { NavigationHelper.onCommand(it) }
        }
    }

    @Suppress("LongMethod")
    private fun usersNormal(event: RegisterCommandsEvent) {
        event.register("shmarkplayer") {
            description = "Add a highlight effect to a player for better visibility"
            callback = { MarkedPlayerManager.command(it) }
        }
        event.register("shtrackcollection") {
            description = "Tracks your collection gain over time"
            callback = { CollectionTracker.command(it) }
        }
        event.register("shcroptime") {
            description =
                "Calculates with your current crop per second speed how long you need to farm a crop to collect this amount of items"
            callback = { GardenCropTimeCommand.onCommand(it) }
        }
        event.register("shcropsin") {
            description = "Calculates with your current crop per second how many items you can collect in this amount of time"
            callback = { GardenCropsInCommand.onCommand(it) }
        }
        event.register("shrpcstart") {
            description = "Manually starts the Discord Rich Presence feature"
            callback = { DiscordRPCManager.startCommand() }
        }
        event.register("shcropstartlocation") {
            description = "Manually sets the crop start location"
            callback = { GardenStartLocation.setLocationCommand() }
        }
        event.register("shclearslayerprofits") {
            description = "Clearing the total slayer profit for the current slayer type"
            callback = { SlayerProfitTracker.clearProfitCommand(it) }
        }
        event.register("shimportghostcounterdata") {
            description = "Manually importing the ghost counter data from GhostCounterV3"
            callback = { GhostUtil.importCTGhostCounterData() }
        }
        event.register("shclearfarmingitems") {
            description = "Clear farming items saved for the Farming Fortune Guide"
            callback = { clearFarmingItems() }
        }
        event.register("shresetghostcounter") {
            description = "Resets the ghost counter"
            callback = { GhostUtil.reset() }
        }
        event.register("shresetpowdertracker") {
            description = "Resets the Powder Tracker"
            callback = { PowderTracker.resetCommand() }
        }
        event.register("shresetdicertracker") {
            description = "Resets the Dicer Drop Tracker"
            callback = { DicerRngDropTracker.resetCommand() }
        }
        event.register("shresetcorpsetracker") {
            description = "Resets the Glacite Mineshaft Corpse Tracker"
            callback = { CorpseTracker.resetCommand() }
        }
        event.register("shresetendernodetracker") {
            description = "Resets the Ender Node Tracker"
            callback = { EnderNodeTracker.resetCommand() }
        }
        event.register("shresetarmordroptracker") {
            description = "Resets the Armor Drop Tracker"
            callback = { ArmorDropTracker.resetCommand() }
        }
        event.register("shresetfrozentreasuretracker") {
            description = "Resets the Frozen Treasure Tracker"
            callback = { FrozenTreasureTracker.resetCommand() }
        }
        event.register("shresetfishingtracker") {
            description = "Resets the Fishing Profit Tracker"
            callback = { FishingProfitTracker.resetCommand() }
        }
        event.register("shresetvisitordrops") {
            description = "Reset the Visitors Drop Statistics"
            callback = { GardenVisitorDropStatistics.resetCommand() }
        }
        event.register("shbingotoggle") {
            description = "Toggle the bingo card display mode"
            callback = { BingoCardDisplay.toggleCommand() }
        }
        event.register("shfarmingprofile") {
            description = "Look up the farming profile from yourself or another player on elitebot.dev"
            callback = { FarmingWeightDisplay.lookUpCommand(it) }
        }
        event.register("shcopytranslation") {
            description =
                "Copy the English translation of a message in another language to the clipboard. Uses a 2 letter language code that can be found at the end of a translation message."
            callback = { Translator.fromEnglish(it) }
        }
        event.register("shtranslate") {
            description = "Translate a message in another language to English"
            callback = { Translator.toEnglish(it) }
        }
        event.register("shmouselock") {
            description = "Lock/Unlock the mouse so it will no longer rotate the player (for farming)"
            callback = { LockMouseLook.toggleLock() }
        }
        event.register("shsensreduce") {
            description = "Lowers the mouse sensitivity for easier small adjustments (for farming)"
            callback = { SensitivityReducer.manualToggle() }
        }
        event.register("shresetvermintracker") {
            description = "Resets the Vermin Tracker"
            callback = { VerminTracker.resetCommand() }
        }
        event.register("shresetdianaprofittracker") {
            description = "Resets the Diana Profit Tracker"
            callback = { DianaProfitTracker.resetCommand() }
        }
        event.register("shresetpestprofittracker") {
            description = "Resets the Pest Profit Tracker"
            callback = { PestProfitTracker.resetCommand() }
        }
        event.register("shresetexperimentsprofittracker") {
            description = "Resets the Experiments Profit Tracker"
            callback = { ExperimentsProfitTracker.resetCommand() }
        }
        event.register("shresetmythologicalcreaturetracker") {
            description = "Resets the Mythological Creature Tracker"
            callback = { MythologicalCreatureTracker.resetCommand() }
        }
        event.register("shresetseacreaturetracker") {
            description = "Resets the Sea Creature Tracker"
            callback = { SeaCreatureTracker.resetCommand() }
        }
        event.register("shresetstrayrabbittracker") {
            description = "Resets the Stray Rabbit Tracker"
            callback = { ChocolateFactoryStrayTracker.resetCommand() }
        }
        event.register("shresetexcavatortracker") {
            description = "Resets the Fossil Excavator Profit Tracker"
            callback = { ExcavatorProfitTracker.resetCommand() }
        }
        event.register("shfandomwiki") {
            description = "Searches the fandom wiki with SkyHanni's own method."
            callback = { WikiManager.otherWikiCommands(it, true) }
        }
        event.register("shfandomwikithis") {
            description = "Searches the fandom wiki with SkyHanni's own method."
            callback = { WikiManager.otherWikiCommands(it, true, true) }
        }
        event.register("shofficialwiki") {
            description = "Searches the official wiki with SkyHanni's own method."
            callback = { WikiManager.otherWikiCommands(it, false) }
        }
        event.register("shofficialwikithis") {
            description = "Searches the official wiki with SkyHanni's own method."
            callback = { WikiManager.otherWikiCommands(it, false, true) }
        }
        event.register("shcalccrop") {
            description = "Calculate how many crops need to be farmed between different crop milestones."
            autoComplete = FarmingMilestoneCommand::onComplete
            callback = { FarmingMilestoneCommand.onCommand(it.getOrNull(0), it.getOrNull(1), it.getOrNull(2), false) }
        }
        event.register("shcalccroptime") {
            description = "Calculate how long you need to farm crops between different crop milestones."
            autoComplete = FarmingMilestoneCommand::onComplete
            callback = { FarmingMilestoneCommand.onCommand(it.getOrNull(0), it.getOrNull(1), it.getOrNull(2), true) }
        }
        event.register("shcropgoal") {
            description = "Define a custom milestone goal for a crop."
            callback = { FarmingMilestoneCommand.setGoal(it) }
            autoComplete = FarmingMilestoneCommand::onComplete
        }
        event.register("shskills") {
            description = "Skills XP/Level related command"
            callback = { SkillAPI.onCommand(it) }
            autoComplete = SkillAPI::onComplete
        }
        event.register("shlimbostats") {
            description = "Prints your Limbo Stats.\n §7This includes your Personal Best, Playtime, and §aSkyHanni User Luck§7!"
            callback = { LimboTimeTracker.printStats() }
        }
        event.register("shlanedetection") {
            description = "Detect a farming lane in the Garden"
            callback = { FarmingLaneCreator.commandLaneDetection() }
        }
        event.register("shignore") {
            description = "Add/Remove a user from your"
            callback = { PartyChatCommands.blacklist(it) }
        }
        event.register("shtpinfested") {
            description = "Teleports you to the nearest infested plot"
            callback = { PestFinder.teleportNearestInfestedPlot() }
        }
        event.register("shhoppitystats") {
            description = "Look up stats for a Hoppity's Event (by SkyBlock year).\nRun standalone for a list of years that have stats."
            callback = { HoppityEventSummary.sendStatsMessage(it) }
        }
        event.register("shcolors") {
            description = "Prints a list of all Minecraft color & formatting codes in chat."
            aliases = listOf("shcolor", "shcolours", "shcolour")
            callback = { ColorFormattingHelper.printColorCodeList() }
        }
        event.register("shtps") {
            description = "Informs in chat about the server ticks per second (TPS)."
            callback = { TpsCounter.tpsCommand() }
        }
        event.register("shcarry") {
            description = "Keep track of carries you do."
            callback = { CarryTracker.onCommand(it) }
        }
    }

    private fun usersBugFix(event: RegisterCommandsEvent) {
        event.register("shupdaterepo") {
            description = "Download the SkyHanni repo again"
            callback = { SkyHanniMod.repo.updateRepo() }
        }
        event.register("shresetburrowwarps") {
            description = "Manually resetting disabled diana burrow warp points"
            callback = { BurrowWarpHelper.resetDisabledWarps() }
        }
        event.register("shtogglehypixelapierrors") {
            description = "Show/hide hypixel api error messages in chat"
            callback = { APIUtils.toggleApiErrorMessages() }
        }
        event.register("shclearcropspeed") {
            description = "Reset garden crop speed data and best crop time data"
            callback = { GardenAPI.clearCropSpeed() }
        }
        event.register("shclearminiondata") {
            description = "Removed bugged minion locations from your private island"
            callback = { MinionFeatures.removeBuggedMinions(isCommand = true) }
        }
        event.register("shwhereami") {
            description = "Print current island in chat"
            callback = { SkyHanniDebugsAndTests.whereAmI() }
        }
        event.register("shclearcontestdata") {
            description = "Resets Jacob's Contest Data"
            callback = { SkyHanniDebugsAndTests.clearContestData() }
        }
        event.register("shconfig") {
            description = "Search or reset config elements §c(warning, dangerous!)"
            callback = { SkyHanniConfigSearchResetCommand.command(it) }
        }
        event.register("shdebug") {
            description = "Copies SkyHanni debug data in the clipboard."
            callback = { DebugCommand.command(it) }
        }
        event.register("shversion") {
            description = "Prints the SkyHanni version in the chat"
            callback = { SkyHanniDebugsAndTests.debugVersion() }
        }
        event.register("shrendertoggle") {
            description = "Disables/enables the rendering of all skyhanni guis."
            callback = { SkyHanniDebugsAndTests.toggleRender() }
        }
        event.register("shcarrolyn") {
            description = "Toggles if the specified crops effect is active from carrolyn"
            callback = { CaptureFarmingGear.handelCarrolyn(it) }
        }
        event.register("shrepostatus") {
            description = "Shows the status of all the mods constants"
            callback = { SkyHanniMod.repo.displayRepoStatus(false) }
        }
        event.register("shclearkismet") {
            description = "Clears the saved values of the applied kismet feathers in Croesus"
            callback = { CroesusChestTracker.resetChest() }
        }
        event.register("shkingfix") {
            description = "Resets the local King Talisman Helper offset."
            callback = { KingTalismanHelper.kingFix() }
        }
        event.register("shupdate") {
            description = "Updates the mod to the specified update stream."
            callback = { forceUpdate(it) }
        }
        event.register("shUpdateBazaarPrices") {
            description = "Forcefully updating the bazaar prices right now."
            callback = { HypixelBazaarFetcher.fetchNow() }
        }
        event.register("shclearsavedrabbits") {
            description = "Clears the saved rabbits on this profile."
            callback = { HoppityCollectionStats.clearSavedRabbits() }
        }
        event.register("shresetpunchcard") {
            description = "Resets the Rift Punchcard Artifact player list."
            callback = { PunchcardHighlight.clearList() }
        }
        event.register("shedittracker") {
            description = "Changes the tracked item amount for Diana, Fishing, Pest, Excavator, and Slayer Item Trackers."
            callback = { TrackerManager.commandEditTracker(it) }
        }
    }

    private fun developersDebugFeatures(event: RegisterCommandsEvent) {
        event.register("shtestbingo") {
            description = "Toggle the test bingo card display mode"
            callback = { TestBingo.toggle() }
        }
        event.register("shprintbingohelper") {
            description = "Prints the next step helper for the bingo card"
            callback = { BingoNextStepHelper.command() }
        }
        event.register("shreloadbingodata") {
            description = "Reloads the bingo card data"
            callback = { BingoCardDisplay.command() }
        }
        event.register("shtestgardenvisitors") {
            description = "Test the garden visitor drop statistics"
            callback = { SkyHanniDebugsAndTests.testGardenVisitors() }
        }
        event.register("shtestcomposter") {
            description = "Test the composter overlay"
            callback = { ComposterOverlay.onCommand(it) }
        }
        event.register("shtestinquisitor") {
            description = "Test the inquisitor waypoint share"
            callback = { InquisitorWaypointShare.test() }
        }
        event.register("shshowcropmoneycalculation") {
            description = "Show the calculation of the crop money"
            callback = { CropMoneyDisplay.toggleShowCalculation() }
        }
        event.register("shcropspeedmeter") {
            description = "Debugs how many crops you collect over time"
            callback = { CropSpeedMeter.toggle() }
        }
        event.register("shworldedit") {
            description = "Select regions in the world"
            callback = { WorldEdit.command(it) }
            autoComplete = { listOf("copy", "reset", "help", "left", "right") }
        }
        event.register("shconfigsave") {
            description = "Manually saving the config"
            callback = { SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "manual-command") }
        }
        event.register("shtestburrow") {
            description = "Sets a test burrow waypoint at your location"
            callback = { GriffinBurrowHelper.setTestBurrow(it) }
        }
        event.register("shtestsackapi") {
            description = "Get the amount of an item in sacks according to internal feature SackAPI"
            callback = { SackAPI.testSackAPI(it) }
        }
        event.register("shtestgriffinspots") {
            description = "Show potential griffin spots around you."
            callback = { GriffinBurrowHelper.testGriffinSpots() }
        }
        event.register("shtestisland") {
            description = "Sets the current skyblock island for testing purposes."
            callback = { SkyBlockIslandTest.onCommand(it) }
        }
        event.register("shdebugprice") {
            description = "Debug different price sources for an item."
            callback = { ItemPriceUtils.debugItemPrice(it) }
        }
        event.register("shdebugscoreboard") {
            description =
                "Monitors the scoreboard changes: Prints the raw scoreboard lines in the console after each update, with time since last update."
            callback = { ScoreboardData.toggleMonitor() }
        }
    }

    @Suppress("LongMethod")
    private fun developersCodingHelp(event: RegisterCommandsEvent) {
        event.register("shrepopatterns") {
            description = "See where regexes are loaded from"
            callback = { RepoPatternGui.open() }
        }
        event.register("shtest") {
            description = "Unused test command."
            callback = { SkyHanniDebugsAndTests.testCommand(it) }
        }
        event.register("shtestrabbitpaths") {
            description = "Tests pathfinding to rabbit eggs. Use a number 0-14."
            callback = { HoppityEggLocator.testPathfind(it) }
        }
        event.register("shtestitem") {
            description = "test item internal name resolving"
            callback = { SkyHanniDebugsAndTests.testItemCommand(it) }
        }
        event.register("shfindnullconfig") {
            description = "Find config elements that are null and prints them into the console"
            callback = { SkyHanniDebugsAndTests.findNullConfig(it) }
        }
        event.register("shtestwaypoint") {
            description = "Set a waypoint on that location"
            callback = { SkyHanniDebugsAndTests.waypoint(it) }
        }
        event.register("shtesttablist") {
            description = "Set your clipboard as a fake tab list."
            callback = { TabListData.toggleDebug() }
        }
        event.register("shreloadlocalrepo") {
            description = "Reloading the local repo data"
            callback = { SkyHanniMod.repo.reloadLocalRepo() }
        }
        event.register("shchathistory") {
            description = "Show the unfiltered chat history"
            callback = { ChatManager.openChatFilterGUI(it) }
        }
        event.register("shstoplisteners") {
            description = "Unregistering all loaded forge event listeners"
            callback = { SkyHanniDebugsAndTests.stopListeners() }
        }
        event.register("shreloadlisteners") {
            description = "Trying to load all forge event listeners again. Might not work at all"
            callback = { SkyHanniDebugsAndTests.reloadListeners() }
        }
        event.register("shcopylocation") {
            description = "Copies the player location as LorenzVec format to the clipboard"
            callback = { SkyHanniDebugsAndTests.copyLocation(it) }
        }
        event.register("shcopyentities") {
            description = "Copies entities in the specified radius around the player to the clipboard"
            callback = { CopyNearbyEntitiesCommand.command(it) }
        }
        event.register("shtracksounds") {
            description = "Tracks the sounds for the specified duration (in seconds) and copies it to the clipboard"
            callback = { TrackSoundsCommand.command(it) }
        }
        event.register("shtrackparticles") {
            description = "Tracks the particles for the specified duration (in seconds) and copies it to the clipboard"
            callback = { TrackParticlesCommand.command(it) }
        }
        event.register("shcopytablist") {
            description = "Copies the tab list data to the clipboard"
            callback = { TabListData.copyCommand(it) }
        }
        event.register("shcopyactionbar") {
            description = "Copies the action bar to the clipboard, including formatting codes"
            callback = { CopyActionBarCommand.command(it) }
        }
        event.register("shcopyscoreboard") {
            description = "Copies the scoreboard data to the clipboard"
            callback = { CopyScoreboardCommand.command(it) }
        }
        event.register("shcopybossbar") {
            description = "Copies the name of the bossbar to the clipboard, including formatting codes"
            callback = { CopyBossbarCommand.command(it) }
        }
        event.register("shcopyitem") {
            description = "Copies information about the item in hand to the clipboard"
            callback = { CopyItemCommand.command() }
        }
        event.register("shtestpacket") {
            description = "Logs incoming and outgoing packets to the console"
            callback = { PacketTest.command(it) }
        }
        event.register("shtestmessage") {
            description = "Sends a custom chat message client side in the chat"
            callback = { TestChatCommand.command(it) }
        }
        event.register("shtestrainbow") {
            description = "Sends a rainbow in chat"
            callback = { ExtendedChatColor.testCommand() }
        }
        event.register("shcopyinternalname") {
            description = "Copies the internal name of the item in hand to the clipboard."
            callback = { SkyHanniDebugsAndTests.copyItemInternalName() }
        }
        event.register("shpartydebug") {
            description = "List persons into the chat SkyHanni thinks are in your party."
            callback = { PartyAPI.listMembers() }
        }
        event.register("shplaysound") {
            description = "Play the specified sound effect at the given pitch and volume."
            callback = { SoundUtils.command(it) }
        }
        event.register("shsendtitle") {
            description = "Display a title on the screen with the specified settings."
            callback = { TitleManager.command(it) }
        }
        event.register("shresetconfig") {
            description = "Reloads the config manager and rendering processors of MoulConfig. " +
                "This §cWILL RESET §7your config, but also updating the java config files " +
                "(names, description, orderings and stuff)."
            callback = { SkyHanniDebugsAndTests.resetConfigCommand() }
        }
        event.register("shreadcropmilestonefromclipboard") {
            description = "Read crop milestone from clipboard. This helps fixing wrong crop milestone data"
            callback = { GardenCropMilestonesCommunityFix.readDataFromClipboard() }
        }
        event.register("shcopyfoundburrowlocations") {
            description = "Copy all ever found burrow locations to clipboard"
            callback = { AllBurrowsList.copyToClipboard() }
        }
        event.register("shaddfoundburrowlocationsfromclipboard") {
            description = "Add all ever found burrow locations from clipboard"
            callback = { AllBurrowsList.addFromClipboard() }
        }
        event.register("shgraph") {
            description = "Enables the graph editor"
            callback = { GraphEditor.commandIn() }
        }
        event.register("shtoggleegglocationdebug") {
            description = "Shows Hoppity egg locations with their internal API names and status."
            callback = { HoppityEggLocations.toggleDebug() }
        }
        event.register("shresetmineshaftpitystats") {
            description = "Resets the mineshaft pity display stats"
            callback = { MineshaftPityDisplay.fullResetCounter() }
        }
    }

    private fun internalCommands(event: RegisterCommandsEvent) {
        event.register("shaction") {
            description = "Internal command for chat click actions"
            callback = { ChatClickActionManager.onCommand(it) }
        }

    }

    private fun shortenedCommands(event: RegisterCommandsEvent) {
        event.register("pko") {
            description = "Kicks offline party members"
            callback = { PartyCommands.kickOffline() }
        }
        event.register("pw") {
            description = "Warps your party"
            callback = { PartyCommands.warp() }
        }
        event.register("pk") {
            description = "Kick a specific party member"
            callback = { PartyCommands.kick(it) }
        }
        event.register("pt") {
            description = "Transfer the party to another party member"
            callback = { PartyCommands.transfer(it) }
        }
        event.register("pp") {
            description = "Promote a specific party member"
            callback = { PartyCommands.promote(it) }
        }
        event.register("pd") {
            description = "Disbands the party"
            callback = { PartyCommands.disband() }
        }
        event.register("rpt") {
            description = "Reverse transfer party to the previous leader"
            callback = { PartyCommands.reverseTransfer() }
        }
    }

    @JvmStatic
    fun openFortuneGuide() {
        if (!LorenzUtils.inSkyBlock) {
            ChatUtils.userError("Join SkyBlock to open the fortune guide!")
        } else {
            FFGuideGUI.open()
        }
    }

    @JvmStatic
    fun openVisualWords() {
        if (!LorenzUtils.onHypixel) {
            ChatUtils.userError("You need to join Hypixel to use this feature!")
        } else {
            if (VisualWordGui.sbeConfigPath.exists()) VisualWordGui.drawImport = true
            SkyHanniMod.screenToOpen = VisualWordGui()
        }
    }

    private fun clearFarmingItems() {
        val storage = GardenAPI.storage?.fortune ?: return
        ChatUtils.chat("clearing farming items")
        storage.farmingItems.clear()
        storage.outdatedItems.clear()
    }

    private fun forceUpdate(args: Array<String>) {
        val currentStream = SkyHanniMod.feature.about.updateStream.get()
        val arg = args.firstOrNull() ?: "current"
        val updateStream = when {
            arg.equals("(?i)(?:full|release)s?".toRegex()) -> UpdateStream.RELEASES
            arg.equals("(?i)(?:beta|latest)s?".toRegex()) -> UpdateStream.BETA
            else -> currentStream
        }

        val switchingToBeta = updateStream == UpdateStream.BETA && (currentStream != UpdateStream.BETA || !UpdateManager.isCurrentlyBeta())
        if (switchingToBeta) {
            ChatUtils.clickableChat(
                "Are you sure you want to switch to beta? These versions may be less stable.",
                onClick = {
                    UpdateManager.checkUpdate(true, updateStream)
                },
                "§eClick to confirm!",
                oneTimeClick = true,
            )
        } else {
            UpdateManager.checkUpdate(true, updateStream)
        }
    }

    fun onCommand(event: RegisterCommandsEvent) {
        event.register("a") {
            description = "a"
            category = CommandCategory.MAIN
        }
    }
}
