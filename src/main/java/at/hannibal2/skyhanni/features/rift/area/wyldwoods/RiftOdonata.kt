package at.hannibal2.skyhanni.features.rift.area.wyldwoods

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.getEntities
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import net.minecraft.entity.item.EntityArmorStand

@SkyHanniModule
object RiftOdonata {

    private val config get() = RiftAPI.config.area.wyldWoods.odonata
    private var hasBottleInHand = false

    // TODO: Move to repo
    @Suppress("MaxLineLength")
    private const val ODONATA_SKULL_TEXTURE =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWZkODA2ZGVmZGZkZjU5YjFmMjYwOWM4ZWUzNjQ2NjZkZTY2MTI3YTYyMzQxNWI1NDMwYzkzNThjNjAxZWY3YyJ9fX0="
    private val emptyBottle by lazy { "EMPTY_ODONATA_BOTTLE".asInternalName() }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return

        checkHand()
        if (!hasBottleInHand) return

        if (event.repeatSeconds(1)) {
            findOdonatas()
        }
    }

    private fun checkHand() {
        hasBottleInHand = InventoryUtils.getItemInHand()?.getInternalName() == emptyBottle
    }

    private fun findOdonatas() {
        for (stand in getEntities<EntityArmorStand>()) {
            if (stand.hasSkullTexture(ODONATA_SKULL_TEXTURE)) {
                RenderLivingEntityHelper.setEntityColor(
                    stand,
                    config.highlightColor.toChromaColor().withAlpha(1)
                ) { isEnabled() && hasBottleInHand }
            }
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.highlight
}
