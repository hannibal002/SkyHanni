package at.hannibal2.skyhanni.features.misc.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarity
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GlowingDroppedItems {

    private val config get() = SkyHanniMod.feature.misc

    /**
     * List of skyblock locations where we might see items in showcases
     */
    private val showcaseItemLocations = listOf(
            "The End",
            "Jerry's Workshop"
            ).toSet()

    @SubscribeEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && event.type === RenderEntityOutlineEvent.Type.XRAY) {
            event.queueEntitiesToOutline(getEntityOutlineColor)
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock &&  config.itemConfig.highlightDroppedItems

    private val getEntityOutlineColor: (entity: Entity) -> Int? = { entity ->
        if (EntityItem::class.java.isInstance(entity) && !shouldHideShowcaseItem(entity as EntityItem)) {
            val rarity = entity.entityItem.getItemRarity()

            if (rarity != LorenzRarity.UNKNOWN && (config.itemConfig.highlightFishingBait || entity.entityItem.name?.endsWith(" Bait") != true)) {
                rarity.color.toColor().rgb
            } else null
        } else null
    }

    private fun isShowcaseArea() = showcaseItemLocations.contains(LorenzUtils.skyBlockArea) || LorenzUtils.inIsland(IslandType.HUB) || LorenzUtils.inIsland(IslandType.PRIVATE_ISLAND) || LorenzUtils.inIsland(IslandType.PRIVATE_ISLAND_GUEST)

    private fun shouldHideShowcaseItem(entity: EntityItem): Boolean {
        if (!isShowcaseArea() || config.itemConfig.highlightShowcaseItems) return false;

        for (entityArmorStand in entity.worldObj.getEntitiesWithinAABB(EntityArmorStand::class.java, entity.entityBoundingBox)) {
            if (entityArmorStand.isInvisible) {
                return true
            }
        }

        return false
    }
}