package at.hannibal2.skyhanni.utils

import net.minecraft.resources.Identifier

object ParticleUtils {

    fun getParticleTypeByName(name: String, shouldError: Boolean = false): Identifier? = Identifier.parse(name.lowercase())

}
