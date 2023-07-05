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
import at.hannibal2.skyhanni.features.garden.farming.CropMoneyDisplay
import at.hannibal2.skyhanni.features.garden.farming.CropSpeedMeter
import at.hannibal2.skyhanni.features.garden.farming.GardenStartLocation
import at.hannibal2.skyhanni.features.garden.fortuneguide.CaptureFarmingGear
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.misc.CityProjectFeatures
import at.hannibal2.skyhanni.features.misc.CollectionCounter
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostCounter
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostUtil
import at.hannibal2.skyhanni.features.slayer.SlayerItemProfitTracker
import at.hannibal2.skyhanni.test.PacketTest
import at.hannibal2.skyhanni.test.SkyHanniTestCommand
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.test.command.*
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.command.ICommandSender
import net.minecraftforge.client.ClientCommandHandler

object Commands {

    private val openMainMenu: (Array<String>) -> Unit = {
        if (it.isNotEmpty()) {
            if (it[0].lowercase() == "gui") {
                GuiEditManager.openGuiPositionEditor()
            } else {
                ConfigGuiManager.openConfigGui(it.joinToString(" "))
            }
        } else {
            ConfigGuiManager.openConfigGui()
        }
    }

    fun init() {

        // main commands
        registerCommand("sh", openMainMenu)
        registerCommand("skyhanni", openMainMenu)

        registerCommand("ff") { openFortuneGuide() }

        // for users - regular commands
        registerCommand("shmarkplayer") { MarkedPlayerManager.command(it) }
        registerCommand("shtrackcollection") { CollectionCounter.command(it) }
        registerCommand("shsetapikey") { ApiDataLoader.command(it) }
        registerCommand("shcropspeedmeter") { CropSpeedMeter.toggle() }
        registerCommand("shcroptime") { GardenCropTimeCommand.onCommand(it) }
        registerCommand("shshareinquis") { InquisitorWaypointShare.sendInquisitor() }
        registerCommand("shrpcstart") { DiscordRPCManager.startCommand() }
        registerCommand("shcropstartlocation") { GardenStartLocation.setLocationCommand() }
        registerCommand("shstopcityprojectreminder") { CityProjectFeatures.disable() }
        registerCommand("shclearslayerprofits") { SlayerItemProfitTracker.clearProfitCommand(it) }
        registerCommand("shimportghostcounterdata") { GhostUtil.importCTGhostCounterData() }
        registerCommand("shclearfarmingitems") { clearFarmingItems() }
        registerCommand("shresetghostcounter") { GhostUtil.reset() }

        // for users - fix bugs
        registerCommand("shupdaterepo") { SkyHanniMod.repo.updateRepo() }
        registerCommand("shconfigsave") { SkyHanniMod.configManager.saveConfig("manual-command") }
        registerCommand("shresetburrowwarps") { BurrowWarpHelper.resetDisabledWarps() }
        registerCommand("shtogglehypixelapierrors") { APIUtil.toggleApiErrorMessages() }
        registerCommand("shclearcropspeed") { GardenAPI.clearCropSpeed() }
        registerCommand("shclearminiondata") { MinionFeatures.clearMinionData() }

        // for developers - debug existing features
        registerCommand("shtest") { SkyHanniTestCommand.testCommand(it) }
        registerCommand("shtestbingo") { TestBingo.toggle() }
        registerCommand("shprintbingohelper") { BingoNextStepHelper.command() }
        registerCommand("shreloadbingodata") { BingoCardDisplay.command() }
        registerCommand("shtestgardenvisitors") { SkyHanniTestCommand.testGardenVisitors() }
        registerCommand("shtestcomposter") { ComposterOverlay.onCommand(it) }
        registerCommand("shtestinquisitor") { InquisitorWaypointShare.test() }
        registerCommand("shshowcropmoneycalculation") { CropMoneyDisplay.toggleShowCalculation() }

        // for developers - coding help
        registerCommand("shreloadlocalrepo") { SkyHanniMod.repo.reloadLocalRepo() }
        registerCommand("shstoplisteners") { SkyHanniTestCommand.stopListeners() }
        registerCommand("shreloadlisteners") { SkyHanniTestCommand.reloadListeners() }
        registerCommand("shcopylocation") { SkyHanniTestCommand.copyLocation() }
        registerCommand("shcopyentities") { CopyNearbyEntitiesCommand.command(it) }
        registerCommand("shcopytablist") { CopyTabListCommand.command(it) }
        registerCommand("shcopyscoreboard") { CopyScoreboardCommand.command(it) }
        registerCommand("shcopyitem") { CopyItemCommand.command(it) }
        registerCommand("shcopyparticles") { CopyNearbyParticlesCommand.command(it) }
        registerCommand("shtestpacket") { PacketTest.toggle() }
        registerCommand("shtestmessage") { TestChatCommand.command(it) }
        registerCommand("shcopyerror") { CopyErrorCommand.command(it) }

    }

    @JvmStatic
    fun openFortuneGuide() {
        if (!LorenzUtils.inSkyBlock) {
            LorenzUtils.chat("§cJoin Skyblock to open the fortune guide!")
        } else {
            CaptureFarmingGear.captureFarmingGear()
            SkyHanniMod.screenToOpen = FFGuideGUI()
        }
    }

    private fun clearFarmingItems() {
        val config = GardenAPI.config?.fortune ?: return
        LorenzUtils.chat("§e[SkyHanni] clearing farming items")
        config.farmingItems.clear()
        config.outdatedItems.clear()
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