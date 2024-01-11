package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class LesserOrbHider {
    private val config get() = SkyHanniMod.feature.misc

    private val hideParticles = mutableMapOf<EntityArmorStand, SimpleTimeMark>()

    private val lesserTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjgzMjM2NjM5NjA3MDM2YzFiYTM5MWMyYjQ2YTljN2IwZWZkNzYwYzhiZmEyOTk2YTYwNTU1ODJiNGRhNSJ9fX0="

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!isEnabled()) return

        val entity = event.entity

        if (entity !is EntityArmorStand) return

        if (entity.inventory[0]?.getSkullTexture() == lesserTexture) {
            event.isCanceled = true
            hideParticles[entity] = SimpleTimeMark.now()
            return
        }
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return

        hideParticles.values.removeIf { it.passedSince() > 100.milliseconds }

        val packetLocation = event.location
        for (armorStand in hideParticles.keys) {
            val distance = packetLocation.distance(armorStand.getLorenzVec())
            if (distance < 4) {
                if (event.type == EnumParticleTypes.REDSTONE) {
                    event.isCanceled = true
                }
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.lesserOrbHider
}