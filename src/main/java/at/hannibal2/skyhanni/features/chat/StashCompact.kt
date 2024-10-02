package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object StashCompact {

    //<editor-fold desc="Patterns">
    private val patternGroup = RepoPattern.group("stash.compact")

    /**
     * REGEX-TEST: §f                 §7You have §3226 §7materials stashed away!
     */
    private val materialCountPattern by patternGroup.pattern(
        "material.count",
        "§f *§7You have §3(?<count>[\\d,]) §7materials stashed away!.*",
    )

    /**
     * REGEX-TEST: §f               §8(This totals 1 type of material stashed!)
     */
    private val differingMaterialsCountPattern by patternGroup.pattern(
        "differing.materials.count",
        "§f *§8(This totals (?<count>[\\d,]) type of material stashed!).*",
    )

    /**
     * REGEX-TEST: §f                §3§l>>> §3§lCLICK HERE§b to pick them up! §3§l<<<
     */
    private val pickupStashPattern by patternGroup.pattern(
        "pickup.stash",
        "§f *§3§l>>> §3§lCLICK HERE§b to pick them up! §3§l<<<.*",
    )
    //</editor-fold>

    private val config get() = SkyHanniMod.feature.chat

    private var lastMaterialCount = 0
    private var lastDifferingMaterialsCount = 0

    private var lastSentMaterialCount = 0
    private var lastSentDifferingMaterialsCount = 0

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.compactStashWarnings) return
        val message = event.message
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
            event.blockedReason = "stash_compact"
        }

        if(pickupStashPattern.matches(message)) {
            event.blockedReason = "stash_compact"
            if (lastMaterialCount != lastSentMaterialCount || lastDifferingMaterialsCount != lastSentDifferingMaterialsCount) {
                sendCompactedStashMessage()
                lastSentMaterialCount = lastMaterialCount
                lastSentDifferingMaterialsCount = lastDifferingMaterialsCount
            }
        }
    }

    private fun sendCompactedStashMessage() {
        ChatUtils.clickableChat(
            "§7You have §3${lastMaterialCount} §7materials in stash, §8totalling $lastDifferingMaterialsCount types. §3Click to pickup§7.",
            onClick = {
                HypixelCommands.pickupStash()
            }
        )
    }
}
