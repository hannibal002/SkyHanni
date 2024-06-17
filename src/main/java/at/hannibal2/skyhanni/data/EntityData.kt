package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object EntityData {

    private val maxHealthMap = mutableMapOf<EntityLivingBase, Int>()
    private val nametagCache = TimeLimitedCache<Entity, IChatComponent>(50.milliseconds)

    private val ignoredEntities = listOf(
        EntityArmorStand::class.java,
        EntityXPOrb::class.java,
        EntityItem::class.java,
        EntityItemFrame::class.java,
        EntityOtherPlayerMP::class.java,
        EntityPlayerSP::class.java,
    )

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        for (entity in EntityUtils.getEntities<EntityLivingBase>()) { // this completely ignores the ignored entities list?
            val maxHealth = entity.baseMaxHealth
            val oldMaxHealth = maxHealthMap.getOrDefault(entity, -1)
            if (oldMaxHealth != maxHealth) {
                maxHealthMap[entity] = maxHealth
                EntityMaxHealthUpdateEvent(entity, maxHealth.derpy()).postAndCatch()
            }
        }
    }

    @HandleEvent
    fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<Entity>) {
        maxHealthMap.remove(event.entity)
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        maxHealthMap.clear()
    }

    @HandleEvent
    fun onHealthUpdatePacket(event: PacketReceivedEvent) {
        val packet = event.packet

        if (packet !is S1CPacketEntityMetadata) return

        val watchableObjects = packet.func_149376_c() ?: return
        val entityId = packet.entityId

        val entity = EntityUtils.getEntityByID(entityId) ?: return
        if (entity.javaClass in ignoredEntities) return

        watchableObjects.find { it.dataValueId == 6 }
            ?.let {
                val health = (it.`object` as Float).toInt()
                if (entity is EntityWither && health == 300 && entityId < 0) return
                if (entity is EntityLivingBase) {
                    EntityHealthUpdateEvent(entity, health.derpy()).postAndCatch()
                }
            }
    }

    @JvmStatic
    fun getDisplayName(entity: Entity, ci: CallbackInfoReturnable<IChatComponent>) {
        ci.returnValue = postRenderNametag(entity, ci.returnValue)
    }

    @JvmStatic
    fun despawnEntity(entity: Entity) {
        EntityLeaveWorldEvent(entity).post()
    }

    private fun postRenderNametag(entity: Entity, chatComponent: IChatComponent) = nametagCache.getOrPut(entity) {
        val event = EntityDisplayNameEvent(entity, chatComponent)
        event.postAndCatch()
        event.chatComponent
    }
}
