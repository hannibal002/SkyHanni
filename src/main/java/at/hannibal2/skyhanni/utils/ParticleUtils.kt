package at.hannibal2.skyhanni.utils

import net.minecraft.util.EnumParticleTypes

object ParticleUtils {

    //#if TODO
    fun getParticleTypeByName(name: String): EnumParticleTypes? =
        EnumParticleTypes.entries.firstOrNull {
            it.name.equals(name, ignoreCase = true)
        }
    //#else

    //#endif

}
