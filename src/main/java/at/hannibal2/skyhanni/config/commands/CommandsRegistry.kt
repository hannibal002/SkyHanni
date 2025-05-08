package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BaseBrigadierBuilder
import at.hannibal2.skyhanni.config.commands.brigadier.CommandData
import at.hannibal2.skyhanni.events.utils.PreInitFinishedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.mojang.brigadier.CommandDispatcher
//#if MC < 1.21
import net.minecraftforge.client.ClientCommandHandler

//#else
//$$ import com.mojang.brigadier.arguments.StringArgumentType
//$$ import com.mojang.brigadier.builder.LiteralArgumentBuilder
//$$ import com.mojang.brigadier.builder.RequiredArgumentBuilder
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//#endif

@SkyHanniModule
object CommandsRegistry {
    private val builders = mutableListOf<CommandData>()

    //#if MC < 1.21
    private val dispatcher: CommandDispatcher<Any?> = CommandDispatcher()
    //#endif

    @HandleEvent
    fun onPreInitFinished(event: PreInitFinishedEvent) {
        //#if MC < 1.21
        CommandRegistrationEvent(builders, dispatcher).post()
        //#else
        //$$ ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
        //$$     CommandRegistrationEvent(builders, dispatcher).post()
        //$$ }
        //#endif
    }

    private fun String.isUnique() {
        require(builders.all { this !in it.getAllNames() }) {
            "The command $this is already registered!"
        }
    }

    fun CommandData.hasUniqueName() {
        name.isUnique()
        aliases.forEach { it.isUnique() }
    }

    fun BaseBrigadierBuilder.addToRegister(dispatcher: CommandDispatcher<Any?>) {
        //#if MC < 1.21
        val command = toCommand(dispatcher)
        ClientCommandHandler.instance.registerCommand(command)
        //#else
        //$$ val original = dispatcher.register(builder as LiteralArgumentBuilder<Any?>)
        //$$ aliases.forEach {
        //$$     dispatcher.register(LiteralArgumentBuilder.literal<Any?>(it).redirect(original))
        //$$ }
        //#endif

        builders.add(this)
    }

    fun <T : CommandBuilderBase> T.addToRegister(dispatcher: CommandDispatcher<Any?>) {
        // TODO: register CommandBuilderBase without using toCommand
        val command = this.toCommand(dispatcher)
        //#if MC < 1.21
        ClientCommandHandler.instance.registerCommand(command)
        //#else
        //$$ val original = dispatcher.register(
        //$$     LiteralArgumentBuilder.literal<Any?>(name).executes {
        //$$         command.processCommand(null, emptyArray())
        //$$         1
        //$$     }.then(
        //$$         RequiredArgumentBuilder.argument<Any?, String>(
        //$$             "please type the arguments of this command here",
        //$$             StringArgumentType.greedyString()
        //$$         ).executes { context ->
        //$$             val arguments = StringArgumentType.getString(context, "please type the arguments of this command here").split(" ")
        //$$             command.processCommand(null, arguments.toTypedArray())
        //$$             1
        //$$         }
        //$$     )
        //$$ )
        //$$ command.commandAliases.forEach {
        //$$     dispatcher.register(LiteralArgumentBuilder.literal<Any?>(it).redirect(original))
        //$$ }
        //#endif

        builders.add(this)
    }
}
