package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactLocation
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Items
import kotlin.time.Duration.Companion.seconds
//#if MC > 1.16
import net.minecraft.item.SpawnEggItem
//#endif

@SkyHanniModule
object SlayerItemsOnGround {

    private val config get() = SlayerApi.config.itemsOnGround

    private val itemsOnGround = TimeLimitedCache<ItemEntity, String>(2.seconds)

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return
        for (entityItem in EntityUtils.getEntitiesNextToPlayer<ItemEntity>(15.0)) {
            val itemStack = entityItem.stack
            //#if MC < 1.16
            //$$ if (itemStack.item == Items.spawn_egg) continue
            //#else
            if (itemStack.item is SpawnEggItem) continue
            //#endif
            if (itemStack.getInternalName() == NeuInternalName.NONE) continue
            val (name, price) = SlayerApi.getItemNameAndPrice(itemStack.getInternalName(), itemStack.count)
            if (config.minimumPrice > price) continue
            itemsOnGround[entityItem] = name
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

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled &&
        SlayerApi.isInCorrectArea && SlayerApi.hasActiveSlayerQuest()
}
