package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.skyblock.SkyblockEquipmentChangeEvent
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeModifier

@SkyHanniModule
object CocoonUtils {
    var canCocoon = false
        private set

    private fun playerCanCocoon() {
        val belt = EquipmentApi.getEquipment(EquipmentSlot.BELT) ?: return
        canCocoon = (belt.getInternalName() == "THE_PRIMORDIAL".toInternalName() || belt.getReforgeModifier() == "blood_shot")
    }

    @HandleEvent
    fun onSkyblockEquipmentChange(event: SkyblockEquipmentChangeEvent) {
        if (!event.isBelt) return
        if (event.newItemStack == null) return
        val belt = event.newItemStack
        canCocoon = (belt.getInternalName() == "THE_PRIMORDIAL".toInternalName() || belt.getReforgeModifier() == "blood_shot")
    }

    @HandleEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        playerCanCocoon()
    }
}
