package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BaseBrigadierBuilder
import at.hannibal2.skyhanni.config.commands.brigadier.CommandData
import at.hannibal2.skyhanni.events.utils.PreInitFinishedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrInsert
import com.mojang.brigadier.CommandDispatcher
//#if MC < 1.21
import net.minecraftforge.client.ClientCommandHandler

//#else
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//$$ import com.mojang.brigadier.builder.LiteralArgumentBuilder
//#endif

@SkyHanniModule
object CommandsRegistry {
    //#if MC < 1.21
    private val dispatcher: CommandDispatcher<Any?> = CommandDispatcher()
    //#endif

    @HandleEvent(PreInitFinishedEvent::class)
    fun onPreInitFinished() {
        //#if MC < 1.21
        CommandRegistrationEvent(dispatcher).post()
        //#else
        //$$ ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
        //$$     CommandRegistrationEvent(dispatcher as CommandDispatcher<Any?>).post()
        //$$ }
        //#endif
    }

    private fun String.isUnique(builders: List<CommandData>) {
        require(builders.all { this !in it.getAllNames() }) {
            "The command $this is already registered!"
        }
    }

    fun CommandData.hasUniqueName(builders: List<CommandData>) {
        name.isUnique(builders)
        aliases.forEach { it.isUnique(builders) }
    }

    fun BaseBrigadierBuilder.addToRegister(dispatcher: CommandDispatcher<Any?>, builders: MutableList<CommandData>) {
        //#if MC < 1.21
        val command = toCommand(dispatcher)
        ClientCommandHandler.instance.registerCommand(command)
        //#else
        //$$ val original = dispatcher.register(builder as LiteralArgumentBuilder<Any?>)
        //$$ aliases.forEach {
        //$$     dispatcher.register(LiteralArgumentBuilder.literal<Any?>(it).redirect(original))
        //$$ }
        //#endif
        addBuilder(builders)
    }

    fun <T : CommandBuilderBase> T.addToRegister(dispatcher: CommandDispatcher<Any?>, builders: MutableList<CommandData>) {
        //#if MC < 1.21
        val command = this.toCommand(dispatcher)
        ClientCommandHandler.instance.registerCommand(command)
        addBuilder(builders)
        //#else
        //$$ if (this !is CommandBuilder) return // complex commands are not supported in 1.21.5 right now
        //$$ val builder = BaseBrigadierBuilder(name).apply {
        //$$     this.description = this@addToRegister.descriptor
        //$$     this.aliases = this@addToRegister.aliases
        //$$     this.category = this@addToRegister.category
        //$$
        //$$     legacyCallbackArgs(this@addToRegister.getCallback())
        //$$ }
        //$$ builder.addToRegister(dispatcher, builders)
        //#endif
    }

    // Adds the command to the builders list in a way that all commands are ordered depending on their category and name.
    private fun CommandData.addBuilder(builders: MutableList<CommandData>) {
        val comparator = compareBy(CommandData::category, CommandData::name)
        for ((i, command) in builders.withIndex()) {
            val comparison = comparator.compare(this, command)
            if (comparison < 0) {
                builders.addOrInsert(i, this)
                return
            }
        }
        builders.add(this)
    }
}
