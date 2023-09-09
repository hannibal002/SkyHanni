package at.hannibal2.skyhanni.features.garden.visitor

import java.util.regex.Pattern

enum class VisitorReward(val displayName: String, val internalName: String, val pattern: Pattern) {
    FLOWERING_BOUQUET("§9Flowering Bouquet", "FLOWERING_BOUQUET", "Flowering Bouquet".toPattern()),
    OVERGROWN_GRASS("§9Overgrown Grass", "OVERGROWN_GRASS", "Overgrown Grass".toPattern()),
    GREEN_BANDANA("§9Green Bandana", "GREEN_BANDANA", "Green Bandana".toPattern()),
    DEDICATION("§9Dedication IV", "DEDICATION;4", "Dedication (IV|4) Book".toPattern()),
    MUSIC_RUNE("§9Music Rune", "MUSIC_RUNE;1", "◆ Music Rune [1I]".toPattern()),
    SPACE_HELMET("§cSpace Helmet", "DCTR_SPACE_HELM", "Space Helmet".toPattern()),
    CULTIVATING(
        "§9Cultivating I", "CULTIVATING;1",
        "Cultivating ([I1]) Book".toPattern()
    ),
    REPLENISH("§9Replenish I", "REPLENISH;1", "Replenish ([I1]) Book".toPattern()),
}