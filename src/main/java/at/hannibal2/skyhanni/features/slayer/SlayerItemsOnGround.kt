package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import com.google.common.cache.CacheBuilder
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Items
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.TimeUnit

class SlayerItemsOnGround {
    private val config get() = SkyHanniMod.feature.slayer.itemsOnGround

    private var itemsOnGround =
        CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS)
            .build<EntityItem, Pair<LorenzVec, String>>()

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        if (!SlayerAPI.isInSlayerArea) return
        if (!SlayerAPI.hasActiveSlayerQuest()) return

        for (entityItem in EntityUtils.getEntities<EntityItem>()) {
            val location = event.exactLocation(entityItem).add(0.0, 0.8, 0.0)
            if (location.distance(LocationUtils.playerLocation()) > 15) continue

            val itemStack = entityItem.entityItem
            val name = itemStack.name ?: continue
            if (SlayerAPI.ignoreSlayerDrop(name)) continue
            // happens in spiders den sometimes
            if (itemStack.item == Items.spawn_egg) continue
            if (itemStack.getInternalName_old() == "") continue

            val (itemName, price) = SlayerAPI.getItemNameAndPrice(itemStack)
            if (config.minimumPrice > price) continue

            itemsOnGround.put(entityItem, location to itemName)
        }

        for ((location, text) in itemsOnGround.asMap().values) {
            event.drawString(location, text)
        }
    }
}