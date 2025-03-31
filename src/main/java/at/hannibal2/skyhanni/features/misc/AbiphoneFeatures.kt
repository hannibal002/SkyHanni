package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.isValidUuid
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AbiphoneFeatures {

    val config get() = SkyHanniMod.feature.misc
    val patternGroup = RepoPattern.group("misc.abiphone")
    private var acceptUUID: String? = null

    /**
     * REGEX-TEST: §a✆ RING... §r §r§2§l[PICK UP]
     * REGEX-TEST: §a✆ RING... RING... §r §r§2§l[PICK UP]
     * REGEX-TEST: §a✆ RING... RING... RING... §r §r§2§l[PICK UP]
     * REGEX-TEST: §a✆ RING... RING... RING...
     */
    private val callRingPattern by patternGroup.pattern(
        "call.ring",
        "§a✆ (?:RING\\.{3} ?){1,3}(?:§r §r§2§l\\[PICK UP])?",
    )

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onChat(event: SkyHanniChatEvent) {
        if (callRingPattern.matches(event.message) && acceptUUID == null) readPickupUuid(event)
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        acceptUUID = null
    }

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        if (InventoryUtils.inInventory()) return
        if (config.abiphoneAcceptKey == Keyboard.KEY_NONE || config.abiphoneAcceptKey != event.keyCode) return
        val acceptUUID = acceptUUID ?: return
        HypixelCommands.callback(acceptUUID)
        AbiphoneFeatures.acceptUUID = null
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(76, "event.hoppityEggs.hoppityCallWarning.acceptHotkey", "misc.abiphoneAcceptKey")
    }

    private fun readPickupUuid(event: SkyHanniChatEvent) {
        val siblings = event.chatComponent.siblings.takeIf { it.size >= 3 } ?: return
        val clickEvent = siblings[2]?.chatStyle?.chatClickEvent ?: return
        if (clickEvent.action.name.lowercase() != "run_command" || !clickEvent.value.lowercase().startsWith("/cb")) return
        acceptUUID = clickEvent.value.lowercase().replace("/cb ", "").takeIf { it.isValidUuid() }
        if (acceptUUID != null) DelayedRun.runDelayed(20.seconds) { acceptUUID = null }
    }
}
