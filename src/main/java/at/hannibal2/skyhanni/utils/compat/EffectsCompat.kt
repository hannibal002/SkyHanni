package at.hannibal2.skyhanni.utils.compat

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
//#if MC > 1.16
import net.minecraft.entity.effect.StatusEffects
//#endif
//#if MC > 1.21
import net.minecraft.registry.entry.RegistryEntry
//#endif

enum class EffectsCompat(
    //#if MC < 1.21
    //$$ val potion: StatusEffect,
    //#else
    val potion: RegistryEntry<StatusEffect>,
    //#endif
) {
    INVISIBILITY(
        //#if MC < 1.16
        //$$ Potion.invisibility
        //#else
        StatusEffects.INVISIBILITY
        //#endif
    ),
    BLINDNESS(
        //#if MC < 1.16
        //$$ Potion.blindness
        //#else
        StatusEffects.BLINDNESS
        //#endif
    ),
    ;

    companion object {
        fun LivingEntity.hasPotionEffect(effect: EffectsCompat): Boolean {
            return this.hasStatusEffect(effect.potion)
        }

        fun LivingEntity.activePotionEffect(effect: EffectsCompat): StatusEffectInstance? {
            return this.getStatusEffect(effect.potion)
        }
    }
}
