package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.jsonobjects.repo.neu.AbiphoneContactInfo
import at.hannibal2.hanni.data.jsonobjects.repo.neu.NeuAbiphoneJson
import at.hannibal2.hanni.events.NeuRepositoryReloadEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.chat.TabCompletionEvent
import at.hannibal2.hanni.events.minecraft.KeyPressEvent
import at.hannibal2.hanni.events.minecraft.WorldChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.StringUtils.isValidUuid
import at.hannibal2.hanni.utils.StringUtils.removeAllNonLettersAndNumbers
import at.hannibal2.hanni.utils.compat.value
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.seconds

@HanniModule
object AbiphoneFeatures {

    val config get() = HanniMod.feature.misc
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
    fun onChat(event: HanniChatEvent) {
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

    private var abiphoneContacts: Set<String>? = null

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val constant = event.getConstant<Map<String, AbiphoneContactInfo>>("abiphone", NeuAbiphoneJson.TYPE)
        abiphoneContacts = constant.flatMap { (key, value) ->
            value.callNames ?: listOf(key.removeAllNonLettersAndNumbers().replace(" ", ""))
        }.toSet()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabCompletion(event: TabCompletionEvent) {
        if (!config.commands.tabComplete.call) return
        if (event.command != "call") return
        abiphoneContacts?.let { event.addSuggestions(it) }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(76, "event.hoppityEggs.hoppityCallWarning.acceptHotkey", "misc.abiphoneAcceptKey")
    }

    private fun readPickupUuid(event: HanniChatEvent) {
        val siblings = event.chatComponent.siblings.takeIf { it.size >= 3 } ?: return
        val clickEvent = siblings[2]?.chatStyle?.chatClickEvent ?: return
        if (clickEvent.action.name.lowercase() != "run_command" || !clickEvent.value().lowercase().startsWith("/cb")) return
        acceptUUID = clickEvent.value().lowercase().replace("/cb ", "").takeIf { it.isValidUuid() }
        if (acceptUUID != null) DelayedRun.runDelayed(20.seconds) { acceptUUID = null }
    }
}
