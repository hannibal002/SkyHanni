package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.CRIMSON_ARMOR
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.inventory.ItemDisplayOverlayFeatures.isSelected
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi.getArmorKuudraTier
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi.isKuudraArmor
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDungeonStarCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getStarCount
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object ItemStars {

    private val config get() = SkyHanniMod.feature.inventory

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
    fun onTooltip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        val stack = event.itemStack
        if (stack.count != 1) return
        val stars = stack.grabStarCount() ?: return
        starPattern.findMatcher(stack.hoverName.formattedTextCompatLeadingWhiteLessResets()) {
            val name = group("name")
            event.toolTip[0] = Component.literal("$name §c$stars✪")
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
        val tier = internalName.getArmorKuudraTier() ?: return baseStars
        return (baseStars ?: 0) + (tier - 1) * 10
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.itemStars
}
