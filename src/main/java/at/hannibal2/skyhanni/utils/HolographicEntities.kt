package at.hannibal2.skyhanni.utils import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorRendererLivingEntity
import at.hannibal2.skyhanni.utils.TimeUtils.inWholeTicks
import at.hannibal2.skyhanni.utils.compat.createWitherSkeleton
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import net.minecraft.client.MinecraftClient
import at.hannibal2.skyhanni.utils.render.ModernGlStateManager
import net.minecraft.client.renderer.entity.RenderLivingBase
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.boss.WitherEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.mob.BlazeEntity
import net.minecraft.entity.mob.CaveSpiderEntity
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.entity.mob.EndermanEntity
import net.minecraft.entity.mob.EndermiteEntity
import net.minecraft.entity.mob.GhastEntity
import net.minecraft.entity.mob.GiantEntity
import net.minecraft.entity.mob.GuardianEntity
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.mob.MagmaCubeEntity
import net.minecraft.entity.mob.ZombifiedPiglinEntity
import net.minecraft.entity.mob.SilverfishEntity
import net.minecraft.entity.mob.AbstractSkeletonEntity
import net.minecraft.entity.mob.SlimeEntity
import net.minecraft.entity.passive.SnowGolemEntity
import net.minecraft.entity.mob.SpiderEntity
import net.minecraft.entity.mob.WitchEntity
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.entity.passive.BatEntity
import net.minecraft.entity.passive.ChickenEntity
import net.minecraft.entity.passive.AbstractCowEntity
import net.minecraft.entity.passive.HorseEntity
import net.minecraft.entity.passive.MooshroomEntity
import net.minecraft.entity.passive.OcelotEntity
import net.minecraft.entity.passive.PigEntity
import net.minecraft.entity.passive.RabbitEntity
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.entity.passive.SquidEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.passive.WolfEntity
import org.lwjgl.opengl.GL11

/**
 * Utility for creating fake entities without an associated world in order to avoid contaminating the world state.
 */
object HolographicEntities {

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

        fun interpolatedPosition(partialTicks: Float): LorenzVec {
            return lastPosition.slope(position, partialTicks.toDouble())
        }

        fun interpolatedYaw(partialTicks: Float): Float {
            return interpolateRotation(lastYaw, yaw, partialTicks)
        }
    }

    /**
     * Template for a [HolographicEntity]. This class exists as a guard for
     * [HolographicEntity] to prevent untested entities with potential NPEs
     * being instantiated. A list of tested entities exist in [HolographicEntities].
     * Some of these entities rely on mixins from NEU for their proper null
     * world handling.
     */
    class HolographicBase<T : LivingEntity> internal constructor(private val entity: T) {
        fun instance(position: LorenzVec, yaw: Float): HolographicEntity<T> {
            return HolographicEntity(entity, position, yaw)
        }
    }

    val zombie = HolographicBase(ZombieEntity(null))
    val chicken = HolographicBase(ChickenEntity(null))
    val slime = HolographicBase(SlimeEntity(null))
    val wolf = HolographicBase(WolfEntity(null))
    val skeleton = HolographicBase(AbstractSkeletonEntity(null))
    val creeper = HolographicBase(CreeperEntity(null))
    val ocelot = HolographicBase(OcelotEntity(null))
    val blaze = HolographicBase(BlazeEntity(null))
    val rabbit = HolographicBase(RabbitEntity(null))
    val sheep = HolographicBase(SheepEntity(null))
    val horse = HolographicBase(HorseEntity(null))
    val eisengolem = HolographicBase(IronGolemEntity(null))
    val silverfish = HolographicBase(SilverfishEntity(null))
    val witch = HolographicBase(WitchEntity(null))
    val endermite = HolographicBase(EndermiteEntity(null))
    val snowman = HolographicBase(SnowGolemEntity(null))
    val villager = HolographicBase(Villager(null))
    val guardian = HolographicBase(GuardianEntity(null))
    val armorStand = HolographicBase(ArmorStand(null))
    val squid = HolographicBase(SquidEntity(null))
    val bat = HolographicBase(BatEntity(null))
    val spider = HolographicBase(SpiderEntity(null))
    val caveSpider = HolographicBase(CaveSpiderEntity(null))
    val pigman = HolographicBase(ZombifiedPiglinEntity(null))
    val ghast = HolographicBase(GhastEntity(null))
    val magmaCube = HolographicBase(MagmaCubeEntity(null))
    val wither = HolographicBase(WitherEntity(null))
    val enderman = HolographicBase(EndermanEntity(null))
    val mooshroom = HolographicBase(MooshroomEntity(null))
    val witherSkeleton = HolographicBase(createWitherSkeleton(null))
    val cow = HolographicBase(AbstractCowEntity(null))
    val pig = HolographicBase(PigEntity(null))
    val giant = HolographicBase(GiantEntity(null))

    private fun interpolateRotation(last: Float, next: Float, progress: Float): Float {
        var direction: Float = next - last
        while (direction < -180f) {
            direction += 360f
        }
        while (direction >= 180f) {
            direction -= 360f
        }
        return last + progress * direction
    }

    /**
     * Render a fake [HolographicEntity]. In order to render a fully opaque entity, set [holographicness] to `1F`.
     */
    fun <T : LivingEntity> SkyHanniRenderWorldEvent.renderHolographicEntity(
        holographicEntity: HolographicEntity<T>,
        holographicness: Float = 0.3f,
    ) {
        val renderManager = MinecraftClient.getInstance().entityRenderDispatcher
        val entity = holographicEntity.entity

        val renderer = renderManager.getRenderer<LivingEntity>(entity)
            ?: error("getEntityRenderObject is null for ${entity.name.formattedTextCompatLessResets()}")
        @Suppress("UNCHECKED_CAST")
        renderer as? RenderLivingBase<T> ?: error("can not cast to RendererLivingEntity")
        @Suppress("UNCHECKED_CAST")
        renderer as? AccessorRendererLivingEntity<T> ?: error("can not cast to AccessorRendererLivingEntity")

        renderer.setRenderOutlines(false)
        if (!renderer.bindEntityTexture_skyhanni(entity)) return

        ModernGlStateManager.pushMatrix()
        val viewerPosition = WorldRenderUtils.getViewerPos(partialTicks)
        val mobPosition = holographicEntity.interpolatedPosition(partialTicks)
        val renderingOffset = mobPosition - viewerPosition
        ModernGlStateManager.translate(renderingOffset.x.toFloat(), renderingOffset.y.toFloat(), renderingOffset.z.toFloat())
        ModernGlStateManager.disableCull()
        ModernGlStateManager.enableRescaleNormal()
        ModernGlStateManager.scale(-1f, -1f, 1f)
        ModernGlStateManager.translate(0F, -1.5078125f, 0f)
        val limbSwing = 0F
        val limbSwingAmount = 0F
        val ageInTicks = 1_000_000.toFloat()
        val netHeadYaw = holographicEntity.interpolatedYaw(partialTicks)
        val headPitch = 0F
        val scaleFactor = 0.0625f
        renderer.setBrightness_skyhanni(entity, 0f, true)
        ModernGlStateManager.color(1f, 1f, 1f, holographicness)
        ModernGlStateManager.depthMask(false)
        ModernGlStateManager.enableBlend()
        ModernGlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        ModernGlStateManager.alphaFunc(GL11.GL_GREATER, 1 / 255F)

        ModernGlStateManager.enableTexture2D()
        renderer.mainModel.isChild = holographicEntity.isChild
        renderer.mainModel.setRotationAngles(
            limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity,
        )
        renderer.mainModel.render(
            entity,
            limbSwing,
            limbSwingAmount,
            ageInTicks,
            netHeadYaw,
            headPitch,
            scaleFactor,
        )
        ModernGlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f)
        ModernGlStateManager.color(1f, 1f, 1f, 1f)
        ModernGlStateManager.depthMask(true)
        ModernGlStateManager.disableBlend()
        renderer.unsetBrightness_skyhanni()
        ModernGlStateManager.popMatrix()
    }

}
