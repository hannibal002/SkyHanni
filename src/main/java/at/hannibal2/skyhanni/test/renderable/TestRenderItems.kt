package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

@SkyHanniModule(devOnly = true)
object TestRenderItems : RenderableTestSuit.TestRenderable("items") {

    override fun renderable(): Renderable? {
        val scale = 0.1

        val scaleList = generateSequence(scale) { it + 0.1 }.take(25).toList()

        val labels = scaleList.map { Renderable.string(it.roundTo(1).toString()) }

        val items = listOf(
            ItemStack(Blocks.glass_pane), ItemStack(Items.diamond_sword), ItemStack(Items.skull),
            ItemStack(Blocks.melon_block),
        ).map { item ->
            scaleList.map { Renderable.itemStack(item, it, xSpacing = 0).renderBounds() }
        }

        val table = listOf(labels) + items

        return Renderable.verticalContainer(
            listOf(
                Renderable.table(table),
                Renderable.horizontalContainer(
                    listOf(
                        Renderable.string("Default:").renderBounds(),
                        Renderable.itemStack(ItemStack(Items.diamond_sword)).renderBounds(),
                    ),
                    spacing = 1,
                ),
            ),
        )
    }
}
