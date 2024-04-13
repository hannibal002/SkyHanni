package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
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
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
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
    private val eggFoundPattern by patternGroup.pattern(
        "egg.found",
        "§d§lHOPPITY'S HUNT §r§dYou found a.*"
    )
    private val sharedEggPattern by patternGroup.pattern(
        "egg.shared",
        ".*\\[SkyHanni] Chocolate egg located at x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)"
    )

    private var inChocolateFactory = false
    private var currentChocolate = 0L

    private var barnFull = false
    private var lastBarnFullWarning = SimpleTimeMark.farPast()

    private val slotsToHighlight: MutableSet<Int> = mutableSetOf()
    private val otherUpgradeSlots = setOf(
        28, 34, 38, 39, 41, 42
    )

    private var bestUpgrade: Int? = null

    // todo update slot highlight and chocolate here as well
    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.inventoryName != "Chocolate Factory") return
        inChocolateFactory = true
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
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inChocolateFactory) return
        val profileStorage = profileStorage ?: return

        bestUpgrade = null
        var bestUpgradeRatio = 0.0

        val chocolateItem = InventoryUtils.getItemsInOpenChest().find { it.slotIndex == 13 }?.stack ?: return
        chocolateAmountPattern.matchMatcher(chocolateItem.name.removeColor()) {
            currentChocolate = group("chocolate").formatLong()
        }

        slotsToHighlight.clear()
        for ((slotIndex, item) in event.inventoryItems) {
            val upgradeCost = item.getLore().getUpgradeCost() ?: continue

            if (slotIndex == 34) {
                item.getLore().matchFirst(barnAmountPattern) {
                    profileStorage.currentRabbits = group("rabbits").formatInt()
                    profileStorage.maxRabbits = group("max").formatInt()

                    trySendBarnFullMessage()
                }
            }
            if (upgradeCost < currentChocolate) {
                slotsToHighlight.add(slotIndex)

                if ((slotIndex) in 29..33) {
                    val upgradeRatio = upgradeCost.toDouble() / (slotIndex - 28)
                    if (upgradeRatio < bestUpgradeRatio || bestUpgradeRatio == 0.0) {
                        bestUpgrade = slotIndex
                        bestUpgradeRatio = upgradeRatio
                    }
                }
            }
        }
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
            DelayedRun.runDelayed(7.seconds) {
                ChatUtils.clickableChat(
                    "Click here to share the location of this chocolate egg with the server!",
                    onClick = { EggLocations.shareNearbyEggLocation(currentLocation) },
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
            // todo add waypoint
            // todo dont hide own message
//             event.blockedReason = "shared_egg"
            return
        }
    }

    private fun trySendBarnFullMessage() {
        val profileStorage = profileStorage ?: return

        barnFull = profileStorage.maxRabbits - profileStorage.currentRabbits <= 5
        if (!barnFull) return

        if (lastBarnFullWarning.passedSince() < 30.seconds) return
        ChatUtils.chat(
            "§cYour barn is almost full! " +
                "§7(${profileStorage.currentRabbits}/${profileStorage.maxRabbits}). §cUpgrade it so they don't get crushed"
        )
        SoundUtils.playBeepSound()
        lastBarnFullWarning = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inChocolateFactory) return
        if (!config.showStackSizes) return

        val item = event.stack
        val itemName = item.name.removeColor()
        val slotNumber = event.slot.slotNumber

        if (slotNumber in 29..33) {
            rabbitAmountPattern.matchMatcher(itemName) {
                val rabbitTip = when (val rabbitAmount = group("amount").formatInt()) {
                    in (0..9) -> "$rabbitAmount"
                    in (10..74) -> "§a$rabbitAmount"
                    in (75..124) -> "§9$rabbitAmount"
                    // todo get more data for colours
                    // next on (manager is §5)
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

        if (slotNumber == 53) {
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
        if (slot.slotNumber == 39) return

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
            if (slot.slotIndex == 34 && barnFull) {
                slot highlight LorenzColor.RED
            }
        }
    }

    private fun List<String>.getUpgradeCost(): Long? {
        val nextLine = this.nextAfter({ it == "§7Cost" }) ?: return null
        chocolateAmountPattern.matchMatcher(nextLine.removeColor()) {
            return group("chocolate").formatLong()
        }
        return null
    }
}
