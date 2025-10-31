package at.hannibal2.hanni.features.rift.area.wyldwoods

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.addAlpha
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.EntityUtils.getEntities
import at.hannibal2.hanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.SkullTextureHolder
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object RiftLarva {

    private val config get() = RiftApi.config.area.wyldWoods.larvas
    private var hasHookInHand = false

    private val LARVA_SKULL_TEXTURE by lazy { SkullTextureHolder.getTexture("RIFT_LARVA") }

    private val LARVA_HOOK = "LARVA_HOOK".toInternalName()

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        checkHand()
        if (!hasHookInHand) return

        findLarvas()
    }

    private fun checkHand() {
        hasHookInHand = InventoryUtils.getItemInHand()?.getInternalName() == LARVA_HOOK
    }

    private fun findLarvas() {
        for (stand in getEntities<EntityArmorStand>()) {
            if (stand.wearingSkullTexture(LARVA_SKULL_TEXTURE)) {
                RenderLivingEntityHelper.setEntityColor(
                    stand,
                    config.highlightColor.toColor().addAlpha(1),
                ) { isEnabled() && hasHookInHand }
            }
        }
    }

    fun isEnabled() = RiftApi.inRift() && config.highlight
}
