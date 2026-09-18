package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.NpcData
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.FakeEntityIdProvider
import at.hannibal2.skyhanni.utils.HolographicEntities
import at.hannibal2.skyhanni.utils.HolographicEntities.renderHolographicEntity
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.StringUtils.parseUUID
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.toLorenzVec
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.ClientMannequin
import net.minecraft.world.item.component.ResolvableProfile
import java.util.Base64
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FakeNpcAPI {

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
        for ((data, entity) in npcs.values) {
            entity.entity.tick()
            entity.entity.aiStep()
            val position = data.position
            val rotation = rotationTowardsPlayer(data.position)
            entity.moveTo(
                position = position,
                yaw = rotation.first,
                pitch = rotation.second,
            )
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
                        val skin = SkullTextureHolder.ALEX_SKIN_TEXTURE
                        val data = NpcData(
                            id = id,
                            name = name,
                            skin = skin,
                            position = LocationUtils.playerLocation(),
                            dialogue = listOf("Hello, I am $name!", "Nice to meet you!"),
                        )
                        addNpc(data)
                    }
                }
            }
            literal("remove") {
                argCallback("id", BrigadierArguments.string(), getNpcs().map { it.id} ) { id ->
                    removeNpc(id)
                }
            }
        }
    }

    fun mannequinFromData(data: NpcData): ClientMannequin {
        val profile = playerProfileFromBase64(data.skin)

        val entity = object : ClientMannequin(
            MinecraftCompat.localWorldOrThrow,
            Minecraft.getInstance().playerSkinRenderCache(),
        ) {
            //? if >= 26.2 {
            init {
                id = FakeEntityIdProvider.getNextId()
            }
            //?}

            override fun getProfile(): ResolvableProfile = profile
        }
        entity.updateSkin()
        return entity
    }

    private data class ActiveNpc(
        val data: NpcData,
        val entity: HolographicEntities.HolographicEntity<ClientMannequin>,
        var talking: Boolean = false,
    )

    // TODO: Figure out how to convert the full skin data into a PlayerSkin directly
    private fun playerProfileFromBase64(textureData: String): ResolvableProfile {
        val decodedBytes = Base64.getDecoder().decode(textureData)
        val jsonString = String(decodedBytes, Charsets.UTF_8)
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
        val profileId = parseUUID(jsonObject.get("profileId").asString)
        return ResolvableProfile.createUnresolved(profileId)
    }
}
