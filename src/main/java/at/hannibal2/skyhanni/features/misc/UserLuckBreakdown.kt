package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class UserLuckBreakdown {
    private var inMiscStats = false
    private var replaceSlot = -1
    private var itemCreateCoolDown = SimpleTimeMark.farPast()

    private val storage get() = ProfileStorageData.playerSpecific

    private lateinit var mainLuckItem: ItemStack
    private val mainLuckID = "ENDER_PEARL".asInternalName()
    private val mainLuckName = "§a✴ SkyHanni User Luck"

    private lateinit var fillerItem: ItemStack
    private var fillerID = "STAINED_GLASS_PANE".asInternalName()
    private val fillerName = " "

    private lateinit var limboItem: ItemStack
    private var limboID = "ENDER_PEARL".asInternalName()
    private val limboName = "§a✴ Limbo Personal Best"

    private var showAllStats = true
    private val showAllStatsPattern = RepoPattern.pattern(
        "misc.statsbreakdown.showallstats",
        "§7Show all stats: §.(?<toggle>.*)"
    )

    private val luckTooltipString = "§5§o §a✴ SkyHanni User Luck §f"
    private var inCustomBreakdown = false

    private val validItemSlots = (10..53).filter { it !in listOf(17, 18, 26, 27, 35, 36) && it !in 44..53 }
    private val invalidItemSlots = (0..53).filter { it !in validItemSlots }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory !is ContainerLocalMenu) return
        if (!inMiscStats) return

        if (event.slotNumber == replaceSlot && !inCustomBreakdown) {
            val limboUserLuck = storage?.limbo?.userLuck ?: 0.0f
            if (limboUserLuck == 0.0f && !showAllStats) return
            if (itemCreateCoolDown.passedSince() > 3.seconds) {
                itemCreateCoolDown = SimpleTimeMark.now()
                createItems()
            }
            event.replaceWith(mainLuckItem)
            return
        }
        if (inCustomBreakdown) {
            if (itemCreateCoolDown.passedSince() > 3.seconds) {
                itemCreateCoolDown = SimpleTimeMark.now()
                createItems()
            }
            when (event.slotNumber) {
                48 -> return
                49 -> return
                10 -> {
                    event.replaceWith(limboItem)
                    return
                }
                in validItemSlots -> {
                    event.replaceWith(null)
                    return
                }
                in invalidItemSlots -> {
                    if (event.original.item == limboID.getItemStack().item) return
                    event.replaceWith(fillerItem)
                    return
                }
            }
        }
    }

    private fun createItemLore(type: String, luckInput: Float): Array<String> {
        when (type) {
            "mainMenu" -> {
                val luckString = tryTruncateFloat(luckInput.round(2))
                return if (luckInput == 0.0f) { arrayOf(
                    "§7SkyHanni User Luck is the best stat.",
                    "",
                    "§7Flat: §a+$luckString✴",
                    "",
                    "§8You have none of this stat!",
                    "§eClick to view!"
                )
                } else { arrayOf(
                    "§7SkyHanni User Luck is the best stat.",
                    "",
                    "§7Flat: §a+$luckString✴",
                    "",
                    "§eClick to view!"
                )
                }
            }
            "limbo" -> {
                val luckString = tryTruncateFloat(luckInput.round(2))
                return arrayOf(
                    "§8Action",
                    "",
                    "§7Value: §a+$luckString✴",
                    "",
                    "§8Gain more by going to Limbo,",
                    "§8and obtaining a higher §6Personal Best§8."
                )
            } else -> return arrayOf("")
        }
    }

    @SubscribeEvent
    fun openInventory(event: InventoryOpenEvent) {
        if (event.inventoryName != "Your Stats Breakdown") {
            inMiscStats = false
            return
        }
        val inventoryName = event.inventoryItems[4]?.name ?: ""
        if (inventoryName != "§dMisc Stats") return
        inMiscStats = true
        replaceSlot = findValidSlot(event.inventoryItems)
        val isAdvancedLore = event.inventoryItems[50]?.getLore() ?: listOf("")
        isAdvancedLore.forEach {
            val matcher = showAllStatsPattern.value.matcher(it)
            if (matcher.find()) {
                showAllStats = when (matcher.group("toggle")) {
                    "Yes" -> true
                    "Nope" -> false
                    else -> false
                }
            }
        }
        return
    }

    @SubscribeEvent
    fun closeInventory(event: InventoryCloseEvent) {
        inMiscStats = false
        inCustomBreakdown = false
    }

    private fun findValidSlot(input: Map<Int, ItemStack>): Int {
        for (slot in input.keys) {
            if (slot !in validItemSlots && slot < 44) continue
            val itemStack = input[slot]
            if (itemStack?.name == " ") {
                return slot
            }
        }
        return -1
    }

    @SubscribeEvent
    fun onHoverItem(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        when (event.slot.inventory.name) {
            "Your Equipment and Stats" -> {
                if (event.slot.slotIndex != 25) return
                val limboUserLuck = storage?.limbo?.userLuck ?: 0.0f
                if (limboUserLuck == 0.0f && !showAllStats) return

                val lastIndex = event.toolTip.indexOfLast { it == "§5§o" }
                if (lastIndex == -1) return

                val luckString = tryTruncateFloat(limboUserLuck.round(1))
                event.toolTip.add(lastIndex, "$luckTooltipString$luckString")
            }
            "Your Stats Breakdown" -> {
                if (!inMiscStats) return
                if (inCustomBreakdown && event.slot.slotIndex == 48) {
                    event.toolTip[1] = "§7To Your Stats Breakdown"
                }
                if (event.slot.slotIndex != 4) return
                val limboUserLuck = storage?.limbo?.userLuck ?: 0.0f
                if (limboUserLuck == 0.0f && !showAllStats) return

                val luckString = tryTruncateFloat(limboUserLuck.round(1))
                event.toolTip.add("§5§o §a✴ SkyHanni User Luck §f$luckString")
            }
            "SkyBlock Menu" -> {
                if (event.slot.slotIndex != 13) return
                val limboUserLuck = storage?.limbo?.userLuck ?: 0.0f
                if (limboUserLuck == 0.0f) return

                val lastIndex = event.toolTip.indexOfLast { it == "§5§o" }
                if (lastIndex == -1) return

                val luckString = tryTruncateFloat(limboUserLuck.round(1))
                event.toolTip.add(lastIndex, "$luckTooltipString$luckString")
            }
            else -> return
        }
    }

    private fun tryTruncateFloat(input: Float): String {
        val string = input.toString()
        return if (string.endsWith(".0")) return string.dropLast(2)
        else string
    }

    @SubscribeEvent
    fun onStackClick(event: SlotClickEvent) {
        if (!inMiscStats) return
        val limboUserLuck = storage?.limbo?.userLuck ?: 0.0f
        if (limboUserLuck == 0.0f && !showAllStats) return

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

    private fun createItems() {
        val limboUserLuck = storage?.limbo?.userLuck ?: 0.0f
        fillerItem = Utils.createItemStack(
            fillerID.getItemStack().item,
            fillerName,
            15,
        )
        val luckString = tryTruncateFloat(limboUserLuck.round(1))
        mainLuckItem = Utils.createItemStack(
            mainLuckID.getItemStack().item,
            "$mainLuckName §f$luckString",
            *createItemLore("mainMenu", limboUserLuck)
        )
        limboItem = Utils.createItemStack(
            limboID.getItemStack().item,
            limboName,
            *createItemLore("limbo", limboUserLuck)
        )
    }
}