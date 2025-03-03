package at.hannibal2.skyhanni.features.combat.end

enum class DragonType(
    val displayName: String,
    val colorCode: String,
    val selectable: Boolean = true,
) {
    PROTECTOR(
        "Protector Dragon",
        "§7",
    ),
    OLD(
        "Old Dragon",
        "§e",
    ),
    UNSTABLE(
        "Unstable Dragon",
        "§5",
    ),
    YOUNG(
        "Young Dragon",
        "§f",
    ),
    STRONG(
        "Strong Dragon",
        "§c",
    ),
    WISE(
        "Wise Dragon",
        "§b",
    ),
    SUPERIOR(
        "Superior Dragon",
        "§6",
    ),
    UNKNOWN(
        "",
        "",
        false,
    ),
}
