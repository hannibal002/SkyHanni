package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * This file exists to load repo patterns that are used in modern features.
 * This way we don't have to have the feature files in the 1.8 folder which means faster builds for modern testing.
 */
@SkyHanniModule
object ModernPatterns {

    /**
     * @regexTest §7Current color: §a§4Red
     */
    val beaconCurrentColorPattern by RepoPattern.pattern(
        "foraging.moonglade.beacon.color",
        "§7Current color: §a(?<color>.+)",
    )

    /**
     * @regexTest §7Current speed: §a3
     */
    val beaconCurrentSpeedPattern by RepoPattern.pattern(
        "foraging.moonglade.beacon.speed",
        "§7Current speed: §a(?<speed>\\d)",
    )

    /**
     * @regexTest §7Current pitch: §aLow
     */
    val beaconCurrentPitchPattern by RepoPattern.pattern(
        "foraging.moonglade.beacon.pitch",
        "§7Current pitch: §a(?<pitch>.+)",
    )

    /**
     * @regexTest §a§lFIG TREE §r§b§l88%
     * @regexTest §2§lMANGROVE TREE §r§b§l5%
     */
    val currentTreeProgressPattern by RepoPattern.pattern(
        "foraging.tree.progress",
        "(?<treeType>§.§l\\w+) TREE §r§b§l(?<percent>\\d+)%",
    )

    /**
     * @regexTest §cNope the Fish
     * @regexTest §cCluck the Fish
     * @regexTest §cHerring the Fish
     */
    val coralFishNamePattern by RepoPattern.pattern(
        "misc.coral.fish.name",
        "§c(?<fishName>\\w+ the Fish)",
    )

    /**
     * @regexTest Fish Shown: 1/31
     * @regexTest Fish Shown: 15/31
     */
    val coralFishFoundPattern by RepoPattern.pattern(
        "misc.coral.fish.shown",
        "Fish Shown: (?<found>\\d+)/(?<total>\\d+)",
    )
}
