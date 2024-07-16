package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChocolateFactoryStrayTracker {

    private val config get() = ChocolateFactoryAPI.config
    private var claimedStraysSlots = mutableListOf<Int>()

    /**
     * REGEX-TEST: §9Zero §d§lCAUGHT!
     * REGEX-TEST: §6§lGolden Rabbit §d§lCAUGHT!
     * REGEX-TEST: §fAudi §d§lCAUGHT!
     */
    private val strayCaughtPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.caught",
        "^§[a-f0-9].* §d§lCAUGHT!",
    )

    /**
     * REGEX-TEST: §7You caught a stray §fMandy §7and §7gained §6+283,574 Chocolate§7!
     * REGEX-TEST: §7You caught a stray §aSven §7and gained §7§6+397,004 Chocolate§7!'
     */
    private val strayLorePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.loreinfo",
        "§7You caught a stray (?<rabbit>(?<name>§[^§]*)) .* (?:§7)?§6\\+(?<amount>[\\d,]*) Chocolate§7!",
    )

    /**
     * REGEX-TEST: §7You caught a stray §6§lGolden Rabbit§7! §7You gained §6+13,566,571 Chocolate§7!
     */
    private val goldenStrayJackpotMountainPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldenrawchoc",
        "§7You caught a stray §6§lGolden Rabbit§7! §7You gained §6\\+(?<amount>[\\d,]*) Chocolate§7!",
    )

    /**
     * REGEX-TEST: §7You caught a stray §6§lGolden Rabbit§7! §7You caught a glimpse of §6El Dorado§7, §7but he escaped and left behind §7§6313,780 Chocolate§7!
     */
    private val goldenStrayDoradoEscape by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldendoradoescape",
        "§7You caught a stray §6§lGolden Rabbit§7! §7You caught a glimpse of §6El Dorado§7, §7but he escaped and left behind §7§6\\+(?<amount>[\\d,]*) Chocolate§7!",
    )

    /**
     * REGEX-TEST: §7You caught a stray §6§lGolden Rabbit§7! §7You caught §6El Dorado §7- quite the elusive rabbit!
     */
    private val goldenStrayDoradoCaught by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldendoradocaught",
        "§7You caught a stray §6§lGolden Rabbit§7! §7You caught §6El Dorado §7- quite the elusive rabbit!"
    )

    /**
     * REGEX-TEST: §7You caught a stray §6§lGolden Rabbit§7! §7You caught §6El Dorado§7! Since you §7already have captured him before, §7you gained §6+324,364,585 Chocolate§7.
     */
    private val goldenStrayDoradoDuplicate by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldendoradoduplicate",
        "§7You caught a stray §6§lGolden Rabbit§7! §7You caught §6El Dorado§7! Since you §7already have captured him before, §7you gained §6\\+(?<amount>[\\d,]*) Chocolate§7."
    )

    /**
     * REGEX-TEST: §7You caught a stray §6§lGolden Rabbit§7! §7You gained §6+5 Chocolate §7until the §7end of the SkyBlock year!
     *
     */
    private val goldenStrayClick by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldenclick",
        "§7You caught a stray §6§lGolden Rabbit§7! §7You gained §6\\+5 Chocolate §7until the §7end of the SkyBlock year!",
    )

    /**
     * REGEX-TEST: §7You caught a stray §9Fish the Rabbit§7! §7You have already found §9Fish the §9Rabbit§7, so you received §655,935,257 §6Chocolate§7!
     */
    private val fishTheRabbitPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.fish",
        "§7You caught a stray (?<color>§.)Fish the Rabbit§7! §7You have already found (?:§.)?Fish the (?:§.)?Rabbit§7, so you received §6(?<amount>[\\d,]*) (?:§6)?Chocolate§7!",
    )

    private val rarityFormatMap = buildMap {
        put("common", "§fCommon§7: §r§f")
        put("uncommon", "§aUncommon§7: §r§a")
        put("rare", "§9Rare§7: §r§9")
        put("epic", "§5Epic§7: §r§5")
        put("legendary", "§6Legendary§7: §r§6")
    }

    private val tracker = SkyHanniTracker("Stray Tracker", { Data() }, { it.chocolateFactory.strayTracker })
    { drawDisplay(it) }

    class Data : TrackerData() {
        override fun reset() {
            straysCaught.clear()
            straysExtraChocMs.clear()
            goldenTypesCaught.clear()
        }

        @Expose
        var straysCaught: MutableMap<String, Int> = mutableMapOf()
        @Expose
        var straysExtraChocMs: MutableMap<String, Long> = mutableMapOf()
        @Expose
        var goldenTypesCaught: MutableMap<String, Int> = mutableMapOf()
    }

    private fun formLoreToSingleLine(lore: List<String>): String {
        val notEmptyLines = lore.filter { it.isNotEmpty() }
        return notEmptyLines.joinToString(" ")
    }

    private fun incrementRarity(rarity: String, chocAmount: Long) {
        tracker.modify { it.straysCaught.addOrPut(rarity, 1) }
        val extraTime = ChocolateFactoryAPI.timeUntilNeed(chocAmount + 1)
        tracker.modify { it.straysExtraChocMs.addOrPut(rarity, extraTime.inWholeMilliseconds) }
    }

    private fun extractRarity(rabbitName: String): String {
        return when {
            rabbitName.startsWith("§f") -> "common"
            rabbitName.startsWith("§a") -> "uncommon"
            rabbitName.startsWith("§9") -> "rare"
            rabbitName.startsWith("§5") -> "epic"
            rabbitName.startsWith("§6") -> "legendary"
            rabbitName.startsWith("§d") -> "mythic"
            rabbitName.startsWith("§b") -> "divine"
            else -> "common"
        }
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        val extraChocMs = data.straysExtraChocMs.values.sum().milliseconds
        val formattedExtraTime = extraChocMs.let { if (it == 0.milliseconds) "0s" else it.format() }

        addAsSingletonList(
            Renderable.hoverTips(
                "§6§lStray Tracker",
                tips = listOf("§a+§b${formattedExtraTime} §afrom strays§7"),
            ),
        )
        for((rarity, _) in rarityFormatMap) {
            val rarityDisplay = extractHoverableOfRarity(rarity, data)
            if(rarityDisplay != null) {
                addAsSingletonList(rarityDisplay)
            }
        }
    }

    private fun extractHoverableOfRarity(rarity: String, data: Data): Renderable? {
        val caughtOfRarity = data.straysCaught[rarity]
        val caughtString = caughtOfRarity?.toString() ?: return null

        val rarityExtraChocMs = data.straysExtraChocMs[rarity]?.milliseconds
        val extraChocFormat = rarityExtraChocMs?.format() ?: ""

        val lineHeader = rarityFormatMap[rarity] ?: ""
        val lineFormat = "${lineHeader}${caughtString}"

        return if (rarityExtraChocMs == null) Renderable.string(lineFormat)
        else {
            val productionTip = "§a+§b${extraChocFormat} §aof production§7" + if (rarity == "legendary") extractGoldenTypesCaught(data) else ""
            Renderable.hoverTips(
                Renderable.string(lineFormat),
                tips = productionTip.split("\n"),
            )
        }
    }

    private fun extractGoldenTypesCaught(data: Data): String {
        val goldenList = mutableListOf<String>()
        data.goldenTypesCaught["sidedish"]?.let {
            goldenList.add("§b$it §6Side Dish" + if (it > 1) "es" else "")
        }
        data.goldenTypesCaught["jackpot"]?.let {
            goldenList.add("§b$it §6Chocolate Jackpot" + if (it > 1) "s" else "")
        }
        data.goldenTypesCaught["mountain"]?.let {
            goldenList.add("§b$it §6Chocolate Mountain" + if (it > 1) "s" else "")
        }
        data.goldenTypesCaught["dorado"]?.let {
            goldenList.add((if (it >= 3) "§a" else "§b") + "$it§7/§a3 §6El Dorado §7Sighting" + if (it > 1) "s" else "")
        }
        data.goldenTypesCaught["stampede"]?.let {
            goldenList.add("§b$it §6Stampede" + if (it > 1) "s" else "")
        }
        data.goldenTypesCaught["goldenclick"]?.let {
            goldenList.add("§b$it §6Golden Click" + if (it > 1) "s" else "")
        }
        return if (goldenList.size == 0) "" else ("\n" + goldenList.joinToString("\n"))
    }

    @SubscribeEvent
    fun onTick(event: SecondPassedEvent) {
        if(!isEnabled()) return
        InventoryUtils.getItemsInOpenChest().filter {
            !claimedStraysSlots.contains(it.slotIndex)
        }.forEach {
            strayCaughtPattern.matchMatcher(it.stack.name) {
                if (it.stack.getLore().isEmpty()) return
                claimedStraysSlots.add(it.slotIndex)
                val loreLine = formLoreToSingleLine(it.stack.getLore())

                // "Base" strays - Common -> Epic, raw choc only reward.
                strayLorePattern.matchMatcher(loreLine) {
                    //Pretty sure base strays max at Epic, but...
                    val rarity = extractRarity(group("rabbit"))
                    val amount = groupOrNull("amount")?.formatLong() ?: return
                    incrementRarity(rarity, amount)
                }

                // Fish the Rabbit
                fishTheRabbitPattern.matchMatcher(loreLine) {
                    //Also fairly sure that Fish maxes out at Rare, but...
                    val rarity = extractRarity(group("color"))
                    val amount = groupOrNull("amount")?.formatLong() ?: return
                    incrementRarity(rarity, amount)
                }

                // Golden Strays, Jackpot and Mountain, raw choc only reward.
                goldenStrayJackpotMountainPattern.matchMatcher(loreLine) {
                    val amount = group("amount").formatLong()
                    incrementRarity("legendary", amount)
                    val cps = ChocolateFactoryAPI.chocolatePerSecond
                    val multiplier = amount / cps
                    if (multiplier in 479.0..481.0) { // If multiplier is close (+/- 1) from 480, this is a Jackpot
                        tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) { t -> t.goldenTypesCaught.addOrPut("jackpot", 1) }
                    } else if (multiplier in 1499.0..1501.0) { //Otherwise, if (+/- 1) from 1500 it's a mountain
                        tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) { t -> t.goldenTypesCaught.addOrPut("mountain", 1) }
                    }
                }

                // Golden Strays, El Dorado "glimpse" - 1/3 before capture
                goldenStrayDoradoEscape.matchMatcher(loreLine) {
                    val amount = group("amount").formatLong()
                    incrementRarity("legendary", amount)
                    tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) { t -> t.goldenTypesCaught.addOrPut("dorado", 1) }
                }

                // Golden Strays, El Dorado caught - 3/3
                if(goldenStrayDoradoCaught.matches(loreLine)){
                    incrementRarity("legendary", 1)
                    tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) { t -> t.goldenTypesCaught["dorado"] = 3 }
                }

                // Golden Strays, El Dorado (duplicate catch)
                goldenStrayDoradoDuplicate.matchMatcher(loreLine) {
                    val amount = group("amount").formatLong()
                    incrementRarity("legendary", amount)
                    tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) {
                        t -> t.goldenTypesCaught["dorado"] = t.goldenTypesCaught["dorado"]?.plus(1) ?: 4
                    }
                }

                // Golden Strays, "Golden Click"
                goldenStrayClick.matchMatcher(loreLine) {
                    tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) { t -> t.goldenTypesCaught.addOrPut("goldenclick", 1) }
                }

                // Golden Strays, hoard/stampede
                if (loreLine == "§7You caught a stray §6§lGolden Rabbit§7! §7A hoard of §aStray Rabbits §7has appeared!") {
                    tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) { t -> t.goldenTypesCaught.addOrPut("stampede", 1) }
                }

                //Asynchronously update to immediately reflect caught stray
                if (isEnabled()) {
                    tracker.renderDisplay(config.strayRabbitTrackerPosition)
                }
            }
        }
        InventoryUtils.getItemsInOpenChest().filter {
            claimedStraysSlots.contains(it.slotIndex)
        }.forEach {
            if (!strayCaughtPattern.matches(it.stack.name)) {
                claimedStraysSlots.removeAt(claimedStraysSlots.indexOf(it.slotIndex))
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if(!isEnabled()) return
        if (event.slot == null || event.slot.slotIndex == -999) return
        if (!InventoryUtils.getItemsInOpenChest().any { it.slotNumber == event.slot.slotNumber }) return
        if (claimedStraysSlots.contains(event.slot.slotIndex)) return
        try {
            val clickedSlot = InventoryUtils.getItemsInOpenChest().first {
                it.hasStack && it.slotNumber == event.slot.slotNumber
            }
            val clickedStack = clickedSlot.stack
            val nameText = (if (clickedStack.hasDisplayName()) clickedStack.displayName else clickedStack.itemName)
            if (nameText.equals("§6§lGolden Rabbit §8- §aSide Dish")) {
                claimedStraysSlots.add(event.slot.slotIndex)
                tracker.modify(SkyHanniTracker.DisplayMode.TOTAL) { it.goldenTypesCaught.addOrPut("sidedish", 1) }
                incrementRarity("legendary", 0)
                DelayedRun.runDelayed(1.seconds) {
                    claimedStraysSlots.remove(claimedStraysSlots.indexOf(event.slot.slotIndex))
                }
            }
        } catch (e: NoSuchElementException) {
            return
        }
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if(!isEnabled()) return
        tracker.renderDisplay(config.strayRabbitTrackerPosition)
    }

    fun resetCommand() {
        tracker.resetCommand()
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.strayRabbitTracker && ChocolateFactoryAPI.inChocolateFactory
}
