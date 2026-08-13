package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventoryAndSacks
import at.hannibal2.skyhanni.utils.ItemUtils.createItemStack
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.seconds


@SkyHanniModule
object StarlynSisterCouponAmount {

    private val config get() = SkyHanniMod.feature.foraging.starlynContest
    private val sisterInventoryNames = StarlynSisterType.entries.map { it.inventoryName }.toSet()
    private var currentSisterType: StarlynSisterType? = null
    private var lastClick = SimpleTimeMark.farPast()
    private const val CUSTOM_STACK_LOCATION = 4
    private inline val NAME_TAG_ITEM get() = Items.NAME_TAG
    private val DEBOUNCE_DELAY = 0.3.seconds
    private var couponAmountItemStack: SafeItemStack? = null


    private val starlynInventory = InventoryDetector(
        checkInventoryName = sisterInventoryNames::contains,
        onOpenInventory = { event ->
            if (config.starlynCouponAmount) {
                StarlynSisterType.entries.find { it.inventoryName == event.inventoryName }?.let { sister ->
                    currentSisterType = sister
                    generateCouponAmountItemStack(sister)
                }
            }
        },
        onCloseInventory = {
            currentSisterType = null
            couponAmountItemStack = null
        },
    )

    private fun generateCouponAmountItemStack(sisterType: StarlynSisterType) {
        val lore = buildList {
            add("§8(From SkyHanni)")
            add("")
            add("§eClick to open Bazaar!")
        }
        couponAmountItemStack = createItemStack(
            NAME_TAG_ITEM,
            "${sisterType.couponName.repoItemName}s§7: §f${sisterType.couponName.getAmountInInventoryAndSacks()}",
            lore,
        )
    }

    @HandleEvent
    private fun replaceItem(event: ReplaceItemEvent) {
        if (!starlynInventory.isInside() || event.slot != CUSTOM_STACK_LOCATION) return
        couponAmountItemStack?.let { event.replace(it) }
    }


    @HandleEvent
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        val sisterType = currentSisterType
        if (event.slotId != CUSTOM_STACK_LOCATION || sisterType == null) return

        event.cancel()
        if (lastClick.passedSince() > DEBOUNCE_DELAY) {
            BazaarApi.searchForBazaarItem(sisterType.couponName)
            lastClick = SimpleTimeMark.now()
        }
    }
}
