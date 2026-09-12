package at.hannibal2.skyhanni.features.rift.area.wyldwoods

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.EntityUtils.getEntities
import at.hannibal2.skyhanni.utils.EntityUtils.holdingSkullTexture
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object RiftOdonata {
    private val ODONATA_SKULL_TEXTURE by SkullTextureHolder.texture("MOB_ODONATA")
    private val EMPTY_ODONATA_BOTTLE = "EMPTY_ODONATA_BOTTLE".toInternalName()

    private val config get() = RiftApi.config.area.wyldWoods.odonata

    private var hasBottleInHand = false

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (isEnabled()) {
            checkHand()
        }
    }

    private fun checkHand() {
        hasBottleInHand = InventoryUtils.getItemInHand()?.getInternalName() == EMPTY_ODONATA_BOTTLE
    }

    @HandleEvent
    fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (isEnabled()) tryAdd(event.entity)
    }

    private fun tryAdd(stand: ArmorStand) {
        if (!stand.holdingSkullTexture(ODONATA_SKULL_TEXTURE)) return
        RenderLivingEntityHelper.setEntityColor(stand, config.highlightColor.toColor().addAlpha(1)) {
            isEnabled() && hasBottleInHand
        }
    }

    // This only gets called on config change, so the performance impact is minimal
    @OptIn(AllEntitiesGetter::class)
    @HandleEvent
    fun onConfigLoad() {
        config.highlight.onToggle {
            getEntities<ArmorStand>().forEach(::tryAdd)
        }
    }

    private fun isEnabled() = RiftApi.inRift() && config.highlight.get()
}
