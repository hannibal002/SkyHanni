package at.hannibal2.hanni.features.dungeon.floor7

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.dungeon.DungeonApi
import at.hannibal2.hanni.features.dungeon.DungeonBossApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.entity.player.EntityPlayerMP

@HanniModule
object TerminalWaypoints {

    private val config get() = HanniMod.feature.dungeon

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return

        for (term in TerminalInfo.entries) {
            if (!term.highlight || !term.phase.isCurrent()) continue
            event.drawWaypointFilled(term.location, LorenzColor.GREEN.toColor(), seeThroughBlocks = true)
            event.drawDynamicText(term.location, term.text, 1.0)
        }
    }

    @HandleEvent
    fun onWorldChange() {
        TerminalInfo.resetTerminals()
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!inBoss()) return

        val playerName = DungeonBossApi.goldorTerminalPattern.matchMatcher(event.message) {
            group("playerName")
        } ?: return

        val playerEntity = EntityUtils.getEntities<EntityPlayerMP>().find { it.name == playerName } ?: return
        val terminal = TerminalInfo.getClosestTerminal(playerEntity.getLorenzVec())
        terminal?.highlight = false
    }

    private fun inBoss() = DungeonApi.inBossRoom && DungeonApi.isOneOf("F7", "M7")

    private fun isEnabled() = inBoss() && config.terminalWaypoints
}
