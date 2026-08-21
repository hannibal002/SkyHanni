package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.misc.HideArmor
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player

object HideArmorHook {
    @JvmStatic
    fun shouldHideArmor(): Boolean = getEntity()?.let {
        it is Player && HideArmor.shouldHideArmor(it)
    } ?: false

    @JvmStatic
    fun shouldHideSlot(slot: EquipmentSlot) = shouldHideArmor() && !(HideArmor.config.onlyHelmet && slot != EquipmentSlot.HEAD)
}
