package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.NpcData
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HolographicEntities
import at.hannibal2.skyhanni.utils.HolographicEntities.renderHolographicEntity
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.ClientMannequin
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.component.ResolvableProfile
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FakeNpcApi {

    private val npcs = mutableMapOf<String, ActiveNpc>()

    /**
     * Adds an NPC to the world.
     *
     * If an NPC with the same ID already exists, it is replaced.
     */
    fun addNpc(data: NpcData) {
        removeNpc(data.id)
        val rotation = rotationTowardsPlayer(data.position)
        val hologram = HolographicEntities.create(
            mannequinFromData(data),
            position = data.position,
            yaw = rotation.first,
            pitch = rotation.second,
        )

        npcs[data.id] = ActiveNpc(
            data = data,
            entity = hologram,
        )
    }

    fun removeNpc(id: String) {
        npcs.remove(id)
    }

    fun removeNpc(data: NpcData) {
        removeNpc(data.id)
    }

    fun getNpc(id: String): NpcData? {
        return npcs[id]?.data
    }

    fun getNpcs(): Collection<NpcData> {
        return npcs.values.map { it.data }
    }

    fun clear() {
        npcs.clear()
    }

    private fun rotationTowardsPlayer(position: LorenzVec): Pair<Float, Float> {
        val playerPos = LocationUtils.playerLocationOrNull() ?: return Pair(0f, 0f)
        val delta = playerPos - position

        val yaw = Math.toDegrees(
            atan2(-delta.x, delta.z)
        ).toFloat()

        val pitch = -Math.toDegrees(
            atan2(
                delta.y,
                sqrt(delta.x * delta.x + delta.z * delta.z),
            )
        ).toFloat()

        return Pair(yaw, pitch)
    }

    private fun showDialogue(npc: ActiveNpc) {
        if (npc.talking) return
        npc.talking = true
        val dialogue = npc.data.dialogue
        dialogue.forEachIndexed { index, line ->
            DelayedRun.runDelayed(index.seconds) {
                ChatUtils.chat(line)
                if (index == dialogue.lastIndex) {
                    npc.talking = false
                }
            }
        }
    }

    @HandleEvent
    private fun onTick() {
        val player = MinecraftCompat.localPlayerOrNull
        val playerEyePos = player?.let { LocationUtils.playerEyeLocation() }

        for ((_, activeNpc) in npcs) {
            val entity = activeNpc.entity
            entity.entity.tickCount++
            entity.entity.tick()
            entity.entity.aiStep()
            entity.updateLook(playerEyePos, maxDistance = 20.0)
            entity.moveTo(activeNpc.data.position)
        }
    }

    @HandleEvent
    private fun onItemClick() {
        if (npcs.isEmpty()) return
        val player = MinecraftCompat.localPlayerOrNull ?: return

        val eyePos = LocationUtils.playerEyeLocation()
        val look = player.getViewVector(1.0f).toLorenzVec()

        val maxDistance = 5.0

        var closestNpc: ActiveNpc? = null
        var closestDistanceSq = Double.MAX_VALUE

        for (npc in npcs.values) {
            val target = npc.data.position.add(x = 0, y = 1, z = 0)

            val toNpc = target - eyePos
            // How far along the player's look ray the NPC is.
            val distanceAlongRay = toNpc.dotProduct(look)
            if (distanceAlongRay !in 0.0..maxDistance) {
                continue
            }
            // Point on the player's look ray closest to the NPC.
            val closestPoint = eyePos + look.scale(distanceAlongRay)
            val distanceSq = closestPoint.distanceSq(target)
            // Approximate NPC hit radius.
            val hitRadius = 0.7
            if (distanceSq > hitRadius * hitRadius) {
                continue
            }
            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq
                closestNpc = npc
            }
        }

        closestNpc?.let {
            showDialogue(it)
        }
    }

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        for ((data, entity) in npcs.values) {
            event.renderHolographicEntity(entity, opacity = 1.0f)

            event.drawString(
                entity.position.add(
                    y = entity.entity.eyeHeight + 0.5,
                ),
                data.name,
            )
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        clear()
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shfakenpc") {
            description = "Fake NPC Testing Command"
            category = DEVELOPER_TEST
            literal("add") {
                arg("id", BrigadierArguments.string()) { idArg ->
                    argCallback("name", BrigadierArguments.string()) { name ->
                        val id = getArg(idArg)
                        val uuid = PlayerUtils.getRawUuid()
                        val data = NpcData(
                            id = id,
                            name = name,
                            skinPlayerUuid = uuid,
                            position = LocationUtils.playerLocation(),
                            dialogue = listOf("Hello, I am $name!", "Nice to meet you!"),
                        )
                        addNpc(data)
                    }
                }
            }
            literal("remove") {
                argCallback("id", BrigadierArguments.string(), getNpcs().map { it.id }) { id ->
                    removeNpc(id)
                }
            }
        }
    }

    fun mannequinFromData(data: NpcData): ClientMannequin {
        val profile = ResolvableProfile.createUnresolved(data.skinPlayerUuid)

        val entity = object : ClientMannequin(
            MinecraftCompat.localWorldOrThrow,
            Minecraft.getInstance().playerSkinRenderCache(),
        ) {
            init {
                entityData.set(DATA_PROFILE, profile)
            }

            override fun getSkin(): PlayerSkin {
                return Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).playerSkin()
            }

            override fun getProfile(): ResolvableProfile = profile
        }
        return entity
    }

    private data class ActiveNpc(
        val data: NpcData,
        val entity: HolographicEntities.HolographicEntity<ClientMannequin>,
        var talking: Boolean = false,
    )
}
