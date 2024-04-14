package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyblockSeason
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object ChocolateFactory {

    private val config get() = SkyHanniMod.feature.misc.chocolateFactory
    private val profileStorage get() = ProfileStorageData.profileSpecific?.chocolateFactory

    private val patternGroup = RepoPattern.group("misc.chocolatefactory")
    private val rabbitAmountPattern by patternGroup.pattern(
        "rabbit.amount",
        "Rabbit \\S+ - \\[(?<amount>\\d+)].*"
    )
    private val unclaimedRewardsPattern by patternGroup.pattern(
        "unclaimedrewards",
        "§7§aYou have \\d+ unclaimed rewards?!"
    )
    private val chocolateAmountPattern by patternGroup.pattern(
        "chocolate.amount",
        "(?<chocolate>[\\d,]+) Chocolate"
    )
    private val upgradeTierPattern by patternGroup.pattern(
        "upgradetier",
        ".*\\s(?<tier>[IVXL]+)"
    )
    private val barnAmountPattern by patternGroup.pattern(
        "barn.amount",
        "§7Your Barn: §.(?<rabbits>\\d+)§7/§.(?<max>\\d+) Rabbits"
    )
    private val rabbitFoundPattern by patternGroup.pattern(
        "rabbit.found",
        "§d§lNEW RABBIT! §6\\+\\d Chocolate §7and §6\\+0.\\d+x Chocolate §7per second!"
    )
    private val rabbitDuplicatePattern by patternGroup.pattern(
        "rabbit.duplicate",
        "§7§lDUPLICATE RABBIT! §6\\+[\\d,]+ Chocolate"
    )
    private val eggFoundPattern by patternGroup.pattern(
        "egg.found",
        "§d§lHOPPITY'S HUNT §r§dYou found a §r§.Chocolate (?<meal>\\w+) Egg.*"
    )
    private val sharedEggPattern by patternGroup.pattern(
        "egg.shared",
        ".*\\[SkyHanni] (?<meal>\\w+) Chocolate Egg located at x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)"
    )
    private val eggSpawnedPattern by patternGroup.pattern(
        "egg.spawned",
        "§d§lHOPPITY'S HUNT §r§dA §r§.Chocolate (?<meal>\\w+) Egg §r§dhas appeared!"
    )
    private val eggAlreadyCollectedPattern by patternGroup.pattern(
        "egg.alreadycollected",
        "§cYou have already collected this Chocolate (?<meal>\\w+) Egg§r§c! Try again when it respawns!"
    )
    private val noEggsLeftPattern by patternGroup.pattern(
        "egg.noneleft",
        "§cThere are no hidden Chocolate Rabbit Eggs nearby! Try again later!"
    )
    private val clickMeRabbitPattern by patternGroup.pattern(
        "rabbit.clickme",
        "§e§lCLICK ME!"
    )

    private var inChocolateFactory = false
    private var currentChocolate = 0L

    private var barnFull = false
    private var lastBarnFullWarning = SimpleTimeMark.farPast()

    private val slotsToHighlight: MutableSet<Int> = mutableSetOf()

    private var rabbitSlots = mapOf<Int, Int>()
    private var otherUpgradeSlots = setOf<Int>()
    private var noPickblockSlots = setOf<Int>()
    private var barnIndex = 34
    private var infoIndex = 13
    private var milestoneIndex = 53

    private var bestUpgrade: Int? = null
    private var bestRabbitUpgrade: String? = null

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.inventoryName != "Chocolate Factory") return
        inChocolateFactory = true
        DelayedRun.runDelayed(50.milliseconds) {
            updateInventoryItems(event.inventoryItems)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        clearData()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        clearData()
    }

    private fun clearData() {
        inChocolateFactory = false
        slotsToHighlight.clear()
        bestUpgrade = null
        bestRabbitUpgrade = null
    }

    private fun updateInventoryItems(inventory: Map<Int, ItemStack>) {
        val profileStorage = profileStorage ?: return

        bestUpgrade = null
        var bestAffordableUpgradeRatio = 0.0
        var bestPossibleUpgradeRatio = 0.0

        val chocolateItem = InventoryUtils.getItemsInOpenChest().find { it.slotIndex == infoIndex }?.stack ?: return
        chocolateAmountPattern.matchMatcher(chocolateItem.name.removeColor()) {
            currentChocolate = group("chocolate").formatLong()
        }
        slotsToHighlight.clear()
        for ((slotIndex, item) in inventory) {
            if (config.rabbitWarning && clickMeRabbitPattern.matches(item.name)) {
                println("tried to play sound")
                SoundUtils.playBeepSound()
            }

            val upgradeCost = item.getLore().getUpgradeCost() ?: continue

            if (slotIndex == barnIndex) {
                item.getLore().matchFirst(barnAmountPattern) {
                    profileStorage.currentRabbits = group("rabbits").formatInt()
                    profileStorage.maxRabbits = group("max").formatInt()

                    trySendBarnFullMessage()
                }
            }

            val canAfford = upgradeCost <= currentChocolate
            if (canAfford) {
                slotsToHighlight.add(slotIndex)
            }

            if ((slotIndex) in rabbitSlots) {
                val chocolateIncrease = rabbitSlots[slotIndex] ?: 0
                val upgradeRatio = upgradeCost.toDouble() / chocolateIncrease

                if (canAfford && upgradeRatio < bestAffordableUpgradeRatio || bestAffordableUpgradeRatio == 0.0) {
                    bestUpgrade = slotIndex
                    bestAffordableUpgradeRatio = upgradeRatio
                }
                if (upgradeRatio < bestPossibleUpgradeRatio || bestPossibleUpgradeRatio == 0.0) {
                    bestPossibleUpgradeRatio = upgradeRatio
                    bestRabbitUpgrade = item.name
                }
            }
        }
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inChocolateFactory) return

        updateInventoryItems(event.inventoryItems)
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        rabbitFoundPattern.matchMatcher(event.message) {
            val profileStorage = profileStorage ?: return
            profileStorage.currentRabbits += 1
            trySendBarnFullMessage()
            return
        }

        eggFoundPattern.matchMatcher(event.message) {
            val currentLocation = LocationUtils.playerLocation()
            EggLocations.eggFound()
            val meal = CakeMealTime.getMealByName(group("meal")) ?: run {
                ErrorManager.skyHanniError(
                    "Unknown meal: ${group("meal")}",
                    "message" to event.message
                )
            }
            meal.markClaimed()

            DelayedRun.runDelayed(3.seconds) {
                ChatUtils.clickableChat(
                    "Click here to share the location of this chocolate egg with the server!",
                    onClick = { EggLocations.shareNearbyEggLocation(currentLocation, meal) },
                    SimpleTimeMark.now() + 30.seconds
                )
            }
            return
        }

        sharedEggPattern.matchMatcher(event.message.removeColor()) {
            val x = group("x").formatInt()
            val y = group("y").formatInt()
            val z = group("z").formatInt()
            val eggLocation = LorenzVec(x, y, z)
            val meal = CakeMealTime.getMealByName(group("meal")) ?: run {
                ErrorManager.skyHanniError(
                    "Unknown meal: ${group("meal")}",
                    "message" to event.message
                )
            }

            // todo add waypoint
            // todo dont hide own message
//             event.blockedReason = "shared_egg"
            return
        }
        noEggsLeftPattern.matchMatcher(event.message) {
            CakeMealTime.allFound()
            return
        }
        eggAlreadyCollectedPattern.matchMatcher(event.message) {
            val meal = CakeMealTime.getMealByName(group("meal")) ?: run {
                ErrorManager.skyHanniError(
                    "Unknown meal: ${group("meal")}",
                    "message" to event.message
                )
            }
            meal.markClaimed()
        }
    }

    private fun trySendBarnFullMessage() {
        val profileStorage = profileStorage ?: return

        barnFull = profileStorage.maxRabbits - profileStorage.currentRabbits <= config.barnCapacityThreshold
        if (!barnFull) return

        if (lastBarnFullWarning.passedSince() < 30.seconds) return

        if (profileStorage.maxRabbits == -1) {
            ChatUtils.clickableChat(
                "Open your chocolate factory to see your barn's capacity status!",
                "cf"
            )
            return
        }

        ChatUtils.clickableChat(
            "§cYour barn is almost full! " +
                "§7(${profileStorage.currentRabbits}/${profileStorage.maxRabbits}). §cUpgrade it so they don't get crushed",
            "cf"
        )
        SoundUtils.playBeepSound()
        lastBarnFullWarning = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inChocolateFactory) return
        if (!config.showStackSizes) return

        val item = event.stack ?: return
        val itemName = item.name
        if (itemName != bestRabbitUpgrade) return

        event.drawSlotText(event.x + 18, event.y, "§6✦", .8f)
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inChocolateFactory) return
        if (!config.showStackSizes) return

        val item = event.stack
        val itemName = item.name.removeColor()
        val slotNumber = event.slot.slotNumber

        if (slotNumber in rabbitSlots) {
            rabbitAmountPattern.matchMatcher(itemName) {
                val rabbitTip = when (val rabbitAmount = group("amount").formatInt()) {
                    in (0..9) -> "$rabbitAmount"
                    in (10..74) -> "§a$rabbitAmount"
                    in (75..124) -> "§9$rabbitAmount"
                    in (125..174) -> "§5$rabbitAmount"
                    in (175..199) -> "§6$rabbitAmount"
                    200 -> "§d$rabbitAmount"
                    else -> "§c$rabbitAmount"
                }

                event.stackTip = rabbitTip
            }
        }
        if (slotNumber in otherUpgradeSlots) {
            upgradeTierPattern.matchMatcher(itemName) {
                event.stackTip = group("tier").romanToDecimal().toString()
            }
        }

        if (slotNumber == milestoneIndex) {
            item.getLore().matchFirst(unclaimedRewardsPattern) {
                event.stackTip = "§c!!!"
            }
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inChocolateFactory) return
        val slot = event.slot ?: return
        if (!config.useMiddleClick) return
        if (slot.slotNumber in noPickblockSlots) return

        event.makePickblock()
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inChocolateFactory) return
        if (!config.highlightUpgrades) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in slotsToHighlight) {
                if (slot.slotIndex == bestUpgrade) {
                    slot highlight LorenzColor.GREEN.addOpacity(200)
                } else {
                    slot highlight LorenzColor.GREEN.addOpacity(75)
                }
            }
            if (slot.slotIndex == barnIndex && barnFull) {
                slot highlight LorenzColor.RED
            }
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        CakeMealTime.checkClaimed()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showClaimedEggs) return
        if (ReminderUtils.isBusy()) return
        if (!isHoppityEvent()) return

        val displayList = mutableListOf<String>()
        displayList.add("§bUnfound Eggs:")

        for (meal in CakeMealTime.entries) {
            if (!meal.claimed) {
                displayList.add("§7 - ${meal.formattedName()}")
            }
        }
        if (displayList.size == 1) return

        config.position.renderStrings(displayList, posLabel = "Hoppity Eggs")
    }

    private fun List<String>.getUpgradeCost(): Long? {
        val nextLine = this.nextAfter({ it == "§7Cost" }) ?: return null
        chocolateAmountPattern.matchMatcher(nextLine.removeColor()) {
            return group("chocolate").formatLong()
        }
        return null
    }

    fun loadRepoData(data: HoppityEggLocationsJson) {
        rabbitSlots = data.rabbitSlots
        otherUpgradeSlots = data.otherUpgradeSlots
        noPickblockSlots = data.noPickblockSlots
        barnIndex = data.barnIndex
        infoIndex = data.infoIndex
        milestoneIndex = data.milestoneIndex
    }

    fun isHoppityEvent() = SkyblockSeason.getCurrentSeason() == SkyblockSeason.SPRING
}
