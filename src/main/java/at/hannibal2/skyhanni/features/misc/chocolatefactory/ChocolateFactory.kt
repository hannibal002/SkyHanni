package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
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

    private val patternGroup = RepoPattern.group("misc.chocolatefactory")
    private val rabbitAmountPattern by patternGroup.pattern(
        "rabbitamount",
        "Rabbit \\S+ - \\[(?<amount>\\d+)].*"
    )
    private val unclaimedRewardsPattern by patternGroup.pattern(
        "unclaimedrewards",
        "§7§aYou have \\d+ unclaimed rewards?!"
    )
    private val chocolateAmountPattern by patternGroup.pattern(
        "chocolateamount",
        "(?<chocolate>[\\d,]+) Chocolate"
    )
    private val upgradeTierPattern by patternGroup.pattern(
        "upgradetier",
        ".*\\s(?<tier>[IVXL]+)"
    )
    private val barnAmountPattern by patternGroup.pattern(
        "barnamount",
        "§7Your Barn: §.(?<rabbits>\\d+)§7/§.(?<max>\\d+) Rabbits"
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
        if (!inChocolateFactory) return

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
                    val rabbits = group("rabbits").formatInt()
                    val max = group("max").formatInt()
                    barnFull = max - rabbits <= 5

                    if (barnFull && lastBarnFullWarning.passedSince() > 30.seconds) {
                        ChatUtils.chat("§cYour barn is almost full! §7(${rabbits}/${max}). §cUpgrade it so they don't get crushed")
                        SoundUtils.playBeepSound()
                        lastBarnFullWarning = SimpleTimeMark.now()
                    }
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
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
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
        if (!inChocolateFactory) return
        val slot = event.slot ?: return
        if (slot.slotNumber == 40) return

        event.makePickblock()
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
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

    private fun List<String>.getUpgradeCost(): Int? {
        val nextLine = this.nextAfter({ it == "§7Cost" }) ?: return null
        chocolateAmountPattern.matchMatcher(nextLine.removeColor()) {
            return group("chocolate").formatInt()
        }
        return null
    }

    // todo when getting a new rabbit in chat send a message saying what the current and max rabbit count is
}
