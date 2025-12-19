package at.hannibal2.skyhanni.utils.compat

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
//#if MC > 1.16
import net.minecraft.world.effect.MobEffects
//#endif
//#if MC > 1.21
import net.minecraft.core.Holder
//#endif

enum class EffectsCompat(
    //#if MC < 1.21
    //$$ val potion: StatusEffect,
    //#else
    val potion: Holder<MobEffect>,
    //#endif
) {
    INVISIBILITY(
        //#if MC < 1.16
        //$$ Potion.invisibility
        //#else
        MobEffects.INVISIBILITY
        //#endif
    ),
    BLINDNESS(
        //#if MC < 1.16
        //$$ Potion.blindness
        //#else
        MobEffects.BLINDNESS
        //#endif
    ),
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
