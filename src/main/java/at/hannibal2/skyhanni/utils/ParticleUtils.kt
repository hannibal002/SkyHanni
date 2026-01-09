package at.hannibal2.skyhanni.utils

import net.minecraft.resources.ResourceLocation

object ParticleUtils {

    fun getParticleTypeByName(name: String, shouldError: Boolean = false): ResourceLocation? = ResourceLocation.parse(name.lowercase())

}
