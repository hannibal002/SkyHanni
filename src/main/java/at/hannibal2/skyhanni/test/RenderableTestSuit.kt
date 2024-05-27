package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabCompletionEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

enum class RenderableTestSuit(private val where: RenderIn) {
    SLIDER(RenderIn.CHEST) {

        private val scrollValue = ScrollValue()

        private var extracted = 0.0

        private val staticRenderable = Renderable.verticalSlider(
            50, handler = { extracted = it }, scrollValue
        ).renderBounds()

        override fun run() {
            position.renderRenderables(
                listOf(
                    Renderable.horizontalContainer(
                        listOf(
                            Renderable.verticalSlider(
                                50, handler = { extracted = it }, scrollValue
                            ).renderBounds(), staticRenderable
                        ),
                        5
                    ),
                    Renderable.string("$extracted"),
                ), posLabel = "Test"
            )
        }
    },
    ITEM(RenderIn.HUD) {

        override fun run() {
            val scale = 0.1
            val renderables = listOf(
                ItemStack(Blocks.glass_pane), ItemStack(Items.diamond_sword), ItemStack(Items.skull),
                ItemStack(Blocks.melon_block)
            ).map { item ->
                generateSequence(scale) { it + 0.1 }.take(25).map {
                    Renderable.itemStack(item, it, xSpacing = 0).renderBounds()
                }.toList()
            }.editCopy {
                this.add(
                    0,
                    generateSequence(scale) { it + 0.1 }.take(25).map { Renderable.string(it.round(1).toString()) }
                        .toList()
                )
            }
            position.renderRenderables(
                listOf(
                    Renderable.table(renderables),
                    Renderable.horizontalContainer(
                        listOf(
                            Renderable.string("Test:").renderBounds(),
                            Renderable.itemStack(ItemStack(Items.diamond_sword)).renderBounds()
                        ), spacing = 1
                    )
                ), posLabel = "Item Debug"
            )
        }
    },

    ;

    abstract fun run()

    val position = Position(20, 20) // TODO use a PositionList

    companion object {

        var active: RenderableTestSuit? = null

        fun commandHandler(args: Array<String>) {
            if (args.isEmpty()) {
                active = null
                ChatUtils.chat("Deactivated renderable test")
                return
            }
            val arg = args.joinToString("_").uppercase()
            val entry = entries.firstOrNull { it.name == arg }

            if (entry == null) {
                ChatUtils.userError("Invalid entry")
                return
            }
            active = entry
            ChatUtils.chat("Activated renderable test: \"${entry.name}\"")
        }

        @SubscribeEvent
        fun onGuiRenderGuiOverlayRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
            active?.let {
                if (it.where == RenderIn.HUD) {
                    it.run()
                }
            }
        }

        @SubscribeEvent
        fun onGuiRenderChestGuiOverlayRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
            active?.let {
                if (it.where == RenderIn.CHEST) {
                    it.run()
                }
            }
        }

        @SubscribeEvent
        fun onTabCompletion(event: TabCompletionEvent) {
            if (!event.isCommand("shrenderable")) return
            event.addSuggestions(entries.map { it.name })
        }
    }
}

private enum class RenderIn {
    HUD,
    CHEST
}
