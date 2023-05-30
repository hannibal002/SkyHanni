package at.hannibal2.skyhanni.features.garden.visitor

import java.util.regex.Pattern

enum class VisitorReward(val displayName: String, val internalName: String, val pattern: Pattern) {
    FLOWERING_BOUQUET("§9Flowering Bouquet", "FLOWERING_BOUQUET", "[+]1x Flowering Bouquet".toPattern()),
    OVERGROWN_GRASS("§9Overgrown Grass", "OVERGROWN_GRASS", "[+]1x Overgrown Grass".toPattern()),
    GREEN_BANDANA("§9Green Bandana", "GREEN_BANDANA", "[+]1x Green Bandana".toPattern()),
    DEDICATION("§9Dedication IV", "DEDICATION;4", "Dedication (IV|4) Book".toPattern()),
    MUSIC_RUNE("§9Music Rune", "MUSIC_RUNE;1", "[+]1x ◆ Music Rune [1I]".toPattern()),
    SPACE_HELMET("§cSpace Helmet", "DCTR_SPACE_HELM", "[+]1x Space Helmet".toPattern()),
    // Pretty sure that the symbol is ◆ but not 100%
}