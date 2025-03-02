package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class DragonType(
    val displayName: String,
) {
    PROTECTOR("Protector Dragon"),
    OLD("Old Dragon"),
    UNSTABLE("Unstable Dragon"),
    YOUNG("Young Dragon"),
    STRONG("Strong Dragon"),
    WISE("Wise Dragon"),
    SUPERIOR("Superior Dragon"),
    UNKNOWN(
        "",
    ),
    ;
}
