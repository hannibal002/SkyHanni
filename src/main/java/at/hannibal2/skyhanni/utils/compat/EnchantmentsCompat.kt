package at.hannibal2.skyhanni.utils.compat

import net.minecraft.enchantment.Enchantment
//#if MC > 1.21
//$$ import net.minecraft.registry.RegistryKeys
//$$ import net.minecraft.registry.entry.RegistryEntry
//$$ import net.minecraft.util.Identifier
//#endif

enum class EnchantmentsCompat(
    //#if MC < 1.21
    val enchantment: Enchantment
    //#else
    //$$ val enchantment: RegistryEntry<Enchantment>
    //#endif
) {
    PROTECTION(
        //#if MC < 1.16
        Enchantment.protection
        //#else
        //$$ MinecraftCompat.localWorld.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(Identifier.ofVanilla("protection")).get()
        //#endif
    ),
}
