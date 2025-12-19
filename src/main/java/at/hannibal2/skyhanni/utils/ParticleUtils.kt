package at.hannibal2.skyhanni.utils

import net.minecraft.core.particles.ParticleTypes
//#if MC > 1.21
import net.minecraft.resources.ResourceLocation
//#endif

object ParticleUtils {

    //#if MC < 1.21
    //$$ fun getParticleTypeByName(name: String): ParticleTypes? =
    //$$     ParticleTypes.entries.firstOrNull {
    //$$         it.name.equals(name, ignoreCase = true)
    //$$     }
    //#else
    fun getParticleTypeByName(name: String, shouldError: Boolean = false): ResourceLocation? = ResourceLocation.parse(name.lowercase())
    //#endif

}
