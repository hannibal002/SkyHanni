package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.skyblock.SkyblockEquipmentDataEvent
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeModifier

@SkyHanniModule
object CocoonUtils {
    var canCocoon: Boolean = false
        private set

    private fun playerCanCocoon(): Boolean {
        val belt = EquipmentApi.getEquipment(EquipmentSlot.BELT) ?: return false
        return (belt.getInternalName() == "THE_PRIMORDIAL".toInternalName() || belt.getReforgeModifier() == "blood_shot")
    }

    @HandleEvent
    fun onSkyblockEquipmentDataEvent(event: SkyblockEquipmentDataEvent) {
        if (!event.isBelt) return
        if (event.newItemStack == null) return
        val belt = event.newItemStack
        canCocoon = (belt.getInternalName() == "THE_PRIMORDIAL".toInternalName() || belt.getReforgeModifier() == "blood_shot")
    }

    @HandleEvent
    fun onProfileJoin() {
        canCocoon = playerCanCocoon()
    }
}
