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
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.zombie.Zombie
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

//? if >= 26.2 {
import net.minecraft.util.LightCoordsUtil
//?} else {
/*import net.minecraft.client.renderer.LevelRenderer
*///?}

/**
 * Utility for creating fake entities without an associated world to avoid contaminating the world state.
 */
@SkyHanniModule
object HolographicEntities {
    private var debugHologram: HolographicEntity<Zombie>? = null
    private var debugHologramTransparency: Float = 1f

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
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
        debugHologram = (base as HolographicBase<Zombie>).instance(pos, player.yRot, player.xRot)
        debugHologramTransparency = transparency
    }


    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        val hologram = debugHologram ?: return
        event.renderHolographicEntity(hologram, opacity = debugHologramTransparency)
    }

    /**
     * An instance of a holographic entity. Maintains a minimal controlled state,
     * separating spatial positioning from rotation and look controls.
     */
    class HolographicEntity<T : LivingEntity> internal constructor(
        val entity: T,
        var position: LorenzVec,
        yaw: Float,
        pitch: Float,
    ) {
        var isChild: Boolean = false
        var lastPosition: LorenzVec = position
        val createdAt = SimpleTimeMark.now()
        internal var cachedRenderState: EntityRenderState? = null

        // Rotation states
        var yaw: Float = yaw
            private set
        var pitch: Float = pitch
            private set
        var lastYaw: Float = yaw
        var lastPitch: Float = pitch

        var headYaw: Float = yaw
        var headPitch: Float = pitch
        var bodyYaw: Float = yaw
        var lastHeadYaw: Float = headYaw
        var lastHeadPitch: Float = headPitch
        var lastBodyYaw: Float = bodyYaw

        var defaultYaw: Float = yaw
        var defaultPitch: Float = pitch

        val monotonicProgress get() = createdAt.passedSince().inWholeTicks

        /**
         * Updates the entity's position, keeping track of previous position for interpolation.
         */
        fun moveTo(position: LorenzVec, isTeleport: Boolean = false) {
            if (isTeleport) {
                this.lastPosition = position
            } else {
                this.lastPosition = this.position
            }
            this.position = position
        }

        /**
         * Manually sets the entity's rotation (yaw and pitch).
         */
        fun setRotation(yaw: Float, pitch: Float, isTeleport: Boolean = false) {
            if (isTeleport) {
                this.lastYaw = yaw
                this.lastPitch = pitch
                this.lastHeadYaw = yaw
                this.headYaw = yaw
                this.lastHeadPitch = pitch
                this.headPitch = pitch
                this.lastBodyYaw = yaw
                this.bodyYaw = yaw
            } else {
                this.lastYaw = this.yaw
                this.lastPitch = this.pitch
                this.lastHeadYaw = this.headYaw
                this.lastHeadPitch = this.headPitch
                this.lastBodyYaw = this.bodyYaw
            }
            this.yaw = yaw
            this.pitch = pitch
            this.headYaw = yaw
            this.headPitch = pitch
            this.bodyYaw = yaw
        }

        /**
         * Automatically looks at a target position if within [maxDistance], otherwise smoothly returns to [defaultYaw]/[defaultPitch].
         */
        fun updateLook(targetPos: LorenzVec?, maxDistance: Double = 20.0) {
            // Save last rotation states before updating
            this.lastYaw = this.yaw
            this.lastPitch = this.pitch
            this.lastHeadYaw = this.headYaw
            this.lastHeadPitch = this.headPitch
            this.lastBodyYaw = this.bodyYaw

            val eyePos = position.add(y = entity.eyeHeight.toDouble(), x = 0.0, z = 0.0)
            val distSq = targetPos?.distanceSq(eyePos) ?: Double.MAX_VALUE

            if (targetPos != null && distSq <= maxDistance * maxDistance) {
                val delta = targetPos - eyePos
                val targetYaw = Math.toDegrees(atan2(-delta.x, delta.z)).toFloat()
                val targetPitch = -Math.toDegrees(atan2(delta.y, sqrt(delta.x * delta.x + delta.z * delta.z))).toFloat()

                headYaw = interpolateRotation(headYaw, targetYaw, 0.4f)
                headPitch = interpolateRotation(headPitch, targetPitch, 0.4f)

                // Body rotation follows head if head turns past 50 degrees (mob-like behavior)
                val diff = interpolateRotation(bodyYaw, headYaw, 1.0f) - bodyYaw
                if (diff < -50f) bodyYaw = headYaw + 50f
                else if (diff > 50f) bodyYaw = headYaw - 50f
            } else {
                headYaw = interpolateRotation(headYaw, defaultYaw, 0.2f)
                headPitch = interpolateRotation(headPitch, defaultPitch, 0.2f)
                bodyYaw = interpolateRotation(bodyYaw, defaultYaw, 0.2f)
            }
            this.yaw = bodyYaw
            this.pitch = headPitch
        }

        fun interpolatedPosition(partialTicks: Float): LorenzVec =
            lastPosition.slope(position, partialTicks.toDouble())

        fun interpolatedYaw(partialTicks: Float): Float =
            interpolateRotation(lastYaw, yaw, partialTicks)

        fun interpolatedPitch(partialTicks: Float): Float =
            interpolateRotation(lastPitch, pitch, partialTicks)

        fun interpolatedHeadYaw(partialTicks: Float): Float =
            interpolateRotation(lastHeadYaw, headYaw, partialTicks)

        fun interpolatedHeadPitch(partialTicks: Float): Float =
            interpolateRotation(lastHeadPitch, headPitch, partialTicks)

        fun interpolatedBodyYaw(partialTicks: Float): Float =
            interpolateRotation(lastBodyYaw, bodyYaw, partialTicks)
    }

    /**
     * Creates a holographic entity from an already-created entity.
     */
    fun <T : LivingEntity> create(
        entity: T,
        position: LorenzVec,
        yaw: Float,
        pitch: Float,
    ): HolographicEntity<T> {
        //? if >= 26.2
        entity.id = FakeEntityIdProvider.getNextId()

        return HolographicEntity(entity, position, yaw, pitch)
    }

    /**
     * Template for a [HolographicEntity].
     */
    class HolographicBase<T : LivingEntity> internal constructor(internal val entityType: EntityType<T>) {
        fun instance(position: LorenzVec, yaw: Float, pitch: Float): HolographicEntity<T>? {
            val level = Minecraft.getInstance().level ?: return null
            val entity = entityType.create(level, EntitySpawnReason.COMMAND) ?: return null
            //? if >= 26.2
            entity.id = FakeEntityIdProvider.getNextId()
            return HolographicEntity(entity, position, yaw, pitch)
        }
    }

    private var _internalEntityHoloBases: Map<KClass<out LivingEntity>, HolographicBase<out LivingEntity>>? = null
    val entityHoloBases: Map<KClass<out LivingEntity>, HolographicBase<out LivingEntity>> get() = _internalEntityHoloBases ?: run {
        val level = Minecraft.getInstance().level ?: return@run emptyMap()
        BuiltInRegistries.ENTITY_TYPE.associateNotNull type@{ entityType ->
            val testEntity: LivingEntity = runCatching {
                entityType.create(level, EntitySpawnReason.COMMAND)?.apply {
                    //? if >= 26.2
                    id = FakeEntityIdProvider.getNextId()
                }
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
     * Render a fake [HolographicEntity].
     */
    fun <T : LivingEntity> SkyHanniRenderWorldEvent.renderHolographicEntity(
        holographicEntity: HolographicEntity<T>,
        opacity: Float = 0.3f,
    ) {
        val entity = holographicEntity.entity
        val mobPosition = holographicEntity.interpolatedPosition(partialTicks)
        val interpolatedBodyYaw = holographicEntity.interpolatedBodyYaw(partialTicks)
        val interpolatedHeadYaw = holographicEntity.interpolatedHeadYaw(partialTicks)
        val interpolatedHeadPitch = holographicEntity.interpolatedHeadPitch(partialTicks)

        // Populate entity fields for separate body/head rendering
        entity.yRot = interpolatedBodyYaw
        entity.yRotO = interpolatedBodyYaw
        entity.yBodyRot = interpolatedBodyYaw
        entity.yBodyRotO = interpolatedBodyYaw
        entity.yHeadRot = interpolatedHeadYaw
        entity.yHeadRotO = interpolatedHeadYaw
        entity.xRot = interpolatedHeadPitch
        entity.xRotO = interpolatedHeadPitch

        val client = Minecraft.getInstance()
        @Suppress("UNCHECKED_CAST")
        val renderer = client.entityRenderDispatcher.getRenderer(entity) as? EntityRenderer<T, EntityRenderState> ?: return
        val gameRenderer = client.gameRenderer
        val entityRenderState = holographicEntity.cachedRenderState
            ?: renderer.createRenderState().also { holographicEntity.cachedRenderState = it }
        //~ if < 26.2 'gameRenderState()' -> 'gameRenderState'
        val cameraRenderState = gameRenderer.gameRenderState().levelRenderState.cameraRenderState
        val cameraPos = cameraRenderState.pos
        renderer.extractRenderState(entity, entityRenderState, partialTicks)
        entityRenderState.`skyhanni$setEntity`(entity)
        (entityRenderState as? LivingEntityRenderState)?.isBaby = holographicEntity.isChild
        client.level?.let { level ->
            //~ if < 26.2 'LightCoordsUtil' -> 'LevelRenderer'
            entityRenderState.lightCoords = LightCoordsUtil.getLightCoords(level, mobPosition.toBlockPos())
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
