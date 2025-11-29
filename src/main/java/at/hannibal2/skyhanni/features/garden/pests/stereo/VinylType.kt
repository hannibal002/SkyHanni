package at.hannibal2.skyhanni.features.garden.pests.stereo

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class VinylType(val displayName: String, internalNameOverride: String? = null) {
    PRETTY_FLY("Pretty Fly"),
    CRICKET_CHOIR("Cricket Choir"),
    CICADA_SYMPHONY("Cicada Symphony"),
    RODENT_REVOLUTION("Rodent Revolution"),
    BUZZIN_BEATS("Buzzin' Beats"),
    EARTHWORM_ENSEMBLE("Earthworm Ensemble"),
    DYNAMITES("DynaMITES"),
    WINGS_OF_HARMONY("Wings of Harmony"),
    SLOW_AND_GROOVY("Slow and Groovy"),
    NOT_JUST_A_PEST("Not Just a Pest", "VINYL_BEETLE"),
    NONE("None"),
    ;

    val internalName: NeuInternalName =
        (internalNameOverride ?: "VINYL_$name").toInternalName()

    companion object {
        fun getByName(name: String): VinylType =
            VinylType.entries.find { it.displayName == name } ?: error("Unknown vinyl: '$name'")

        fun getByInternalNameOrNull(internalName: NeuInternalName): VinylType? =
            VinylType.entries.find { it.internalName == internalName }
    }
}
