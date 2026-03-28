package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonBossApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.world.phys.Vec3

private typealias BossPhase = DungeonBossApi.DungeonBossPhase

// TODO use repo (although these are unlikely to change)
enum class TerminalInfo(val location: Vec3, val phase: BossPhase, val text: String) {
    P1_TERMINAL_1(Vec3(111.0, 113.0, 73.0), BossPhase.F7_GOLDOR_1, "Terminal"),
    P1_TERMINAL_2(Vec3(111.0, 119.0, 79.0), BossPhase.F7_GOLDOR_1, "Terminal"),
    P1_TERMINAL_3(Vec3(89.0, 112.0, 92.0), BossPhase.F7_GOLDOR_1, "Terminal"),
    P1_TERMINAL_4(Vec3(89.0, 122.0, 101.0), BossPhase.F7_GOLDOR_1, "Terminal"),
    P1_LEVER_1(Vec3(106.0, 124.0, 113.0), BossPhase.F7_GOLDOR_1, "Lever"),
    P1_LEVER_2(Vec3(94.0, 124.0, 113.0), BossPhase.F7_GOLDOR_1, "Lever"),
    P1_DEVICE(Vec3(110.0, 119.0, 93.0), BossPhase.F7_GOLDOR_1, "Device"),

    P2_TERMINAL_1(Vec3(68.0, 109.0, 121.0), BossPhase.F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL_2(Vec3(59.0, 120.0, 122.0), BossPhase.F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL_3(Vec3(47.0, 109.0, 121.0), BossPhase.F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL_4(Vec3(40.0, 124.0, 122.0), BossPhase.F7_GOLDOR_2, "Terminal"),
    P2_TERMINAL_5(Vec3(39.0, 108.0, 143.0), BossPhase.F7_GOLDOR_2, "Terminal"),
    P2_LEVER_1(Vec3(23.0, 132.0, 138.0), BossPhase.F7_GOLDOR_2, "Lever"),
    P2_LEVER_2(Vec3(27.0, 124.0, 127.0), BossPhase.F7_GOLDOR_2, "Lever"),
    P2_DEVICE(Vec3(60.0, 131.0, 142.0), BossPhase.F7_GOLDOR_2, "Device"),

    P3_TERMINAL_1(Vec3(-3.0, 109.0, 112.0), BossPhase.F7_GOLDOR_3, "Terminal"),
    P3_TERMINAL_2(Vec3(-3.0, 119.0, 93.0), BossPhase.F7_GOLDOR_3, "Terminal"),
    P3_TERMINAL_3(Vec3(19.0, 123.0, 93.0), BossPhase.F7_GOLDOR_3, "Terminal"),
    P3_TERMINAL_4(Vec3(-3.0, 109.0, 77.0), BossPhase.F7_GOLDOR_3, "Terminal"),
    P3_LEVER_1(Vec3(14.0, 122.0, 55.0), BossPhase.F7_GOLDOR_3, "Lever"),
    P3_LEVER_2(Vec3(2.0, 122.0, 55.0), BossPhase.F7_GOLDOR_3, "Lever"),
    P3_DEVICE(Vec3(-2.0, 119.0, 77.0), BossPhase.F7_GOLDOR_3, "Device"),

    P4_TERMINAL_1(Vec3(41.0, 109.0, 29.0), BossPhase.F7_GOLDOR_4, "Terminal"),
    P4_TERMINAL_2(Vec3(44.0, 121.0, 29.0), BossPhase.F7_GOLDOR_4, "Terminal"),
    P4_TERMINAL_3(Vec3(67.0, 109.0, 29.0), BossPhase.F7_GOLDOR_4, "Terminal"),
    P4_TERMINAL_4(Vec3(72.0, 115.0, 48.0), BossPhase.F7_GOLDOR_4, "Terminal"),
    P4_LEVER_1(Vec3(84.0, 121.0, 34.0), BossPhase.F7_GOLDOR_4, "Lever"),
    P4_LEVER_2(Vec3(86.0, 128.0, 46.0), BossPhase.F7_GOLDOR_4, "Lever"),
    P4_DEVICE(Vec3(63.0, 126.0, 35.0), BossPhase.F7_GOLDOR_4, "Device"),
    ;

    var highlight: Boolean = true

    @SkyHanniModule
    companion object {

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("shresetterminal") {
                description = "Resets terminal highlights in F7."
                category = CommandCategory.USERS_RESET
                simpleCallback { TerminalInfo.resetTerminals() }
            }
        }

        fun resetTerminals() = entries.forEach { it.highlight = true }

        fun getClosestTerminal(input: Vec3): TerminalInfo? =
            entries.filter { it.highlight }.minByOrNull { it.location.distanceTo(input) }
    }
}
