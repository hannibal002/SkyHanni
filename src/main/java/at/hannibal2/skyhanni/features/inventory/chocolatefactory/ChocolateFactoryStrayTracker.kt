package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChocolateFactoryStrayTracker {

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage
    private var straysDisplay = listOf<Renderable>()
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
        "§7You caught a stray §6§lGolden Rabbit§7! §7You gained §6\\+(?<amount>[\\d,]*) Chocolate§7!"
    )

    /**
     * REGEX-TEST: §7You caught a stray §6§lGolden Rabbit§7! §7You caught a glimpse of §6El Dorado§7, §7but he escaped and left behind §7§6313,780 Chocolate§7!
     */
    private val goldenStrayDoradoEscape by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldendoradoescape",
        "§7You caught a stray §6§lGolden Rabbit§7! §7You caught a glimpse of §6El Dorado§7, §7but he escaped and left behind §7§6\\+(?<amount>[\\d,]*) Chocolate§7!"
    )

    /**
     * REGEX-TEST: §7You caught a stray §6§lGolden Rabbit§7! §7You gained §6+5 Chocolate §7until the end of the SkyBlock year!
     */
    private val goldenStrayClick by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldenclick",
        "§7You caught a stray §6§lGolden Rabbit§7! §7You gained §6\\+5 Chocolate §7until the end of the SkyBlock year!"
    )

    private val goldenStraySideDish by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldensidedish",
        "§6§lGolden Rabbit §8- §aSide Dish"
    )

    private val goldenStrayHoardPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldenhoard",
        "§7You caught a stray §6§lGolden Rabbit§7! §7A hoard of §aStray Rabbits §7has appeared!",
    )

    /**
     * REGEX-TEST: §7You caught a stray §9Fish the Rabbit§7! §7You have already found §9Fish the §9Rabbit§7, so you received §655,935,257 §6Chocolate§7!
     */
    private val fishTheRabbitPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.fish",
        "§7You caught a stray (?<color>§.)Fish the Rabbit§7! §7You have already found (?:§.)?Fish the (?:§.)?Rabbit§7, so you received §6(?<amount>[\\d,]*) (?:§6)?Chocolate§7!",
    )

    private fun formLoreToSingleLine(lore: List<String>): String {
        val notEmptyLines = lore.filter { it.isNotEmpty() }
        return notEmptyLines.joinToString(" ")
    }

    private fun incrementRarity(rarity: String, chocAmount: Long) {
        val profileStorage = profileStorage ?: return
        profileStorage.straysCaught[rarity] = profileStorage.straysCaught[rarity]?.plus(1) ?: 1
        val extraTime = ChocolateFactoryAPI.timeUntilNeed(chocAmount + 1)
        profileStorage.straysExtraChocMs[rarity] = profileStorage.straysExtraChocMs[rarity]?.plus(extraTime.inWholeMilliseconds)
            ?: extraTime.inWholeMilliseconds
    }

    @SubscribeEvent
    fun onTick(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.strayRabbitTracker) return
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        val profileStorage = profileStorage ?: return
        InventoryUtils.getItemsInOpenChest().filter {
            !claimedStraysSlots.contains(it.slotIndex)
        }.forEach {
            strayCaughtPattern.matchMatcher(it.stack.name) {
                if (it.stack.getLore().isEmpty()) return
                claimedStraysSlots.add(it.slotIndex)
                val loreLine = formLoreToSingleLine(it.stack.getLore())

                // "Base" strays - Common -> Epic, raw choc only reward.
                strayLorePattern.matchMatcher(loreLine) {
                    val rarity = extractRarity(group("rabbit"))
                    val amount = group("amount").formatLong()
                    incrementRarity(rarity, amount)
                }

                // Fish the Rabbit
                fishTheRabbitPattern.matchMatcher(loreLine) {
                    val rarity = extractRarity(group("color"))
                    val amount = group("amount").formatLong()
                    incrementRarity(rarity, amount)
                }

                // Golden Strays, Jackpot and Mountain, raw choc only reward.
                goldenStrayJackpotMountainPattern.matchMatcher(loreLine) {
                    val amount = group("amount").formatLong()
                    incrementRarity("legendary", amount)
                    val cps = ChocolateFactoryAPI.chocolatePerSecond
                    val multiplier = amount / cps
                    if (multiplier in 479.0..481.0) { // If multiplier is close (+/- 1) from 480, this is a Jackpot
                        profileStorage.goldenTypesCaught["jackpot"] = profileStorage.goldenTypesCaught["jackpot"]?.plus(1) ?: 1
                    } else if (multiplier in 1499.0..1501.0) { //Otherwise, if (+/- 1) from 1500 it's a mountain
                        profileStorage.goldenTypesCaught["mountain"] = profileStorage.goldenTypesCaught["mountain"]?.plus(1) ?: 1
                    }
                }

                // Golden Strays, El Dorado "glimpse" - 1/3 before capture
                goldenStrayDoradoEscape.matchMatcher(loreLine) {
                    val amount = group("amount").formatLong()
                    incrementRarity("legendary", amount)
                    profileStorage.goldenTypesCaught["dorado"] = profileStorage.goldenTypesCaught["dorado"]?.plus(1) ?: 1
                }

                // Golden Strays, "Golden Click"
                goldenStrayClick.matchMatcher(loreLine) {
                    profileStorage.goldenTypesCaught["goldenclick"] = profileStorage.goldenTypesCaught["goldenclick"]?.plus(1) ?: 1
                }

                // Golden Strays, hoard/stampede
                goldenStrayHoardPattern.matchMatcher(loreLine) {
                    profileStorage.goldenTypesCaught["stampede"] = profileStorage.goldenTypesCaught["stampede"]?.plus(1) ?: 1
                }

                //Asynchronously update to immediately reflect caught stray
                updateStraysDisplay()
                if (ChocolateFactoryAPI.inChocolateFactory) {
                    config.strayRabbitTrackerPosition.renderRenderables(straysDisplay, posLabel = "Stray Tracker")
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
        val profileStorage = profileStorage ?: return
        if (!LorenzUtils.inSkyBlock) return
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (event.slot == null || event.slot.slotIndex == -999) return
        if (!InventoryUtils.getItemsInOpenChest().any { it.slotNumber == event.slot.slotNumber }) return
        if (claimedStraysSlots.contains(event.slot.slotIndex)) return
        try {
            val clickedSlot = InventoryUtils.getItemsInOpenChest().first {
                it.hasStack && it.slotNumber == event.slot.slotNumber
            }
            val clickedStack = clickedSlot.stack
            val nameText = (if (clickedStack.hasDisplayName()) clickedStack.displayName else clickedStack.itemName)
            if (goldenStraySideDish.matches(nameText)) {
                claimedStraysSlots.add(event.slot.slotIndex)
                profileStorage.goldenTypesCaught["sidedish"] = profileStorage.goldenTypesCaught["sidedish"]?.plus(1) ?: 1
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
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        updateStraysDisplay()
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.strayRabbitTracker) return

        config.strayRabbitTrackerPosition.renderRenderables(straysDisplay, posLabel = "Stray Tracker")
    }

    private val rarityFormatMap = buildMap {
        put("common", "§fCommon§7: §r§f")
        put("uncommon", "§aUncommon§7: §r§a")
        put("rare", "§9Rare§7: §r§9")
        put("epic", "§5Epic§7: §r§5")
        put("legendary", "§6Legendary§7: §r§6")
    }

    private fun extractRarity(rabbitName: String) : String {
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

    private fun updateStraysDisplay() {
        val profileStorage = profileStorage ?: return
        val extraChocMs = profileStorage.straysExtraChocMs.values.sum().milliseconds
        val formattedExtraTime = extraChocMs.let { if (it == 0.milliseconds) "0s" else it.format() }

        straysDisplay = listOfNotNull(
            Renderable.hoverTips(
                "§6§lStray Tracker",
                tips = listOf("§a+§b${formattedExtraTime} §afrom strays§7")
            ),
            extractHoverableOfRarity("common"),
            extractHoverableOfRarity("uncommon"),
            extractHoverableOfRarity("rare"),
            extractHoverableOfRarity("epic"),
            extractHoverableOfRarity("legendary"),
        )
    }

    private fun extractHoverableOfRarity(rarity: String): Renderable? {
        val profileStorage = profileStorage ?: return null
        val caughtOfRarity = profileStorage.straysCaught[rarity]
        val caughtString = caughtOfRarity?.toString() ?: return null

        val rarityExtraChocMs = profileStorage.straysExtraChocMs[rarity]?.milliseconds
        val extraChocFormat = rarityExtraChocMs?.format() ?: ""

        val lineHeader = rarityFormatMap[rarity] ?: ""
        val lineFormat = "${lineHeader}${caughtString}"

        return if (rarityExtraChocMs == null) {
            Renderable.string(lineFormat)
        } else {
            val productionTip = "§a+§b${extraChocFormat} §aof production§7" + if (rarity == "legendary") extractGoldenTypesCaught() else ""
            Renderable.hoverTips(
                Renderable.string(lineFormat),
                tips = productionTip.split("\n")
            )
        }
    }

    private fun extractGoldenTypesCaught(): String {
        val profileStorage = profileStorage ?: return ""
        val goldenList = mutableListOf<String>()
        profileStorage.goldenTypesCaught["sidedish"]?.let {
            goldenList.add("§b$it §6Side Dish" + if (it > 1) "es" else "")
        }
        profileStorage.goldenTypesCaught["jackpot"]?.let {
            goldenList.add("§b$it §6Chocolate Jackpot" + if (it > 1) "s" else "")
        }
        profileStorage.goldenTypesCaught["mountain"]?.let {
            goldenList.add("§b$it §6Chocolate Mountain" + if (it > 1) "s" else "")
        }
        profileStorage.goldenTypesCaught["dorado"]?.let {
            goldenList.add((if (it == 3) "§a" else "§b") + "$it§7/§a3 §6El Dorado §7Sighting" + if (it > 1) "s" else "")
        }
        profileStorage.goldenTypesCaught["stampede"]?.let {
            goldenList.add("§b$it §6Stampede" + if (it > 1) "s" else "")
        }
        profileStorage.goldenTypesCaught["goldenclick"]?.let {
            goldenList.add("§b$it §6Golden Click" + if (it > 1) "s" else "")
        }
        return if (goldenList.size == 0) "" else ("\n" + goldenList.joinToString("\n"))
    }
}
