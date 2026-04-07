package at.hannibal2.skyhanni.events.skyblock

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import net.minecraft.world.item.ItemStack
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onSkyblockEquipmentChange")
class SkyblockEquipmentChangeEvent(val slot: EquipmentSlot, val newItemStack: ItemStack?): SkyHanniEvent() {

    val isNecklace get() = slot == EquipmentSlot.NECKLACE
    val isCloak get() = slot == EquipmentSlot.CLOAK
    val isBelt get() = slot == EquipmentSlot.BELT
    val isGloves get() = slot == EquipmentSlot.GLOVES

}
