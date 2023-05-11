package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GardenVisitorStats {
    private val config get() = SkyHanniMod.feature.garden.visitorDrops
    private val hidden get() = SkyHanniMod.feature.hidden.visitorDrops
    private var visitorStats = listOf<List<Any>>()

    private var acceptedVisitors = 0
    var deniedVisitors = 0
    private var totalVisitors = 0
    private var visitorRarities = mutableListOf<Long>()
    private var totalCopper = 0
    private var totalEXP = 0L
    var totalCost = 0L
    private var bandanaCount = 0
    private var grassCount = 0
    private var bouquetCount = 0
    private var dedicationCount = 0
    private var musicCount = 0
    private var helmetCount = 0

    private val acceptPattern = "OFFER ACCEPTED with (?<visitor>.*) [(](?<rarity>.*)[)]".toPattern()
    private val copperPattern = "[+](?<amount>.*) Copper".toPattern()
    private val farmingExpPattern = "[+](?<amount>.*) Farming XP".toPattern()
    private val bandanaPattern = "[+]1x Green Bandana".toPattern()
    private val grassPattern = "[+]1x Overgrown Grass".toPattern()
    private val bouquetPattern = "[+]1x Flowering Bouquet".toPattern()
    private val dedicationPattern = "Dedication (IV|4) Book".toPattern()
    private val spacePattern = "[+]Space Helmet".toPattern()
    // Pretty sure that the symbol is ◆ but not 100%
    private val musicPattern = "[+]1x ◆ Music Rune [1I]".toPattern()

    private fun formatDisplay(map: List<List<Any>>): MutableList<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.visitorDropText) {
            newList.add(map[index])
        }
        return newList
    }



    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.onBarnPlot) return
        val message = event.message.removeColor().trim()

        var matcher = copperPattern.matcher(message)
        if (matcher.matches()) {
            val amount = matcher.group("amount").formatNumber().toInt()
            totalCopper += amount
        }
        matcher = farmingExpPattern.matcher(message)
        if (matcher.matches()) {
            val amount = matcher.group("amount").formatNumber()
            totalEXP += amount
        }
        matcher = acceptPattern.matcher(message)
        if (matcher.matches()) {
            setRarities(matcher.group("rarity"))
        }
        else if (bandanaPattern.matcher(message).matches()) bandanaCount += 1
        else if (grassPattern.matcher(message).matches()) grassCount += 1
        else if (bouquetPattern.matcher(message).matches()) bouquetCount += 1
        else if (dedicationPattern.matcher(message).matches()) dedicationCount += 1
        else if (musicPattern.matcher(message).matches()) musicCount += 1
        else if (spacePattern.matcher(message).matches()) helmetCount += 1
        // we only need to update if something matched
        else return
        saveAndUpdate()
    }

    private fun setRarities(rarity: String) {
        acceptedVisitors += 1
        val currentRarity = VisitorRarity.values().first { it.rarity == rarity }
        val temp = visitorRarities[currentRarity.index] + 1
        visitorRarities[currentRarity.index] = temp
        saveAndUpdate()
    }

    private fun drawVisitorStatsDisplay() = buildList<List<Any>> {
        //0
        addAsSingletonList("§e§lVisitor Statistics")
        //1
        addAsSingletonList(if (config.displayNumbersFirst) "§e${totalVisitors.addSeparators()} Total"
        else "§eTotal: ${totalVisitors.addSeparators()}")
        //2
        addAsSingletonList("§a${visitorRarities[0].addSeparators()}§f-§9${visitorRarities[1].addSeparators()}" +
                "§f-§6${visitorRarities[2].addSeparators()}§f-§c${visitorRarities[3].addSeparators()}")
        //3
        addAsSingletonList(if (config.displayNumbersFirst) "§2${acceptedVisitors.addSeparators()} Accepted"
        else "§2Accepted: ${acceptedVisitors.addSeparators()}")
        //4
        addAsSingletonList(if (config.displayNumbersFirst) "§c${deniedVisitors.addSeparators()} Denied"
        else "§cDenied: ${deniedVisitors.addSeparators()}")
        //5
        addAsSingletonList("")
        //6
        addAsSingletonList(if (config.displayNumbersFirst) "§c${totalCopper.addSeparators()} Copper"
        else "§cCopper: ${totalCopper.addSeparators()}")
        //7
        addAsSingletonList(if (config.displayNumbersFirst) "§2${NumberUtil.format(totalEXP)} Farming EXP"
        else "§2Farming EXP: ${NumberUtil.format(totalEXP)}")
        //8
        addAsSingletonList(if (config.displayNumbersFirst) "§6${NumberUtil.format(totalCost)} Coins Spent"
        else "§6Coins Spent: ${NumberUtil.format(totalCost)}")
        // Icons
        if (config.displayIcons) {
            //9
            if (config.displayNumbersFirst) add(listOf("§b${bouquetCount.addSeparators()} ", NEUItems.getItemStack("FLOWERING_BOUQUET")))
            else add(listOf(NEUItems.getItemStack("FLOWERING_BOUQUET"), " §b${bouquetCount.addSeparators()}"))
            //10
            if (config.displayNumbersFirst) add(listOf("§b${grassCount.addSeparators()} ", NEUItems.getItemStack("OVERGROWN_GRASS")))
            else add(listOf(NEUItems.getItemStack("OVERGROWN_GRASS"), " §b${grassCount.addSeparators()}"))
            //11
            if (config.displayNumbersFirst) add(listOf("§b${bandanaCount.addSeparators()} ", NEUItems.getItemStack("GREEN_BANDANA")))
            else add(listOf(NEUItems.getItemStack("GREEN_BANDANA"), " §b${bandanaCount.addSeparators()}"))
            //12
            if (config.displayNumbersFirst) add(listOf("§b${dedicationCount.addSeparators()} ", NEUItems.getItemStack("DEDICATION;4")))
            else add(listOf(NEUItems.getItemStack("DEDICATION;4"), " §b${dedicationCount.addSeparators()}"))
            //13
            if (config.displayNumbersFirst) add(listOf("§b${musicCount.addSeparators()} ", NEUItems.getItemStack("MUSIC_RUNE;1")))
            else add(listOf(NEUItems.getItemStack("MUSIC_RUNE;1"), " §b${musicCount.addSeparators()}"))
            //14
            if (config.displayNumbersFirst) add(listOf("§b${helmetCount.addSeparators()} ", NEUItems.getItemStack("DCTR_SPACE_HELM")))
            else add(listOf(NEUItems.getItemStack("DCTR_SPACE_HELM"), " §b${helmetCount.addSeparators()}"))
        }
        // No Icons
        else {
            //9
            addAsSingletonList(if (config.displayNumbersFirst) "§b${bouquetCount.addSeparators()} §9Flowering Bouquet"
            else "§9Flowering Bouquet: §b${bouquetCount.addSeparators()}")
            //10
            addAsSingletonList(if (config.displayNumbersFirst) "§b${grassCount.addSeparators()} §9Overgrown Grass"
            else "§9Overgrown Grass: §b${grassCount.addSeparators()}")
            //11
            addAsSingletonList(if (config.displayNumbersFirst) "§b${bandanaCount.addSeparators()} §9Green Bandana"
            else "§9Green Bandana: §b${bandanaCount.addSeparators()}")
            //12
            addAsSingletonList(if (config.displayNumbersFirst) "§b${dedicationCount.addSeparators()} §9Dedication IV"
            else "§9Dedication IV: §b${dedicationCount.addSeparators()}")
            //13
            addAsSingletonList(if (config.displayNumbersFirst) "§b${musicCount.addSeparators()} §9Music Rune"
            else "§9Music Rune: §b${musicCount.addSeparators()}")
            //14
            addAsSingletonList(if (config.displayNumbersFirst) "§b${helmetCount.addSeparators()} §cSpace Helmet"
            else "§cSpace Helmet: §b${helmetCount.addSeparators()}")
        }
        //15
        addAsSingletonList("")
    }

    fun saveAndUpdate() {
        if (!GardenAPI.inGarden()) return
        hidden.acceptedVisitors = acceptedVisitors
        hidden.deniedVisitors = deniedVisitors
        totalVisitors = acceptedVisitors + deniedVisitors
        hidden.visitorRarities = visitorRarities
        hidden.totalCopper = totalCopper
        hidden.totalEXP = totalEXP
        hidden.totalCost = totalCost
        hidden.bandanaCount = bandanaCount
        hidden.grassCount = grassCount
        hidden.bouquetCount = bouquetCount
        hidden.dedicationCount = dedicationCount
        hidden.musicCount = musicCount
        hidden.helmetCount = helmetCount
        visitorStats = emptyList()
        visitorStats = drawVisitorStatsDisplay()
        visitorStats = formatDisplay(visitorStats)
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
        totalCopper = hidden.totalCopper
        totalEXP = hidden.totalEXP
        totalCost = hidden.totalCost
        bandanaCount = hidden.bandanaCount
        grassCount = hidden.grassCount
        bouquetCount = hidden.bouquetCount
        dedicationCount = hidden.dedicationCount
        musicCount = hidden.musicCount
        helmetCount = hidden.helmetCount
        saveAndUpdate()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!config.visitorDropsDisplay) return
        if (!GardenAPI.inGarden()) return
        if (GardenAPI.hideExtraGuis()) return
        if (config.onlyOnBarn && !GardenAPI.onBarnPlot) return
        config.visitorDropPos.renderStringsAndItems(visitorStats, posLabel = "Visitor Stats")
    }
}
// not sure if there is a better way to do this
enum class VisitorRarity(val rarity: String, val index: Int) {
    UNCOMMON("UNCOMMON", 0),
    RARE("RARE", 1),
    LEGENDARY("LEGENDARY", 2),
    SPECIAL("SPECIAL", 3),
}