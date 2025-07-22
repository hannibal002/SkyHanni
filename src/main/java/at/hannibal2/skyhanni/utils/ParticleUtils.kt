package at.hannibal2.skyhanni.utils

import net.minecraft.util.EnumParticleTypes
//#if MC > 1.21
//$$ import net.minecraft.util.Identifier
//#endif

object ParticleUtils {

    //#if MC < 1.21
    fun getParticleTypeByName(name: String): EnumParticleTypes? =
        EnumParticleTypes.entries.firstOrNull {
            it.name.equals(name, ignoreCase = true)
        }
    //#else
    //$$ fun getParticleTypeByName(name: String, shouldError: Boolean = false): Identifier? = Identifier.of(name.lowercase())
    //#endif

}
