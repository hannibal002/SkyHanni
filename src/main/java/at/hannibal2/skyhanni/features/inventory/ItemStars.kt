package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.CRIMSON_ARMOR
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.inventory.ItemDisplayOverlayFeatures.isSelected
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDungeonStarCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getUpgradeLevel
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.max

class ItemStars {

    private val config get() = SkyHanniMod.feature.inventory

    private var armorNames = listOf<String>()
    private var tiers = mapOf<String, Int>()
    private val armorParts = listOf("Helmet", "Chestplate", "Leggings", "Boots")

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!isEnabled()) return
        val stack = event.itemStack ?: return
        if (stack.stackSize != 1) return
        val name = stack.name ?: return

        var stars: Int

        if (usesMasterStars()) {
            stars = stack.getDungeonStarCount() ?: -1
        } else {
            stars = stack.getUpgradeLevel() ?: 0

            val isKuudraArmor = armorNames.any { name.contains(it) } && armorParts.any { name.contains(it) }
            if (isKuudraArmor && config.starType == InventoryConfig.StarType.ALLSTAR)
                stars += tiers.entries.find { name.contains(it.key) }?.value ?: 0
        }

        if (stars > 0) {
            val displayName = name.replace("§.[✪➊➋➌➍➎]".toRegex(), "").trim()

            if (usesMasterStars()) {
                val masterStars = max(0, stars - 5)
                val normalStars = 5 - masterStars
                event.toolTip[0] = "$displayName ${"§c✪".repeat(masterStars) + "§6✪".repeat(normalStars)}"
            } else {
                event.toolTip[0] = "$displayName §c$stars✪"
            }
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ItemsJson>("Items")
        armorNames = data.crimson_armors
        tiers = data.crimson_tiers
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!CRIMSON_ARMOR.isSelected()) return
        val stack = event.stack
        val name = stack.name ?: return

        var number = stack.getUpgradeLevel() ?: 0
        val isKuudraArmor = armorNames.any { name.contains(it) } && armorParts.any { name.contains(it) }
        if (isKuudraArmor)
            number += (tiers.entries.find { name.contains(it.key) }?.value ?: 0)

        if (number > 0) {
            event.stackTip = number.toString()
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.starType != InventoryConfig.StarType.OFF

    private fun usesMasterStars() = config.starType == InventoryConfig.StarType.MASTERSTAR
}
