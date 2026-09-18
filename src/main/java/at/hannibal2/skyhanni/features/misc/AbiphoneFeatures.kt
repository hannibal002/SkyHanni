package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.AbiphoneContactInfo
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.isValidUuid
import at.hannibal2.skyhanni.utils.StringUtils.removeAllNonLettersAndNumbers
import at.hannibal2.skyhanni.utils.compat.value
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AbiphoneFeatures {

    val config get() = SkyHanniMod.feature.misc
    val patternGroup = RepoPattern.group("misc.abiphone")
    private var acceptUUID: String? = null

    /**
     * REGEX-TEST: ✆ RING...  [PICK UP]
     * REGEX-TEST: ✆ RING... RING...  [PICK UP]
     * REGEX-TEST: ✆ RING... RING... RING...  [PICK UP]
     * REGEX-TEST: ✆ RING... RING... RING...
     */
    private val callRingPattern by patternGroup.pattern(
        "call.ring.colorless",
        "✆ (?:RING\\.{3} ?){1,3}(?: \\[PICK UP])?",
    )

    @HandleEvent(priority = HandleEvent.HIGHEST)
    private fun onChat(event: SystemMessageEvent.Allow) {
        if (callRingPattern.matches(event.cleanMessage) && acceptUUID == null) readPickupUuid(event)
    }

    @HandleEvent
    private fun onWorldChange() {
        acceptUUID = null
    }

    @HandleEvent
    private fun onKeyPress(event: KeyPressEvent) {
        if (InventoryUtils.inInventory()) return
        if (config.abiphoneAcceptKey == GLFW.GLFW_KEY_UNKNOWN || config.abiphoneAcceptKey != event.keyCode) return
        val acceptUUID = acceptUUID ?: return
        HypixelCommands.callback(acceptUUID)
        AbiphoneFeatures.acceptUUID = null
    }

    private var abiphoneContacts: Set<String>? = null

    @HandleEvent
    private fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val constant = event.getConstant<Map<String, AbiphoneContactInfo>>("abiphone")
        abiphoneContacts = constant.flatMap { (key, value) ->
            value.callNames ?: listOf(key.removeAllNonLettersAndNumbers().replace(" ", ""))
        }.toSet()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onTabCompletion(event: TabCompletionEvent) {
        if (!config.commands.tabComplete.call) return
        if (event.command != "call") return
        abiphoneContacts?.let { event.addSuggestions(it) }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(76, "event.hoppityEggs.hoppityCallWarning.acceptHotkey", "misc.abiphoneAcceptKey")
    }

    private fun readPickupUuid(event: SystemMessageEvent.Allow) {
        val siblings = event.chatComponent.siblings.takeIf { it.size >= 3 } ?: return
        val clickEvent = siblings[2].style.clickEvent ?: return
        if (clickEvent.action().name.lowercase() != "run_command" || !clickEvent.value().lowercase().startsWith("/cb")) return
        acceptUUID = clickEvent.value().lowercase().replace("/cb ", "").takeIf { it.isValidUuid() }
        if (acceptUUID != null) DelayedRun.runDelayed(20.seconds) { acceptUUID = null }
    }
}
