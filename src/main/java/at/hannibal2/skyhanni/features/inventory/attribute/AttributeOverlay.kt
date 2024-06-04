package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeAPI.getAttributesWithLevels
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeAPI.getGodRollType
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
        val attributes = stack.getAttributesWithLevels() ?: return
        val internalName = stack.getInternalNameOrNull() ?: return

        val godRollType = attributes.getGodRollType(internalName)
        attributes.filter { (attr, level) ->
            val first = (attr in config.attributesList && level >= config.minimumLevel)
            val second = if (config.highlightGodrolls) godRollType != null else true
            first && second
        }.forEachIndexed { index, (attr, level) ->
            event.drawAttribute(attr, level, godRollType, index)
        }
    }

    private fun GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost.drawAttribute(
        attribute: AttributeAPI.Attribute,
        level: Int,
        godRollType: AttributeAPI.GodRollType?,
        index: Int
    ) {
        val scale = 0.5714286f
        val color = when (godRollType) {
            AttributeAPI.GodRollType.GODROLL -> "§e"
            AttributeAPI.GodRollType.GOOD_ROLL -> "§a"
            else -> "§b"
        }
        val attributeString = color + attribute.shortName
        val attributeWidth = attributeString.width()
        val attributeX = x + attributeWidth + (if (index == 1) 16 - attributeWidth * scale else 0).toInt()
        val attributeY = y
        drawSlotText(attributeX, attributeY, attributeString, scale)

        val levelString = "§a$level"
        val levelWidth = levelString.width()
        val levelX = x + levelWidth + (if (index == 1) 16 - levelWidth * scale else 0).toInt()
        val levelY = y + (10 * scale).toInt()
        drawSlotText(levelX, levelY, levelString, scale)
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
