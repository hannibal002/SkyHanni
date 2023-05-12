package at.hannibal2.skyhanni.features.garden.visitor

import java.util.regex.Pattern

enum class VisitorReward(val displayName: String, val internalName: String, val pattern: Pattern) {
    GREEN_BANDANA("§9Green Bandana", "GREEN_BANDANA", "[+]1x Green Bandana".toPattern()),
    OVERGROWN_GRASS("§9Overgrown Grass", "OVERGROWN_GRASS", "[+]1x Overgrown Grass".toPattern()),
    FLOWERING_BOUQUET("§9Flowering Bouquet", "FLOWERING_BOUQUET", "[+]1x Flowering Bouquet".toPattern()),
    DEDICATION("§9Dedication IV", "DEDICATION;4", "Dedication (IV|4) Book".toPattern()),
    SPACE_HELMET("§cSpace Helmet", "DCTR_SPACE_HELM", "[+]Space Helmet".toPattern()),
    // Pretty sure that the symbol is ◆ but not 100%
    MUSIC_RUNE("§9Music Rune", "MUSIC_RUNE;1", "[+]1x ◆ Music Rune [1I]".toPattern()),
}