package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.features.bingo.card.BingoCardDisplay
import at.hannibal2.skyhanni.features.bingo.card.nextstephelper.BingoNextStepHelper
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.features.dungeon.CroesusChestTracker
import at.hannibal2.skyhanni.features.dungeon.floor7.TerminalInfo
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper
import at.hannibal2.skyhanni.features.event.diana.InquisitorWaypointShare
import at.hannibal2.skyhanni.features.garden.FarmingMilestoneCommand
import at.hannibal2.skyhanni.features.garden.GardenCropTimeCommand
import at.hannibal2.skyhanni.features.garden.GardenCropsInCommand
import at.hannibal2.skyhanni.features.garden.farming.CropSpeedMeter
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLaneCreator
import at.hannibal2.skyhanni.features.garden.pests.PestFinder
import at.hannibal2.skyhanni.features.mining.MineshaftPityDisplay
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.rift.everywhere.PunchcardHighlight
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.test.WorldEdit
import at.hannibal2.skyhanni.test.command.TestChatCommand
import at.hannibal2.skyhanni.utils.ExtendedChatColor
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
        usersNormal(event)
        usersNormalReset(event)
        usersBugFix(event)
        devTest(event)
        devDebug(event)
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
            callback { WikiManager.otherWikiCommands(it, true, true) }
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
    }

    private fun usersNormalReset(event: CommandRegistrationEvent) {
        // non trackers
        event.register("shresetkismet") {
            description = "Resets the saved values of the applied kismet feathers in Croesus"
            category = CommandCategory.USERS_RESET
            callback { CroesusChestTracker.resetChest() }
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
    }

    private fun devDebug(event: CommandRegistrationEvent) {
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
        event.register("shtestgriffinspots") {
            description = "Show potential griffin spots around you."
            category = CommandCategory.DEVELOPER_DEBUG
            callback { GriffinBurrowHelper.testGriffinSpots() }
        }
    }

    @Suppress("LongMethod")
    private fun devTest(event: CommandRegistrationEvent) {
        event.register("shrepopatterns") {
            description = "See where regexes are loaded from"
            category = CommandCategory.DEVELOPER_TEST
            callback { RepoPatternGui.open() }
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
    }
}
