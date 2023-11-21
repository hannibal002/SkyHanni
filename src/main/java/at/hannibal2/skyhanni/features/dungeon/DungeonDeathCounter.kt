package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonDeathCounter {

    private var display = ""
    private var deaths = 0

    private fun isDeathMessage(message: String): Boolean = when {
        message.matchRegex("§c ☠ §r§7You were killed by (.*)§r§7 and became a ghost§r§7.") -> true
        message.matchRegex("§c ☠ §r§7(.*) was killed by (.*) and became a ghost§r§7.") -> true

        message.matchRegex("§c ☠ §r§7You were crushed and became a ghost§r§7.") -> true
        message.matchRegex("§c ☠ §r§7§r(.*)§r§7 was crushed and became a ghost§r§7.") -> true

        message.matchRegex("§c ☠ §r§7You died to a trap and became a ghost§r§7.") -> true
        message.matchRegex("§c ☠ §r(.*)§r§7 died to a trap and became a ghost§r§7.") -> true

        message.matchRegex("§c ☠ §r§7You burnt to death and became a ghost§r§7.") -> true
        message.matchRegex("§c ☠ §r(.*)§r§7 burnt to death and became a ghost§r§7.") -> true

        message.matchRegex("§c ☠ §r§7You died and became a ghost§r§7.") -> true
        message.matchRegex("§c ☠ §r(.*)§r§7 died and became a ghost§r§7.") -> true

        message.matchRegex("§c ☠ §r§7You suffocated and became a ghost§r§7.") -> true
        message.matchRegex("§c ☠ §r§7§r(.*)§r§7 suffocated and became a ghost§r§7.") -> true

        message.matchRegex("§c ☠ §r§7You died to a mob and became a ghost§r§7.") -> true
        message.matchRegex("§c ☠ §r(.*)§7 died to a mob and became a ghost§r§7.") -> true

        message.matchRegex("§c ☠ §r§7You fell into a deep hole and became a ghost§r§7.") -> true
        message.matchRegex("§c ☠ §r(.*)§r§7 fell into a deep hole and became a ghost§r§7.") -> true

        message.matchRegex("§c ☠ §r§(.*)§r§7 disconnected from the Dungeon and became a ghost§r§7.") -> true

        message.matchRegex("§c ☠ §r§7(.*)§r§7 fell to their death with help from §r(.*)§r§7 and became a ghost§r§7.") -> true

        else -> false
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatPacket(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (isDeathMessage(event.message)) {
            deaths++
            LorenzUtils.chat("§c§l$deaths. DEATH!", false)
            update()
        }
    }

    private fun update() {
        if (deaths == 0) {
            display = ""
            return
        }

        val color = when (deaths) {
            1, 2 -> "§e"
            3 -> "§c"
            else -> "§4"
        }
        display = color + "Deaths: $deaths"
    }

    @SubscribeEvent
    fun onDungeonStart(event: DungeonStartEvent) {
        deaths = 0
        update()
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        deaths = 0
        update()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        SkyHanniMod.feature.dungeon.deathCounterPos.renderString(DungeonMilestonesDisplay.color + display, posLabel = "Dungeon Death Counter")
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inDungeons && SkyHanniMod.feature.dungeon.deathCounterDisplay
    }
}
