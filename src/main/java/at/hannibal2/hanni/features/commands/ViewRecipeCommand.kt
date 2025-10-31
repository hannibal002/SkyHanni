package at.hannibal2.hanni.features.commands

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.MessageSendToServerEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils.senderIsHanni
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.NeuItems
import at.hannibal2.hanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.hanni.utils.NumberUtil.isInt
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object ViewRecipeCommand {

    private val config get() = HanniMod.feature.misc.commands

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
        if (event.senderIsHanni()) return

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
        if (!SkyBlockUtils.inSkyBlock) return null
        if (command == "viewrecipe" && config.tabComplete.viewrecipeItems) {
            return list
        }

        return null
    }
}
