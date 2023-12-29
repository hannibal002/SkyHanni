package at.hannibal2.skyhanni.data

import kotlin.math.ceil
import kotlin.math.pow

enum class hotmData(val guiName: String, val maxLevel: Int, val costFun: ((Int) -> (Double?)), val rewardFun: ((Int) -> (Pair<Double, Double?>))) {
    miningSpeed("", 50, { currentLevel -> (currentLevel + 2.0).pow(3) }, { level -> level * 20.0 to null }),
    miningFortune("", 50, { currentLevel -> (currentLevel + 1.0).pow(3.5) }, { level -> level * 5.0 to null }),
    quickForge("", 20, { currentLevel -> (currentLevel + 2.0).pow(4) }, { level -> 10.0 + (level * 0.5) to null }),
    titaniumInsanium("", 50, { currentLevel -> (currentLevel + 2.0).pow(3.1) }, { level -> 2.0 + (level * 0.1) to null }),
    dailyPowder("", 100, { currentLevel -> 200.0 + (currentLevel * 18.0) }, { level -> (200.0 + ((level - 1.0) * 18.0)) * 2.0 to null }),
    luckOfTheCave("", 45, { currentLevel -> (currentLevel + 2.0).pow(3.07) }, { level -> 5.0 + level to null }),
    crystallized("", 30, { currentLevel -> (currentLevel + 2.0).pow(3.4) }, { level -> 20.0 + ((level - 1.0) * 6.0) to 20.0 + ((level - 1.0) * 5.0) }),
    efficientMiner("", 100, { currentLevel -> (currentLevel + 2.0).pow(2.6) }, { level -> 10.0 + (level * 0.4) to 1.0 + (level * 0.05) }),
    orbiter("", 80, { currentLevel -> (currentLevel + 1.0) * 70.0 }, { level -> 0.2 + (level * 0.01) to null }),
    seasonedMineman("", 100, { currentLevel -> (currentLevel + 2.0).pow(2.3) }, { level -> 5.0 + (level * 0.1) to null }),
    mole("", 190, { currentLevel -> (currentLevel + 2.0).pow(2.2) }, { level -> 1.0 + ((level + 9.0) * 0.05 * ((level + 8) % 20)) to null }),
    professional("", 140, { currentLevel -> (currentLevel + 2.0).pow(2.3) }, { level -> 50.0 + (level * 5.0) to null }),
    lonesomeMiner("", 45, { currentLevel -> (currentLevel + 2.0).pow(3.07) }, { level -> 5.0 + ((level - 1.0) * 0.5) to null }),
    greatExplorer("", 20, { currentLevel -> (currentLevel + 2.0).pow(4.0) }, { level -> (0.2 * (0.2 + 0.04 * (level - 1.0))) to 1 + level * 0.2 }),
    fortunate("", 20, { currentLevel -> (currentLevel + 1.0).pow(3.05) }, { level -> 20.0 + (level * 4.0) to null }),
    powderBuff("", 50, { currentLevel -> (currentLevel + 1.0).pow(3.2) }, { level -> level.toDouble() to null }),
    miningSpeedII("", 50, { currentLevel -> (currentLevel + 2.0).pow(3.2) }, { level -> level * 40.0 to null }),
    miningFortuneII("", 50, { currentLevel -> (currentLevel + 2.0).pow(3.2) }, { level -> level * 5.0 to null }),

    // Static
    miningMadness("", 1, { null }, { 50.0 to 50.0 }),
    skyMall("", 1, { null }, { 0.0 to null }),
    precisionMining("", 1, { null }, { 30.0 to null }),
    frontLoaded("", 1, { null }, { 100.0 to 2.0 }),
    starPowder("", 1, { null }, { 3.0 to null }),
    goblinKiller("", 1, { null }, { 0.0 to null }),

    // Abilities
    pickobulus("", 3, { null }, { level -> ceil(level * 0.5) + 1.0 to 130.0 - 10.0 * level }),
    miningSpeedBoost("", 3, { null }, { level -> level + 1.0 to 10.0 + 5.0 * level }),
    veinSeeker("", 3, { null }, { level -> level + 1.0 to 10.0 + 2.0 * level }),
    maniacMiner("", 3, { null }, { level -> 5.0 + level * 5.0 to 60.0 - level }),

}
