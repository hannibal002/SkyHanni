package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AshfangNextResetCooldown {

    private val config get() = SkyHanniMod.feature.crimsonIsle.ashfang
    private var spawnTime = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (EntityUtils.getEntities<EntityArmorStand>().any {
                it.posY > 145 && (it.name.contains("§c§9Ashfang Acolyte§r") || it.name.contains("§c§cAshfang Underling§r"))
            }) {
            spawnTime = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (spawnTime.isFarPast()) return

        val passedSince = spawnTime.passedSince()
        if (passedSince < 46.1.seconds) {
            val format = passedSince.format(TimeUnit.SECOND, showMilliSeconds = true)
            config.nextResetCooldownPos.renderString(
                "§cAshfang next reset in: §a$format",
                posLabel = "Ashfang Reset Cooldown"
            )
        } else {
            spawnTime = SimpleTimeMark.farPast()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        spawnTime = SimpleTimeMark.farPast()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.nextResetCooldown", "crimsonIsle.ashfang.nextResetCooldown")
        event.move(2, "ashfang.nextResetCooldownPos", "crimsonIsle.ashfang.nextResetCooldownPos")
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && config.nextResetCooldown &&
            DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }
}
