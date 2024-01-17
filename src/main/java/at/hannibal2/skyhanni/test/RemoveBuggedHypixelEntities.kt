package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorWorldClient
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.makeAccessible
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.equalsIgnoreColor
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S13PacketDestroyEntities
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object RemoveBuggedHypixelEntities {

    private val neverUpdatedTime = (-1).seconds

    private var lastCheckTime = SimpleTimeMark.farPast()

    private var foundEntities = mutableListOf<Int>()
    private var lastFoundTime = mapOf<Int, SimpleTimeMark>()
    private var destroyedEntities = mutableListOf<Int>()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return

        checkNearby()

        if (event.repeatSeconds(1)) {
            collectFound()
        }
    }

    private fun collectFound() {
        val helpList = foundEntities.toList()
        foundEntities.clear()

        val lastFound = lastFoundTime.toMutableMap()
        val now = SimpleTimeMark.now()
        for (id in helpList) {
            lastFound[id] = now
        }
        lastFoundTime = lastFound
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        foundEntities.clear()
        lastFoundTime = emptyMap()
        destroyedEntities.clear()
    }

    @SubscribeEvent
    fun onReceiveCurrentShield(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

        if (packet is S13PacketDestroyEntities) {
            for (entityID in packet.entityIDs) {
                destroyedEntities.add(entityID)
            }
            return
        }

        detectEntityUpdate(packet)
    }

    private fun detectEntityUpdate(packet: Packet<*>) {
        val clazz = packet.javaClass
        if (!clazz.simpleName.contains("entity", ignoreCase = true)) return
        if (packet is S14PacketEntity) {
            tryToAdd(packet, S14PacketEntity::class.java)
        } else {
            tryToAdd(packet, clazz)
        }
    }

    private fun tryToAdd(packet: Packet<*>, clazz: Class<*>) {
        for (field in clazz.declaredFields) {
            val name = field.name
            if (name.equalsIgnoreColor("entityId")) {
                val id = field.makeAccessible().get(packet) as Int
                foundEntities.add(id)
            }
        }
    }

    private fun checkNearby() {
        if (!Minecraft.getMinecraft().thePlayer.isSneaking()) return
        if (lastCheckTime.passedSince() < 500.milliseconds) return

        lastCheckTime = SimpleTimeMark.now()
        collectFound()

        val entities = EntityUtils.getEntitiesNextToPlayer<Entity>(5.0)
                .filter { it != Minecraft.getMinecraft().thePlayer }

        filterInvalidEntities(entities)
        debug(entities)
    }

    private fun filterInvalidEntities(entities: Sequence<Entity>) {
        for (entity in entities) {
            val entityId = entity.entityId
            val lastUpdateTime = lastFoundTime[entityId]?.passedSince() ?: neverUpdatedTime
            val ticksExisted = entity.ticksExisted

            val name = entity.name
            if (name == "§c☣ §fBleeds: §8-") {
                if (ticksExisted > 20 * 4) {
                    removeInvalidEntity(entityId, "Minotaur ability name")
                }
            }

            if (lastUpdateTime == neverUpdatedTime) {
                if (ticksExisted > 20) {
                    removeInvalidEntity(entityId, "Mob never updated")
                }
            }
        }
    }

    private fun debug(list: Sequence<Entity>) {
        val result = calculateEntities(list)

        if (result.isEmpty()) {
            println("check (nothing)")
            return
        }

        println(" ")
        println("check:")
        for (s in result) {
            println(" $s")
        }
    }

    private fun calculateEntities(list: Sequence<Entity>): MutableList<String> {
        val result = mutableListOf<String>()
        for (entity in list) {
            val entityId = entity.entityId
            val lastUpdateTime = lastFoundTime[entityId]?.passedSince() ?: neverUpdatedTime
            val ticksExisted = entity.ticksExisted
            val npc = entity.isNPC()
            val worldClient = Minecraft.getMinecraft().theWorld
            val inLoadedEntityList = entity in worldClient.loadedEntityList
            val inDestroyedEntities = entityId in destroyedEntities
            val client = worldClient as AccessorWorldClient
            val entityList: Set<Entity> = client.getEntityList_skyhanni()
            val inEntityList = entity in entityList

            val name = entity.name
            val text = "'$name' | " +
                    lastUpdateTime + " | " +
                    "$ticksExisted ticks | " +
                    "npc:$npc | " +
                    "inLoadedEntityList:$inLoadedEntityList | " +
                    "inEntityList:$inEntityList | " +
                    "inDestroyedEntities:$inDestroyedEntities"
            result.add(text)
        }
        return result
    }

    private fun removeInvalidEntity(entityId: Int, reason: String) {
        Minecraft.getMinecraft().theWorld.removeEntityFromWorld(entityId)
        LorenzUtils.chat("Manually removing a broken Hypixel entity: $reason")
    }
}
