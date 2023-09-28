package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.Storage.ProfileSpecific.SlayerProfitList.ProfitTrackerItem
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.getBazaarData
import at.hannibal2.skyhanni.features.bazaar.BazaarData
import at.hannibal2.skyhanni.features.slayer.SlayerItemProfitTracker
import at.hannibal2.skyhanni.test.PriceSource
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPrice
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.renderables.Renderable

class ProfitTracker {

    enum class DisplayMode(val displayName: String) {
        TOTAL("Total"),
        CURRENT("This Session"),
        ;
    }

    open class AbstractProfitList<ListData>(
        private val total: ProfitListWrapper<ListData>,
        private val currentSession: ProfitListWrapper<ListData>,
        private val update: () -> Unit,
    ) {
        var inventoryOpen = false
        var lastClickDelay = 0L
        private var currentDisplayMode = DisplayMode.TOTAL

        fun modify(modifyFunction: (ListData) -> Unit) {
            modifyFunction(total.getType())
            modifyFunction(currentSession.getType())
        }

        fun get(displayMode: DisplayMode) = when (displayMode) {
            DisplayMode.TOTAL -> total
            DisplayMode.CURRENT -> currentSession
        }

        private fun resetData(displayMode: DisplayMode) {
            val list = get(displayMode)
            list.reset()
        }

        fun drawDisplay(title: String) = buildList<List<Any>> {
//            val both = currentLog() ?: return@buildList
            val itemLog = get(currentDisplayMode)

            addAsSingletonList(title)
            if (inventoryOpen) {
                addSelector<DisplayMode>(
                        "§7Display Mode: ",
                        getName = { type -> type.displayName },
                        isCurrent = { it == currentDisplayMode },
                        onChange = {
                            currentDisplayMode = it
                            update()
                        }
                )
            }

            var profit = 0.0
            val map = mutableMapOf<Renderable, Long>()

//            for ((internalName, itemProfit) in itemLog.items) {
            for ((internalName, itemProfit) in itemLog.getList()) {
                val amount = itemProfit.totalAmount

                val price = (getPrice(internalName) * amount).toLong()

                val cleanName = SlayerAPI.getNameWithEnchantmentFor(internalName)
                var name = cleanName
                val priceFormat = NumberUtil.format(price)
                val hidden = itemProfit.hidden
                if (hidden) {
                    while (name.startsWith("§f")) {
                        name = name.substring(2)
                    }
                    name = StringUtils.addFormat(name, "§m")
                }
                val text = " §7${amount.addSeparators()}x $name§7: §6$priceFormat"

                val timesDropped = itemProfit.timesDropped
                val percentage = timesDropped.toDouble() / itemLog.slayerCompletedCount
                val perBoss = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))

                val renderable = if (inventoryOpen) Renderable.clickAndHover(
                        text, listOf(
                        "§7Dropped §e${timesDropped.addSeparators()} §7times.",
                        "§7Your drop rate: §c$perBoss",
                        "",
                        "§eClick to " + (if (hidden) "show" else "hide") + "!",
                        "§eControl + Click to remove this item!",
                )
                ) {
                    if (System.currentTimeMillis() > lastClickDelay + 150) {

                        if (LorenzUtils.isControlKeyDown()) {
                            itemLog.getList().remove(internalName)
//                            itemLog.items.remove(internalName)
                            LorenzUtils.chat("§e[SkyHanni] Removed $cleanName §efrom slayer profit display.")
                            lastClickDelay = System.currentTimeMillis() + 500
                        } else {
                            itemProfit.hidden = !hidden
                            lastClickDelay = System.currentTimeMillis()
                        }
                        update()
                    }
                } else Renderable.string(text)
                if (inventoryOpen || !hidden) {
                    map[renderable] = price
                }
                profit += price
            }
            val mobKillCoins = itemLog.mobKillCoins
            if (mobKillCoins != 0L) {
                val mobKillCoinsFormat = NumberUtil.format(mobKillCoins)
                map[Renderable.hoverTips(
                        " §7Mob kill coins: §6$mobKillCoinsFormat",
                        listOf(
                                "§7Killing mobs gives you coins (more with scavenger)",
                                "§7You got §e$mobKillCoinsFormat §7coins in total this way"
                        )
                )] = mobKillCoins
                profit += mobKillCoins
            }
            val slayerSpawnCost = itemLog.slayerSpawnCost
            if (slayerSpawnCost != 0L) {
                val mobKillCoinsFormat = NumberUtil.format(slayerSpawnCost)
                map[Renderable.hoverTips(
                        " §7Slayer Spawn Costs: §c$mobKillCoinsFormat",
                        listOf("§7You paid §c$mobKillCoinsFormat §7in total", "§7for starting the slayer quests.")
                )] = slayerSpawnCost
                profit += slayerSpawnCost
            }

            for (text in map.sortedDesc().keys) {
                addAsSingletonList(text)
            }

            val slayerCompletedCount = itemLog.slayerCompletedCount
            addAsSingletonList(
                    Renderable.hoverTips(
                            "§7Bosses killed: §e${slayerCompletedCount.addSeparators()}",
                            listOf("§7You killed the ${itemLogCategory} boss", "§e${slayerCompletedCount.addSeparators()} §7times.")
                    )
            )

            val profitFormat = NumberUtil.format(profit)
            val profitPrefix = if (profit < 0) "§c" else "§6"

            val profitPerBoss = profit / itemLog.slayerCompletedCount
            val profitPerBossFormat = NumberUtil.format(profitPerBoss)

            val text = "§eTotal Profit: $profitPrefix$profitFormat"
            addAsSingletonList(Renderable.hoverTips(text, listOf("§7Profit per boss: $profitPrefix$profitPerBossFormat")))

            if (inventoryOpen) {
                addSelector<PriceSource>(
                        "",
                        getName = { type -> type.displayName },
                        isCurrent = { it.ordinal == config.priceFrom },
                        onChange = {
                            config.priceFrom = it.ordinal
                            update()
                        }
                )
            }
            if (inventoryOpen && currentDisplayMode == DisplayMode.CURRENT) {
                addAsSingletonList(
                        Renderable.clickAndHover(
                                "§cReset session!",
                                listOf("§cThis will reset your", "§ccurrent session of", title),
                        ) {
                            resetData(DisplayMode.CURRENT)
                            update()
                        })
            }
        }

        fun getPrice(internalName: NEUInternalName) =
                internalName.getBazaarData()?.let { getPrice(internalName, it) } ?: internalName.getPrice()

        fun getPrice(internalName: NEUInternalName, bazaarData: BazaarData) = when (SlayerItemProfitTracker.config.priceFrom) {
            0 -> bazaarData.sellPrice
            1 -> bazaarData.buyPrice

            else -> internalName.getNpcPrice()
        }
    }
}

abstract class ProfitListWrapper<T> {
    abstract fun reset()

    abstract fun getList(): MutableMap<NEUInternalName, ProfitTrackerItem>

    abstract fun getType(): T
}