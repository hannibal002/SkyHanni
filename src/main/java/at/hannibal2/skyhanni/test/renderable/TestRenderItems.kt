package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.AnimatedItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackAnimationFrame
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackBounceDefinition
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRotationDefinition
import at.hannibal2.skyhanni.utils.renderables.item.NeuItemStackProvider
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing

@SkyHanniModule(devOnly = true)
object TestRenderItems : RenderableTestSuite.TestRenderable("items") {

    private val boxOfSeedsProvider = NeuItemStackProvider("BOX_OF_SEEDS".toInternalName())
    private val animationFrames = listOf(ItemStackAnimationFrame(boxOfSeedsProvider, ticks = 0))

    private val animatedItemStackRenderable by lazy {
        AnimatedItemStackRenderable(
            animationFrames,
            rotation = ItemStackRotationDefinition(
                axis = EnumFacing.Axis.Y,
                rotationSpeed = 65.0,
            ),
            bounce = ItemStackBounceDefinition(
                upwardBounce = 25,
                downwardBounce = 25,
                bounceSpeed = 8.0,
            ),
            scale = 4.0,
        ).renderBounds()
    }

    override fun renderable(): Renderable {
        val scale = 0.1

        val scaleList = generateSequence(scale) { it + 0.1 }.take(25).toList()

        val labels = scaleList.map { StringRenderable(it.roundTo(1).toString()) }

        val items = listOf(
            ItemStack(Blocks.glass_pane), ItemStack(Items.diamond_sword), ItemStack(Items.skull),
            ItemStack(Blocks.melon_block),
        ).map { item ->
            scaleList.map { ItemStackRenderable(item, it, 0).renderBounds() }
        }

        val table = listOf(labels) + items

        return HorizontalContainerRenderable(
            listOf(
                VerticalContainerRenderable(
                    listOf(
                        Renderable.table(table),
                        HorizontalContainerRenderable(
                            listOf(
                                StringRenderable("Default:").renderBounds(),
                                ItemStackRenderable(ItemStack(Items.diamond_sword)).renderBounds(),
                            ),
                            spacing = 1,
                        ),
                    ),
                ),
                animatedItemStackRenderable,
            ),
            4
        )
    }
}
