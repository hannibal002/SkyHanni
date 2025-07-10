package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SignUtils
import at.hannibal2.skyhanni.utils.SignUtils.isMousematSign
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.gui.inventory.GuiEditSign

@SkyHanniModule
object GardenOptimalAngles {

    private val config get() = GardenApi.config.optimalAngles

    private var cropInHand: CropType? = null
    private var lastCrop: CropType? = null
    private var display = listOf<Renderable>()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onGuiOpen(event: GuiScreenOpenEvent) {
        if (!config.signEnabled) return
        val gui = event.gui as? GuiEditSign ?: return
        if (!gui.isMousematSign()) return

        val crops = CropType.entries.map { it to it.getAngles() }

        display = if (config.compactMousematGui) {
            crops.groupBy({ it.second }, { it.first }).map { (angles, crops) ->
                val stacks = HorizontalContainerRenderable(crops.map { ItemStackRenderable(it.icon) })
                val clickable = Renderable.clickable(
                    " §7- §e${angles.first}§7/§e${angles.second}",
                    tips = listOf(
                        "§7Click to set optimal angles",
                        "§7into the mousemat sign.",
                    ),
                    onLeftClick = { setAngles(angles) },
                )
                HorizontalContainerRenderable(
                    listOf(stacks, clickable),
                    2,
                    RenderUtils.HorizontalAlignment.LEFT, RenderUtils.VerticalAlignment.TOP,
                )
            }
        } else {
            crops.map { (crop, angles) ->
                val color = if (lastCrop == crop) LorenzColor.GOLD else LorenzColor.GREEN
                val stack = ItemStackRenderable(crop.icon)
                val clickable = Renderable.clickable(
                    "${color.getChatColor()}${crop.cropName} §7- §e${angles.first}§7/§e${angles.second}",
                    tips = listOf(
                        "§7Click to set optimal angles for ${crop.cropName}",
                        "§7into the mousemat sign.",
                    ),
                    onLeftClick = { setAngles(angles) },
                )
                HorizontalContainerRenderable(
                    listOf(stack, clickable),
                    2,
                    RenderUtils.HorizontalAlignment.LEFT, RenderUtils.VerticalAlignment.TOP,
                )
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onScreenDrawn(event: ScreenDrawnEvent) {
        if (!config.signEnabled) return
        val gui = event.gui as? GuiEditSign ?: return
        if (!gui.isMousematSign()) return
        config.signPosition.renderRenderables(
            display,
            posLabel = "Optimal Angles Mousemat Overlay",
        )
    }

    @HandleEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        cropInHand = event.crop
        event.crop?.let { lastCrop = it }
    }

    private fun setAngles(angles: Pair<Float, Float>) {
        SignUtils.setTextIntoSign("${angles.first}", 0)
        SignUtils.setTextIntoSign("${angles.second}", 3)
    }

    private fun CropType.getAngles() = getConfig().let { Pair(it.first.get(), it.second.get()) }

    private fun CropType.getConfig(): Pair<Property<Float>, Property<Float>> = with(config.customAngles) {
        when (this@getConfig) {
            CropType.CACTUS -> cactusYaw to cactusPitch
            CropType.WHEAT -> wheatYaw to wheatPitch
            CropType.CARROT -> carrotYaw to carrotPitch
            CropType.POTATO -> potatoYaw to potatoPitch
            CropType.NETHER_WART -> netherWartYaw to netherWartPitch
            CropType.PUMPKIN -> pumpkinYaw to pumpkinPitch
            CropType.MELON -> melonYaw to melonPitch
            CropType.COCOA_BEANS -> cocoaBeansYaw to cocoaBeansPitch
            CropType.SUGAR_CANE -> sugarCaneYaw to sugarCanePitch
            CropType.MUSHROOM -> mushroomYaw to mushroomPitch
        }
    }
}
