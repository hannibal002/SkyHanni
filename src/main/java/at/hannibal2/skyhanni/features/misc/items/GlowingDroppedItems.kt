package at.hannibal2.hanni.features.misc.items

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandTypeTags
import at.hannibal2.hanni.events.RenderEntityOutlineEvent
import at.hannibal2.hanni.features.garden.pests.SprayType
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EnumUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.hanni.utils.RecalculatingValue
import at.hannibal2.hanni.utils.SkyBlockUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

@HanniModule
object GlowingDroppedItems {

    private val config get() = HanniMod.feature.misc.glowingDroppedItems

    /**
     * List of SkyBlock locations where we might see items in showcases
     */
    private val showcaseItemLocations = setOf(
        "The End",
        "Jerry's Workshop",
        "Dark Auction",
        "Photon Pathway",
        "Barrier Street",
        "Village Plaza",
        "Déjà Vu Alley",
    )

    @HandleEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && event.type === RenderEntityOutlineEvent.Type.XRAY) {
            event.queueEntitiesToOutline { getEntityOutlineColor(it) }
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled

    private fun getEntityOutlineColor(entity: Entity): Color? {
        val item = entity as? EntityItem ?: return null
        if (shouldHideShowcaseItem(entity)) return null

        val entityItem = item.entityItem
        if (!config.highlightFishingBait && entityItem.displayName.endsWith(" Bait")) {
            return null
        }

        val internalName = entityItem.getInternalNameOrNull() ?: return null
        val isSprayItem = EnumUtils.enumValueOfOrNull<SprayType>(internalName.asString()) != null
        if (isSprayItem) return null
        val rarity = entityItem.getItemRarityOrNull()
        return rarity?.color?.toColor()
    }

    private val isShowcaseArea by RecalculatingValue(1.seconds) {
        // TODO use graph area when fixing the end area
        IslandTypeTags.HAS_SHOWCASES.inAny() || SkyBlockUtils.scoreboardArea in showcaseItemLocations
    }

    private fun shouldHideShowcaseItem(entity: EntityItem): Boolean {
        if (!isShowcaseArea || config.highlightShowcase) return false

        for (entityArmorStand in entity.worldObj.getEntitiesWithinAABB(
            EntityArmorStand::class.java,
            entity.entityBoundingBox,
        )) {
            if (entityArmorStand.isInvisible) {
                return true
            }
        }

        return false
    }
}
