package at.hannibal2.skyhanni.features.garden.pests.stereo

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class VinylType(val displayName: String) {
    PRETTY_FLY("Pretty Fly"),
    CRICKET_CHOIR("Cricket Choir"),
    CICADA_SYMPHONY("Cicada Symphony"),
    RODENT_REVOLUTION("Rodent Revolution"),
    BUZZIN_BEATS("Buzzin' Beats"),
    EARTHWORM_ENSEMBLE("Earthworm Ensemble"),
    DYNAMITES("DynaMITES"),
    WINGS_OF_HARMONY("Wings of Harmony"),
    SLOW_AND_GROOVY("Slow and Groovy"),
    NOT_JUST_A_PEST("Not Just a Pest"),
    NONE("Nothing")
    ;

    companion object {
        private val BEETLE_VINYL = "VINYL_BEETLE".toInternalName()

        fun getByName(name: String) = VinylType.entries.firstOrNull { it.displayName == name } ?: NONE
        fun getByInternalNameOrNull(internalName: NeuInternalName) = when (internalName) {
            BEETLE_VINYL -> NOT_JUST_A_PEST
            else -> VinylType.entries.firstOrNull {
                internalName.asString() == "VINYL_${it.name}"
            }
        }
    }
}
