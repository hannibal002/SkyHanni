package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.dungeon.DungeonBossApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.client.player.RemotePlayer

@SkyHanniModule
object TerminalWaypoints {

    private val config get() = SkyHanniMod.feature.dungeon.terminalWaypoints

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        for (term in TerminalInfo.entries) {
            if (!term.phase.isCurrent() && !term.shouldShowActiveWaypoint()) continue
            if (term.unsolved) event.drawWaypointFilled(term.location, config.inactiveTerminalColor.toColor(), seeThroughBlocks = true)
            else if (!config.removeActiveTerminals)
                event.drawWaypointFilled(term.location, config.activeTerminalColor.toColor(), seeThroughBlocks = true)
            event.drawDynamicText(term.location, term.text, 1.0)
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        TerminalInfo.resetTerminals()
    }

    // Only calls getEntities when terminals get completed, so the performance impact is minimal
    @OptIn(AllEntitiesGetter::class)
    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!inBoss()) return

        val playerName = DungeonBossApi.goldorTerminalPattern.matchMatcher(event.cleanMessage) {
            group("playerName")
        } ?: return

        val playerEntity = EntityUtils.getEntities<RemotePlayer>().find { it.name.string == playerName } ?: return
        val terminal = TerminalInfo.getClosestTerminal(playerEntity.getLorenzVec())
        terminal?.unsolved = false
    }

    private fun TerminalInfo.shouldShowActiveWaypoint() = config.removeActiveTerminals && !this.unsolved

    private fun inBoss() = DungeonApi.inBossRoom && DungeonApi.isOneOf("F7", "M7")

    private fun isEnabled() = inBoss() && config.enabled
}
