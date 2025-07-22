package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils.senderIsSkyhanni
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ViewRecipeCommand {

    private val config get() = SkyHanniMod.feature.misc.commands

    /**
     * REGEX-TEST: /viewrecipe aspect of the end
     * REGEX-TEST: /viewrecipe aspect_of_the_end
     * REGEX-TEST: /viewrecipe ASPECT_OF_THE_END
     */
    private val pattern by RepoPattern.pattern(
        "commands.viewrecipe",
        "\\/viewrecipe (?<item>.*)"
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.viewRecipeLowerCase) return
        if (event.senderIsSkyhanni()) return

        val input = pattern.matchMatcher(event.message.lowercase()) {
            group("item").uppercase()
        } ?: return

        val args = input.split(" ")
        val endsWithPageNumber = args.last().isInt()

        val (item, page) = if (endsWithPageNumber) {
            val testItem = args.joinToString(" ").toInternalName().getItemStackOrNull()
            if (testItem == null) {
                args.dropLast(1).joinToString("_") to args.last().toInt()
            } else {
                input.replace(" ", "_") to 1
            }
        } else {
            input.replace(" ", "_") to 1
        }

        event.cancel()
        HypixelCommands.viewRecipe(item.toInternalName(), page)
    }

    val list by lazy {
        val list = mutableListOf<String>()
        for ((key, value) in NeuItems.allNeuRepoItems()) {
            if (value.has("recipe")) {
                list.add(key.lowercase())
            }
        }
        list
    }

    fun customTabComplete(command: String): List<String>? {
        if (command == "viewrecipe" && config.tabComplete.viewrecipeItems) {
            return list
        }

        return null
    }
}
