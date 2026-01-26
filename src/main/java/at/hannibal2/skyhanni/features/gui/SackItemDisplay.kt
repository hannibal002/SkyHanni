package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object SackItemDisplay {
    var items: MutableList<NeuInternalName> = mutableListOf()
    var display: List<Renderable> = emptyList()
    private val config get() = SkyHanniMod.feature.gui.sackItemDisplay

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdisplaysackitem") {
            description = "Displays Sack Contained Amount of Item"
            category = CommandCategory.USERS_ACTIVE
            arg("Item", BrigadierArguments.greedyString(), BrigadierUtils.dynamicSuggestionProvider({ handleTabComplete() })) { args ->
                callback { command(getArg(args)) }
            }

        }
    }

    fun handleTabComplete(): List<String> {
        return SackApi.sackListNames.map { it.replace(" ", "_") }
    }

    fun command(args: String) {
        val fixedarg = args.replace("_", " ")
        val foundInternalName = NeuInternalName.fromItemNameOrNull(fixedarg)
        if (foundInternalName == null) {
            ChatUtils.chat("Item $fixedarg could not be found.")
            return
        }
        if (items.remove(foundInternalName)) {
            updateDisplay()
            ChatUtils.chat("Stopped Tracking ${foundInternalName.repoItemName}")
            return
        }
        items.add(foundInternalName)
        ChatUtils.chat("Started Tracking ${foundInternalName.repoItemName}")
        updateDisplay()
    }

    @HandleEvent
    fun onSackUpdate(event: SackChangeEvent) {
        updateDisplay()
    }

    private fun updateDisplay() {
        val nonNullItems = items
        display = createRenderable(nonNullItems)
    }

    private fun createRenderable(internalNames: List<NeuInternalName>): List<Renderable> {
        val renderables: MutableList<Renderable> = mutableListOf()
        internalNames.forEach { item ->
            val stored = item.getAmountInSacks()
            val price = (item.getPrice(config.priceSource) * stored).formatCoin()
            renderables.add(Renderable.text("${item.repoItemName}: §2$stored $price"))
        }
        return renderables
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (items.isEmpty()) return
        config.position.renderRenderables(display, posLabel = "Sack Item Display")
    }
}
