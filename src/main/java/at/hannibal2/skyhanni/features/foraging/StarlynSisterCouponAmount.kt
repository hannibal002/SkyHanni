package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.foraging.StarlynSisterDetector.createStarlynDetector
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventoryAndSacks
import at.hannibal2.skyhanni.utils.ItemUtils.createItemStack
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.itemType
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object StarlynSisterCouponAmount {

    private val config get() = SkyHanniMod.feature.foraging.starlynContest

    private var currentSisterType: StarlynSisterType? = null

    private var lastClick = SimpleTimeMark.farPast()
    private const val CUSTOM_STACK_LOCATION = 4
    private val DEBOUNCE_DELAY = 0.3.seconds
    private var couponAmountItemStack: SafeItemStack? = null

    private val emptyGlassItem = Blocks.STAINED_GLASS_PANE.black().asItem()

    private var itemReplaced: Boolean = false
    private var canReplace: Boolean = false

    private fun isEnabled() = config.starlynCouponAmount && starlynInventory.isInside()

    private val starlynInventory = createStarlynDetector(
        isEnabled = { config.starlynCouponAmount },
        setSisterType = { currentSisterType = it },
        onOpen = { event, sister ->
            generateCouponAmountItemStack(sister)

            if (event.inventoryItems[CUSTOM_STACK_LOCATION]?.itemType == emptyGlassItem) {
                canReplace = true
            } else {
                ErrorManager.logErrorStateWithData(
                    "Unexpected item found in slot $CUSTOM_STACK_LOCATION of Starlyn Shop",
                    "Unexpected item found in Starlyn Shop Coupon Amount slot",
                    "slot" to CUSTOM_STACK_LOCATION,
                    "found item" to event.inventoryItems[CUSTOM_STACK_LOCATION],
                    "expected item type" to emptyGlassItem,
                )
            }
        },
        onClose = {
            couponAmountItemStack = null
            canReplace = false
            itemReplaced = false
        },
    )

    private fun generateCouponAmountItemStack(sisterType: StarlynSisterType) {
        val lore = buildList {
            add("§8(From SkyHanni)")
            add("")
            add("§eClick to open Bazaar!")
        }
        couponAmountItemStack = createItemStack(
            Items.NAME_TAG,
            "${sisterType.couponName.repoItemName}s§7: §f${sisterType.couponName.getAmountInInventoryAndSacks()}",
            lore,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled() || event.slot != CUSTOM_STACK_LOCATION || !canReplace) return

        couponAmountItemStack?.let { stack ->
            event.replace(stack)
            itemReplaced = true
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || event.slotId != CUSTOM_STACK_LOCATION || !itemReplaced) return

        event.cancel()
        if (lastClick.passedSince() > DEBOUNCE_DELAY) {
            BazaarApi.searchForBazaarItem(currentSisterType?.couponName ?: return)
            lastClick = SimpleTimeMark.now()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onSackChange(event: SackChangeEvent) {
        if (!isEnabled()) return
        generateCouponAmountItemStack(currentSisterType ?: return)
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryUpdated() {
        if (!isEnabled()) return
        generateCouponAmountItemStack(currentSisterType ?: return)
    }
}
