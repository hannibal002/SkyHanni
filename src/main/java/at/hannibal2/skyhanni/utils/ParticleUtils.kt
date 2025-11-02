package at.hannibal2.skyhanni.utils

import net.minecraft.particle.ParticleTypes
//#if MC > 1.21
import net.minecraft.util.Identifier
//#endif

object ParticleUtils {

    //#if MC < 1.21
    //$$ fun getParticleTypeByName(name: String): ParticleTypes? =
    //$$     ParticleTypes.entries.firstOrNull {
    //$$         it.name.equals(name, ignoreCase = true)
    //$$     }
    //#else
    fun getParticleTypeByName(name: String, shouldError: Boolean = false): Identifier? = Identifier.of(name.lowercase())
    //#endif

}
