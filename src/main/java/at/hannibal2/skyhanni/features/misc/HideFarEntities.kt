package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HideFarEntities {
    private val config get() = SkyHanniMod.feature.dev.hideFarEntities

    private var ignored = emptySet<Int>()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (isEnabled()) {
            val maxAmount = config.maxAmount.coerceAtLeast(1)
            val minDistance = config.minDistance.coerceAtLeast(3)
            ignored = EntityUtils.getAllEntities()
                .map { it to it.distanceToPlayer() }
                .toMap()
                .filter { it.value > minDistance }
                .sorted().keys.drop(maxAmount)
                .map { it.entityId }.toSet()

        }
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (isEnabled() && event.entity.entityId in ignored) {
            event.cancel()
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
