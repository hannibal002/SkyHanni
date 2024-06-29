package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.UltraRareBookAlert.bookPattern
import at.hannibal2.skyhanni.features.inventory.experimentationtable.UltraRareBookAlert.ultraRarePattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
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

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.dryStreakConfig
    private val storage get() = ProfileStorageData.profileSpecific?.dryStreakStorage

    private var display = emptyList<String>()

    private var inExperiment = false
    private var enchantsFound = false
    private var didJustFind = false

    private val patternGroup = RepoPattern.group("enchanting.experiments.drystreak")
    val experimentInventoriesPattern by patternGroup.pattern(
        "inventories",
        "(?:Superpairs|Chronomatron|Ultrasequencer) (?:\\(.+\\)|➜ Stakes|Rewards)|Experimentation Table",
    )
    private val enchantingExpChatPattern by patternGroup.pattern(
        "exp",
        "^ \\+(?<amount>\\d+|\\d+,\\d+)k? Enchanting Exp$",
    )

    @SubscribeEvent
    fun onChestGuiOverlayRendered(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!experimentInventoriesPattern.matches(InventoryUtils.openInventoryName())) return

        display = drawDisplay()
        config.dryStreakDisplayPosition.renderStrings(
            display,
            posLabel = "Experiment Dry Streak Display",
        )
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventoryName == "Experimentation Table" && didJustFind) didJustFind = false
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return

        if (InventoryUtils.getCurrentExperiment() != null) {
            inExperiment = true
            for (lore in event.inventoryItems.map { it.value.getLore() }) {
                val firstLine = lore.firstOrNull() ?: continue
                if (!ultraRarePattern.matches(firstLine)) continue
                val bookNameLine = lore.getOrNull(2) ?: continue
                bookPattern.matchMatcher(bookNameLine) {
                    val storage = storage ?: return
                    storage.attemptsSince = 0
                    storage.xpSince = 0
                    didJustFind = true
                    enchantsFound = true
                }
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (didJustFind) inExperiment = false

        if (inExperiment && !enchantsFound) {
            val storage = storage ?: return
            storage.attemptsSince += 1
        }
        enchantsFound = false
        inExperiment = false
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled() || didJustFind) return

        enchantingExpChatPattern.matchMatcher(event.message.removeColor()) {
            val storage = storage ?: return
            storage.xpSince += group("amount").substringBefore(",").toInt() * 1000
        }
    }

    private fun drawDisplay() = buildList {
        if (!config.attemptsSince && !config.xpSince) return@buildList

        storage?.let {
            add("§cDry-Streak since last §5ULTRA-RARE")

            val colorPrefix = "§e"
            val attemptsSince = it.attemptsSince
            val xpSince = it.xpSince.shortFormat()
            val attemptsSuffix = if (attemptsSince == 1) "" else "s"

            when {
                config.attemptsSince && config.xpSince -> {
                    add("$colorPrefix ├ $attemptsSince Attempt$attemptsSuffix")
                    add("$colorPrefix └ $xpSince XP")
                }
                config.attemptsSince -> add("$colorPrefix └ $attemptsSince Attempt$attemptsSuffix")
                config.xpSince -> add("$colorPrefix └ $xpSince XP")
            }
        }
    }

    private fun isEnabled() =
        config.enabled && LorenzUtils.inSkyBlock
}
