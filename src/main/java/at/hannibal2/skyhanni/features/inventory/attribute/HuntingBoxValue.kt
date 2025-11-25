package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

@SkyHanniModule
object HuntingBoxValue {

    private val config get() = AttributeShardsData.config
    private val storage get() = ProfileStorageData.profileSpecific?.attributeShards

    private var display = emptyList<Renderable>()

    private var totalShards = 0
    private var totalInstantSell = 0L
    private var totalInstantBuy = 0L

    fun processInventory(slots: List<Slot>) {
        if (!config.huntingBoxValue) return

        totalShards = 0
        totalInstantSell = 0
        totalInstantBuy = 0

        val table = mutableListOf<DisplayTableEntry>()

        for (slot in slots) {
            val slotNumber = slot.slotNumber
            if (!isValidSlotNumber(slotNumber)) continue
            val stack = slot.stack.orNull() ?: continue
            processAttributeShardSlot(slotNumber, stack, table)
        }

        display = buildList {
            addString("§eHunting Box Value")

            if (table.isNotEmpty()) {
                add(RenderableUtils.fillScrollTable(table, padding = 5, itemScale = 0.7, height = 225, velocity = 5.0))
            } else {
                possiblyAddWarning()
            }

            addString("§7Total Attribute Shards: §a$totalShards")
            addString("§7Total Instant Sell Value: §6${totalInstantSell.addSeparators()}")
            addString("§7Total Instant Buy Value: §6${totalInstantBuy.addSeparators()}")
            addExportToSkyShardsButton()
        }
    }

    private fun MutableList<Renderable>.possiblyAddWarning() {
        InventoryUtils.getItemAtSlotIndex(10).orNull() ?: return

        addString("§cError detected!")
        addString("§cPlease run §e/shdebug repo§c to get debug information.")
        addString("§cThen send the data on discord.")
        addExportToSkyShardsButton()
    }

    private fun MutableList<Renderable>.addExportToSkyShardsButton() {
        if (!config.exportToSkyShards) return

        val clickable = Renderable.clickable(
            "§eExport to and open SkyShards",
            tips = listOf(
                "§7Click to copy your shard data to your clipboard,",
                "§7Then opens SkyShards in your browser.",
            ),
            onLeftClick = {
                exportToSkyShards()
            },
        )
        add(clickable)
    }

    private data class SkyShardsAttributeData(
        @Expose @SerializedName("hunting_box") val huntingBox: Map<String, Int>,
        @Expose @SerializedName("attribute_menu") val attributeMenu: Map<String, Int>,
    )

    private fun Map<String, Int>.toShardIds(): Map<String, Int> {
        return this.mapKeys { (key, _) ->
            AttributeShardsData.shardNameToAttributeInformation(key)?.shardId
                ?: ErrorManager.skyHanniError("Could not find shard ID for attribute shard with internal name $key")
        }
    }

    private fun exportToSkyShards() {
        val huntingBoxShards = storage?.map { it.key to it.value.amountInBox }?.toMap()?.filter { it.value > 0 }.orEmpty()
        val attributeMenuShards = storage?.map { it.key to it.value.amountSyphoned }?.toMap()?.filter { it.value > 0 }.orEmpty()
        val data = SkyShardsAttributeData(huntingBoxShards.toShardIds(), attributeMenuShards.toShardIds())
        val json = ConfigManager.gson.toJson(data)
        ClipboardUtils.copyToClipboard(json)
        OSUtils.openBrowser("https://skyshards.com/smart")
        ChatUtils.chat("§aCopied your attribute shard data to your clipboard and opened §dSkyShards§a.")
    }

    private fun processAttributeShardSlot(slotNumber: Int, stack: ItemStack, table: MutableList<DisplayTableEntry>) {
        val internalName = stack.getInternalNameOrNull() ?: return

        val amountOwned = AttributeShardsData.amountOwnedPattern.firstMatcher(stack.getLore()) {
            group("amount").formatInt()
        } ?: return
        totalShards += amountOwned

        val pricePerInstantSell = internalName.getPrice(ItemPriceSource.BAZAAR_INSTANT_SELL)
        val totalPriceInstantSell = pricePerInstantSell * amountOwned
        totalInstantSell += totalPriceInstantSell.toLong()

        val pricePerInstantBuy = internalName.getPrice(ItemPriceSource.BAZAAR_INSTANT_BUY)
        val totalPriceInstantBuy = pricePerInstantBuy * amountOwned
        totalInstantBuy += totalPriceInstantBuy.toLong()

        val hover = buildList {
            add(internalName.repoItemName)
            add("")
            add("§7Price per Instant Sell: §6${pricePerInstantSell.addSeparators()}")
            add("§7Price per Instant Buy: §6${pricePerInstantBuy.addSeparators()}")
            add("")
            add("§7Amount Owned: §a$amountOwned")
            add("§7Total Price Instant Sell: §6${totalPriceInstantSell.addSeparators()}")
            add("§7Total Price Instant Buy: §6${totalPriceInstantBuy.addSeparators()}")
        }

        table.add(
            DisplayTableEntry(
                "${internalName.repoItemName} §8x$amountOwned",
                "§6${totalPriceInstantSell.addSeparators()}",
                totalPriceInstantSell,
                internalName,
                hover,
                highlightsOnHoverSlots = listOf(slotNumber),
            ),
        )
    }

    private fun isValidSlotNumber(slot: Int): Boolean {
        if (slot < 9 || slot > 44) return false
        val modNine = slot % 9
        return modNine != 0 && modNine != 8
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    fun onRenderOverlay() {
        if (!config.huntingBoxValue) return
        if (!AttributeShardsData.huntingBoxInventory.isInside()) return

        config.huntingBoxValuePosition.renderRenderables(display, posLabel = "Hunting Box Value")
    }
}
