package at.hannibal2.skyhanni.config.features.gui.customscoreboard

enum class NumberDisplayFormat(val displayName: String) {
    TEXT_COLOR_NUMBER("§fPurse: §6123"),
    COLOR_TEXT_NUMBER("§6Purse: 123"),
    COLOR_NUMBER_TEXT("§6123 Purse"),
    COLOR_NUMBER_RESET_TEXT("§6123 §fPurse"),
    ;

    override fun toString() = displayName
}
