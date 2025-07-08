package at.hannibal2.skyhanni.utils

import net.minecraft.util.EnumParticleTypes
//#if MC > 1.21
//$$ import net.minecraft.particle.ParticleType
//$$ import net.minecraft.registry.Registries
//$$ import net.minecraft.util.Identifier
//#endif

object ParticleUtils {

    //#if TODO
    fun getParticleTypeByName(name: String): EnumParticleTypes? =
        EnumParticleTypes.entries.firstOrNull {
            it.name.equals(name, ignoreCase = true)
        }
    //#else
    //$$ fun getParticleTypeByName(name: String, shouldError: Boolean = false): ParticleType<*>? = Identifier.of(name.lowercase())?.let { id ->
    //$$    Registries.PARTICLE_TYPE.get(id) ?: run {
    //$$        if (shouldError) ChatUtils.userError("Unknown particle type: '$name'")
    //$$        null
    //$$    }
    //$$ }
    //#endif

}
