package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GardenVisitorDropStatistics {
    private val config get() = SkyHanniMod.feature.garden.visitorDropsStatistics
    private val hidden get() = SkyHanniMod.feature.hidden.visitorDrops
    private var display = listOf<List<Any>>()

    private var acceptedVisitors = 0
    var deniedVisitors = 0
    private var totalVisitors = 0
    private var visitorRarities = mutableListOf<Long>()
    private var copper = 0
    private var farmingExp = 0L
    var coinsSpent = 0L

    private val acceptPattern = "OFFER ACCEPTED with (?<visitor>.*) [(](?<rarity>.*)[)]".toPattern()
    private val copperPattern = "[+](?<amount>.*) Copper".toPattern()
    private val farmingExpPattern = "[+](?<amount>.*) Farming XP".toPattern()
    private var rewardsCount = mapOf<VisitorReward, Int>()

    private fun formatDisplay(map: List<List<Any>>): MutableList<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            newList.add(map[index])
        }
        return newList
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.onBarnPlot) return
        val message = event.message.removeColor().trim()

        copperPattern.matchMatcher(message) {
            val amount = group("amount").formatNumber().toInt()
            copper += amount
            saveAndUpdate()
        }
        farmingExpPattern.matchMatcher(message) {
            val amount = group("amount").formatNumber()
            farmingExp += amount
            saveAndUpdate()
        }
        acceptPattern.matchMatcher(message) {
            setRarities(group("rarity"))
            saveAndUpdate()
        }

        for (reward in VisitorReward.values()) {
            reward.pattern.matchMatcher(message) {
                val old = rewardsCount[reward] ?: 0
                rewardsCount = rewardsCount.editCopy { this[reward] = old + 1 }
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

    private fun drawVisitorStatsDisplay() = buildList<List<Any>> {
        //0
        addAsSingletonList("§e§lVisitor Statistics")
        //1
        addAsSingletonList(format(totalVisitors, "Total", "§e", ""))
        //2
        addAsSingletonList(
            "§a${visitorRarities[0].addSeparators()}§f-" +
                    "§9${visitorRarities[1].addSeparators()}§f-" +
                    "§6${visitorRarities[2].addSeparators()}§f-" +
                    "§c${visitorRarities[3].addSeparators()}"
        )
        //3
        addAsSingletonList(format(acceptedVisitors, "Accepted", "§2", ""))
        //4
        addAsSingletonList(format(deniedVisitors, "Denied", "§c", ""))
        //5
        addAsSingletonList("")
        //6
        addAsSingletonList(format(copper, "Copper", "§c", ""))
        //7
        addAsSingletonList(format(farmingExp, "Farming EXP", "§3", "§7"))
        //8
        addAsSingletonList(format(coinsSpent, "Coins Spent", "§6", ""))

        //9 - 14
        for (reward in VisitorReward.values()) {
            val count = rewardsCount[reward] ?: 0
            if (config.displayIcons) {// Icons
                val stack = NEUItems.getItemStack(reward.internalName)
                if (config.displayNumbersFirst)
                    add(listOf("§b${count.addSeparators()} ", stack))
                else add(listOf(stack, " §b${count.addSeparators()}"))
            } else { // No Icons
                addAsSingletonList(format(count, reward.displayName, "§b"))
            }
        }
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
        hidden.acceptedVisitors = acceptedVisitors
        hidden.deniedVisitors = deniedVisitors
        totalVisitors = acceptedVisitors + deniedVisitors
        hidden.visitorRarities = visitorRarities
        hidden.copper = copper
        hidden.farmingExp = farmingExp
        hidden.coinsSpent = coinsSpent
        hidden.rewardsCount = rewardsCount
        display = formatDisplay(drawVisitorStatsDisplay())
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
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
        copper = hidden.copper
        farmingExp = hidden.farmingExp
        coinsSpent = hidden.coinsSpent
        rewardsCount = hidden.rewardsCount
        saveAndUpdate()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!config.enabled) return
        if (!GardenAPI.inGarden()) return
        if (GardenAPI.hideExtraGuis()) return
        if (config.onlyOnBarn && !GardenAPI.onBarnPlot) return
        config.visitorDropPos.renderStringsAndItems(display, posLabel = "Visitor Stats")
    }
}

enum class VisitorRarity {
    UNCOMMON, RARE, LEGENDARY, SPECIAL,
}