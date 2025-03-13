package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.HEGEMONY_ARTIFACT
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.RIFT_PRISM
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.getAccessoryRarityOrNull
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.getBaseMagicalPower
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object MagicalPowerDisplay {
    private val config get() = SkyHanniMod.feature.inventory.stats.magicalPower
    private var contactAmount: Int?
        get() = ProfileStorageData.profileSpecific?.abiphoneContactAmount
        private set(value) {
            ProfileStorageData.profileSpecific?.abiphoneContactAmount = value
        }

    /**
     * REGEX-TEST: Accessory Bag
     * REGEX-TEST: Accessory Bag (1/75)
     * REGEX-TEST: Accessory Bag (909/394294)
     * REGEX-TEST: Auctions Browser
     * REGEX-TEST: Auctions: "ligma"
     * REGEX-TEST: Auctions: ""sugoma""
     * */
    private val acceptedInvPattern by RepoPattern.pattern(
        "inv.acceptable",
        "^(?:Accessory Bag(?: \\(\\d+\\/\\d+\\))?|Auctions Browser|Manage Auctions|Auctions: \".*\"?)$",
    )

    private val abiphoneGroup = RepoPattern.group("data.abiphone")

    /**
     * REGEX-TEST: Abiphone X Plus
     * REGEX-TEST: Abiphone X Plus Special Edition
     * REGEX-TEST: Abiphone XI Ultra Style
     * REGEX-TEST: Abiphone XII Mega Color
     * REGEX-TEST: Abiphone XIII Pro
     * REGEX-TEST: Abiphone XIV Enormous Purple
     * REGEX-TEST: Abiphone Flip
     * */
    private val abiphoneNamePattern by abiphoneGroup.pattern(
        "name",
        "Abiphone .*",
    )

    /**
     * REGEX-TEST: Your contacts: 0/0
     * REGEX-TEST: Your contacts: 1/75
     * REGEX-TEST: Your contacts: 52/60
     * */
    private val yourContactPattern by abiphoneGroup.pattern(
        "contacts",
        "Your contacts: (?<contacts>\\d+)\\/\\d+",
    )

    // Todo:
    //  A lot of this data collection should either get merged with or moved to AccessoryApi
    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!config.enabled) return
        if (!acceptedInvPattern.matches(InventoryUtils.openInventoryName().removeColor())) return

        val item = event.stack
        val rarity = item.getAccessoryRarityOrNull() ?: return
        val internalName = item.getInternalNameOrNull() ?: return

        var endMP = rarity.getBaseMagicalPower() ?: ErrorManager.skyHanniError(
            "Unknown rarity '$rarity' for item '${item.displayName}§7'",
        )

        when (internalName) {
            HEGEMONY_ARTIFACT -> endMP *= 2
            RIFT_PRISM -> endMP = 11
            else -> if (internalName.isAbicase()) endMP += (contactAmount ?: 0) / 2
        }

        event.stackTip = "${if (config.colored) rarity.chatColorCode else "§7"}$endMP"
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.enabled) return
        if (!abiphoneNamePattern.matches(event.inventoryName)) return

        val theBookLore = event.inventoryItems[51]?.getLore() ?: return
        for (line in theBookLore) {
            yourContactPattern.matchMatcher(line.removeColor()) {
                contactAmount = group("contacts").toInt()
                return
            }
        }
    }

    private fun NeuInternalName.isAbicase(): Boolean = AccessoryApi.isAbiCasePattern.matches(asString())
}
