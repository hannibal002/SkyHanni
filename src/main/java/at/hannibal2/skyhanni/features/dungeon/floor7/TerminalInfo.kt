package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.utils.LorenzVec

enum class TerminalInfo(val location: LorenzVec, val phase: Int, val text: String, var highlight: Boolean = true) {
    P1_TERMINAL1(LorenzVec(111, 113, 73), 1, "Terminal"),
    P1_TERMINAL2(LorenzVec(111, 119, 79), 1, "Terminal"),
    P1_TERMINAL3(LorenzVec(89, 112, 92), 1, "Terminal"),
    P1_TERMINAL4(LorenzVec(89, 122, 101), 1, "Terminal"),
    P1_LEVER1(LorenzVec(106, 124, 113), 1, "Lever"),
    P1_LEVER2(LorenzVec(94, 124, 113), 1, "Lever"),
    P1_DEVICE(LorenzVec(110, 119, 93), 1, "Device"),

    P2_TERMINAL1(LorenzVec(68, 109, 121), 2, "Terminal"),
    P2_TERMINAL2(LorenzVec(59, 120, 122), 2, "Terminal"),
    P2_TERMINAL3(LorenzVec(47, 109, 121), 2, "Terminal"),
    P2_TERMINAL4(LorenzVec(40, 124, 122), 2, "Terminal"),
    P2_TERMINAL5(LorenzVec(39, 108, 143), 2, "Terminal"),
    P2_LEVER1(LorenzVec(23, 132, 138), 2, "Lever"),
    P2_LEVER2(LorenzVec(27, 124, 127), 2, "Lever"),
    P2_DEVICE(LorenzVec(60, 131, 142), 2, "Device"),

    P3_TERMINAL1(LorenzVec(-3, 109, 112), 3, "Terminal"),
    P3_TERMINAL2(LorenzVec(-3, 119, 93), 3, "Terminal"),
    P3_TERMINAL3(LorenzVec(19, 123, 93), 3, "Terminal"),
    P3_TERMINAL4(LorenzVec(-3, 109, 77), 3, "Terminal"),
    P3_LEVER1(LorenzVec(14, 122, 55), 3, "Lever"),
    P3_LEVER2(LorenzVec(2, 122, 55), 3, "Lever"),
    P3_DEVICE(LorenzVec(-2, 119, 77), 3, "Device"),

    P4_TERMINAL1(LorenzVec(41, 109, 29), 4, "Terminal"),
    P4_TERMINAL2(LorenzVec(44, 121, 29), 4, "Terminal"),
    P4_TERMINAL3(LorenzVec(67, 109, 29), 4, "Terminal"),
    P4_TERMINAL4(LorenzVec(72, 115, 48), 4, "Terminal"),
    P4_LEVER1(LorenzVec(84, 121, 34), 4, "Lever"),
    P4_LEVER2(LorenzVec(86, 128, 46), 4, "Lever"),
    P4_DEVICE(LorenzVec(63, 126, 35), 4, "Device"),
    ;


    companion object {
        fun resetTerminals() = entries.forEach { it.highlight = true }

        fun getClosestTerminal(input:LorenzVec): TerminalInfo? {
            return entries.minByOrNull { it.location.distance(input) }
        }
    }
}
