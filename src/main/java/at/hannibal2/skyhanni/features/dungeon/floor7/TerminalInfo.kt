package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.LorenzVec

enum class TerminalInfo(val location: LorenzVec, val phase: DungeonAPI.DungeonPhase, val text: String, var highlight: Boolean = true) {
    P1_TERMINAL1(LorenzVec(111, 113, 73), DungeonAPI.DungeonPhase.F7_GOLDOR_1, "Terminal"),
    P1_TERMINAL2(LorenzVec(111, 119, 79), DungeonAPI.DungeonPhase.F7_GOLDOR_1, "Terminal"),
    P1_TERMINAL3(LorenzVec(89, 112, 92), DungeonAPI.DungeonPhase.F7_GOLDOR_1, "Terminal"),
    P1_TERMINAL4(LorenzVec(89, 122, 101), DungeonAPI.DungeonPhase.F7_GOLDOR_1, "Terminal"),
    P1_LEVER1(LorenzVec(106, 124, 113), DungeonAPI.DungeonPhase.F7_GOLDOR_1, "Lever"),
    P1_LEVER2(LorenzVec(94, 124, 113), DungeonAPI.DungeonPhase.F7_GOLDOR_1, "Lever"),
    P1_DEVICE(LorenzVec(110, 119, 93), DungeonAPI.DungeonPhase.F7_GOLDOR_1, "Device"),

    P2_TERMINAL1(LorenzVec(68, 109, 121), DungeonAPI.DungeonPhase.F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL2(LorenzVec(59, 120, 122), DungeonAPI.DungeonPhase.F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL3(LorenzVec(47, 109, 121), DungeonAPI.DungeonPhase.F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL4(LorenzVec(40, 124, 122), DungeonAPI.DungeonPhase.F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL5(LorenzVec(39, 108, 143), DungeonAPI.DungeonPhase.F7_GOLDOR_2, "Terminal"),
    P2_LEVER1(LorenzVec(23, 132, 138), DungeonAPI.DungeonPhase.F7_GOLDOR_2, "Lever"),
    P2_LEVER2(LorenzVec(27, 124, 127), DungeonAPI.DungeonPhase.F7_GOLDOR_2, "Lever"),
    P2_DEVICE(LorenzVec(60, 131, 142), DungeonAPI.DungeonPhase.F7_GOLDOR_2, "Device"),

    P3_TERMINAL1(LorenzVec(-3, 109, 112), DungeonAPI.DungeonPhase.F7_GOLDOR_3, "Terminal"),
    P3_TERMINAL2(LorenzVec(-3, 119, 93), DungeonAPI.DungeonPhase.F7_GOLDOR_3, "Terminal"),
    P3_TERMINAL3(LorenzVec(19, 123, 93), DungeonAPI.DungeonPhase.F7_GOLDOR_3, "Terminal"),
    P3_TERMINAL4(LorenzVec(-3, 109, 77), DungeonAPI.DungeonPhase.F7_GOLDOR_3, "Terminal"),
    P3_LEVER1(LorenzVec(14, 122, 55), DungeonAPI.DungeonPhase.F7_GOLDOR_3, "Lever"),
    P3_LEVER2(LorenzVec(2, 122, 55), DungeonAPI.DungeonPhase.F7_GOLDOR_3, "Lever"),
    P3_DEVICE(LorenzVec(-2, 119, 77), DungeonAPI.DungeonPhase.F7_GOLDOR_3, "Device"),

    P4_TERMINAL1(LorenzVec(41, 109, 29), DungeonAPI.DungeonPhase.F7_GOLDOR_4, "Terminal"),
    P4_TERMINAL2(LorenzVec(44, 121, 29), DungeonAPI.DungeonPhase.F7_GOLDOR_4, "Terminal"),
    P4_TERMINAL3(LorenzVec(67, 109, 29), DungeonAPI.DungeonPhase.F7_GOLDOR_4, "Terminal"),
    P4_TERMINAL4(LorenzVec(72, 115, 48), DungeonAPI.DungeonPhase.F7_GOLDOR_4, "Terminal"),
    P4_LEVER1(LorenzVec(84, 121, 34), DungeonAPI.DungeonPhase.F7_GOLDOR_4, "Lever"),
    P4_LEVER2(LorenzVec(86, 128, 46), DungeonAPI.DungeonPhase.F7_GOLDOR_4, "Lever"),
    P4_DEVICE(LorenzVec(63, 126, 35), DungeonAPI.DungeonPhase.F7_GOLDOR_4, "Device"),
    ;


    companion object {
        fun resetTerminals() = entries.forEach { it.highlight = true }

        fun getClosestTerminal(input:LorenzVec): TerminalInfo? {
            return entries.filter{ it.highlight }.minByOrNull { it.location.distance(input) }
        }
    }
}
