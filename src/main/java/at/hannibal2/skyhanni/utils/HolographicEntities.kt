package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.entity.EntityTransparencyManager
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.hooks.activeHolographicEntities
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.TimeUtils.inWholeTicks
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.associateNotNull
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.zombie.Zombie
import kotlin.math.cos
import kotlin.math.sin
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

/**
 * Utility for creating fake entities without an associated world to avoid contaminating the world state.
 */
@SkyHanniModule
object HolographicEntities {

    private var debugHologram: HolographicEntity<Zombie>? = null
    private var debugHologramTransparency: Float = 1f

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdebughologram") {
            description = "Spawns a holographic zombie 5 blocks in front of you for testing"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { spawnDebugHologram() }
            argCallback("transparency", BrigadierArguments.integer(0, 100)) { transparency ->
                spawnDebugHologram(transparency / 100f)
            }
            literalCallback("stop") {
                description = "Removes the debug hologram"
                simpleCallback {
                    debugHologram = null
                    ChatUtils.chat("Removed debug hologram", replaceSameMessage = true)
                }
            }
        }
    }

    private fun spawnDebugHologram(transparency: Float = 1f) {
        val player = MinecraftCompat.localPlayerOrNull ?: return
        val yaw = Math.toRadians(player.yRot.toDouble())
        val pos = LorenzVec(
            player.x - sin(yaw) * 5,
            player.y,
            player.z + cos(yaw) * 5,
        )
        val base = entityHoloBases[Zombie::class] ?: ErrorManager.skyHanniError(
            "HolographicEntityDebug: Zombie not found in entityHoloBases (size=${entityHoloBases.size})"
        )
        @Suppress("UNCHECKED_CAST")
        debugHologram = (base as HolographicBase<Zombie>).instance(pos, player.yRot)
        debugHologramTransparency = transparency
    }


    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        val hologram = debugHologram ?: return
        event.renderHolographicEntity(hologram, opacity = debugHologramTransparency)
    }

    /**
     * An instance of a holographic entity. Maintains a minimal controlled state,
     * which has just enough information for rendering and basic manipulations, such as
     * interpolated positioning. The underlying [entity] should not be accessed directly.
     */
    class HolographicEntity<T : LivingEntity> internal constructor(
        val entity: T,
        var position: LorenzVec,
        var yaw: Float,
    ) {
        var isChild: Boolean = false
        var lastPosition: LorenzVec = position
        var lastYaw: Float = yaw
        val createdAt = SimpleTimeMark.now()
        internal var cachedRenderState: EntityRenderState? = null

        val monotonicProgress get() = createdAt.passedSince().inWholeTicks

        /**
         * Should be called exactly once per tick or never over the lifetime of this [HolographicEntity].
         */
        fun moveTo(position: LorenzVec, yaw: Float, isTeleport: Boolean = false) {
            if (isTeleport) {
                this.lastYaw = yaw
                this.lastPosition = position
            } else {
                this.lastYaw = this.yaw
                this.lastPosition = this.position
            }
            this.position = position
            this.yaw = yaw
        }

        fun interpolatedPosition(partialTicks: Float): LorenzVec =
            lastPosition.slope(position, partialTicks.toDouble())

        fun interpolatedYaw(partialTicks: Float): Float =
            interpolateRotation(lastYaw, yaw, partialTicks)
    }

    /**
     * Template for a [HolographicEntity]. This class exists as a guard for
     * [HolographicEntity] to prevent untested entities with potential NPEs
     * being instantiated.
     */
    class HolographicBase<T : LivingEntity> internal constructor(internal val entityType: EntityType<T>) {
        fun instance(position: LorenzVec, yaw: Float): HolographicEntity<T>? {
            val level = Minecraft.getInstance().level ?: return null
            val entity = entityType.create(level, EntitySpawnReason.COMMAND) ?: return null
            return HolographicEntity(entity, position, yaw)
        }
    }

    private var _internalEntityHoloBases: Map<KClass<out LivingEntity>, HolographicBase<out LivingEntity>>? = null
    val entityHoloBases: Map<KClass<out LivingEntity>, HolographicBase<out LivingEntity>> get() = _internalEntityHoloBases ?: run {
        val level = Minecraft.getInstance().level ?: return@run emptyMap()
        BuiltInRegistries.ENTITY_TYPE.associateNotNull type@{ entityType ->
            // Create a throwaway instance only to determine the KClass key.
            val testEntity: LivingEntity = runCatching {
                entityType.create(level, EntitySpawnReason.COMMAND)
            }.getOrNull() as? LivingEntity ?: return@type null
            @Suppress("UNCHECKED_CAST")
            testEntity::class to HolographicBase(entityType as EntityType<LivingEntity>)
        }.also { _internalEntityHoloBases = it }
    }

    fun <T : LivingEntity> getFilteredEntityHoloBases(
        vararg classes: KClass<out T>
    ): Map<KClass<out LivingEntity>, HolographicBase<out LivingEntity>> =
        entityHoloBases.filterKeys { clazz -> classes.any { it.isSuperclassOf(clazz) } }

    private fun interpolateRotation(last: Float, next: Float, progress: Float): Float {
        var direction: Float = next - last
        while (direction < -180f) direction += 360f
        while (direction >= 180f) direction -= 360f
        return last + progress * direction
    }

    /**
     * Render a fake [HolographicEntity]. To render a fully opaque entity, set [opacity] to `1F`.
     */
    fun <T : LivingEntity> SkyHanniRenderWorldEvent.renderHolographicEntity(
        holographicEntity: HolographicEntity<T>,
        opacity: Float = 0.3f,
    ) {
        val entity = holographicEntity.entity
        val mobPosition = holographicEntity.interpolatedPosition(partialTicks)
        val interpolatedYaw = holographicEntity.interpolatedYaw(partialTicks)

        // Populate entity fields that extractRenderState will read.
        // These are safe to set because HolographicBase entities are never ticked.
        entity.yRot = interpolatedYaw
        entity.yRotO = interpolatedYaw
        entity.yBodyRot = interpolatedYaw
        entity.yBodyRotO = interpolatedYaw
        entity.yHeadRot = interpolatedYaw
        entity.yHeadRotO = interpolatedYaw

        val client = Minecraft.getInstance()
        @Suppress("UNCHECKED_CAST")
        val renderer = client.entityRenderDispatcher?.getRenderer(entity) as? EntityRenderer<T, EntityRenderState> ?: return
        val gameRenderer = client.gameRenderer ?: return
        val entityRenderState = holographicEntity.cachedRenderState
            ?: renderer.createRenderState()?.also { holographicEntity.cachedRenderState = it }
            ?: return
        val cameraRenderState = gameRenderer.levelRenderState.cameraRenderState ?: return
        val cameraPos = cameraRenderState.pos
        val submitNodeCollector = gameRenderer.featureRenderDispatcher.submitNodeStorage ?: return
        renderer.extractRenderState(entity, entityRenderState, partialTicks)
        entityRenderState.`skyhanni$setEntity`(entity)
        (entityRenderState as? LivingEntityRenderState)?.isBaby = holographicEntity.isChild
        client.level?.let { level ->
            entityRenderState.lightCoords = LevelRenderer.getLightColor(level, mobPosition.toBlockPos())
        }

        activeHolographicEntities.add(entity)
        try {
            EntityTransparencyManager.withHolographicTransparency(entity, opacity) {
                client.entityRenderDispatcher.submit(
                    entityRenderState,
                    cameraRenderState,
                    mobPosition.x - cameraPos.x,
                    mobPosition.y - cameraPos.y,
                    mobPosition.z - cameraPos.z,
                    matrices,
                    submitNodeCollector,
                )
            }
        } finally {
            activeHolographicEntities.remove(entity)
        }
    }
}
