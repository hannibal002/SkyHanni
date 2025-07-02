package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.misc.HideArmor
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity

fun shouldHideArmor(): Boolean = HideArmor.getCurrentEntity()?.let {
    it is PlayerEntity && HideArmor.shouldHideArmor(it)
} ?: false

fun shouldHideHead(slot: EquipmentSlot) = shouldHideArmor() && !(HideArmor.config.onlyHelmet && slot != EquipmentSlot.HEAD)

