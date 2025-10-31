package at.hannibal2.hanni.features.misc.massconfiguration

enum class ResetSuggestionState(val label: String) {
    TURN_ALL_OFF("§c§lTurn all off"),
    TURN_ALL_ON("§a§lTurn all on"),
    LEAVE_DEFAULTS("§b§lLeave unchanged"),
    ;

    val next get() = entries[(ordinal + 1) % entries.size]
}
