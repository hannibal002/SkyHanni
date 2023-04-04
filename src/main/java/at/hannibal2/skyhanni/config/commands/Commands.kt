package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.config.commands.SimpleCommand.ProcessCommandRunnable
import at.hannibal2.skyhanni.data.ApiDataLoader
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.features.bazaar.BazaarDataGrabber
import at.hannibal2.skyhanni.features.bingo.BingoCardDisplay
import at.hannibal2.skyhanni.features.bingo.BingoNextStepHelper
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper
import at.hannibal2.skyhanni.features.misc.CollectionCounter
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.test.LorenzTest
import at.hannibal2.skyhanni.test.PacketTest
import at.hannibal2.skyhanni.test.command.CopyItemCommand
import at.hannibal2.skyhanni.test.command.CopyNearbyEntitiesCommand
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
        registerCommand("testhanni") { LorenzTest.testCommand(it) }
        registerCommand("copylocation") { LorenzTest.copyLocation() }
        registerCommand("copyentities") { CopyNearbyEntitiesCommand.command(it) }
        registerCommand("copyitem") { CopyItemCommand.command(it) }
        registerCommand("shconfigsave") { SkyHanniMod.configManager.saveConfig() }
        registerCommand("shmarkplayer") { MarkedPlayerManager.command(it) }
        registerCommand("shtestpacket") { PacketTest.toggle() }
        registerCommand("shreloadlisteners") { LorenzTest.reloadListeners() }
        registerCommand("shstoplisteners") { LorenzTest.stopListeners() }
        registerCommand("shresetburrowwarps") { BurrowWarpHelper.resetDisabledWarps() }
        registerCommand("shtrackcollection") { CollectionCounter.command(it) }
        registerCommand("shreloadbingodata") { BingoCardDisplay.command() }
        registerCommand("shprintbingohelper") { BingoNextStepHelper.command() }
        registerCommand("shsetapikey") { ApiDataLoader.command(it) }
        registerCommand("shtestgardenvisitors") { LorenzTest.testGardenVisitors() }
        registerCommand("shresetitemnames") { BazaarDataGrabber.resetItemNames() }
        registerCommand("shtogglehypixelapierrors") { APIUtil.toggleApiErrorMessages() }
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