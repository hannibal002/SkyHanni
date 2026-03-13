package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
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
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object MagicalPowerDisplay {
    private val config get() = SkyHanniMod.feature.inventory.magicalPower
    private var contactAmount: Int?
        get() = ProfileStorageData.profileSpecific?.abiphoneContactAmount
        private set(value) {
            ProfileStorageData.profileSpecific?.abiphoneContactAmount = value
        }

    private val hegemonyArtifact = "HEGEMONY_ARTIFACT".toInternalName()
    private val riftPrism = "RIFT_PRISM".toInternalName()

    /**
     * @regexTest Accessory Bag
     * @regexTest Accessory Bag (1/75)
     * @regexTest Accessory Bag (909/394294)
     * @regexTest Auctions Browser
     * @regexTest Auctions: "ligma"
     * @regexTest Auctions: ""sugoma""
     * */
    private val acceptedInvPattern by RepoPattern.pattern(
        "inv.acceptable",
        "^(?:Accessory Bag(?: \\(\\d+\\/\\d+\\))?|Auctions Browser|Manage Auctions|Auctions: \".*\"?)$",
    )

    private val abiphoneGroup = RepoPattern.group("data.abiphone")

    /**
     * @regexTest Abiphone X Plus
     * @regexTest Abiphone X Plus Special Edition
     * @regexTest Abiphone XI Ultra Style
     * @regexTest Abiphone XII Mega Color
     * @regexTest Abiphone XIII Pro
     * @regexTest Abiphone XIV Enormous Purple
     * @regexTest Abiphone Flip
     * */
    private val abiphoneNamePattern by abiphoneGroup.pattern(
        "name",
        "Abiphone .*",
    )

    /**
     * @regexTest Your contacts: 0/0
     * @regexTest Your contacts: 1/75
     * @regexTest Your contacts: 52/60
     * */
    private val yourContactPattern by abiphoneGroup.pattern(
        "contacts",
        "Your contacts: (?<contacts>\\d+)\\/\\d+",
    )

    @HandleEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return
        if (!acceptedInvPattern.matches(InventoryUtils.openInventoryName().removeColor())) return

        val item = event.stack
        val rarity = item.getAccessoryRarityOrNull() ?: return
        val internalName = item.getInternalNameOrNull() ?: return

        var endMP = rarity.toMP() ?: ErrorManager.skyHanniError(
            "Unknown rarity '$rarity' for item '${item.hoverName.formattedTextCompatLeadingWhiteLessResets()}§7'",
        )

        when (internalName) {
            hegemonyArtifact -> endMP *= 2
            riftPrism -> endMP = 11
            else -> if (internalName.isAbicase()) endMP += (contactAmount ?: 0) / 2
        }

        event.stackTip = "${if (config.colored) rarity.chatColorCode else "§7"}$endMP"
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!abiphoneNamePattern.matches(event.inventoryName)) return

        val theBookLore = event.inventoryItems[51]?.getLore() ?: return
        for (line in theBookLore) {
            yourContactPattern.matchMatcher(line.removeColor()) {
                contactAmount = group("contacts").toInt()
                return
            }
        }
    }

    private fun NeuInternalName.isAbicase(): Boolean = this.startsWith("ABICASE_")

    private fun LorenzRarity.toMP(): Int? = when (this) {
        LorenzRarity.COMMON, LorenzRarity.SPECIAL -> 3
        LorenzRarity.UNCOMMON, LorenzRarity.VERY_SPECIAL -> 5
        LorenzRarity.RARE -> 8
        LorenzRarity.EPIC -> 12
        LorenzRarity.LEGENDARY -> 16
        LorenzRarity.MYTHIC -> 22
        else -> null
    }

    private fun ItemStack.getAccessoryRarityOrNull(): LorenzRarity? {
        val category = this.getItemCategoryOrNull() ?: return null
        if (category != ItemCategory.ACCESSORY && category != ItemCategory.HATCESSORY) return null
        return this.getItemRarityOrNull()
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && !IslandType.THE_RIFT.isCurrent() && config.enabled
}
