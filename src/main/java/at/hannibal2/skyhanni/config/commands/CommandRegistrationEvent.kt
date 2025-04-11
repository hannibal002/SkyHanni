package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
//#if MC < 1.21
import net.minecraftforge.client.ClientCommandHandler
//#else
//$$ import com.mojang.brigadier.arguments.StringArgumentType
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//#endif

class CommandRegistrationEvent(private val builders: MutableList<CommandBuilder>) : SkyHanniEvent() {

    val commands: List<CommandBuilder> get() = builders

    fun register(name: String, block: CommandBuilder.() -> Unit) {
        val command = CommandBuilder(name).apply(block)
        if (builders.any { it.name == name || it.aliases.contains(name) }) {
            error("The command '$name is already registered!'")
        }
        if (command.description.isEmpty() && command.category !in CommandCategory.developmentCategories) {
            error("The command '$name' has no description!")
        }

        val simpleCommand = command.toSimpleCommand()

        //#if MC < 1.21
        ClientCommandHandler.instance.registerCommand(simpleCommand)
        //#else
        //$$ ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
        //$$     val original = dispatcher.register(
        //$$         literal(name).executes {
        //$$             simpleCommand.processCommand(emptyArray())
        //$$             1
        //$$         }.then(
        //$$             argument("please type the arguments of this here command", StringArgumentType.greedyString()).executes { context ->
        //$$                 val itemName = StringArgumentType.getString(context, "please type the arguments of this here command").split(" ")
        //$$                 simpleCommand.processCommand(itemName.toTypedArray())
        //$$                 1
        //$$             },
        //$$         ),
        //$$     )
        //$$     simpleCommand.getCommandAliases().forEach {
        //$$         dispatcher.register(literal(it).redirect(original))
        //$$     }
        //$$ }
        //#endif

        builders.add(command)
    }
}
