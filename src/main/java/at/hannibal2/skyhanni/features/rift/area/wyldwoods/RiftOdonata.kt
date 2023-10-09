package at.hannibal2.skyhanni.features.rift.area.wyldwoods

import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils.getEntities
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RiftOdonata {
    private val config get() = RiftAPI.config.area.wyldWoodsConfig.odonata
    private var hasBottleInHand = false
    val odonataSkullTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWZkODA2ZGVmZGZkZjU5YjFmMjYwOWM4ZWUzNjQ2NjZkZTY2MTI3YTYyMzQxNWI1NDMwYzkzNThjNjAxZWY3YyJ9fX0="

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        checkHand()
        if (!hasBottleInHand) return

        if (event.repeatSeconds(1)) {
            findOdonatas()
        }
    }

    private fun checkHand() {
        hasBottleInHand = InventoryUtils.getItemInHand()?.getInternalName()?.equals("EMPTY_ODONATA_BOTTLE") ?: false
    }

    private fun findOdonatas() {
        for (stand in getEntities<EntityArmorStand>()) {
            if (stand.hasSkullTexture(odonataSkullTexture)) {
                RenderLivingEntityHelper.setEntityColor(
                    stand,
                    config.highlightColor.toChromaColor().withAlpha(1)
                ) { isEnabled() && hasBottleInHand }
            }
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.highlight
}
