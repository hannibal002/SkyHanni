package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.chat.ChatConfig
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object StashCompact {

    //<editor-fold desc="Patterns">
    private val patternGroup = RepoPattern.group("stash.compact")

    /**
     * REGEX-TEST: §f                 §7You have §3226 §7materials stashed away!
     * REGEX-TEST: §f                 §7You have §31,000 §7items stashed away!
     */
    private val materialCountPattern by patternGroup.pattern(
        "material.count",
        "§f *§7You have §3(?<count>[\\d,]+) (§.)+(?<type>item|material)s? stashed away!.*",
    )

    /**
     * REGEX-TEST: §f               §8(This totals 1 type of material stashed!)
     * REGEX-TEST: §f               §8(This totals 2 types of items stashed!)
     */
    private val differingMaterialsCountPattern by patternGroup.pattern(
        "differing.materials.count",
        "§f *§8(This totals (?<count>[\\d,]+) type of (§.)+(?<type>item|material)s? stashed!).*",
    )

    /**
     * REGEX-TEST: §f                §3§l>>> §3§lCLICK HERE§b to pick them up! §3§l<<<
     */
    private val pickupStashPattern by patternGroup.pattern(
        "pickup.stash",
        "§f *§3§l>>> §3§lCLICK HERE§b to pick (?:them|it) up! §3§l<<<.*",
    )

    /**
     * REGEX-TEST: §eOne or more items didn't fit in your inventory and were added to your item stash! §6Click here to pick them up!
     * REGEX-TEST: §eOne or more materials didn't fit in your inventory and were added to your material stash! §6Click here to pick them up!
     */
    @Suppress("MaxLineLength")
    private val genericAddedToStashPattern by patternGroup.pattern(
        "generic",
        "§eOne or more (?:item|material)s? didn't fit in your inventory and were added to your (?:item|material) stash! §6Click here §eto pick them up!",
    )

    //</editor-fold>

    private val config get() = SkyHanniMod.feature.chat

    private var lastMaterialCount = 0
    private var lastDifferingMaterialsCount = 0
    private var lastType = ""

    private var lastSentMaterialCount = 0
    private var lastSentDifferingMaterialsCount = 0
    private var lastSentType = ""

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        genericAddedToStashPattern.matchMatcher(event.message) {
            event.blockedReason = "stash_compact"
        }

        materialCountPattern.matchMatcher(event.message) {
            groupOrNull("count")?.replace(",", "")?.toIntOrNull()?.let { count ->
                lastMaterialCount = count
            }
            event.blockedReason = "stash_compact"
        }

        differingMaterialsCountPattern.matchMatcher(event.message) {
            groupOrNull("count")?.replace(",", "")?.toIntOrNull()?.let { count ->
                lastDifferingMaterialsCount = count
            }
            groupOrNull("type")?.let { type ->
                lastType = type
            }
            event.blockedReason = "stash_compact"
        }

        if (pickupStashPattern.matches(event.message)) {
            event.blockedReason = "stash_compact"
            if (lastMaterialCount != lastSentMaterialCount || lastDifferingMaterialsCount != lastSentDifferingMaterialsCount) {
                sendCompactedStashMessage()
                lastSentMaterialCount = lastMaterialCount
                lastSentDifferingMaterialsCount = lastDifferingMaterialsCount
                lastSentType = lastType
            }
        }
    }

    private fun sendCompactedStashMessage() {
        if (config.stashWarnings == ChatConfig.StashHandlerType.HIDE) return
        ChatUtils.clickableChat(
            "§7You have §3${lastMaterialCount} §7${StringUtils.pluralize(lastMaterialCount, lastType)} in stash, " +
                "§8totalling $lastDifferingMaterialsCount ${StringUtils.pluralize(lastDifferingMaterialsCount, "type")}. " +
                "§3Click to pickup§7.",
            onClick = {
                HypixelCommands.pickupStash()
            },
            prefix = false,
        )
    }

    private fun isEnabled() = config.stashWarnings != ChatConfig.StashHandlerType.NONE
}
