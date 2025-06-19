package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.bazaar.HypixelBazaarFetcher
import at.hannibal2.skyhanni.features.bingo.card.BingoCardDisplay
import at.hannibal2.skyhanni.features.bingo.card.nextstephelper.BingoNextStepHelper
import at.hannibal2.skyhanni.features.chat.ColorFormattingHelper
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.features.dungeon.CroesusChestTracker
import at.hannibal2.skyhanni.features.dungeon.floor7.TerminalInfo
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper
import at.hannibal2.skyhanni.features.event.diana.InquisitorWaypointShare
import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionStats
import at.hannibal2.skyhanni.features.garden.FarmingMilestoneCommand
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenCropTimeCommand
import at.hannibal2.skyhanni.features.garden.GardenCropsInCommand
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.farming.CropSpeedMeter
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLaneCreator
import at.hannibal2.skyhanni.features.garden.fortuneguide.CaptureFarmingGear
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGui
import at.hannibal2.skyhanni.features.garden.pests.PestFinder
import at.hannibal2.skyhanni.features.mining.MineshaftPityDisplay
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.misc.limbo.LimboTimeTracker
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationHelper
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
import at.hannibal2.skyhanni.features.rift.everywhere.PunchcardHighlight
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniConfigSearchResetCommand
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.test.WorldEdit
import at.hannibal2.skyhanni.test.command.TestChatCommand
import at.hannibal2.skyhanni.utils.ExtendedChatColor
import at.hannibal2.skyhanni.utils.ItemPriceUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternGui

@SkyHanniModule
@Suppress("LargeClass", "LongMethod")
@Deprecated("do not use this class anymore")
object Commands {
    // Do not add new commands in this class
    // TODO move all command loading away from this class

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        usersMain(event)
        usersNormal(event)
        usersNormalReset(event)
        usersBugFix(event)
        devTest(event)
        devDebug(event)
    }

    private fun usersMain(event: CommandRegistrationEvent) {
        event.register("ff") {
            description = "Opens the Farming Fortune Guide"
            callback { FFGuideGui.onCommand() }
        }
        event.register("shwords") {
            description = "Opens the config list for modifying visual words"
            callback { VisualWordGui.onCommand() }
        }
        event.register("shnavigate") {
            description = "Using path finder to go to locations"
            callback { NavigationHelper.onCommand(it) }
        }
    }

    @Suppress("LongMethod")
    private fun usersNormal(event: CommandRegistrationEvent) {
        event.register("shcroptime") {
            description =
                "Calculates with your current crop per second speed how long you need to farm a crop to collect this amount of items"
            category = CommandCategory.USERS_ACTIVE
            callback { GardenCropTimeCommand.onCommand(it) }
        }
        event.register("shcropsin") {
            description = "Calculates with your current crop per second how many items you can collect in this amount of time"
            category = CommandCategory.USERS_ACTIVE
            callback { GardenCropsInCommand.onCommand(it) }
        }
        event.register("shfandomwiki") {
            description = "Searches the fandom wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            callback { WikiManager.otherWikiCommands(it, true) }
        }
        event.register("shfandomwikithis") {
            description = "Searches the fandom wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            callback { WikiManager.otherWikiCommands(it, useFandom = true, wikithis = true) }
        }
        event.register("shofficialwiki") {
            description = "Searches the official wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            callback { WikiManager.otherWikiCommands(it, false) }
        }
        event.register("shofficialwikithis") {
            description = "Searches the official wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            callback { WikiManager.otherWikiCommands(it, useFandom = false, wikithis = true) }
        }
        event.register("shcalccrop") {
            description = "Calculate how many crops need to be farmed between different crop milestones."
            category = CommandCategory.USERS_ACTIVE
            autoComplete { FarmingMilestoneCommand.onComplete(it) }
            callback { FarmingMilestoneCommand.onCommand(it.getOrNull(0), it.getOrNull(1), it.getOrNull(2), false) }
        }
        event.register("shcalccroptime") {
            description = "Calculate how long you need to farm crops between different crop milestones."
            category = CommandCategory.USERS_ACTIVE
            autoComplete { FarmingMilestoneCommand.onComplete(it) }
            callback { FarmingMilestoneCommand.onCommand(it.getOrNull(0), it.getOrNull(1), it.getOrNull(2), true) }
        }
        event.register("shcropgoal") {
            description = "Define a custom milestone goal for a crop."
            category = CommandCategory.USERS_ACTIVE
            callback { FarmingMilestoneCommand.setGoal(it) }
            autoComplete { FarmingMilestoneCommand.onComplete(it) }
        }
        event.register("shskills") {
            description = "Skills XP/Level related command"
            category = CommandCategory.USERS_ACTIVE
            callback { SkillApi.onCommand(it) }
            autoComplete { SkillApi.onComplete(it) }
        }
        event.register("shlimbostats") {
            description = "Prints your Limbo Stats.\n §7This includes your Personal Best, Playtime, and §aSkyHanni User Luck§7!"
            category = CommandCategory.USERS_ACTIVE
            callback { LimboTimeTracker.printStats() }
        }
        event.register("shlanedetection") {
            description = "Detect a farming lane in the Garden"
            category = CommandCategory.USERS_ACTIVE
            callback { FarmingLaneCreator.commandLaneDetection() }
        }
        event.register("shtpinfested") {
            description = "Teleports you to the nearest infested plot"
            category = CommandCategory.USERS_ACTIVE
            callback { PestFinder.teleportNearestInfestedPlot() }
        }
        event.register("shcolors") {
            description = "Prints a list of all Minecraft color & formatting codes in chat."
            category = CommandCategory.USERS_ACTIVE
            @Suppress("AvoidBritishSpelling")
            aliases = listOf("shcolor", "shcolours", "shcolour")
            callback { ColorFormattingHelper.printColorCodeList() }
        }
    }

    private fun usersNormalReset(event: CommandRegistrationEvent) {
        // non trackers
        event.register("shresetcropspeed") {
            description = "Resets garden crop speed data and best crop time data"
            category = CommandCategory.USERS_RESET
            callback { GardenApi.resetCropSpeed() }
        }
        event.register("shresetkismet") {
            description = "Resets the saved values of the applied kismet feathers in Croesus"
            category = CommandCategory.USERS_RESET
            callback { CroesusChestTracker.resetChest() }
        }
        event.register("shresetcontestdata") {
            description = "Resets Jacob's Contest Data"
            category = CommandCategory.USERS_RESET
            callback { SkyHanniDebugsAndTests.resetContestData() }
        }
        event.register("shresetfarmingitems") {
            description = "Resets farming items saved for the Farming Fortune Guide"
            category = CommandCategory.USERS_RESET
            callback { CaptureFarmingGear.onResetGearCommand() }
        }
        event.register("shresetmineshaftpitystats") {
            description = "Resets the mineshaft pity display stats"
            category = CommandCategory.USERS_RESET
            callback { MineshaftPityDisplay.fullResetCounter() }
        }
        event.register("shresetterminal") {
            description = "Resets terminal highlights in F7."
            category = CommandCategory.USERS_RESET
            callback { TerminalInfo.resetTerminals() }
        }
        event.register("shresetsavedrabbits") {
            description = "Resets the saved rabbits on this profile."
            category = CommandCategory.USERS_RESET
            callback { HoppityCollectionStats.resetSavedRabbits() }
        }
        event.register("shresetpunchcard") {
            description = "Resets the Rift Punchcard Artifact player list."
            category = CommandCategory.USERS_RESET
            callback { PunchcardHighlight.onResetCommand() }
        }
    }

    private fun usersBugFix(event: CommandRegistrationEvent) {
        event.register("shfixminions") {
            description = "Removed bugged minion locations from your private island"
            category = CommandCategory.USERS_BUG_FIX
            callback { MinionFeatures.removeBuggedMinions(isCommand = true) }
        }
        event.register("shwhereami") {
            description = "Print current island in chat"
            category = CommandCategory.USERS_BUG_FIX
            callback { SkyHanniDebugsAndTests.whereAmI() }
        }
        event.register("shrendertoggle") {
            description = "Disables/enables the rendering of all skyhanni guis."
            category = CommandCategory.USERS_BUG_FIX
            callback { SkyHanniDebugsAndTests.toggleRender() }
        }
        event.register("shcarrolyn") {
            description = "Toggles if the specified crops effect is active from carrolyn"
            category = CommandCategory.USERS_BUG_FIX
            callback { CaptureFarmingGear.handelCarrolyn(it) }
        }
        event.register("shupdate") {
            description = "Updates the mod to the specified update stream."
            category = CommandCategory.USERS_BUG_FIX
            callback { UpdateManager.updateCommand(it) }
        }
        event.register("shupdatebazaarprices") {
            description = "Forcefully updating the bazaar prices right now."
            category = CommandCategory.USERS_BUG_FIX
            callback { HypixelBazaarFetcher.fetchNow() }
        }
    }

    private fun devDebug(event: CommandRegistrationEvent) {
        event.register("shconfig") {
            description = "Searches or resets config elements §c(warning, dangerous!)"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { SkyHanniConfigSearchResetCommand.command(it) }
        }
        event.register("shversion") {
            description = "Prints the SkyHanni version in the chat"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { SkyHanniDebugsAndTests.debugVersion() }
        }
        event.register("shtestbingo") {
            description = "Toggle the test bingo card display mode"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { TestBingo.toggle() }
        }
        event.register("shprintbingohelper") {
            description = "Prints the next step helper for the bingo card"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { BingoNextStepHelper.command() }
        }
        event.register("shreloadbingodata") {
            description = "Reloads the bingo card data"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { BingoCardDisplay.command() }
        }
        event.register("shtestgardenvisitors") {
            description = "Test the garden visitor drop statistics"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { SkyHanniDebugsAndTests.testGardenVisitors() }
        }
        event.register("shtestcomposter") {
            description = "Test the composter overlay"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { ComposterOverlay.onCommand(it) }
        }
        event.register("shtestinquisitor") {
            description = "Test the inquisitor waypoint share"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { InquisitorWaypointShare.test() }
        }
        event.register("shcropspeedmeter") {
            description = "Debugs how many crops you collect over time"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { CropSpeedMeter.toggle() }
        }
        event.register("shworldedit") {
            description = "Select regions in the world"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { WorldEdit.command(it) }
            autoComplete { listOf("copy", "reset", "help", "left", "right") }
        }
        event.register("shtestsackapi") {
            description = "Get the amount of an item in sacks according to internal feature SackAPI"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { SackApi.testSackApi(it) }
        }
        event.register("shtestgriffinspots") {
            description = "Show potential griffin spots around you."
            category = CommandCategory.DEVELOPER_DEBUG
            callback { GriffinBurrowHelper.testGriffinSpots() }
        }
        event.register("shdebugprice") {
            description = "Debug different price sources for an item."
            category = CommandCategory.DEVELOPER_DEBUG
            callback { ItemPriceUtils.debugItemPrice(it) }
        }
        event.register("shdebugscoreboard") {
            description =
                "Monitors the scoreboard changes: " +
                "Prints the raw scoreboard lines in the console after each update, with time since last update."
            category = CommandCategory.DEVELOPER_DEBUG
            callback { ScoreboardData.toggleMonitor() }
        }
        event.register("shcopyinternalname") {
            description = "Copies the internal name of the item in hand to the clipboard."
            category = CommandCategory.DEVELOPER_DEBUG
            callback { SkyHanniDebugsAndTests.copyItemInternalName() }
        }
        event.register("shcopylocation") {
            description = "Copies the player location as LorenzVec format to the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { SkyHanniDebugsAndTests.copyLocation(it) }
        }
    }

    @Suppress("LongMethod")
    private fun devTest(event: CommandRegistrationEvent) {
        event.register("shtest") {
            description = "Unused test command."
            category = CommandCategory.DEVELOPER_TEST
            callback { SkyHanniDebugsAndTests.testCommand(it) }
        }
        event.register("shrepopatterns") {
            description = "See where regexes are loaded from"
            category = CommandCategory.DEVELOPER_TEST
            callback { RepoPatternGui.open() }
        }
        event.register("shfindnullconfig") {
            description = "Find config elements that are null and prints them into the console"
            category = CommandCategory.DEVELOPER_TEST
            callback { SkyHanniDebugsAndTests.findNullConfig(it) }
        }
        event.register("shtestwaypoint") {
            description = "Set a waypoint on that location"
            category = CommandCategory.DEVELOPER_TEST
            callback { SkyHanniDebugsAndTests.waypoint(it) }
        }
        event.register("shstoplisteners") {
            description = "Unregistering all loaded event listeners"
            category = CommandCategory.DEVELOPER_TEST
            callback { SkyHanniDebugsAndTests.stopListeners() }
        }
        event.register("shreloadlisteners") {
            description = "Reloads all event listeners again"
            category = CommandCategory.DEVELOPER_TEST
            callback { SkyHanniDebugsAndTests.reloadListeners() }
        }
        event.register("shtestmessage") {
            description = "Sends a custom chat message client side in the chat"
            category = CommandCategory.DEVELOPER_TEST
            callback { TestChatCommand.command(it) }
        }
        event.register("shtestrainbow") {
            description = "Sends a rainbow in chat"
            category = CommandCategory.DEVELOPER_TEST
            callback { ExtendedChatColor.testCommand() }
        }
        event.register("shplaysound") {
            description = "Play the specified sound effect at the given pitch and volume."
            category = CommandCategory.DEVELOPER_TEST
            callback { SoundUtils.command(it) }
        }
        event.register("shconfigsave") {
            description = "Manually saving the config"
            category = CommandCategory.DEVELOPER_TEST
            callback { SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "manual-command") }
        }
    }
}
