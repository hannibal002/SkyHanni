package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.chat.StashCompact.StashType.Companion.fromGroup
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.message
import at.hannibal2.skyhanni.utils.ChatUtils.passedSinceSent
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object StashCompact {
    private const val REASON = "stash_compact"

    // <editor-fold desc="Patterns">
    private val patternGroup = RepoPattern.group("stash.compact")

    /**
     * REGEX-TEST: §f                 §7You have §3226 §7materials stashed away!
     * REGEX-TEST: §f                 §7You have §322 §7materials stashed away!
     * REGEX-TEST: §f                 §7You have §a1,000 §7items stashed away!
     * REGEX-TEST: §f                     §7You have §a2 §7items stashed away!
     * REGEX-TEST: §f                   §7You have §a109 §7items stashed away!
     */
    private val materialCountPattern by patternGroup.pattern(
        "material.count",
        "§f *§7You have §.(?<count>[\\d,]+) (?:§.)+(?<type>item|material)s? stashed away!.*",
    )

    /**
     * REGEX-TEST: §f               §8(This totals 1 type of material stashed!)
     * REGEX-TEST: §f               §8(This totals 2 types of items stashed!)
     * REGEX-TEST: §f               §8(This totals 3 types of materials stashed!)
     * REGEX-TEST: §f               §8(This totals 4 types of items stashed!)
     * REGEX-TEST: §f              §8(This totals 8 types of materials stashed!)
     */
    private val differingMaterialsCountPattern by patternGroup.pattern(
        "differing.materials.count",
        "§f *§8\\(This totals (?<count>[\\d,]+) types? of (?<type>item|material)s? stashed!\\).*",
    )

    /**
     * REGEX-TEST: §f                §3§l>>> §3§lCLICK HERE§b to pick them up! §3§l<<<
     * REGEX-TEST: §f                §6§l>>> §6§lCLICK HERE§e to pick them up! §6§l<<<
     * REGEX-TEST: §f                §3§l>>> §3§lCLICK HERE§b to pick them up! §3§l<<<
     */
    private val pickupStashPattern by patternGroup.pattern(
        "pickup.stash",
        "§f *§.§l>>> §.§lCLICK HERE§. to pick (?:them|it) up! §.§l<<<.*",
    )

    /**
     * REGEX-TEST: §eOne or more items didn't fit in your inventory and were added to your item stash! §6Click here §eto pick them up!
     * REGEX-TEST: §eOne or more materials didn't fit in your inventory and were added to your material stash! §6Click here §eto pick them up!
     */
    @Suppress("MaxLineLength")
    private val genericAddedToStashPattern by patternGroup.pattern(
        "generic",
        "§eOne or more (?:item|material)s? didn't fit in your inventory and were added to your (?:item|material) stash! §6Click here §eto pick them up!",
    )
    // </editor-fold>

    private val config get() = SkyHanniMod.feature.chat.filterType.stashMessages
    private val filterConfig get() = SkyHanniMod.feature.chat.filterType

    private var currentType: StashType? = null
    private val currentMessages: MutableMap<StashType, StashMessage?> = mutableMapOf()
    private val lastMessages: MutableMap<StashType, StashMessage?> = mutableMapOf()
    private var joinedProfileAt: SimpleTimeMark? = null

    enum class StashType(val displayName: String, val colorCodePair: Pair<String, String>) {
        ITEM("item", Pair("§e", "§6")),
        MATERIAL("material", Pair("§b", "§3")),
        ;

        companion object {
            fun Matcher.fromGroup() = StashType.fromStringOrNull(group("type"))
            private fun fromStringOrNull(string: String) = entries.find { it.displayName == string }
        }
    }

    data class StashMessage(val materialCount: Int, val type: String) {
        var differingMaterialsCount: Int? = null
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        joinedProfileAt = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return

        // TODO make a system for detecting message "groups" (multiple consecutive messages)
        materialCountPattern.matchMatcher(event.message) {
            currentType = fromGroup() ?: return@matchMatcher
            val currentType = currentType ?: return@matchMatcher
            currentMessages[currentType] = StashMessage(group("count").formatInt(), group("type"))
            event.blockedReason = REASON
            ChatUtils.deleteMessage(REASON, 2) {
                StringUtils.isEmpty(it.message) && it.passedSinceSent() < 500.milliseconds
            }
        }

        differingMaterialsCountPattern.matchMatcher(event.message) {
            currentType = fromGroup() ?: return@matchMatcher
            currentMessages[currentType]?.differingMaterialsCount = group("count").formatInt()
            event.blockedReason = REASON
        }

        pickupStashPattern.matchMatcher(event.message) {
            event.blockedReason = REASON
            ChatUtils.deleteNextMessage(REASON) { StringUtils.isEmpty(it) }
            val currentType = currentType ?: return@matchMatcher

            val currentMessage = currentMessages[currentType] ?: return@matchMatcher
            if (currentMessage.materialCount <= config.hideLowWarningsThreshold) return@matchMatcher
            lastMessages[currentType]?.let { lastMessage ->
                if (config.hideDuplicateCounts && lastMessage == currentMessage) return@matchMatcher
            }

            currentMessage.sendCompactedStashMessage()
        }

        if (!config.hideAddedMessages) return
        genericAddedToStashPattern.matchMatcher(event.message) {
            event.blockedReason = REASON
            ChatUtils.deleteNextMessage(REASON) { StringUtils.isEmpty(it) }
        }
    }

    private fun StashMessage.sendCompactedStashMessage() {
        val currentType = currentType ?: return

        val typeNameFormat = StringUtils.pluralize(materialCount, currentType.displayName)
        val (mainColor, accentColor) = currentType.colorCodePair

        val typeStringExtra = differingMaterialsCount?.let {
            ", ${mainColor}totalling $accentColor$it ${StringUtils.pluralize(it, "type")}$mainColor"
        }.orEmpty()
        val action = if (config.useViewStash) "view" else "pickup"

        ChatUtils.clickableChat(
            "${mainColor}You have $accentColor${materialCount.shortFormat()} $mainColor$typeNameFormat in stash$typeStringExtra. " +
                "${mainColor}Click to $accentColor$action ${mainColor}your stash!",
            onClick = {
                if (config.useViewStash) HypixelCommands.viewStash(type)
                else HypixelCommands.pickupStash()
            },
            hover = "§eClick to $action your $type stash!",
        )

        currentMessages.replace(currentType, null)
        lastMessages[currentType] = this
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
