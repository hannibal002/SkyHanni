package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityMob
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightAreaMiniBoss {

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!isEnabled()) return

        for (bossType in AreaMiniBossType.values()) {
            val clazz = bossType.clazz
            val entity = event.entity

            if (!clazz.isInstance(entity)) continue
            if (event.maxHealth != bossType.health) continue

            RenderLivingEntityHelper.setEntityColor(entity, bossType.color)
            RenderLivingEntityHelper.setNoHurtTime(entity)
        }
    }

    private fun isEnabled(): Boolean =
        LorenzUtils.inSkyBlock && SkyHanniMod.feature.misc.highlightAreaMinisBoss

    enum class AreaMiniBossType(val clazz: Class<out EntityMob>, val health: Int, val color: Int) {
        ENDERMAN(EntityEnderman::class.java, 8_000_000, LorenzColor.LIGHT_PURPLE.toColor().withAlpha(127)),
        BLAZE(EntityBlaze::class.java, 30_000_000, LorenzColor.DARK_RED.toColor().withAlpha(60)),
        ;
    }
}