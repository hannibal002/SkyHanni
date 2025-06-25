package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

@SkyHanniModule(devOnly = true)
object TestRenderItems : RenderableTestSuite.TestRenderable("items") {

    override fun renderable(): Renderable? {
        val scale = 0.1

        val scaleList = generateSequence(scale) { it + 0.1 }.take(25).toList()

        val labels = scaleList.map { RenderableString(it.roundTo(1).toString()) }

        val items = listOf(
            ItemStack(Blocks.glass_pane), ItemStack(Items.diamond_sword), ItemStack(Items.skull),
            ItemStack(Blocks.melon_block),
        ).map { item ->
            scaleList.map { ItemStackRenderable(item, it, 0).renderBounds() }
        }

        val table = listOf(labels) + items

        return VerticalContainerRenderable(
            listOf(
                Renderable.table(table),
                HorizontalContainerRenderable(
                    listOf(
                        RenderableString("Default:").renderBounds(),
                        ItemStackRenderable(ItemStack(Items.diamond_sword)).renderBounds(),
                    ),
                    spacing = 1,
                ),
            )
        )
    }
}
