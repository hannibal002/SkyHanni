package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import net.minecraft.item.ItemStack

object PestAPI {
    val config get() = GardenAPI.config.pests

    val vacuumVariants = listOf(
        "SKYMART_VACUUM".asInternalName(),
        "SKYMART_TURBO_VACUUM".asInternalName(),
        "SKYMART_HYPER_VACUUM".asInternalName(),
        "INFINI_VACUUM".asInternalName(),
        "INFINI_VACUUM_HOOVERIUS".asInternalName(),
    )

    fun hasVacuumInHand() = InventoryUtils.itemInHandId in vacuumVariants

    fun SprayType.getPests() = PestType.entries.filter { it.spray == this }

    fun isVacuum(item: ItemStack): Boolean {
        return item.getInternalName() in vacuumVariants
    }
}
