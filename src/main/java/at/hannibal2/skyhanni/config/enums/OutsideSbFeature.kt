package at.hannibal2.skyhanni.config.enums

enum class OutsideSbFeature(private val displayName: String) {
    MODIFY_VISUAL_WORDS("Modify Visual Words"),
    CUSTOM_TEXT_BOX("Custom Text Box"),
    REAL_TIME("Real Time"),
    TPS_DISPLAY("TPS Display"),
    MARKED_PLAYERS("Marked Players"),
    FARMING_WEIGHT("Farming Weight"),
    NEXT_JACOB_CONTEXT("Next Jacobs's Context"),
    COMPOSTER_TIME("Composter Time"),
    YAW_AND_PITCH("Yaw and Pitch"),
    QUICK_MOD_MENU_SWITCH("Quick Mod Menu Switch"),
    FOLLOWING_LINE("Following Line"),
    ARROW_TRAIL("Arrow Trail"),
    HIGHLIGHT_PARTY_MEMBERS("Highlight Party Members"),
    MOVEMENT_SPEED("Movement Speed");

    override fun toString() = displayName
}
