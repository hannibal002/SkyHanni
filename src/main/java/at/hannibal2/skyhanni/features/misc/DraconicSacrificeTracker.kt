package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object DraconicSacrificeTracker {

    private val config get() = SkyHanniMod.feature.misc.draconicSacrificeTracker
    private val patternGroup = RepoPattern.group("misc.draconicsacrifice")
    private val sacrificeLoot by patternGroup.pattern(
        "sacrifice",
        "§c§lSACRIFICE! §r§eYou turned §r(?<item>.*) §r§einto §r§d(?<amount>\\d+) Dragon Essence§r§e!",
    )
    private val bonusLoot by patternGroup.pattern(
        "bonus",
        "§c§lBONUS LOOT! §r§eYou also received §r(?:§\\w(?<amount>\\d+)?x)?(?: §r)?(?<item>.*) §r§efrom your sacrifice!",
    )

    private val tracker =
        SkyHanniItemTracker("Draconic Sacrifice Profit Tracker", { Data() }, { it.draconicSacrificeTracker }) {
            drawDisplay(it)
        }

    private val altarArea = AxisAlignedBB(-601.0, 4.0, -282.0, -586.0, 15.0, -269.0)

    class Data : ItemTrackerData() {
        override fun resetItems() {
            sacrifiedItemsMap.clear()
            itemsSacrifice = 0
        }

        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / itemsSacrifice
            val dropRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        override fun getCoinName(item: TrackedItem) = "§dDragon Essence"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val essences = NumberUtil.format(item.totalAmount)
            return listOf(
                "§7Sacrificed items give you dragon essence.",
                "§7You got §6$essences essence §7that way.",
            )
        }

        @Expose
        var itemsSacrifice = 0L

        @Expose
        var sacrifiedItemsMap: MutableMap<String, Long> = mutableMapOf()
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§5§lDraconic Sacrifice Profit Tracker")
        val profit = tracker.drawItems(data, { true }, this)

        addAsSingletonList(
            Renderable.hoverTips(
                "§b${data.itemsSacrifice.addSeparators()} §6Items Sacrified",
                data.sacrifiedItemsMap.map { (key, value) -> "$key: §b$value" },
            ),
        )

        addAsSingletonList(tracker.addTotalProfit(profit, data.itemsSacrifice, "sacrifice"))

        tracker.addPriceFromButton(this)
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        sacrificeLoot.matchMatcher(event.message) {
            val amount = group("amount").toInt()
            val item = group("item")
            tracker.addItem("ESSENCE_DRAGON".asInternalName(), amount)
            tracker.modify {
                it.itemsSacrifice += 1
                it.sacrifiedItemsMap.addOrPut(item, 1)
            }
        }

        bonusLoot.matchMatcher(event.message) {
            val item = group("item")
            val amount = groupOrNull("amount")?.toInt() ?: 1
            val internalName = NEUInternalName.fromItemNameOrNull(item) ?: return
            tracker.addItem(internalName, amount)
        }
        tracker.update()
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (config.onlyInVoidSlate && !altarArea.isPlayerInside()) return

        tracker.renderDisplay(config.position)
    }

    fun resetCommand() {
        tracker.resetCommand()
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
