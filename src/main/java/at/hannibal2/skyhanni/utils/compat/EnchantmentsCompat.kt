package at.hannibal2.skyhanni.utils.compat

import net.minecraft.enchantment.Enchantment
//#if MC >= 1.12
//$$ import net.minecraft.init.Enchantments
//#endif
//#if MC > 1.21
//$$ import net.minecraft.registry.RegistryKey
//#endif

enum class EnchantmentsCompat(
    //#if MC < 1.21
    val enchantment: Enchantment
    //#else
    //$$ val enchantment: RegistryKey<Enchantment>
    //#endif
) {
    PROTECTION(
        //#if MC < 1.12
        Enchantment.protection
        //#else
        //$$ Enchantments.PROTECTION
        //#endif
    ),
}
