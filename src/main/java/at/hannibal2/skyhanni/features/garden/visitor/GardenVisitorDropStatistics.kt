package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.VisitorAcceptEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GardenVisitorDropStatistics {
    private val config get() = SkyHanniMod.feature.garden.visitors.dropsStatistics
    private var display = emptyList<List<Any>>()

    private var acceptedVisitors = 0
    var deniedVisitors = 0
    private var totalVisitors = 0
    private var visitorRarities = mutableListOf<Long>()
    var coinsSpent = 0L

    var lastAccept = 0L

    private val acceptPattern = "OFFER ACCEPTED with (?<visitor>.*) [(](?<rarity>.*)[)]".toPattern()
    private val copperPattern = "[+](?<amount>.*) Copper".toPattern()
    private val gardenExpPattern = "[+](?<amount>.*) Garden Experience".toPattern()
    private val farmingExpPattern = "[+](?<amount>.*) Farming XP".toPattern()
    private val bitsPattern = "[+](?<amount>.*) Bits".toPattern()
    private val mithrilPowderPattern = "[+](?<amount>.*) Mithril Powder".toPattern()
    private val gemstonePowderPattern = "[+](?<amount>.*) Gemstone Powder".toPattern()
    private var rewardsCount = mapOf<VisitorReward, Int>()

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            newList.add(map[index])
        }
        return newList
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onVisitorAccept(event: VisitorAcceptEvent) {
        if (!GardenAPI.onBarnPlot) return
        if (!ProfileStorageData.loaded) return

        for (internalName in event.visitor.allRewards) {
            val reward = VisitorReward.getByInternalName(internalName) ?: continue
            rewardsCount = rewardsCount.editCopy { addOrPut(reward, 1) }
            saveAndUpdate()
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.onBarnPlot) return
        if (!ProfileStorageData.loaded) return
        if (lastAccept - System.currentTimeMillis() <= 0 && lastAccept - System.currentTimeMillis() > -1000) {
            val message = event.message.removeColor().trim()
            val hidden = GardenAPI.config?.visitorDrops ?: return

            copperPattern.matchMatcher(message) {
                val amount = group("amount").formatNumber().toInt()
                hidden.copper += amount
                saveAndUpdate()
            }
            farmingExpPattern.matchMatcher(message) {
                val amount = group("amount").formatNumber()
                hidden.farmingExp += amount
                saveAndUpdate()
            }
            gardenExpPattern.matchMatcher(message) {
                val amount = group("amount").formatNumber().toInt()
                if (amount > 80) return // some of the low visitor milestones will get through but will be minimal
                hidden.gardenExp += amount
                saveAndUpdate()
            }
            bitsPattern.matchMatcher(message) {
                val amount = group("amount").formatNumber().toInt()
                hidden.bits += amount
                saveAndUpdate()
            }
            mithrilPowderPattern.matchMatcher(message) {
                val amount = group("amount").formatNumber().toInt()
                hidden.mithrilPowder += amount
                saveAndUpdate()
            }
            gemstonePowderPattern.matchMatcher(message) {
                val amount = group("amount").formatNumber().toInt()
                hidden.gemstonePowder += amount
                saveAndUpdate()
            }
            acceptPattern.matchMatcher(message) {
                setRarities(group("rarity"))
                saveAndUpdate()
            }
        }
    }

    private fun setRarities(rarity: String) {
        acceptedVisitors += 1
        val currentRarity = VisitorRarity.valueOf(rarity)
        val temp = visitorRarities[currentRarity.ordinal] + 1
        visitorRarities[currentRarity.ordinal] = temp
        saveAndUpdate()
    }

    private fun drawDisplay(hidden: Storage.ProfileSpecific.GardenStorage.VisitorDrops) = buildList<List<Any>> {
        //0
        addAsSingletonList("§e§lVisitor Statistics")
        //1
        addAsSingletonList(format(totalVisitors, "Total", "§e", ""))
        //2
        if (visitorRarities.isNotEmpty()) {
            addAsSingletonList(
                "§a${visitorRarities[0].addSeparators()}§f-" +
                        "§9${visitorRarities[1].addSeparators()}§f-" +
                        "§6${visitorRarities[2].addSeparators()}§f-" +
                        "§c${visitorRarities[3].addSeparators()}"
            )
        } else {
            addAsSingletonList("§c?")
            CopyErrorCommand.logError(
                RuntimeException("visitorRarities is empty, maybe visitor refusing was the cause?"),
                "Error rendering visitor drop statistics"
            )
        }
        //3
        addAsSingletonList(format(acceptedVisitors, "Accepted", "§2", ""))
        //4
        addAsSingletonList(format(deniedVisitors, "Denied", "§c", ""))
        //5
        addAsSingletonList("")
        //6
        addAsSingletonList(format(hidden.copper, "Copper", "§c", ""))
        //7
        addAsSingletonList(format(hidden.farmingExp, "Farming EXP", "§3", "§7"))
        //8
        addAsSingletonList(format(coinsSpent, "Coins Spent", "§6", ""))

        //9 – 16
        for (reward in VisitorReward.entries) {
            val count = rewardsCount[reward] ?: 0
            if (config.displayIcons) {// Icons
                val stack = reward.itemStack
                if (config.displayNumbersFirst)
                    add(listOf("§b${count.addSeparators()} ", stack))
                else add(listOf(stack, " §b${count.addSeparators()}"))
            } else { // No Icons
                addAsSingletonList(format(count, reward.displayName, "§b"))
            }
        }
        //17
        addAsSingletonList("")
        //18
        addAsSingletonList(format(hidden.gardenExp, "Garden EXP", "§2", "§7"))
        //19
        addAsSingletonList(format(hidden.bits, "Bits", "§b", "§b"))
        //20
        addAsSingletonList(format(hidden.mithrilPowder, "Mithril Powder", "§2", "§2"))
        //21
        addAsSingletonList(format(hidden.gemstonePowder, "Gemstone Powder", "§d", "§d"))
    }

    fun format(amount: Number, name: String, color: String, amountColor: String = color) =
        if (config.displayNumbersFirst)
            "$color${format(amount)} $name"
        else
            "$color$name: $amountColor${format(amount)}"

    fun format(amount: Number): String {
        if (amount is Int) return amount.addSeparators()
        if (amount is Long) return NumberUtil.format(amount)
        return "$amount"
    }

    fun saveAndUpdate() {
        if (!GardenAPI.inGarden()) return
        val hidden = GardenAPI.config?.visitorDrops ?: return
        hidden.acceptedVisitors = acceptedVisitors
        hidden.deniedVisitors = deniedVisitors
        totalVisitors = acceptedVisitors + deniedVisitors
        hidden.visitorRarities = visitorRarities
        hidden.coinsSpent = coinsSpent
        hidden.rewardsCount = rewardsCount
        display = formatDisplay(drawDisplay(hidden))
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val hidden = GardenAPI.config?.visitorDrops ?: return
        if (hidden.visitorRarities.size == 0) {
            hidden.visitorRarities.add(0)
            hidden.visitorRarities.add(0)
            hidden.visitorRarities.add(0)
            hidden.visitorRarities.add(0)
        }
        acceptedVisitors = hidden.acceptedVisitors
        deniedVisitors = hidden.deniedVisitors
        totalVisitors = acceptedVisitors + deniedVisitors
        visitorRarities = hidden.visitorRarities
        coinsSpent = hidden.coinsSpent
        rewardsCount = hidden.rewardsCount
        saveAndUpdate()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!GardenAPI.inGarden()) return
        if (GardenAPI.hideExtraGuis()) return
        if (config.onlyOnBarn && !GardenAPI.onBarnPlot) return
        config.pos.renderStringsAndItems(display, posLabel = "Visitor Stats")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent){
        event.move(3, "garden.visitorDropsStatistics.enabled", "garden.visitors.dropsStatistics.enabled")
        event.move(3, "garden.visitorDropsStatistics.textFormat", "garden.visitors.dropsStatistics.textFormat")
        event.move(3, "garden.visitorDropsStatistics.displayNumbersFirst", "garden.visitors.dropsStatistics.displayNumbersFirst")
        event.move(3, "garden.visitorDropsStatistics.displayIcons", "garden.visitors.dropsStatistics.displayIcons")
        event.move(3, "garden.visitorDropsStatistics.onlyOnBarn", "garden.visitors.dropsStatistics.onlyOnBarn")
        event.move(3, "garden.visitorDropsStatistics.visitorDropPos", "garden.visitors.dropsStatistics.pos")
    }
}

enum class VisitorRarity {
    UNCOMMON, RARE, LEGENDARY, SPECIAL,
}