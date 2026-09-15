package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonBossApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec

enum class TerminalInfo(val location: LorenzVec, val phase: DungeonBossApi.DungeonBossPhase, val text: String) {
    P1_TERMINAL_1(LorenzVec(111, 113, 73), F7_GOLDOR_1, "Terminal"),
    P1_TERMINAL_2(LorenzVec(111, 119, 79), F7_GOLDOR_1, "Terminal"),
    P1_TERMINAL_3(LorenzVec(89, 112, 92), F7_GOLDOR_1, "Terminal"),
    P1_TERMINAL_4(LorenzVec(89, 122, 101), F7_GOLDOR_1, "Terminal"),
    P1_LEVER_1(LorenzVec(106, 124, 113), F7_GOLDOR_1, "Lever"),
    P1_LEVER_2(LorenzVec(94, 124, 113), F7_GOLDOR_1, "Lever"),
    P1_DEVICE(LorenzVec(110, 119, 93), F7_GOLDOR_1, "Device"),

    P2_TERMINAL_1(LorenzVec(68, 109, 121), F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL_2(LorenzVec(59, 120, 122), F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL_3(LorenzVec(47, 109, 121), F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL_4(LorenzVec(40, 124, 122), F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL_5(LorenzVec(39, 108, 143), F7_GOLDOR_2, "Terminal"),
    P2_LEVER_1(LorenzVec(23, 132, 138), F7_GOLDOR_2, "Lever"),
    P2_LEVER_2(LorenzVec(27, 124, 127), F7_GOLDOR_2, "Lever"),
    P2_DEVICE(LorenzVec(60, 131, 142), F7_GOLDOR_2, "Device"),

    P3_TERMINAL_1(LorenzVec(-3, 109, 112), F7_GOLDOR_3, "Terminal"),
    P3_TERMINAL_2(LorenzVec(-3, 119, 93), F7_GOLDOR_3, "Terminal"),
    P3_TERMINAL_3(LorenzVec(19, 123, 93), F7_GOLDOR_3, "Terminal"),
    P3_TERMINAL_4(LorenzVec(-3, 109, 77), F7_GOLDOR_3, "Terminal"),
    P3_LEVER_1(LorenzVec(14, 122, 55), F7_GOLDOR_3, "Lever"),
    P3_LEVER_2(LorenzVec(2, 122, 55), F7_GOLDOR_3, "Lever"),
    P3_DEVICE(LorenzVec(-2, 119, 77), F7_GOLDOR_3, "Device"),

    P4_TERMINAL_1(LorenzVec(41, 109, 29), F7_GOLDOR_4, "Terminal"),
    P4_TERMINAL_2(LorenzVec(44, 121, 29), F7_GOLDOR_4, "Terminal"),
    P4_TERMINAL_3(LorenzVec(67, 109, 29), F7_GOLDOR_4, "Terminal"),
    P4_TERMINAL_4(LorenzVec(72, 115, 48), F7_GOLDOR_4, "Terminal"),
    P4_LEVER_1(LorenzVec(84, 121, 34), F7_GOLDOR_4, "Lever"),
    P4_LEVER_2(LorenzVec(86, 128, 46), F7_GOLDOR_4, "Lever"),
    P4_DEVICE(LorenzVec(63, 126, 35), F7_GOLDOR_4, "Device"),
    ;

    var unsolved: Boolean = true

    @SkyHanniModule
    companion object {

        @HandleEvent
        private fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shresetterminal") {
                description = "Resets terminal highlights in F7."
                category = CommandCategory.USERS_RESET
                simpleCallback { TerminalInfo.resetTerminals() }
            }
        }

        fun resetTerminals() = entries.forEach { it.unsolved = true }

        fun getClosestTerminal(input: LorenzVec): TerminalInfo? {
            return entries.filter { it.unsolved }.minByOrNull { it.location.distance(input) }
        }
    }
}
