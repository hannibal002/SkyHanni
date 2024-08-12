package at.hannibal2.skyhanni.features.gui.bar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.gui.CustomHUDBarConfig
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
// TODO better name?
object CustomHUDBar {

    private val config get() = SkyHanniMod.feature.gui.bar
    private var display: Renderable? = null

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) {
            display = null
            return
        }
        val padding = 5

        val scaledWidth = ScaledResolution(Minecraft.getMinecraft()).scaledWidth

        val (leftEntries, rightEntries) = splitEntries()

        val leftRenderable = createHorizontalContainer(leftEntries, HorizontalAlignment.LEFT)
        val rightRenderable = createHorizontalContainer(rightEntries, HorizontalAlignment.RIGHT)

        // Calculate remaining space after entries' width
        val totalEntriesWidth = leftEntries.sumOf { it.width } + rightEntries.sumOf { it.width }
        val remainingSpace = scaledWidth - totalEntriesWidth

        // Calculate the width of the middle space (gap) between left and right groups
        val middleSpaceWidth = remainingSpace - config.spacing * (leftEntries.size + rightEntries.size - 2) - padding * 2
        val middleSpace = Renderable.placeholder(middleSpaceWidth, leftRenderable.height)

        display = Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.placeholder(scaledWidth, maxOf(leftRenderable.height, rightRenderable.height)),
                Renderable.horizontalContainer(listOf(leftRenderable, middleSpace, rightRenderable)),
            ),
            config.color.toChromaColor(),
            padding = padding,
            radius = 0,
            smoothness = 0,
        )
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        display?.let {
            val scaledHeight = ScaledResolution(Minecraft.getMinecraft()).scaledHeight
            val position = Position(
                0,
                if (config.alignment == CustomHUDBarConfig.BarAlignment.TOP) 0 else scaledHeight - it.height,
            )
            position.renderRenderable(it, "Bar", addToGuiManager = false)
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    private fun splitEntries(): Pair<List<Renderable>, List<Renderable>> {
        val leftEntries = mutableListOf<Renderable>()
        val rightEntries = mutableListOf<Renderable>()
        var alignLeft = true

        config.entries.forEach {
            if (it == CustomHUDBarEntry.ALIGN_LEFT_RIGHT) {
                alignLeft = false
            } else {
                val renderable = Renderable.string(it.element.getString())
                if (alignLeft) {
                    leftEntries.add(renderable)
                } else {
                    rightEntries.add(renderable)
                }
            }
        }

        return leftEntries to rightEntries
    }

    private fun createHorizontalContainer(entries: List<Renderable>, alignment: HorizontalAlignment): Renderable =
        Renderable.horizontalContainer(
            entries,
            spacing = config.spacing,
            horizontalAlign = alignment,
        )
}
