package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.utils.PreInitFinishedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
//#if MC < 1.21
import net.minecraftforge.client.ClientCommandHandler

//#else
//$$ import com.mojang.brigadier.arguments.StringArgumentType
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//#endif

@SkyHanniModule
object CommandsRegistry {
    private val builders = mutableListOf<CommandBuilderBase>()

    @HandleEvent
    fun onPreInitFinished(event: PreInitFinishedEvent) {
        CommandRegistrationEvent(builders).post()
    }

    private fun String.isUnique() {
        if (builders.any { it.name == this || it.aliases.contains(this) }) {
            error("The command '$this is already registered!'")
        }
    }

    fun <T : CommandBuilderBase> T.hasUniqueName() {
        name.isUnique()
        aliases.forEach { it.isUnique() }
    }

    fun <T : CommandBuilderBase> T.addToRegister() {
        val command = this.toCommand()
        //#if MC < 1.21
        ClientCommandHandler.instance.registerCommand(command)
        //#else
        //$$ ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
        //$$     val original = dispatcher.register(
        //$$         literal(name).executes {
        //$$             command.processCommand(emptyArray())
        //$$             1
        //$$         }.then(
        //$$             argument("please type the arguments of this here command", StringArgumentType.greedyString()).executes { context ->
        //$$                 val itemName = StringArgumentType.getString(context, "please type the arguments of this here command").split(" ")
        //$$                 command.processCommand(itemName.toTypedArray())
        //$$                 1
        //$$             },
        //$$         ),
        //$$     )
        //$$     command.getCommandAliases().forEach {
        //$$         dispatcher.register(literal(it).redirect(original))
        //$$     }
        //$$ }
        //#endif

        builders.add(this)
    }
}
