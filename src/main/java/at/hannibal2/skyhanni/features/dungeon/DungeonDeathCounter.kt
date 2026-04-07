package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonStartEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object DungeonDeathCounter {
    private val config get() = SkyHanniMod.feature.dungeon
    private val patternGroup = RepoPattern.group("dungeon.deathcounter")

    /**
     * REGEX-TEST:  ☠ Someone disconnected from the Dungeon and became a ghost.
     * REGEX-TEST:  ☠ You were killed by Someone and became a ghost.
     * REGEX-TEST:  ☠ You were crushed and became a ghost.
     * REGEX-TEST:  ☠ You suffocated and became a ghost.
     * REGEX-TEST:  ☠ You fell into a deep hole and became a ghost.
     * REGEX-TEST:  ☠ You died to a trap and became a ghost.
     * REGEX-TEST:  ☠ You died to a mob and became a ghost.
     * REGEX-TEST:  ☠ You died and became a ghost.
     * REGEX-TEST:  ☠ You burnt to death and became a ghost.
     * REGEX-TEST:  ☠ Someone was killed by Someone and became a ghost.
     * REGEX-TEST:  ☠ Someone was crushed and became a ghost.
     * REGEX-TEST:  ☠ Someone suffocated and became a ghost.
     * REGEX-TEST:  ☠ Someone fell to their death with help from Someone and became a ghost.
     * REGEX-TEST:  ☠ Someone fell into a deep hole and became a ghost.
     * REGEX-TEST:  ☠ Someone died to a trap and became a ghost.
     * REGEX-TEST:  ☠ Someone died to a mob and became a ghost.
     * REGEX-TEST:  ☠ Someone died and became a ghost.
     * REGEX-TEST:  ☠ Someone burnt to death and became a ghost.
     */
    private val deathPattern by patternGroup.pattern(
        "death.message",
        " ☠ .+(?:(?:crush|di(?:sconnect)?|kill|suffocat)ed|fell|burnt).+became a ghost\\."
    )

    private var display: Renderable? = null
    private var deaths = 0

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.deathCounterDisplay || !deathPattern.matches(event.chatComponent)) return
        deaths++
        ChatUtils.chat("§c§l$deaths. DEATH!", false)
        update()
    }

    private fun clear() {
        deaths = 0
        update()
    }

    private fun update() {
        if (deaths == 0) {
            display = null
            return
        }

        val color = when (deaths) {
            1, 2 -> "§e"
            3 -> "§c"
            else -> "§4"
        }
        display = Renderable.text(color + "Deaths: $deaths")
    }

    @HandleEvent
    fun onDungeonStart(event: DungeonStartEvent) = clear()

    @HandleEvent
    fun onWorldChange() = clear()

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.deathCounterDisplay) return
        val display = display ?: return
        config.deathCounterPos.renderRenderable(display, posLabel = "Dungeon Death Counter")
    }
}
