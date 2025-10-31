package at.hannibal2.hanni.features.fishing

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.fishing.FishingApi.isBait
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ConditionalUtils.transformIf
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.hanni.utils.SkullTextureHolder
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.collection.TimeLimitedCache
import at.hannibal2.hanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.hanni.utils.render.WorldRenderUtils.exactLocation
import net.minecraft.entity.item.EntityItem
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object ShowFishingItemName {

    private val config get() = HanniMod.feature.fishing.fishedItemName
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
            val itemStack = entityItem.entityItem.orNull() ?: continue
            // On 1.8 if the itemstack is null it returns stone instead
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
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return

        for ((item, text) in itemsOnGround) {
            val location = event.exactLocation(item).up(0.8)
            event.drawString(location, text)
        }
    }

    private fun inCorrectArea(): Boolean {
        if (IslandType.HUB.isCurrent()) {
            SkyBlockUtils.graphArea?.let {
                if (it.endsWith(" Atrium") || it.endsWith(" Museum")) return false
                if (it == "Fashion Shop" || it == "Shen's Auction") return false
            }
        }
        return !(IslandType.THE_END.isCurrent())
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled && FishingApi.holdingRod && inCorrectArea()
}
