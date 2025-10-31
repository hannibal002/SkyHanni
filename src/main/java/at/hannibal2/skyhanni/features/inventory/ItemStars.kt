package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.features.inventory.InventoryConfig.ItemNumberEntry.CRIMSON_ARMOR
import at.hannibal2.hanni.events.RenderItemTipEvent
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.features.inventory.ItemDisplayOverlayFeatures.isSelected
import at.hannibal2.hanni.features.nether.kuudra.KuudraApi.getKuudraTier
import at.hannibal2.hanni.features.nether.kuudra.KuudraApi.isKuudraArmor
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.RegexUtils.findMatcher
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getDungeonStarCount
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getStarCount
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@HanniModule
object ItemStars {

    private val config get() = HanniMod.feature.inventory

    private val patternGroup = RepoPattern.group("inventory.itemstars")

    /**
     * REGEX-TEST: §6Ancient Terror Leggings §d✪✪§6✪✪✪
     * REGEX-TEST: §dRenowned Burning Crimson Helmet §6✪✪✪✪✪
     */
    private val starPattern by patternGroup.pattern(
        "stars",
        "^(?<name>.+) (?<stars>(?:(?:§.)?✪)+)",
    )

    @HandleEvent(priority = HandleEvent.LOW)
    fun onTooltip(event: ToolTipEvent) {
        if (!isEnabled()) return
        val stack = event.itemStack
        if (stack.stackSize != 1) return
        val stars = stack.grabStarCount() ?: return
        starPattern.findMatcher(stack.displayName) {
            val name = group("name")
            event.toolTip[0] = "$name §c$stars✪"
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!CRIMSON_ARMOR.isSelected()) return
        val stack = event.stack
        if (stack.getInternalNameOrNull()?.isKuudraArmor() != true) return
        val stars = stack.grabStarCount() ?: return
        event.stackTip = stars.toString()
    }

    private fun ItemStack.grabStarCount(): Int? {
        val internalName = getInternalNameOrNull() ?: return null
        val baseStars = getDungeonStarCount() ?: getStarCount()
        if (!internalName.isKuudraArmor()) return baseStars
        val tier = internalName.getKuudraTier() ?: return baseStars
        return (baseStars ?: 0) + (tier - 1) * 10
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.itemStars
}
