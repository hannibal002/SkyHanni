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
     * REGEX-TEST: §a§lFIG TREE §r§b§l88%
     * REGEX-TEST: §2§lMANGROVE TREE §r§b§l5%
     */
    val currentTreeProgressPattern by RepoPattern.pattern(
        "foraging.tree.progress",
        "(?<treeType>§.§l\\w+) TREE §r§b§l(?<percent>\\d+)%",
    )

    /**
     * REGEX-TEST: §cNope the Fish
     * REGEX-TEST: §cCluck the Fish
     * REGEX-TEST: §cHerring the Fish
     */
    val coralFishNamePattern by RepoPattern.pattern(
        "misc.coral.fish.name",
        "§c(?<fishName>\\w+ the Fish)",
    )

    /**
     * REGEX-TEST: Fish Shown: 1/31
     * REGEX-TEST: Fish Shown: 15/31
     */
    val coralFishFoundPattern by RepoPattern.pattern(
        "misc.coral.fish.shown",
        "Fish Shown: (?<found>\\d+)/(?<total>\\d+)",
    )
}
