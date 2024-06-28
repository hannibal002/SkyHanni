package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeAPI.getAttributesLevels
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeAPI.isGoodRoll
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.StringUtils.width
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object AttributeOverlay {

    private val config get() = SkyHanniMod.feature.inventory.attributeOverlay

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        if (!isEnabled()) return

        val stack = event.stack ?: return
        val attributes = stack.getAttributesLevels() ?: return
        val internalName = stack.getInternalNameOrNull() ?: return

        val isGoodRoll = attributes.isGoodRoll(internalName)
        attributes.toList().filter { (attr, level) ->
            val isInConfig = (attr in config.attributesList && level >= config.minimumLevel)
            val godRoll = !config.hideNonGoodRolls || isGoodRoll
            isInConfig && godRoll
        }.forEachIndexed { index, attribute ->
            event.drawAttribute(attribute, isGoodRoll && config.highlightGodrolls, index)
        }
    }

    private fun GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost.drawAttribute(
        attribute: AttributeAPI.Attribute,
        goodRoll: Boolean,
        index: Int,
    ) {
        val scale = 0.5714286f
        val color = if (goodRoll) "§e" else "§b"
        val attributeString = color + attribute.type.shortName
        val attributeWidth = attributeString.width()
        val attributeX = x + attributeWidth + (if (index == 1) 16 - attributeWidth * scale else 0).toInt()
        val attributeY = y
        drawSlotText(attributeX, attributeY, attributeString, scale)

        val levelString = "§a${attribute.level}"
        val levelWidth = levelString.width()
        val levelX = x + levelWidth + (if (index == 1) 16 - levelWidth * scale else 0).toInt()
        val levelY = y + (10 * scale).toInt()
        drawSlotText(levelX, levelY, levelString, scale)
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
