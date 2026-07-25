package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrCommon
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import java.awt.Color

@SkyHanniModule
object GlowingDroppedItems {

    private val config get() = SkyHanniMod.feature.misc

    @HandleEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (!isEnabled()) return
        if (event.type !== RenderEntityOutlineEvent.Type.NO_XRAY) return

        event.queueEntitiesToOutline(::getGlowColor)
    }

    private fun getGlowColor(entity: Entity): Color? {
        if (entity.isCurrentlyGlowing) return null
        val itemEntity = entity as? ItemEntity ?: return null
        val stack = itemEntity.item
        val rarity = stack.getItemRarityOrCommon()
        return rarity.color.toColor()
    }

    private fun isEnabled(): Boolean = SkyBlockUtils.inSkyBlock && config.glowingDroppedItems
}
