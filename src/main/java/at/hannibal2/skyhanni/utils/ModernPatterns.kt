package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * This file exists to load repo patterns that are used in modern features.
 * This way we don't have to have the feature files in the 1.8 folder which means faster builds for modern testing.
 */
@SkyHanniModule
object ModernPatterns {
    val beaconCurrentColorPattern by RepoPattern.pattern(
        "foraging.moonglade.beacon.color",
        "§7Current color: §a(?<color>.+)",
    )

    val beaconCurrentSpeedPattern by RepoPattern.pattern(
        "foraging.moonglade.beacon.speed",
        "§7Current speed: §a(?<speed>\\d)",
    )

    val beaconCurrentPitchPattern by RepoPattern.pattern(
        "foraging.moonglade.beacon.pitch",
        "§7Current pitch: §a(?<pitch>.+)",
    )

}
