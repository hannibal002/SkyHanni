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
    private fun onRenderEntityOutline(event: RenderEntityOutlineEvent) {
        if (!isEnabled()) return

        event.queueEntitiesToOutline(::getGlowColor)
    }

    private fun getGlowColor(entity: Entity): Color? {
        // The config option explicitly says it glows items Hypixel doesn't.
        // While Hypixel and SkyHanni will probably agree on the glow color
        // the majority of the time, we don't want to inadvertently override
        // Hypixel's glow color if it happens to be different.
        if (entity.isCurrentlyGlowing) return null

        val itemEntity = entity as? ItemEntity ?: return null
        val stack = itemEntity.item
        val rarity = stack.getItemRarityOrCommon()
        return rarity.color.toColor()
    }

    private fun isEnabled(): Boolean = SkyBlockUtils.inSkyBlock && config.glowingDroppedItems
}
