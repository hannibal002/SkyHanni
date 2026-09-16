package at.hannibal2.skyhanni.utils.compat

import net.minecraft.core.Holder
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity

/**
 * This is a compatibility layer that helps with multiple minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
enum class EffectsCompat(
    val potion: Holder<MobEffect>,
) {
    INVISIBILITY(MobEffects.INVISIBILITY),
    BLINDNESS(MobEffects.BLINDNESS),
    ;

    companion object {
        fun LivingEntity.hasPotionEffect(effect: EffectsCompat): Boolean {
            return this.hasEffect(effect.potion)
        }

        fun LivingEntity.activePotionEffect(effect: EffectsCompat): MobEffectInstance? {
            return this.getEffect(effect.potion)
        }
    }
}
