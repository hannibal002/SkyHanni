package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.ItemStackDirectProvider.Companion.asProvider
import at.hannibal2.skyhanni.utils.renderables.ItemStackProvider
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AnimatedBounceDefinition
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemStackRenderable.Companion.animatedItemStack
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AnimatedBounceLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AxisBounceDefinition
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrameLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.framed.ItemStackAnimatedFrame
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AxisRotationDefinition
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.core.Direction.Axis
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

@SkyHanniModule(devOnly = true)
object TestRenderItems : RenderableTestSuite.TestRenderableFor<GameOverlayRenderPostEvent>(
    "items",
    eventClass = GameOverlayRenderPostEvent::class,
) {
    private val scaleList = generateSequence(0.1) { it + 0.1 }.take(25).toList()
    private val boxOfSeedsProvider = NeuItemStackProvider("BOX_OF_SEEDS".toInternalName())
    private val bambooProvider = NeuItemStackProvider("BAMBOO".toInternalName())
    private val animationFrames = listOf(ItemStackAnimatedFrame(boxOfSeedsProvider, ticks = 0))

    private val poleBounceDef = Axis.Y to AxisBounceDefinition(25.0, 8.0)
    private val animatedBounceStorage = AnimatedBounceLocalStorage(AnimatedBounceDefinition(poleBounceDef))
    private val sideBounceDef = Axis.X to AxisBounceDefinition(30.0, 10.0)
    private val sidewaysAnimatedBounceStorage = AnimatedBounceLocalStorage(AnimatedBounceDefinition(sideBounceDef))
    private val multiBounceDef = AnimatedBounceLocalStorage(
        AnimatedBounceDefinition(poleBounceDef, sideBounceDef)
    )
    private val itemProviders: List<ItemStackProvider> = listOf(
        ItemStack(Blocks.GLASS_PANE).asProvider(),
        ItemStack(Items.DIAMOND_SWORD).asProvider(),
        ItemStack(Items.PLAYER_HEAD).asProvider(),
        ItemStack(Blocks.MELON).asProvider(),
        bambooProvider
    )
    private val spinningStacks by lazy {
        Axis.entries.map {
            "${it.name.uppercase()} Axis" to Renderable.animatedItemStack {
                frameStorage = AnimatedFrameLocalStorage(animationFrames)
                rotationStorage = AnimatedRotationLocalStorage(it to AxisRotationDefinition(65.0))
                bounceStorage = animatedBounceStorage
                scale = 4.0
            }
        }.toList() + listOf(
            "All Axes" to Renderable.animatedItemStack {
                frameStorage = AnimatedFrameLocalStorage(animationFrames)
                rotationStorage = AnimatedRotationLocalStorage(65.0).apply {
                    this.rotationDefinition.setStaticRotation(Axis.X, 25.0)
                }
                bounceStorage = animatedBounceStorage
                scale = 4.0
            },
            "All Axes (Side)" to Renderable.animatedItemStack {
                frameStorage = AnimatedFrameLocalStorage(animationFrames)
                rotationStorage = AnimatedRotationLocalStorage(-65.0)
                bounceStorage = sidewaysAnimatedBounceStorage
                scale = 4.0
            },
            "All Axes (Multi)" to Renderable.animatedItemStack {
                frameStorage = AnimatedFrameLocalStorage(animationFrames)
                rotationStorage = AnimatedRotationLocalStorage(65.0)
                bounceStorage = multiBounceDef
                scale = 4.0
                alpha = 0.5f
            },
        )
    }
    private val itemRenderables by lazy {
        itemProviders.map { provider ->
            scaleList.map { scale ->
                Renderable.item(provider) {
                    this.scale = scale
                    xSpacing = 0
                }.renderBounds()
            }
        }
    }
    private val labels by lazy {
        scaleList.map { Renderable.text(it.roundTo(1).toString()) }
    }
    private val tableContent by lazy {
        listOf(labels) + itemRenderables
    }

    override fun renderable(): Renderable = with(Renderable) {
        vertical(
            vertical(
                table(tableContent),
                horizontal(
                    text("Default:").renderBounds(),
                    item(ItemStack(Items.DIAMOND_SWORD)).renderBounds(),
                    spacing = 1,
                ),
            ),
            horizontal(
                spinningStacks.map { (axisLabel, renderable) ->
                    vertical(
                        text(axisLabel),
                        text("(#${renderable.stableRenderId})"),
                        renderable.renderBounds(),
                        spacing = 1,
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    )
                },
                spacing = 2,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            ),
            spacing = 4,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
    }
}
