package at.hannibal2.hanni.features.skillprogress

data class SkillLevel(
    val level: Int,
    val xpCurrent: Long,
    val xpForNext: Long,
    val overflowXP: Long,
)
