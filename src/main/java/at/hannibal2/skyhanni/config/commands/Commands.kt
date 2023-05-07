package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.config.commands.SimpleCommand.ProcessCommandRunnable
import at.hannibal2.skyhanni.data.ApiDataLoader
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.features.bingo.BingoCardDisplay
import at.hannibal2.skyhanni.features.bingo.BingoNextStepHelper
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper
import at.hannibal2.skyhanni.features.event.diana.InquisitorWaypointShare
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenCropTimeCommand
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.farming.CropSpeedMeter
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.misc.CollectionCounter
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.test.PacketTest
import at.hannibal2.skyhanni.test.SkyHanniTestCommand
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.test.command.*
import at.hannibal2.skyhanni.utils.APIUtil
import net.minecraft.command.ICommandSender
import net.minecraftforge.client.ClientCommandHandler

object Commands {

    private val openMainMenu: (Array<String>) -> Unit = {
        if (it.isNotEmpty()) {
            if (it[0].lowercase() == "gui") {
                GuiEditManager.openGuiEditor()
            } else {
                ConfigGuiManager.openConfigGui(it.joinToString(" "))
            }
        } else {
            ConfigGuiManager.openConfigGui()
        }
    }

    fun init() {
        registerCommand("sh", openMainMenu)
        registerCommand("skyhanni", openMainMenu)
        registerCommand("shreloadlocalrepo") { SkyHanniMod.repo.reloadLocalRepo() }
        registerCommand("shupdaterepo") { SkyHanniMod.repo.updateRepo() }
        registerCommand("shtest") { SkyHanniTestCommand.testCommand(it) }
        registerCommand("shcopylocation") { SkyHanniTestCommand.copyLocation() }
        registerCommand("shcopyentities") { CopyNearbyEntitiesCommand.command(it) }
        registerCommand("shcopytablist") { CopyTabListCommand.command(it) }
        registerCommand("shcopyscoreboard") { CopyScoreboardCommand.command(it) }
        registerCommand("shcopyitem") { CopyItemCommand.command(it) }
        registerCommand("shconfigsave") { SkyHanniMod.configManager.saveConfig("manual-command") }
        registerCommand("shmarkplayer") { MarkedPlayerManager.command(it) }
        registerCommand("shtestpacket") { PacketTest.toggle() }
        registerCommand("shreloadlisteners") { SkyHanniTestCommand.reloadListeners() }
        registerCommand("shstoplisteners") { SkyHanniTestCommand.stopListeners() }
        registerCommand("shresetburrowwarps") { BurrowWarpHelper.resetDisabledWarps() }
        registerCommand("shtrackcollection") { CollectionCounter.command(it) }
        registerCommand("shreloadbingodata") { BingoCardDisplay.command() }
        registerCommand("shprintbingohelper") { BingoNextStepHelper.command() }
        registerCommand("shsetapikey") { ApiDataLoader.command(it) }
        registerCommand("shtestgardenvisitors") { SkyHanniTestCommand.testGardenVisitors() }
        registerCommand("shtogglehypixelapierrors") { APIUtil.toggleApiErrorMessages() }
        registerCommand("shcropspeedmeter") { CropSpeedMeter.toggle() }
        registerCommand("shcroptime") { GardenCropTimeCommand.onCommand(it) }
        registerCommand("shtestcomposter") { ComposterOverlay.onCommand(it) }
        registerCommand("shclearcropspeed") { GardenAPI.clearCropSpeed() }
        registerCommand("shclearminiondata") { MinionFeatures.clearMinionData() }
        registerCommand("shtestbingo") { TestBingo.toggle() }
        registerCommand("shtestmessage") { TestChatCommand.command(it) }
        registerCommand("shshareinquis") { InquisitorWaypointShare.sendInquisitor() }
    }

    private fun registerCommand(name: String, function: (Array<String>) -> Unit) {
        ClientCommandHandler.instance.registerCommand(SimpleCommand(name, createCommand(function)))
    }

    private fun createCommand(function: (Array<String>) -> Unit) =
        object : ProcessCommandRunnable() {
            override fun processCommand(sender: ICommandSender?, args: Array<out String>) {
                function(args.asList().toTypedArray())
            }
        }
}