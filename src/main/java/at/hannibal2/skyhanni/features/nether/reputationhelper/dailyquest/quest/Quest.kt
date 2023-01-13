package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

abstract class Quest(val category: QuestCategory, val internalName: String, var state: QuestState, val displayName: String = internalName) {
}