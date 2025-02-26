package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class DragonType(
    val displayName: String,
) {
    PROTECTOR("Protector Dragon"),
    Old("Old Dragon"),
    Unstable("Unstable Dragon"),
    Young("Young Dragon"),
    Strong("Strong Dragon"),
    Wise("Wise Dragon"),
    Superior("Superior Dragon"),
    // For use in the Pest Profit Tracker, in cases where an item cannot have an identified PestType
    // Display name intentionally omitted to aid in filtering out this entry.
    UNKNOWN(
        "",
    ),
    ;
}
