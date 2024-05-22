package at.hannibal2.skyhanni.features.dungeon

enum class DungeonPhase {
    F6_TERRACOTTA,
    F6_GIANTS,
    F6_SADAN,
    F7_MAXOR,
    F7_STORM,
    F7_GOLDOR_1,
    F7_GOLDOR_2,
    F7_GOLDOR_3,
    F7_GOLDOR_4,
    F7_GOLDOR_5,
    F7_NECRON,
    M7_WITHER_KING
}

// class DungeonPhaseHandler {
//
//     private val patternGroup = RepoPattern.group("dungeon.phases")
//
//     private val
//
//     @SubscribeEvent
//     fun onChat(event: LorenzChatEvent) {
//         if (!DungeonAPI.inDungeon()) return
//         when (DungeonAPI.dungeonFloor) { //move to enum
//             "F6", "M6" -> {
//
//             }
//             "F7", "M7" -> {
//
//             }
//         }
//     }
// }
