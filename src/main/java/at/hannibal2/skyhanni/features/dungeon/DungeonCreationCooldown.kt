package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DungeonCreationCooldown {
    private val config get() = SkyHanniMod.feature.dungeon.creationCooldown

    /**
     * REGEX-TEST: §r§b[MVP§r§5+§r§b] Chissl§r§f §r§eentered §r§aThe Catacombs§r§e, §r§eFloor I§r§e!
     * REGEX-TEST: §r§b[MVP§r§5+§r§b] Chissl§r§f §r§eentered §r§c§lMM§r§c The Catacombs§r§e, §r§eFloor I§r§e!
     */
    private val join by RepoPattern.group("dungeon.join").pattern(
        "dungeon.join",
        ".*\n§r(?<player>.*)§r§f §r§eentered §r(?:.*)?The Catacombs§r§e,.*\n.*",
    )
    private const val ENTRANCE_ID = "102,66"

    private var cooldown = SimpleTimeMark.farPast()
    private var hasWarned = false


    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        join.matchMatcher(event.message) {
            val player = group("player").cleanPlayerName()
            if (player == PlayerUtils.getName()) {
                cooldown = SimpleTimeMark.now() + 30.seconds
            }
            hasWarned = false
        }
    }

    @HandleEvent
    fun onCommand(event: MessageSendToServerEvent) {
        if (!shouldBlock()) return
        if (event.message.startsWith("/joininstance")) {
            event.cancel()
            ChatUtils.chat(
                "Blocked instance creation due to cooldown! Cooldown: §b${cooldown.timeUntil().format()}",
                replaceSameMessage = true,
            )
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!cooldown.isInFuture()) return
        val display = Renderable.text("§eDungeon Creation Cooldown: §b${cooldown.timeUntil().format()}")
        config.position.renderRenderable(display, posLabel = "Dungeon Cooldown Timer")
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (hasWarned || !config.sendChatMessage) return
        if (cooldown.isInPast()) {
            ChatUtils.chat("Dungeon creation cooldown is over!")
            hasWarned = true
        }
    }

    private fun isEntrance(roomID: String?): Boolean {
        // roomID only shows once dungeon starts, so assume that a null value means dungeon hasn't started and player is in entrance
        if (roomID == null) return true
        return roomID == ENTRANCE_ID
    }

    private fun isEnabled(): Boolean {
        if (!config.enabled) return false
        return if (DungeonApi.inDungeon()) {
            if (config.entranceOnly) isEntrance(DungeonApi.roomId) else true
        } else {
            SkyBlockUtils.inSkyBlock && config.showOutside
        }
    }

    private fun shouldBlock(): Boolean {
        return cooldown.isInFuture() && config.blockInstanceCreation
    }
}
