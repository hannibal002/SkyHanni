package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
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
        "/viewrecipe (?<item>.*)",
    )

    var list = emptyList<String>()
        private set

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

    @HandleEvent(NeuRepositoryReloadEvent::class)
    fun onNeuRepoReload() {
        list = NeuItems.allNeuRepoItems().asSequence().filter { (_, json) ->
            json.recipe != null || json.recipes.any { it.type == NeuRecipeType.CRAFTING }
        }.map { (key, json) -> key.asString().toSkyblockCommandId(json) }.toList()
    }

    private fun String.toSkyblockCommandId(itemData: NeuItemJson): String = when {
        PetUtils.isNeuRepoPetItem(itemData) -> substringBefore(';')
        contains(';') && itemData.isEnchantedBook -> {
            val (name, level) = split(";", limit = 2)
            "ENCHANTED_BOOK_${name}_$level"
        }

        else -> this
    }

    private val NeuItemJson.isEnchantedBook: Boolean
        get() = itemId.removePrefix("minecraft:") == "enchanted_book"

    fun customTabComplete(command: String): List<String>? {
        if (!SkyBlockUtils.inSkyBlock) return null
        if (command == "viewrecipe" && config.tabComplete.viewrecipeItems) {
            return list
        }

        return null
    }
}
