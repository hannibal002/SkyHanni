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

    private lateinit var luckItem: ItemStack
    private val itemID = "ENDER_PEARL".asInternalName()
    private val itemName = "§a✴ SkyHanni User Luck"

    private var isAdvanced = true
    private val isAdvancedPattern = RepoPattern.pattern(
        "misc.statsbreakdown.advanced",
        "§7Show all stats: §.(?<toggle>.*)"
    )

    private val luckTooltipString = "§5§o §a✴ SkyHanni User Luck §f"

    private val validItemSlots = (10..43).filter { it !in listOf(17, 18, 26, 27, 35, 36) }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory !is ContainerLocalMenu) return
        if (!inMiscStats) return
        if (event.slotNumber != replaceSlot) return

        if (event.slotNumber == replaceSlot) {
            val limboUserLuck = storage?.limbo?.userLuck ?: 0.0f
            if (limboUserLuck == 0.0f && !isAdvanced) return
            if (itemCreateCoolDown.passedSince() > 3.seconds) {
                itemCreateCoolDown = SimpleTimeMark.now()
                luckItem = Utils.createItemStack(
                    itemID.getItemStack().item,
                    "$itemName §f${limboUserLuck.round(1)}",
                    *createItemLore(limboUserLuck)
                )
            }
            event.replaceWith(luckItem)
        }
    }

    private fun createItemLore(luckInput: Float): Array<String> {
        return if (luckInput == 0.0f) { arrayOf(
                "§7SkyHanni User Luck is the best stat.",
                "",
                "§7Flat: §a+${luckInput.round(2)}✴",
                "",
                "§8You have none of this stat!",
                "§cDON'T §eclick to view!"
            )
        } else { arrayOf(
                "§7SkyHanni User Luck is the best stat.",
                "",
                "§7Flat: §a+${luckInput.round(2)}✴",
                "",
                "§cDON'T §eclick to view!"
            )
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
            val matcher = isAdvancedPattern.value.matcher(it)
            if (matcher.find()) {
                isAdvanced = when (matcher.group("toggle")) {
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
    }

    private fun findValidSlot(input: Map<Int, ItemStack>): Int {
        for (slot in input.keys) {
            if (slot !in validItemSlots) continue
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
                if (limboUserLuck == 0.0f && !isAdvanced) return

                val lastIndex = event.toolTip.indexOfLast { it == "§5§o" }
                if (lastIndex == -1) return

                event.toolTip.add(lastIndex, "$luckTooltipString${limboUserLuck.round(1)}")
            }
            "Your Stats Breakdown" -> {
                if (!inMiscStats) return
                if (event.slot.slotIndex != 4) return
                val limboUserLuck = storage?.limbo?.userLuck ?: 0.0f
                if (limboUserLuck == 0.0f && !isAdvanced) return

                event.toolTip.add("§5§o §a✴ SkyHanni User Luck §f${limboUserLuck.round(1)}")
            }
            "SkyBlock Menu" -> {
                if (event.slot.slotIndex != 13) return
                val limboUserLuck = storage?.limbo?.userLuck ?: 0.0f
                if (limboUserLuck == 0.0f) return

                val lastIndex = event.toolTip.indexOfLast { it == "§5§o" }
                if (lastIndex == -1) return

                event.toolTip.add(lastIndex, "$luckTooltipString${limboUserLuck.round(1)}")
            }
            else -> return
        }
    }
}