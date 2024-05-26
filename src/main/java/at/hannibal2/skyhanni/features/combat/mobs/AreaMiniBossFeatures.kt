package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.ignoreDerpy
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityWolf
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class AreaMiniBossFeatures {

    private val config get() = SkyHanniMod.feature.combat.mobs
    private var lastSpawnTime = SimpleTimeMark.farPast()
    private var miniBossType: AreaMiniBossType? = null
    private var respawnCooldown = 11.seconds

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val entity = event.entity
        // TODO remove workaround by change derpy logic either in hasMaxHealth or in EntityMaxHealthUpdateEvent
        val maxHealth = event.maxHealth.ignoreDerpy()
        for (bossType in AreaMiniBossType.entries) {
            if (!bossType.clazz.isInstance(entity)) continue
            if (!entity.hasMaxHealth(bossType.health, false, maxHealth)) continue

            miniBossType = bossType
            val time = SimpleTimeMark.now()
            val diff = time - lastSpawnTime
            if (diff in 5.seconds..20.seconds) {
                respawnCooldown = diff
            }
            lastSpawnTime = time

            if (config.areaBossHighlight) {
                val color = bossType.color.toColor().withAlpha(bossType.colorOpacity)
                RenderLivingEntityHelper.setEntityColorWithNoHurtTime(entity, color)
                { config.areaBossHighlight && SlayerAPI.isInAnyArea }
            }

            // TODO add sound
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.areaBossRespawnTimer) return

        miniBossType?.apply {
            val time = getTime()
            val playerLocation = LocationUtils.playerLocation()
            spawnLocations.filter { it.distance(playerLocation) < 15 }
                .forEach { event.drawDynamicText(it, time, 1.2, ignoreBlocks = false) }
        }
    }

    private fun AreaMiniBossType.getTime(): String {
        val spawnedSince = lastSpawnTime.passedSince()
        if (respawnCooldown <= spawnedSince) return "§c?"

        val estimatedTime = respawnCooldown - spawnedSince
        val format = estimatedTime.format(showMilliSeconds = true)
        return color.getChatColor() + format
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        miniBossType = null
    }

    enum class AreaMiniBossType(
        val clazz: Class<out EntityLiving>,
        val health: Int,
        val color: LorenzColor,
        val colorOpacity: Int,
        vararg val spawnLocations: LorenzVec,
    ) {

        GOLDEN_GHOUL(
            EntityZombie::class.java, 45_000, LorenzColor.YELLOW, 127,
            LorenzVec(-99.7, 39.0, -86.4),
            LorenzVec(-128.5, 42.0, -138.5),
        ),
        OLD_WOLF(
            EntityWolf::class.java, 15_000, LorenzColor.GOLD, 60,
            LorenzVec(-248.0, 123.0, 54.0),
            LorenzVec(-256.7, 105.0, 75.7),
            LorenzVec(-268.5, 90.0, 97.7),
            LorenzVec(-258.1, 94.0, 75.5),
            LorenzVec(-225.7, 92.0, 127.5),
        ),
        SOUL_OF_THE_ALPHA(
            EntityWolf::class.java, 31_150, LorenzColor.GOLD, 60,
            LorenzVec(-381.5, 56.0, -94.5),
            LorenzVec(-394.5, 63.0, -52.5),
            LorenzVec(-386.5, 50.0, -2.5),
            LorenzVec(-396.5, 58.0, 29.5),
        ),
        VOIDLING_EXTREMIST(
            EntityEnderman::class.java, 8_000_000, LorenzColor.LIGHT_PURPLE, 127,
            LorenzVec(-591.1, 10.0, -304.0),
            LorenzVec(-544.8, 21.0, -301.1),
            LorenzVec(-593.5, 26.0, -328.7),
            LorenzVec(-565.0, 41.0, -307.1),
            LorenzVec(-573.2, 51.0, -353.4),
        ),
        MILLENNIA_AGED_BLAZE(
            EntityBlaze::class.java, 30_000_000, LorenzColor.DARK_RED, 60,
            LorenzVec(-292.5, 97.0, -999.7),
            LorenzVec(-232.3, 77.0, -951.1),
            LorenzVec(-304.1, 73.0, -952.9),
            LorenzVec(-281.6, 82.0, -1010.7),
            LorenzVec(-342.8, 86.0, -1035.2),
        ),
        ;
    }
}
