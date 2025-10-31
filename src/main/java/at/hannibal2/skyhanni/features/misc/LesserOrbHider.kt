package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.hanni.utils.LocationUtils.distanceTo
import at.hannibal2.hanni.utils.SkullTextureHolder
import at.hannibal2.hanni.utils.collection.CollectionUtils
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.EnumParticleTypes

@HanniModule
object LesserOrbHider {

    private val config get() = HanniMod.feature.misc
    private val enabled = config.lesserOrbHider
    private val hiddenEntities = CollectionUtils.weakReferenceList<EntityArmorStand>()

    private val LESSER_TEXTURE by lazy { SkullTextureHolder.getTexture("LESSER_ORB") }

    @HandleEvent(onlyOnSkyblock = true)
    fun onArmorChange(event: EntityEquipmentChangeEvent<EntityArmorStand>) {
        val entity = event.entity
        val itemStack = event.newItemStack ?: return

        if (event.isHand && itemStack.getSkullTexture() == LESSER_TEXTURE) {
            hiddenEntities.add(entity)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<EntityArmorStand>) {
        if (!enabled) return

        if (event.entity in hiddenEntities) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!enabled) return
        if (event.type != EnumParticleTypes.REDSTONE) return

        for (armorStand in hiddenEntities) {
            val distance = armorStand.distanceTo(event.location)
            if (distance < 4) {
                event.cancel()
            }
        }
    }
}
