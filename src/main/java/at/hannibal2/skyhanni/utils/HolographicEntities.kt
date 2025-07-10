package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorRendererLivingEntity
import at.hannibal2.skyhanni.utils.TimeUtils.inWholeTicks
import at.hannibal2.skyhanni.utils.compat.createWitherSkeleton
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.monster.EntityCaveSpider
import net.minecraft.entity.monster.EntityCreeper
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityEndermite
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGiantZombie
import net.minecraft.entity.monster.EntityGuardian
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.entity.monster.EntityPigZombie
import net.minecraft.entity.monster.EntitySilverfish
import net.minecraft.entity.monster.EntitySkeleton
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.monster.EntitySnowman
import net.minecraft.entity.monster.EntitySpider
import net.minecraft.entity.monster.EntityWitch
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntityChicken
import net.minecraft.entity.passive.EntityCow
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.entity.passive.EntityMooshroom
import net.minecraft.entity.passive.EntityOcelot
import net.minecraft.entity.passive.EntityPig
import net.minecraft.entity.passive.EntityRabbit
import net.minecraft.entity.passive.EntitySheep
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.passive.EntityWolf
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
    class HolographicEntity<T : EntityLivingBase> internal constructor(
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
    class HolographicBase<T : EntityLivingBase> internal constructor(private val entity: T) {
        fun instance(position: LorenzVec, yaw: Float): HolographicEntity<T> {
            return HolographicEntity(entity, position, yaw)
        }
    }

    val zombie = HolographicBase(EntityZombie(null))
    val chicken = HolographicBase(EntityChicken(null))
    val slime = HolographicBase(EntitySlime(null))
    val wolf = HolographicBase(EntityWolf(null))
    val skeleton = HolographicBase(EntitySkeleton(null))
    val creeper = HolographicBase(EntityCreeper(null))
    val ocelot = HolographicBase(EntityOcelot(null))
    val blaze = HolographicBase(EntityBlaze(null))
    val rabbit = HolographicBase(EntityRabbit(null))
    val sheep = HolographicBase(EntitySheep(null))
    val horse = HolographicBase(EntityHorse(null))
    val eisengolem = HolographicBase(EntityIronGolem(null))
    val silverfish = HolographicBase(EntitySilverfish(null))
    val witch = HolographicBase(EntityWitch(null))
    val endermite = HolographicBase(EntityEndermite(null))
    val snowman = HolographicBase(EntitySnowman(null))
    val villager = HolographicBase(EntityVillager(null))
    val guardian = HolographicBase(EntityGuardian(null))
    val armorStand = HolographicBase(EntityArmorStand(null))
    val squid = HolographicBase(EntitySquid(null))
    val bat = HolographicBase(EntityBat(null))
    val spider = HolographicBase(EntitySpider(null))
    val caveSpider = HolographicBase(EntityCaveSpider(null))
    val pigman = HolographicBase(EntityPigZombie(null))
    val ghast = HolographicBase(EntityGhast(null))
    val magmaCube = HolographicBase(EntityMagmaCube(null))
    val wither = HolographicBase(EntityWither(null))
    val enderman = HolographicBase(EntityEnderman(null))
    val mooshroom = HolographicBase(EntityMooshroom(null))
    val witherSkeleton = HolographicBase(createWitherSkeleton(null))
    val cow = HolographicBase(EntityCow(null))
    val pig = HolographicBase(EntityPig(null))
    val giant = HolographicBase(EntityGiantZombie(null))

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
    fun <T : EntityLivingBase> SkyHanniRenderWorldEvent.renderHolographicEntity(
        holographicEntity: HolographicEntity<T>,
        holographicness: Float = 0.3f,
    ) {
        val renderManager = Minecraft.getMinecraft().renderManager
        val entity = holographicEntity.entity

        val renderer = renderManager.getEntityRenderObject<EntityLivingBase>(entity)
            ?: error("getEntityRenderObject is null for ${entity.name}")
        @Suppress("UNCHECKED_CAST")
        renderer as? RendererLivingEntity<T> ?: error("can not cast to RendererLivingEntity")
        @Suppress("UNCHECKED_CAST")
        renderer as? AccessorRendererLivingEntity<T> ?: error("can not cast to AccessorRendererLivingEntity")

        renderer.setRenderOutlines(false)
        if (!renderer.bindEntityTexture_skyhanni(entity)) return

        GlStateManager.pushMatrix()
        val viewerPosition = WorldRenderUtils.getViewerPos(partialTicks)
        val mobPosition = holographicEntity.interpolatedPosition(partialTicks)
        val renderingOffset = mobPosition - viewerPosition
        GlStateManager.translate(renderingOffset.x.toFloat(), renderingOffset.y.toFloat(), renderingOffset.z.toFloat())
        GlStateManager.disableCull()
        GlStateManager.enableRescaleNormal()
        GlStateManager.scale(-1f, -1f, 1f)
        GlStateManager.translate(0F, -1.5078125f, 0f)
        val limbSwing = 0F
        val limbSwingAmount = 0F
        val ageInTicks = 1_000_000.toFloat()
        val netHeadYaw = holographicEntity.interpolatedYaw(partialTicks)
        val headPitch = 0F
        val scaleFactor = 0.0625f
        renderer.setBrightness_skyhanni(entity, 0f, true)
        GlStateManager.color(1f, 1f, 1f, holographicness)
        GlStateManager.depthMask(false)
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.alphaFunc(GL11.GL_GREATER, 1 / 255F)

        GlStateManager.enableTexture2D()
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
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f)
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.depthMask(true)
        GlStateManager.disableBlend()
        renderer.unsetBrightness_skyhanni()
        GlStateManager.popMatrix()
    }

}
