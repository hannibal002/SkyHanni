package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigEditor
import at.hannibal2.skyhanni.config.commands.SimpleCommand.ProcessCommandRunnable
import at.hannibal2.skyhanni.config.core.GuiScreenElementWrapper
import at.hannibal2.skyhanni.features.MarkedPlayerManager
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper
import at.hannibal2.skyhanni.test.LorenzTest
import at.hannibal2.skyhanni.test.command.CopyItemCommand
import at.hannibal2.skyhanni.test.command.CopyNearbyEntitiesCommand
import net.minecraft.command.ICommandSender
import net.minecraftforge.client.ClientCommandHandler
import org.apache.commons.lang3.StringUtils

object Commands {

    private val openMainMenu: (Array<String>) -> Unit = {
        if (it.isNotEmpty()) {
            SkyHanniMod.screenToOpen =
                GuiScreenElementWrapper(ConfigEditor(SkyHanniMod.feature, StringUtils.join(it, " ")))
        } else {
            SkyHanniMod.screenToOpen = GuiScreenElementWrapper(ConfigEditor(SkyHanniMod.feature))
        }
    }

    fun init() {
        registerCommand("sh", openMainMenu)
        registerCommand("skyhanni", openMainMenu)
        registerCommand("shreloadlocalrepo") { SkyHanniMod.repo.reloadLocalRepo() }
        registerCommand("shupdaterepo") { SkyHanniMod.repo.updateRepo() }
        registerCommand("testhanni") { LorenzTest.testCommand(it) }
        registerCommand("copyentities") { CopyNearbyEntitiesCommand.command(it) }
        registerCommand("copyitem") { CopyItemCommand.command(it) }
        registerCommand("shconfigsave") { SkyHanniMod.configManager.saveConfig() }
        registerCommand("shmarkplayer") { MarkedPlayerManager.command(it) }
        registerCommand("togglepacketlog") { LorenzTest.togglePacketLog() }
        registerCommand("shreloadlisteners") { LorenzTest.reloadListeners() }
        registerCommand("shresetburrowwarps") { BurrowWarpHelper.resetDisabledWarps() }
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