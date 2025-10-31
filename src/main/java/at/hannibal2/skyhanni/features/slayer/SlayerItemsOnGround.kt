package at.hannibal2.hanni.features.slayer

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.collection.TimeLimitedCache
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.hanni.utils.render.WorldRenderUtils.exactLocation
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Items
import kotlin.time.Duration.Companion.seconds
//#if MC > 1.16
//$$ import net.minecraft.item.SpawnEggItem
//#endif

@HanniModule
object SlayerItemsOnGround {

    private val config get() = SlayerApi.config.itemsOnGround

    private val itemsOnGround = TimeLimitedCache<EntityItem, String>(2.seconds)

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return
        for (entityItem in EntityUtils.getEntitiesNextToPlayer<EntityItem>(15.0)) {
            val itemStack = entityItem.entityItem
            //#if MC < 1.16
            if (itemStack.item == Items.spawn_egg) continue
            //#else
            //$$ if (itemStack.item is SpawnEggItem) continue
            //#endif
            if (itemStack.getInternalName() == NeuInternalName.NONE) continue
            val (name, price) = SlayerApi.getItemNameAndPrice(itemStack.getInternalName(), itemStack.stackSize)
            if (config.minimumPrice > price) continue
            itemsOnGround[entityItem] = name
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

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled &&
        SlayerApi.isInCorrectArea && SlayerApi.hasActiveQuest()
}
