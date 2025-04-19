package at.hannibal2.skyhanni.utils.compat

import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
//#if MC > 1.12
//$$ import net.minecraft.init.MobEffects
//#endif
//#if MC > 1.21
//$$ import net.minecraft.registry.entry.RegistryEntry
//#endif

enum class EffectsCompat(
    //#if MC < 1.21
    val potion: Potion,
    //#else
    //$$ val potion: RegistryEntry<StatusEffect>,
    //#endif
) {
    INVISIBILITY(
        //#if MC < 1.12
        Potion.invisibility
        //#else
        //$$ MobEffects.INVISIBILITY
        //#endif
    ),
    BLINDNESS(
        //#if MC < 1.12
        Potion.blindness
        //#else
        //$$ MobEffects.BLINDNESS
        //#endif
    ),
    ;

    companion object {
        fun EntityLivingBase.hasPotionEffect(effect: EffectsCompat): Boolean {
            return this.isPotionActive(effect.potion)
        }

        fun EntityLivingBase.activePotionEffect(effect: EffectsCompat): PotionEffect? {
            return this.getActivePotionEffect(effect.potion)
        }
    }
}
