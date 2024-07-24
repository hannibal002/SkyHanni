package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.util.stripControlCodes
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object MagicalPowerDisplay {
    private val config get() = SkyHanniMod.feature.inventory.magicalPower
    private var contactAmount: Int
        get() = ProfileStorageData.profileSpecific?.abiphoneContactAmount ?: -1
        private set(value) { ProfileStorageData.profileSpecific?.abiphoneContactAmount = value }

    /*
    * REGEX-TEST: Accessory Bag
    * REGEX-TEST: Accessory Bag (1/75)
    * REGEX-TEST: Accessory Bag (909/394294)
    * REGEX-TEST: Auctions Browser
    * REGEX-TEST: Auctions: "ligma"
    * REGEX-TEST: Auctions: ""sugoma""
    * */
    private val acceptedInvPattern by RepoPattern.pattern(
        "inv.acceptable",
        """^(Accessory Bag(?: \(\d+/\d+\))?|Auctions Browser|Manage Auctions|Auctions: ".*"?)$"""
    )

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return
        if (!acceptedInvPattern.matches(InventoryUtils.openInventoryName().stripControlCodes())) return

        val item = event.stack
        val rarity = item.isAccessory() ?: return
        val itemID = item.getInternalNameOrNull() ?: return

        var endMP = rarity.toMP() ?: run {
            ErrorManager.skyHanniError(
                "Unknown rarity '$rarity' for item '${item.displayName}§7'"
            )
        }
        if (itemID == "HEGEMONY_ARTIFACT".asInternalName())
            endMP *= 2
        if (itemID == "RIFT_PRISM".asInternalName())
            endMP = 11
        if (item.isAbicase())
            endMP += contactAmount / 2
        event.stackTip = "${if (config.colored) rarity.chatColorCode else "§7"}${endMP}"
    }

    @SubscribeEvent
    fun onInventoryOpened(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (event.inventoryName.startsWith("Abiphone")) {
            val theBookLore = event.inventoryItems[51]?.getLore() ?: return
            for (line in theBookLore) {
                val stripped = line.stripControlCodes()
                if (stripped.startsWith("Your contacts: ")) {
                    contactAmount = stripped.split(" ")[2].split("/")[0].toInt()
                    return
                }
            }
        }
    }

    private fun ItemStack.isAbicase(): Boolean {
        val id = this.getInternalNameOrNull() ?: return false
        return id.startsWith("ABICASE_")
    }

    private fun LorenzRarity.toMP(): Int? {
        return when (this) {
            LorenzRarity.COMMON, LorenzRarity.SPECIAL -> 3
            LorenzRarity.UNCOMMON, LorenzRarity.VERY_SPECIAL -> 5
            LorenzRarity.RARE -> 8
            LorenzRarity.EPIC -> 12
            LorenzRarity.LEGENDARY -> 16
            LorenzRarity.MYTHIC -> 22
            else -> null
        }
    }

    private fun ItemStack.isAccessory(): LorenzRarity? {
        val category = this.getItemCategoryOrNull() ?: return null
        if (category != ItemCategory.ACCESSORY && category != ItemCategory.HATCESSORY) return null
        return this.getItemRarityOrNull()
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
