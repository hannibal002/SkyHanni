package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorRendererLivingEntity
import at.hannibal2.skyhanni.utils.TimeUtils.inWholeTicks
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.render.ModernGlStateManager
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.RenderLivingBase
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.AbstractCow
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.animal.MushroomCow
import net.minecraft.world.entity.animal.Ocelot
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.Rabbit
import net.minecraft.world.entity.animal.SnowGolem
import net.minecraft.world.entity.animal.Squid
import net.minecraft.world.entity.animal.horse.Horse
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.AbstractSkeleton
import net.minecraft.world.entity.monster.Blaze
import net.minecraft.world.entity.monster.CaveSpider
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Endermite
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.monster.Giant
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.monster.MagmaCube
import net.minecraft.world.entity.monster.Silverfish
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.entity.monster.Spider
import net.minecraft.world.entity.monster.Witch
import net.minecraft.world.entity.monster.WitherSkeleton
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.monster.ZombifiedPiglin
import net.minecraft.world.entity.npc.Villager
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

    val zombie = HolographicBase(Zombie(null))
    val chicken = HolographicBase(Chicken(null))
    val slime = HolographicBase(Slime(null))
    val wolf = HolographicBase(Wolf(null))
    val skeleton = HolographicBase(AbstractSkeleton(null))
    val creeper = HolographicBase(Creeper(null))
    val ocelot = HolographicBase(Ocelot(null))
    val blaze = HolographicBase(Blaze(null))
    val rabbit = HolographicBase(Rabbit(null))
    val sheep = HolographicBase(Sheep(null))
    val horse = HolographicBase(Horse(null))
    val eisengolem = HolographicBase(IronGolem(null))
    val silverfish = HolographicBase(Silverfish(null))
    val witch = HolographicBase(Witch(null))
    val endermite = HolographicBase(Endermite(null))
    val snowman = HolographicBase(SnowGolem(null))
    val villager = HolographicBase(Villager(null))
    val guardian = HolographicBase(Guardian(null))
    val armorStand = HolographicBase(ArmorStand(null))
    val squid = HolographicBase(Squid(null))
    val bat = HolographicBase(Bat(null))
    val spider = HolographicBase(Spider(null))
    val caveSpider = HolographicBase(CaveSpider(null))
    val pigman = HolographicBase(ZombifiedPiglin(null))
    val ghast = HolographicBase(Ghast(null))
    val magmaCube = HolographicBase(MagmaCube(null))
    val wither = HolographicBase(WitherBoss(null))
    val enderman = HolographicBase(EnderMan(null))
    val mooshroom = HolographicBase(MushroomCow(null))
    val witherSkeleton = HolographicBase(WitherSkeleton(null))
    val cow = HolographicBase(AbstractCow(null))
    val pig = HolographicBase(Pig(null))
    val giant = HolographicBase(Giant(null))

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
        val renderManager = Minecraft.getInstance().entityRenderDispatcher
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
