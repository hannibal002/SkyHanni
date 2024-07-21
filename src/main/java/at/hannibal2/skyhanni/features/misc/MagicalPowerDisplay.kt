package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.util.stripControlCodes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object MagicalPowerDisplay {
    private val config get() = SkyHanniMod.feature.inventory.magicalPower

    private val MPMap = mapOf(
        LorenzRarity.COMMON to 3, LorenzRarity.SPECIAL to 3,
        LorenzRarity.UNCOMMON to 5, LorenzRarity.VERY_SPECIAL to 5,
        LorenzRarity.RARE to 8,
        LorenzRarity.EPIC to 12,
        LorenzRarity.LEGENDARY to 16,
        LorenzRarity.MYTHIC to 22
    )

    /*
    * REGEX-TEST: Accessory Bag
    * REGEX-TEST: Accessory Bag (1/75)
    * REGEX-TEST: Accessory Bag (909/394294)
    * REGEX-TEST: Auctions Browser
    * */
    private val acceptedInvPattern by RepoPattern.pattern(
        "inv.acceptable",
        """^(Accessory Bag(?: \(\d+/\d+\))?|Auctions Browser)$"""
    )

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return
        if (!acceptedInvPattern.matches(InventoryUtils.openInventoryName().stripControlCodes())) return

        val item = event.stack
        val rarity = item.getItemRarityOrNull() ?: return
        val itemID = item.getInternalNameOrNull() ?: return
        var isAccessory = false

        for (line in item.getLore()) {
            val plain = line.stripControlCodes()
            if (plain.contains("ACCESSORY") || line.contains("HATCESSORY")) isAccessory = true
            break
        }

        println("$itemID -> ${if (isAccessory) "acc" else "not acc"}")

        var endMP = MPMap[rarity] ?: run {
            ErrorManager.skyHanniError(
                "Unknown rarity '$rarity' for item '${item.displayName}§7'"
            )
        }

        if (itemID == "HEGEMONY_ARTIFACT".asInternalName())
            endMP *= 2
        if (itemID == "RIFT_PRISM".asInternalName())
            endMP = 11
        if (isAccessory)
            event.stackTip = "${if (config.colored) rarity else "§7"}${endMP}"
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
