package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemStackRenderable.Companion.animatedItemStack
import at.hannibal2.skyhanni.utils.renderables.animated.ItemStackAnimationFrame
import at.hannibal2.skyhanni.utils.renderables.animated.ItemStackBounceDefinition
import at.hannibal2.skyhanni.utils.renderables.animated.ItemStackRotationDefinition
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing

@SkyHanniModule(devOnly = true)
object TestRenderItems : RenderableTestSuite.TestRenderable("items") {

    private val boxOfSeedsProvider = NeuItemStackProvider("BOX_OF_SEEDS".toInternalName())
    private val bambooProvider = NeuItemStackProvider("BAMBOO".toInternalName())
    private val animationFrames = listOf(ItemStackAnimationFrame(boxOfSeedsProvider, ticks = 0))

    private val spinningStacks by lazy {
        EnumFacing.Axis.entries.map {
            val rotationDef = ItemStackRotationDefinition(
                axis = it,
                rotationSpeed = 65.0,
            )
            it to Renderable.animatedItemStack(
                animationFrames,
                rotation = rotationDef,
                bounce = ItemStackBounceDefinition(
                    upwardBounce = 25,
                    downwardBounce = 25,
                    bounceSpeed = 8.0,
                ),
                scale = 4.0,
            )
        }
    }

    override fun renderable(): Renderable {
        val scale = 0.1

        val scaleList = generateSequence(scale) { it + 0.1 }.take(25).toList()

        val labels = scaleList.map { Renderable.text(it.roundTo(1).toString()) }

        val items = listOf(
            ItemStack(Blocks.glass_pane), ItemStack(Items.diamond_sword), ItemStack(Items.skull),
            ItemStack(Blocks.melon_block),
        ).map { item ->
            scaleList.map { Renderable.item(item, it, 0).renderBounds() }
        } + listOf(scaleList.map { Renderable.item(bambooProvider, it, 0).renderBounds() })

        val tableContent = listOf(labels) + items

        return with(Renderable) {
            horizontal(
                vertical(
                    table(tableContent),
                    horizontal(
                        text("Default:").renderBounds(),
                        item(ItemStack(Items.diamond_sword)).renderBounds(),
                        spacing = 1,
                    ),
                ),
                horizontal(
                    spinningStacks.map { (axis, renderable) ->
                        vertical(
                            text("${axis.name.uppercase()} Axis"),
                            renderable.renderBounds(),
                            spacing = 1,
                            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        )
                    },
                    spacing = 2,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                ),
                spacing = 4,
            )
        }
    }
}
