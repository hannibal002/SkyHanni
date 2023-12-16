package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.DianaDrops
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DianaProfitTracker {
    private val config get() = SkyHanniMod.feature.event.diana.dianaProfitTracker
    private var allowedDrops = listOf<NEUInternalName>()

    private val chatDugOutPattern by RepoPattern.pattern(
        "diana.chat.burrow.dug",
        "(§eYou dug out a Griffin Burrow!|§eYou finished the Griffin burrow chain!) .*"
    )
    private val chatDugOutCoinsPattern by RepoPattern.pattern(
        "diana.chat.coins",
        "§6§lWow! §r§eYou dug out §r§6(?<coins>.*) coins§r§e!"
    )

    private val tracker = SkyHanniItemTracker(
        "Diana Profit Tracker",
        { Data() },
        { it.diana.dianaProfitTracker }) { drawDisplay(it) }

    class Data : ItemTrackerData() {
        override fun resetItems() {
            burrowsDug = 0
        }

        @Expose
        var burrowsDug: Long = 0

        override fun getDescription(timesDropped: Long): List<String> {
            val percentage = timesDropped.toDouble() / burrowsDug
            val perBurrow = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))

            return listOf(
                "§7Dropped §e${timesDropped.addSeparators()} §7times.",
                "§7Your drop chance per burrow: §c$perBurrow",
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Dug Out Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val burrowDugCoinsFormat = NumberUtil.format(item.totalAmount)
            return listOf(
                "§7Digging treasures gave you",
                "§6$burrowDugCoinsFormat coins §7in total."
            )
        }
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§e§lDiana Profit Tracker")

        val profit = tracker.drawItems(data, { true }, this)

        val treasureCoins = data.burrowsDug
        addAsSingletonList(
            Renderable.hoverTips(
                "§7Burrows dug: §e${treasureCoins.addSeparators()}",
                listOf("§7You dug out griffin burrows §e${treasureCoins.addSeparators()} §7times.")
            )
        )

        val profitFormat = NumberUtil.format(profit)
        val profitPrefix = if (profit < 0) "§c" else "§6"

        val profitPerBurrow = profit / data.burrowsDug
        val profitPerBurrowFormat = NumberUtil.format(profitPerBurrow)

        val text = "§eTotal Profit: $profitPrefix$profitFormat coins"
        addAsSingletonList(
            Renderable.hoverTips(
                text,
                listOf("§7Profit per burrow: $profitPrefix$profitPerBurrowFormat")
            )
        )

        tracker.addPriceFromButton(this)
    }

    @SubscribeEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (!isEnabled()) return

        val internalName = event.internalName

        if (!isAllowedItem(internalName)) {
            LorenzUtils.debug("Ignored non-diana item pickup: '$internalName'")
            return
        }

        tracker.addItem(internalName, event.amount)
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message
        if (chatDugOutPattern.matches(message)) {
            tracker.modify {
                it.burrowsDug++
            }
            tryHide(event)
        }
        chatDugOutCoinsPattern.matchMatcher(message) {
            val coins = group("coins").formatNumber().toInt()
            tracker.addCoins(coins)
            tryHide(event)
        }

        if (message == "§6§lRARE DROP! §r§eYou dug out a §r§9Griffin Feather§r§e!" ||
            message == "§eFollow the arrows to find the §r§6treasure§r§e!"
        ) {
            tryHide(event)
        }
    }

    private fun tryHide(event: LorenzChatEvent) {
        if (SkyHanniMod.feature.chat.filterType.diana) {
            event.blockedReason = "diana_chain_or_drops"
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        tracker.renderDisplay(config.position)
    }

    private fun isAllowedItem(internalName: NEUInternalName): Boolean = internalName in allowedDrops

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        allowedDrops = event.getConstant<DianaDrops>("DianaDrops").diana_drops
    }

    fun resetCommand(args: Array<String>) {
        tracker.resetCommand(args, "shresetdianaprofittracker")
    }

    private fun isEnabled() = DianaAPI.isDoingDiana() && config.enabled
}
