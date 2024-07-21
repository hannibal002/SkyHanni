package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.util.stripControlCodes
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object MagicalPowerDisplay {
    private val config get() = SkyHanniMod.feature.inventory.magicalPower
    private val colored get() = config.colored
    private fun isEnabled() = config.enabled && LorenzUtils.inSkyBlock

    private val MPMap = mapOf(
        AccessoryRarity.COMMON to 3, AccessoryRarity.SPECIAL to 3,
        AccessoryRarity.UNCOMMON to 5, AccessoryRarity.VERY_SPECIAL to 5,
        AccessoryRarity.RARE to 8,
        AccessoryRarity.EPIC to 12,
        AccessoryRarity.LEGENDARY to 16,
        AccessoryRarity.MYTHIC to 22
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

    /*
    * REGEX-TEST: LEGENDARY ACCESSORY
    * REGEX-TEST: a UNCOMMON ACCESSORY a
    * REGEX-TEST: a RARE ACCESSORY a
    * REGEX-TEST: RARE DUNGEON ACCESSORY
    * REGEX-TEST: RARE ACCESSORY
    * REGEX-TEST: COMMON ACCESSORY
    * REGEX-TEST: a EPIC DUNGEON ACCESSORY a
    * */
    private val accessoryLorePattern by RepoPattern.pattern(
        "accessory.lore",
        """^a?\s*(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC)\s*(?:DUNGEON\s*)?ACCESSORY\s*a?$"""
    )

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return
        if (!acceptedInvPattern.matches(InventoryUtils.openInventoryName().stripControlCodes())) return

        val item = event.stack
        val rarity = item.isSkyblockAccessory() ?: return
        val itemName = item.displayName.stripControlCodes()

        var endMP = MPMap[rarity]!!
        if (itemName == "Hegemony Artifact") {
            endMP *= 2
        }
        if (itemName == "Rift Prism") {
            endMP = 11
        }

        event.stackTip = "${if (colored) rarity else "§7"}${endMP}"
    }

    private fun ItemStack.isSkyblockAccessory(): AccessoryRarity? {
        val lore = this.getLore()
        for (line in lore) {
            val stripped = line.stripControlCodes()

            if (stripped == "SPECIAL HATCESSORY") {
                return AccessoryRarity.SPECIAL
            } else if (stripped == "a VERY SPECIAL HATCESSORY a") {
                return AccessoryRarity.VERY_SPECIAL
            }

            accessoryLorePattern.matchMatcher(stripped) {
                val rarityGroup = group(1) ?: return@matchMatcher null
                when (rarityGroup) {
                    "COMMON" -> return AccessoryRarity.COMMON
                    "UNCOMMON" -> return AccessoryRarity.UNCOMMON
                    "RARE" -> return AccessoryRarity.RARE
                    "EPIC" -> return AccessoryRarity.EPIC
                    "LEGENDARY" -> return AccessoryRarity.LEGENDARY
                    "MYTHIC" -> return AccessoryRarity.MYTHIC
                    else -> return@matchMatcher null
                }
            }
        }
        return null
    }

    private enum class AccessoryRarity(val code: String) {
        COMMON("§f"),
        UNCOMMON("§a"),
        RARE("§9"),
        EPIC("§5"),
        LEGENDARY("§6"),
        MYTHIC("§d"),
        SPECIAL("§c"),
        VERY_SPECIAL("§c");

        override fun toString(): String {
            return code
        }
    }
}
