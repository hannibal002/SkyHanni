package at.hannibal2.hanni.features.rift.area.wyldwoods

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.addAlpha
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.EntityUtils.getEntities
import at.hannibal2.hanni.utils.EntityUtils.holdingSkullTexture
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.SkullTextureHolder
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object RiftOdonata {

    private val config get() = RiftApi.config.area.wyldWoods.odonata
    private var hasBottleInHand = false

    private val ODONATA_SKULL_TEXTURE by lazy { SkullTextureHolder.getTexture("MOB_ODONATA") }
    private val emptyBottle = "EMPTY_ODONATA_BOTTLE".toInternalName()

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        checkHand()
        if (!hasBottleInHand) return

        findOdonatas()
    }

    private fun checkHand() {
        hasBottleInHand = InventoryUtils.getItemInHand()?.getInternalName() == emptyBottle
    }

    private fun findOdonatas() {
        for (stand in getEntities<EntityArmorStand>()) {
            if (stand.holdingSkullTexture(ODONATA_SKULL_TEXTURE)) {
                RenderLivingEntityHelper.setEntityColor(
                    stand,
                    config.highlightColor.toColor().addAlpha(1),
                ) { isEnabled() && hasBottleInHand }
            }
        }
    }

    fun isEnabled() = RiftApi.inRift() && config.highlight
}
