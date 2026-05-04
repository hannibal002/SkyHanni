package at.hannibal2.skyhanni.features.inventory.sacks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacksOrNull
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal

@SkyHanniModule
object SackDisplayOverlay {

    private val config get() = SkyHanniMod.feature.inventory.sackDisplayOverlay

    val displayedItems: MutableSet<NeuInternalName> = mutableSetOf()

    private fun drawDisplay(): List<Renderable> = buildList {
        addString("§7Sack Display:")
        for (item in displayedItems.sortedByDescending { it.getAmountInSacksOrNull() }) {
            val count = item.getAmountInSacksOrNull() ?: 0
            add(Renderable.horizontal {
                addString(" §7- §e${count.addSeparators()}x")
                addItemStack(item)
                addString(item.repoItemName)
            })
        }
    }

    @HandleEvent
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled || displayedItems.isEmpty()) return

        config.position.renderRenderables(drawDisplay(), posLabel = "Sack Display Overlay")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shsackdisplayoverlay") {
            description = "Track counts for specified sack items"
            category = CommandCategory.USERS_ACTIVE
            arg("itemType", BrigadierArguments.string()) { args ->
                callback {
                    val itemType = getArg(args).toInternalName()

                    if (displayedItems.contains(itemType)) {
                        displayedItems.remove(itemType)
                        ChatUtils.chat("Stopped tracking ${itemType.itemNameWithoutColor}.")
                        return@callback
                    }

                    displayedItems.add(itemType)
                    ChatUtils.chat("Started tracking ${itemType.itemNameWithoutColor}.")
                }
            }
        }
    }

    fun handleTabComplete(command: String): List<String>? {
        if (command != "shsackdisplayoverlay") return null

        return SackApi.sackListInternalNames.toList()
    }
}