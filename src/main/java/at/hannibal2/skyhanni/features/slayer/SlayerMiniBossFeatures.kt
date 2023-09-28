package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.EntityCreature
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntitySpider
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityWolf
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SlayerMiniBossFeatures {
    private val config get() = SkyHanniMod.feature.slayer
    private var miniBosses = listOf<EntityCreature>()

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!isEnabled()) return
        val entity = event.entity as? EntityCreature ?: return
        if (DamageIndicatorManager.isBoss(entity)) return

        val maxHealth = event.maxHealth
        for (bossType in SlayerMiniBossType.entries) {
            if (!bossType.health.any { entity.hasMaxHealth(it, true, maxHealth) }) continue
            if (!bossType.clazz.isInstance(entity)) continue

            miniBosses = miniBosses.editCopy { add(entity) }
            RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.AQUA.toColor().withAlpha(127))
            { config.slayerMinibossHighlight }
            RenderLivingEntityHelper.setNoHurtTime(entity) { config.slayerMinibossHighlight }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        miniBosses = emptyList()
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!config.slayerMinibossLine) return
        for (mob in miniBosses) {
            if (mob.health <= 0) continue
            if (mob.isDead) continue
            if (mob.distanceToPlayer() > 10) continue

            event.draw3DLine(
                event.exactPlayerEyeLocation(),
                mob.getLorenzVec().add(0, 1, 0),
                LorenzColor.AQUA.toColor(),
                3,
                true
            )
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight

    enum class SlayerMiniBossType(val clazz: Class<out EntityCreature>, vararg val health: Int) {
        REVENANT(EntityZombie::class.java, 24_000, 90_000, 360_000, 600_000, 2_400_000),
        TARANTULA(EntitySpider::class.java, 54_000, 144_000, 576_000),
        SVEN(EntityWolf::class.java, 45_000, 120_000, 480_000),
        VOIDLING(EntityEnderman::class.java, 8_400_000, 17_500_000, 52_500_000),
        INFERNAL(EntityBlaze::class.java, 12_000_000, 25_000_000),
        ;
    }
}
