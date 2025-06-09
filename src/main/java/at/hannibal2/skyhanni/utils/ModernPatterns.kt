package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * This file exists to load repo patterns that are used in modern features.
 * This way we don't have to have the feature files in the 1.8 folder which means faster builds for modern testing.
 */
@SkyHanniModule
object ModernPatterns {
    val currentColorPattern by RepoPattern.pattern(
        "foraging.moonglade.beacon.color",
        "Current color: (?<color>.+)",
    )

    val currentSpeedPattern by RepoPattern.pattern(
        "foraging.moonglade.beacon.speed",
        "Current speed: (?<speed>\\d)",
    )

    val currentPitchPattern by RepoPattern.pattern(
        "foraging.moonglade.beacon.pitch",
        "Current pitch: (?<pitch>.+)",
    )

}
