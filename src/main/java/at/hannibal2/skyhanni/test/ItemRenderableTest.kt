package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.item.AnimatedItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackBounceDefinition
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRotationDefinition
import net.minecraft.util.EnumFacing

@SkyHanniModule
object ItemRenderableTest {

    private val config get() = SkyHanniMod.feature.dev.debug
    private val BOX_OF_SEEDS_ITEM = "BOX_OF_SEEDS".toInternalName()
    private val itemStackRenderable by lazy {
        ItemStackRenderable(BOX_OF_SEEDS_ITEM.getItemStack(), scale = 3.0)
    }
    private val animatedItemStackRenderable by lazy {
        AnimatedItemStackRenderable(
            BOX_OF_SEEDS_ITEM.getItemStack(),
            rotation = ItemStackRotationDefinition(
                axis = EnumFacing.Axis.Y,
                rotationSpeed = 65.0
            ),
            bounce = ItemStackBounceDefinition(
                upwardBounce = 25,
                downwardBounce = 25,
                bounceSpeed = 8.0
            ),
            scale = 4.0,
        )
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (config.itemStack) {
            config.itemStackPosition.renderRenderable(itemStackRenderable, "Test ItemStack Renderable")
        }
        if (config.animatedItemStack) {
            config.animatedItemStackPosition.renderRenderable(
                animatedItemStackRenderable,
                "Test Animated ItemStack Renderable"
            )
        }
    }

}
