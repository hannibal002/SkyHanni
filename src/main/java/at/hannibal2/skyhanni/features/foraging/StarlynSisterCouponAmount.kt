package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventoryAndSacks
import at.hannibal2.skyhanni.utils.ItemUtils.createItemStack
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.seconds


@SkyHanniModule
object StarlynSisterCouponAmount {

    private val config get() = SkyHanniMod.feature.foraging.starlynContest

    private val sisterTypeMap = StarlynSisterType.entries.associateBy { it.inventoryName }
    private var currentSisterType: StarlynSisterType? = null

    private var lastClick = SimpleTimeMark.farPast()
    private const val CUSTOM_STACK_LOCATION = 4
    private val DEBOUNCE_DELAY = 0.3.seconds
    private var couponAmountItemStack: SafeItemStack? = null

    fun isEnabled() = config.starlynCouponAmount && starlynInventory.isInside()

    private val starlynInventory = InventoryDetector(
        checkInventoryName = sisterTypeMap.keys::contains,
        onOpenInventory = { event ->
            if (config.starlynCouponAmount) {
                sisterTypeMap[event.inventoryName]?.let { sister ->
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
            Items.NAME_TAG,
            "${sisterType.couponName.repoItemName}s§7: §f${sisterType.couponName.getAmountInInventoryAndSacks()}",
            lore,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled() || event.slot != CUSTOM_STACK_LOCATION) return
        if (!event.hasItem) return
        couponAmountItemStack?.let { event.replace(it) }
    }


    @HandleEvent(onlyOnSkyblock = true)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || event.slotId != CUSTOM_STACK_LOCATION) return

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
    private fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        generateCouponAmountItemStack(currentSisterType ?: return)
    }
}
