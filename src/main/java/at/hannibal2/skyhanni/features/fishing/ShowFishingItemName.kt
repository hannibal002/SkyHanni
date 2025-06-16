package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi.isBait
import at.hannibal2.skyhanni.features.misc.IslandAreas
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import net.minecraft.entity.item.EntityItem
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object ShowFishingItemName {

    private val config get() = SkyHanniMod.feature.fishing.fishedItemName
    private val itemsOnGround = TimeLimitedCache<EntityItem, String>(750.milliseconds)

    // Textures taken from Skytils - moved to REPO
    private val cheapCoins by lazy {
        setOf(
            SkullTextureHolder.getTexture("COINS_1"),
            SkullTextureHolder.getTexture("COINS_2"),
        )
    }

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return
        for (entityItem in EntityUtils.getEntitiesNextToPlayer<EntityItem>(15.0)) {
            val itemStack = entityItem.entityItem
            // Hypixel sometimes replaces the bait item midair with a stone
            if (itemStack.displayName.removeColor() == "Stone") continue
            var text = ""

            val isBait = itemStack.isBait()
            if (isBait && !config.showBaits) continue

            if (itemStack.getSkullTexture() in cheapCoins) {
                text = "§6Coins"
            } else {
                val name = itemStack.displayName.transformIf({ isBait }) { "§7" + this.removeColor() }
                text += if (isBait) "§c§l- §r" else "§a§l+ §r"

                val size = itemStack.stackSize
                if (size != 1) text += "§7x$size §r"
                text += name
            }

            itemsOnGround[entityItem] = text
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        for ((item, text) in itemsOnGround) {
            val location = event.exactLocation(item).up(0.8)
            event.drawString(location, text)
        }
    }

    private fun inCorrectArea(): Boolean {
        if (IslandType.HUB.isCurrent()) {
            IslandAreas.currentAreaName.let {
                if (it.endsWith(" Atrium") || it.endsWith(" Museum")) return false
                if (it == "Fashion Shop" || it == "Shen's Auction") return false
            }
        }
        return !(IslandType.THE_END.isCurrent())
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled && FishingApi.holdingRod && inCorrectArea()
}
