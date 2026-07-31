package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.data.BestiaryApi
import com.google.gson.annotations.Expose

data class MarkedMob(
    @Expose
    val family: String,
    @Expose
    val name: String,
    @Expose
    val level: Int,
) {

    fun matches(other: MarkedMob): Boolean {
        if (family != other.family) return false
        if (name != other.name) return false
        if (level != other.level) return false
        return true
    }

    fun matchesVariant(
        family: String,
        variant: BestiaryApi.BestiaryMobVariant,
    ): Boolean {
        if (this.family != family) return false
        if (this.name != variant.cleanName) return false
        if (this.level != variant.level) return false
        return true
    }

    fun matches(
        name: String,
        level: Int,
    ): Boolean {
        if (this.name != name) return false
        if (this.level != level) return false
        return true
    }

    companion object {
        fun BestiaryApi.BestiaryMobVariant.toMarkedVariant(
            family: String,
        ): MarkedMob {
            return MarkedMob(
                family = family,
                name = cleanName,
                level = level,
            )
        }
    }
}
