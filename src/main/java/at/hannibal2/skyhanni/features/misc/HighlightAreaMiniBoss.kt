package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntitySpider
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityWolf
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightAreaMiniBoss {

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!isEnabled()) return

        val entity = event.entity
        val maxHealth = event.maxHealth

        for (bossType in AreaMiniBossType.values()) {
            if (!bossType.clazz.isInstance(entity)) continue

            if (entity.hasMaxHealth(bossType.health, false, maxHealth)) {
                RenderLivingEntityHelper.setEntityColor(entity, bossType.color)
                RenderLivingEntityHelper.setNoHurtTime(entity)
            }
        }
    }

    private fun isEnabled(): Boolean =
        LorenzUtils.inSkyBlock && SkyHanniMod.feature.misc.highlightAreaMinisBoss

    enum class AreaMiniBossType(val clazz: Class<out EntityLiving>, val health: Int, val color: Int) {
        GOLDEN_GHOUL(EntityZombie::class.java, 45_000, LorenzColor.YELLOW.toColor().withAlpha(127)),
//        OLD_WOLF(EntityWolf::class.java, 15_000, LorenzColor.RED.toColor().withAlpha(60)),
        OLD_WOLF(EntityWolf::class.java, 15_000, LorenzColor.GOLD.toColor().withAlpha(60)),
        KEEPER(EntitySpider::class.java, 3000, LorenzColor.GREEN.toColor().withAlpha(60)),
        ENDERMAN(EntityEnderman::class.java, 8_000_000, LorenzColor.LIGHT_PURPLE.toColor().withAlpha(127)),
        BLAZE(EntityBlaze::class.java, 30_000_000, LorenzColor.DARK_RED.toColor().withAlpha(60)),
        ;
    }
}