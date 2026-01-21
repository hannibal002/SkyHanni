package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.collection.CollectionUtils
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object LesserOrbHider {

    private fun isEnabled() = SkyHanniMod.feature.misc.lesserOrbHider
    private val hiddenEntities = CollectionUtils.weakReferenceList<ArmorStand>()

    private val LESSER_TEXTURE by lazy { SkullTextureHolder.getTexture("LESSER_ORB") }

    @HandleEvent(onlyOnSkyblock = true)
    fun onArmorChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        val entity = event.entity
        val itemStack = event.newItemStack ?: return

        if (event.isHand && itemStack.getSkullTexture() == LESSER_TEXTURE) {
            hiddenEntities.add(entity)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<ArmorStand>) {
        if (!isEnabled()) return

        if (event.entity in hiddenEntities) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (event.type != ParticleTypes.DUST) return

        for (armorStand in hiddenEntities) {
            val distance = armorStand.distanceTo(event.location)
            if (distance < 4) {
                event.cancel()
            }
        }
    }
}
