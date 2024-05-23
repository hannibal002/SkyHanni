package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TerminalWaypoints {
    private val config get() = SkyHanniMod.feature.dungeon
    @SubscribeEvent
    fun onWorld(event: LorenzRenderWorldEvent) {
        if(!isEnabled()) return

        TerminalInfo.entries.filter { it.highlight && DungeonAPI.dungeonPhase == it.phase}.forEach {
            event.drawWaypointFilled(it.location, LorenzColor.GREEN.toColor(), seeThroughBlocks = true)
            event.drawDynamicText(it.location, it.text, 1.0)
        }
    }
    @SubscribeEvent
    fun dungeonStart(event: DungeonStartEvent) {
        TerminalInfo.resetTerminals()
    }
    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        DungeonAPI.goldorTerminalPattern.matchMatcher(event.message){
            val playerName = group("playerName")
            val playerEntity = EntityUtils.getAllEntities().filter { it is EntityPlayer }.firstOrNull{ it.name == playerName } ?: return
            val terminal = TerminalInfo.getClosestTerminal(playerEntity.position.toLorenzVec())
            terminal?.highlight = false
        }
    }
    private fun isEnabled(): Boolean = DungeonAPI.inDungeon() && config.terminalWaypoints
}
