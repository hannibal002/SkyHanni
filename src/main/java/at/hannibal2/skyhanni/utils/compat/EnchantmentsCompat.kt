package at.hannibal2.skyhanni.utils.compat

import net.minecraft.world.item.enchantment.Enchantment
//#if MC > 1.21
import net.minecraft.core.registries.Registries
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
//#endif

enum class EnchantmentsCompat(
    //#if MC < 1.21
    //$$ val enchantment: Enchantment
    //#else
    val enchantment: Holder<Enchantment>
    //#endif
) {
    PROTECTION(
        //#if MC < 1.16
        //$$ Enchantment.protection
        //#else
        MinecraftCompat.localWorld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceLocation.withDefaultNamespace("protection")).get()
        //#endif
    ),
}
