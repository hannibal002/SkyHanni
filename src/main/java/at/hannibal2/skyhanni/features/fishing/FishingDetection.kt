package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.fishing.FishingAPI.isFishingRod
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object FishingDetection {
    var isFishing = false
    private var holdingRod = false
    private var lastRodCastLocation: LorenzVec? = null
    private var lastRodCastTime = SimpleTimeMark.farPast()
    private var lastInAreaTime = SimpleTimeMark.farPast()

    private var lastSeaCreatureKillArea: LorenzVec? = null
    private var lastSeaCreatureKillAreaTime = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        // TODO correct rod type per island water/lava
        holdingRod = event.newItem.isFishingRod()
    }

    @SubscribeEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        lastRodCastLocation = LocationUtils.playerLocation()
        lastRodCastTime = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (inRodCastArea()) {
            lastInAreaTime = SimpleTimeMark.now()
        }

        if (lastInAreaTime.passedSince() < 5.seconds) {
            if (EntityUtils.getEntitiesNextToPlayer<EntityArmorStand>(5.0)
                    .filter { FishingAPI.seaCreatureCount(it) > 0 }.any()
            ) {
                lastSeaCreatureKillArea = LocationUtils.playerLocation()
                lastSeaCreatureKillAreaTime = SimpleTimeMark.now()
            }
        }

        isFishing = testIsFishing()
    }

    private fun testIsFishing(): Boolean {
        if (holdingRod) return true

        if (inRodCastArea()) return true

        if (lastRodCastTime.passedSince() < 5.seconds) return true

        if (inKillArea()) return true


        return false
    }

    private fun inRodCastArea(): Boolean {
        if (lastRodCastTime.passedSince() < 2.minutes) {
            lastRodCastLocation?.let {
                if (it.distanceToPlayer() < 10) {
                    return true
                }
            }
        }

        return false
    }

    private fun inKillArea(): Boolean {
        if (lastSeaCreatureKillAreaTime.passedSince() < 2.minutes) {
            lastSeaCreatureKillArea?.let {
                if (it.distanceToPlayer() < 10) {
                    return true
                }
            }
        }

        return false
    }
}
