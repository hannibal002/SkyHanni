package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.UserLuckCalculateEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.isTopInventory
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object UserLuckBreakdown {
    private var inMiscStats = false
    private var replaceSlot: Int? = null
    private var itemCreateCoolDown = SimpleTimeMark.farPast()
    private var skillCalcCoolDown = SimpleTimeMark.farPast()

    private val storage get() = ProfileStorageData.playerSpecific
    private val config get() = SkyHanniMod.feature.misc

    private val mainLuckID = Items.ENDER_PEARL
    private const val MAIN_LUCK_NAME = "§a✴ SkyHanni User Luck"

    private var fillerItem: ItemStack? = null
    private val fillerID = Blocks.BLACK_STAINED_GLASS_PANE.asItem()

    private var showAllStats = true

    /**
     * REGEX-TEST: §7Show all stats: §aYes
     * REGEX-TEST: §7Show all stats: §cNope
     */
    private val showAllStatsPattern by RepoPattern.pattern(
        "misc.statsbreakdown.showallstats",
        "§7Show all stats: §.(?<toggle>.*)",
    )

    private const val LUCK_TOOLTIP = "§5§o §a✴ SkyHanni User Luck §f"
    private var inCustomBreakdown = false

    private val validItemSlots = (10..53).filter { it !in listOf(17, 18, 26, 27, 35, 36) && it !in 44..53 }
    private val invalidItemSlots = (0..53).filter { it !in validItemSlots }

    private val skillOverflowLuck = mutableMapOf<SkillType, Int>()

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!config.userLuck) return
        if (event.inventory !is SimpleContainer) return
        if (!inMiscStats) return

        if (event.slot == replaceSlot && !inCustomBreakdown) {
            val luckEvent = getOrPostLuckEvent()
            if (luckEvent.getTotalLuck() == 0f && !showAllStats) return
            event.replace(luckEvent.mainLuckStack)
            return
        }
        if (inCustomBreakdown) {
            getOrPostLuckEvent()
            checkItemSlot(event)
        }
    }

    private fun checkItemSlot(event: ReplaceItemEvent) {
        when (event.slot) {
            48, 49 -> return

            in validItemSlots -> {
                val luckEvent = getOrPostLuckEvent()
                val stack = luckEvent.getStack(event.slot)
                if (stack == null) {
                    event.remove()
                    return
                }
                event.replace(stack)
            }

            in invalidItemSlots -> {
                var stack = fillerItem
                if (stack == null) {
                    stack = createFillerItem()
                    fillerItem = stack
                }
                event.replace(stack)
                return
            }
        }
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventoryName != "Your Stats Breakdown") {
            inMiscStats = false
            return
        }
        val inventoryName = event.inventoryItems[4]?.hoverName.formattedTextCompatLeadingWhiteLessResets().orEmpty()
        if (inventoryName != "§dMiscellaneous Stats") return
        inMiscStats = true
        replaceSlot = findValidSlot(event.inventoryItemsWithNull)
        val showAllStatsLore = event.inventoryItems[50]?.getLore() ?: listOf("")
        for (line in showAllStatsLore) {
            showAllStatsPattern.matchMatcher(line) {
                showAllStats = when (group("toggle")) {
                    "Yes" -> true
                    else -> false
                }
            }
        }
        return
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inMiscStats = false
        inCustomBreakdown = false
    }

    private fun findValidSlot(input: Map<Int, ItemStack?>): Int? {
        for (slot in input.keys) {
            if (slot !in validItemSlots && slot < 44) continue
            if (input[slot].orNull() == null) {
                return slot
            }
        }
        return null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipTextEvent) {
        if (!config.userLuck) return
        event.slot ?: return
        if (!event.slot.isTopInventory()) return
        if (skillCalcCoolDown.passedSince() > 3.seconds) {
            skillCalcCoolDown = SimpleTimeMark.now()
            calcSkillLuck()
        }
        when (InventoryUtils.openInventoryName()) {
            "Your Equipment and Stats" -> equipmentMenuTooltip(event)
            "Your Stats Breakdown" -> statsBreakdownLoreTooltip(event)
            "SkyBlock Menu" -> skyblockMenuTooltip(event)
        }
    }

    private fun equipmentMenuTooltip(event: ToolTipTextEvent) {
        event.slot ?: return
        if (event.slot.containerSlot != 25) return
        val luckEvent = getOrPostLuckEvent()
        val totalLuck = luckEvent.getTotalLuck()
        if (totalLuck == 0f && !showAllStats) return

        val lastIndex = event.toolTip.indexOfLast { it.string.removeColor().isEmpty() }
        if (lastIndex == -1) return

        val luckString = tryTruncateFloat(totalLuck)
        event.toolTip.add(lastIndex, "$LUCK_TOOLTIP$luckString")
    }

    private fun statsBreakdownLoreTooltip(event: ToolTipTextEvent) {
        event.slot ?: return
        if (!inMiscStats) return
        if (inCustomBreakdown && event.slot.containerSlot == 48) {
            event.toolTip[1] = Component.nullToEmpty("§7To Your Stats Breakdown")
        }
        if (event.slot.containerSlot != 4 || inCustomBreakdown) return
        val luckEvent = getOrPostLuckEvent()
        val totalLuck = luckEvent.getTotalLuck()
        if (totalLuck == 0f && !showAllStats) return

        val luckString = tryTruncateFloat(totalLuck)
        event.toolTip.add("$LUCK_TOOLTIP$luckString")
    }

    private fun skyblockMenuTooltip(event: ToolTipTextEvent) {
        event.slot ?: return
        if (event.slot.containerSlot != 13) return
        val luckEvent = getOrPostLuckEvent()
        val lastIndex = event.toolTip.indexOfLast { it.string.removeColor() == " and more..." }
        if (lastIndex == -1) return

        val luckString = tryTruncateFloat(luckEvent.getTotalLuck())
        event.toolTip.add(lastIndex, "$LUCK_TOOLTIP$luckString")
    }

    private fun tryTruncateFloat(input: Float): String {
        val string = input.addSeparators()
        return if (string.endsWith(".0")) return string.dropLast(2)
        else string
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.userLuck) return
        if (!inMiscStats) return
        val luckEvent = getOrPostLuckEvent()
        if (luckEvent.getTotalLuck() == 0f && !showAllStats) return

        if (inCustomBreakdown && event.slotId != 49) event.cancel()
        when (event.slotId) {
            replaceSlot -> {
                if (inCustomBreakdown) return
                event.cancel()
                inCustomBreakdown = true
            }

            48 -> {
                if (!inCustomBreakdown) return
                inCustomBreakdown = false
            }

            else -> return
        }
    }

    private fun createFillerItem(): ItemStack {
        return ItemUtils.createItemStack(
            fillerID,
            " ",
            listOf(),
            1,
            15,
        )
    }

    private fun createItemLore(type: String, luckInput: Float = 0f): Array<String> {
        calcSkillLuck()
        return when (type) {
            "mainMenu" -> {
                val luckString = tryTruncateFloat(luckInput.roundTo(2))
                if (luckInput == 0f) {
                    arrayOf(
                        "§7SkyHanni User Luck is the best stat.",
                        "",
                        "§7Flat: §a+$luckString✴",
                        "",
                        "§8You have none of this stat!",
                        "§eClick to view!",
                    )
                } else {
                    arrayOf(
                        "§7SkyHanni User Luck increases your",
                        "§7overall fortune around Hypixel SkyBlock.",
                        "",
                        "§7(Disclaimer: May not affect real drop chances)",
                        "",
                        "§eClick to view!",
                    )
                }
            }

            "limbo" -> {
                val luckString = tryTruncateFloat(luckInput.roundTo(2))
                arrayOf(
                    "§8Action",
                    "",
                    "§7Value: §a+$luckString✴",
                    "",
                    "§8Gain more by going to Limbo,",
                    "§8and obtaining a higher Personal Best§8.",
                )
            }

            "skills" -> {
                val luckString = skillOverflowLuck.values.sum()
                val firstHalf = arrayOf(
                    "§8Grouped",
                    "",
                    "§7Value: §a+$luckString✴",
                    "",
                )
                val secondHalf = arrayOf(
                    "§8Stats from your overflow skills.",
                    "§8Obtain more each 5 overflow levels!",
                )
                val sourcesList = mutableListOf<String>()
                for ((skillType, luck) in skillOverflowLuck) {
                    if (luck == 0) continue
                    sourcesList.add(" §a+$luck✴ §f${skillType.displayName} Skill")
                }
                val finalList = mutableListOf<String>()
                finalList.addAll(firstHalf)
                if (sourcesList.isNotEmpty()) {
                    finalList.addAll(sourcesList)
                    finalList.add("")
                }
                finalList.addAll(secondHalf)
                finalList.toTypedArray()
            }

            "jerry" -> {
                val luckString = tryTruncateFloat(luckInput.roundTo(2))
                arrayOf(
                    "§8Elected Mayor",
                    "",
                    "§7Value: §a+$luckString✴",
                    "",
                    "§8Stats from the currently elected",
                    "§8mayor. Proof that voting does matter.",
                )
            }

            else -> arrayOf("")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(95, "misc.userluckEnabled", "misc.userLuck")
    }

    private fun calcSkillLuck() {
        val storage = ProfileStorageData.profileSpecific?.skillData ?: return
        skillOverflowLuck.clear()
        for ((skillType, skillInfo) in storage) {
            val level = skillInfo.level
            val overflow = skillInfo.overflowLevel
            val luck = ((overflow - level) / 5) * 50
            skillOverflowLuck.addOrPut(skillType, luck)
        }
    }

    private var userLuckEvent: UserLuckCalculateEvent? = null

    private fun getOrPostLuckEvent(): UserLuckCalculateEvent {
        val oldLuckEvent = userLuckEvent
        if (oldLuckEvent != null && itemCreateCoolDown.passedSince() < 3.seconds) return oldLuckEvent
        itemCreateCoolDown = SimpleTimeMark.now()
        val userLuckEvent = UserLuckCalculateEvent()
        userLuckEvent.post()
        this.userLuckEvent = userLuckEvent
        return userLuckEvent
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun skillLuck(event: UserLuckCalculateEvent) {
        val lore = createItemLore("skills")
        val luck = skillOverflowLuck.values.sum().toFloat()
        event.addLuck(luck)
        val stack = ItemUtils.createItemStack(
            Items.DIAMOND_SWORD,
            "§a✴ Category: Skills",
            lore,
        )
        event.addItem(stack)
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun limboLuck(event: UserLuckCalculateEvent) {
        val luck = storage?.limbo?.userLuck ?: 0f
        event.addLuck(luck)
        val stack = ItemUtils.createItemStack(
            Items.ENDER_PEARL,
            "§a✴ Limbo Personal Best",
            createItemLore("limbo", luck),
        )
        event.addItem(stack)
    }

    @HandleEvent
    fun modernLuck(event: UserLuckCalculateEvent) {
        if (PlatformUtils.IS_LEGACY) return
        event.addLuck(5f)
        val stack = ItemUtils.createItemStack(
            Items.TRIDENT,
            "§a✴ Modern Minecraft Bonus",
            arrayOf(
                "§8Minecraft",
                "",
                "§7Value: §a+5✴",
                "",
                "§8We put a lot of effort into updating SkyHanni.",
                "§8This is a small bonus for using modern Minecraft.",
            ),
        )
        event.addItem(stack)
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun jerryLuck(event: UserLuckCalculateEvent) {
        if (!Perk.STATSPOCALYPSE.isActive) return
        val jerryLuck = event.getTotalLuck() * .1f
        event.addLuck(jerryLuck)
        val stack = ItemUtils.createItemStack(
            Items.PAPER,
            "§a✴ Statspocalypse",
            createItemLore("jerry", jerryLuck),
        )
        event.addItem(stack)
    }

    @HandleEvent(priority = 100)
    fun totalLuck(event: UserLuckCalculateEvent) {
        val totalLuck = event.getTotalLuck()
        event.mainLuckStack = ItemUtils.createItemStack(
            mainLuckID,
            "$MAIN_LUCK_NAME §f${tryTruncateFloat(totalLuck)}",
            createItemLore("mainMenu", totalLuck),
        )
    }

    fun getTotalUserLuck(): Float {
        val luckEvent = getOrPostLuckEvent()
        return luckEvent.getTotalLuck()
    }
}
