package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.StorageApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import net.minecraft.item.ItemStack

@SkyHanniModule
object SearchableStorage {

    private val config get() = SkyHanniMod.feature.inventory.searchableStorage

    private var display = emptyList<Renderable>()

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        config.displayPosition.renderRenderables(display, posLabel = "Searchable Storage Display")
    }

    private fun searchStorage(args: Array<String>) {
        display = listOf()

        val search = args.drop(1).joinToString(" ")
        val result = mutableListOf<Renderable>()

        for (storage in StorageApi.accessStorage) {
            val highlightSlots = mutableListOf<Int>()

            val matches = storage.value.items.filter {
                when (args[0]) {
                    "name" -> it?.matchesName(search) ?: false
                    "lore" -> it?.matchesLore(search) ?: false
                    else -> run {
                        ChatUtils.userError("Wrong usage! Use /shsearchstorage <name|lore> <search...>")
                        return
                    }
                }
            }.takeIfNotEmpty() ?: continue
            matches.forEach { highlightSlots += storage.value.items.indexOf(it) }

            result += storage.value.toRenderable(highlightSlots = highlightSlots)
        }

        val rows = result.chunked(3).map { row ->
            HorizontalContainerRenderable(row)
        }

        display = listOf(VerticalContainerRenderable(rows))
    }

    private fun ItemStack.matchesName(search: String) =
        displayName.removeColor() == search || displayName.removeColor().contains(search)

    private fun ItemStack.matchesLore(search: String) =
        getLore().any { it.removeColor() == search || it.removeColor().contains(search) }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shsearchstorage") {
            description = "Search your storage for items with their names or lore."
            category = CommandCategory.USERS_ACTIVE
            callback { searchStorage(it) }
        }
    }
}
