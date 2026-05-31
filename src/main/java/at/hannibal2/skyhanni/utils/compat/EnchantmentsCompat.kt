package at.hannibal2.skyhanni.utils.compat

import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.enchantment.Enchantment

/**
 * This is a compatibility layer that helps with multiple minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
enum class EnchantmentsCompat(
    val enchantment: Holder<Enchantment>,
) {
    PROTECTION(
        MinecraftCompat.localWorld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .get(Identifier.withDefaultNamespace("protection")).get(),
    ),
}
