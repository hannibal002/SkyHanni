package at.hannibal2.skyhanni.utils.compat

import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.enchantment.Enchantment

enum class EnchantmentsCompat(
    val enchantment: Holder<Enchantment>,
) {
    PROTECTION(
        MinecraftCompat.localWorld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .get(ResourceLocation.withDefaultNamespace("protection")).get(),
    ),
}
