package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.inventory.UltraRareBookAlert.enchantsFound
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ExperimentsDryStreakDisplay {

    private val config get() = SkyHanniMod.feature.inventory.helper.enchanting.dryStreakConfig
    private val storage get() = ProfileStorageData.profileSpecific?.dryStreakStorage

    private var display = emptyList<String>()

    private var inExperiment = false

    private val patternGroup = RepoPattern.group("enchanting.experiments.drystreak")
    private val superpairsPattern by patternGroup.pattern(
        "superpairs",
        "Superpairs \\((?<experiment>.+)\\)")
    private val experimentInventoriesPattern by patternGroup.pattern(
        "inventories",
        "(?:Superpairs|Chronomatron|Ultrasequencer) (?:\\(.+\\)|➜ Stakes|Rewards)|Experimentation Table")
    private val enchantingExpChatPattern by patternGroup.pattern(
        "exp",
        "^ \\+(?<amount>\\d+|\\d+,\\d+)k? Enchanting Exp$"
    )

    @SubscribeEvent
    fun onChestGuiOverlayRendered(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!experimentInventoriesPattern.matches(InventoryUtils.openInventoryName())) return

        display = drawDisplay()
        config.dryStreakDisplayPosition.renderStrings(
            display,
            posLabel = "Experiment Dry Streak Display"
        )
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return

        if (superpairsPattern.matches(event.inventoryName)) {
            inExperiment = true
            if (enchantsFound) {
                val storage = storage ?: return
                storage.attemptsSince = 0
                storage.xpSince = 0
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (inExperiment && !enchantsFound) {
            val storage = storage ?: return
            storage.attemptsSince += 1
            inExperiment = false
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.enabled || !LorenzUtils.inSkyBlock) return
        enchantingExpChatPattern.matchMatcher(event.message.removeColor()) {
            val storage = storage ?: return
            storage.xpSince += group("amount").substringBefore(",").toInt() * 1000
        }
    }

    private fun drawDisplay() = buildList {
        if (!config.attemptsSince && !config.xpSince) return@buildList

        add("§4Dry-Streak since last ULTRA-RARE")
        storage?.let {
            val attemptsSince = it.attemptsSince
            val xpSince = it.xpSince.shortFormat()
            val attemptsSuffix = if (attemptsSince == 1) "" else "s"

            when {
                config.attemptsSince && config.xpSince -> {
                    add("§c ├ $attemptsSince Attempt$attemptsSuffix")
                    add("§c └ $xpSince XP")
                }
                config.attemptsSince -> add("§c └ $attemptsSince Attempt$attemptsSuffix")
                config.xpSince -> add("§c └ $xpSince XP")
            }
        }
    }

    private fun isEnabled() =
        config.enabled && LorenzUtils.inSkyBlock
}
