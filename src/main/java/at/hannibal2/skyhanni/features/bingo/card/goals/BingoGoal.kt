package at.hannibal2.skyhanni.features.bingo.card.goals

class BingoGoal(
    val displayName: String,
    val description: String,
    val type: GoalType,
    val slot: Int,
    var done: Boolean,
    val hiddenGoalData: HiddenGoalData,
) {
    override fun toString(): String = displayName
}
