package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.visitor.VisitorConfig.HighlightMode
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRenderEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemBlink
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.world.entity.LivingEntity

/**
 * Handles all world-space visitor rendering:
 * - Entity highlighting/coloring based on status
 * - Text above visitor heads (status + reward warnings)
 * - Special effects (Jerry/Spaceman item blinking)
 */
@SkyHanniModule
object GardenVisitorHighlight {

    private val config get() = VisitorApi.config

    // Special visitor items for blinking effect
    private val LEGENDARY_JERRY = "JERRY;4".toInternalName()
    private val SPACE_HELM = "DCTR_SPACE_HELM".toInternalName()
    private val DEDICATION_4 = "DEDICATION;4".toInternalName()

    /**
     * Renders text above a visitor's head.
     * Shows status (NEW/READY/WAITING) and reward warnings.
     */
    @HandleEvent
    fun onVisitorRender(event: VisitorRenderEvent) {
        val visitor = event.visitor
        val text = visitor.status.displayName
        val location = event.location

        event.parent.drawString(location.up(2.23), text)

        if (config.rewardWarning.showOverName) {
            drawRewardWarnings(visitor, event)
        }
    }

    /**
     * Draws reward warning text above visitor's head.
     * Stacks multiple warnings vertically if needed.
     */
    private fun drawRewardWarnings(visitor: VisitorApi.Visitor, event: VisitorRenderEvent) {
        val initialOffset = 2.73
        val heightOffset = 0.25
        var counter = 0

        visitor.getRewardWarningAwards().forEach { reward ->
            val name = reward.displayName
            val offset = initialOffset + (counter * heightOffset)
            event.parent.drawString(event.location.up(offset), "§c§l! $name §c§l!")
            counter++
        }
    }

    /**
     * Updates entity color based on visitor status.
     * Called by GardenVisitorStatus for each visitor.
     */
    fun updateEntityColor(visitor: VisitorApi.Visitor, entity: LivingEntity) {
        if (config.highlightStatus != HighlightMode.COLOR &&
            config.highlightStatus != HighlightMode.BOTH
        ) return

        val color = visitor.status.color
        if (color != null) {
            RenderLivingEntityHelper.setEntityColor(
                entity,
                color,
            ) {
                config.highlightStatus == HighlightMode.COLOR ||
                    config.highlightStatus == HighlightMode.BOTH
            }
        } else if (!GardenApi.inGarden()) {
            RenderLivingEntityHelper.removeEntityColor(entity)
        }
    }

    /**
     * Triggers special visual effects when certain visitors arrive.
     * Jerry → Blinks Legendary Jerry pet
     * Spaceman → Blinks Space Helmet
     */
    @HandleEvent
    fun onVisitorArrival(event: VisitorArrivalEvent) {
        triggerArrivalEffects(event.visitor.visitorName)
    }

    private fun triggerArrivalEffects(visitorName: String) {
        val cleanName = visitorName.removeColor()
        when {
            cleanName == "Jerry" -> ItemBlink.setBlink(LEGENDARY_JERRY.getItemStackOrNull(), 5_000)
            cleanName.contains("Spaceman") -> ItemBlink.setBlink(SPACE_HELM.getItemStackOrNull(), 5_000)
            cleanName.contains("Rhino") -> ItemBlink.setBlink(DEDICATION_4.getItemStackOrNull(), 5_000)
        }
    }
}
