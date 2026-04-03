package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeModifier

object CocoonUtils {
    fun playerCanCocoon(): Boolean {
        var canCocoon = false
        val belt = EquipmentApi.getEquipment(EquipmentSlot.BELT) ?: return false
        canCocoon = (belt.getInternalName() == "THE_PRIMORDIAL".toInternalName() || belt.getReforgeModifier() == "blood_shot")
        return canCocoon
    }
}
