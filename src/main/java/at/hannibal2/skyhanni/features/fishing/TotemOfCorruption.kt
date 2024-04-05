package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.fishing.TotemOfCorruptionConfig.OutlineType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sendTitle
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawSphereInWorld
import at.hannibal2.skyhanni.utils.RenderUtils.drawSphereWireframeInWorld
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SoundUtils.playPlingSound
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TotemOfCorruption {

    private val config get() = SkyHanniMod.feature.fishing.totemOfCorruption

    private var display = emptyList<String>()
    private var totems: List<Totem> = emptyList()

    private val group = RepoPattern.group("fishing.totemofcorruption")
    private val totemNamePattern by group.pattern(
        "totemname",
        "§5§lTotem of Corruption"
    )
    private val timeRemainingPattern by group.pattern(
        "timeremaining",
        "§7Remaining: §e(?:(?<min>\\d+)m )?(?<sec>\\d+)s"
    )
    private val ownerPattern by group.pattern(
        "owner",
        "§7Owner: §e(?<owner>.+)"
    )

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isOverlayEnabled() || display.isEmpty()) return
        config.position.renderStrings(display, posLabel = "Totem of Corruption")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.repeatSeconds(2)) return
        if (!isOverlayEnabled()) return

        totems = getTotems()
        display = createDisplay()
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isHideParticlesEnabled()) return

        for (totem in totems) {
            if (event.type == EnumParticleTypes.SPELL_WITCH && event.speed == 0.0f) {
                if (totem.location.distance(event.location) < 4.0) {
                    event.isCanceled = true
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEffectiveAreaEnabled()) return
        if (totems.isEmpty()) return

        val color = config.color.toChromaColor()
        for (totem in totems) {
            // The center of the totem is the upper part of the armor stand
            when (config.outlineType) {
                OutlineType.FILLED -> {
                    event.drawSphereInWorld(color, totem.location.add(y = 1), 16f)
                }

                OutlineType.WIREFRAME -> {
                    event.drawSphereWireframeInWorld(color, totem.location.add(y = 1), 16f)
                }

                else -> return
            }
        }
    }

    private fun getTimeRemaining(totem: EntityArmorStand): Duration? =
        EntityUtils.getEntitiesNearby<EntityArmorStand>(totem.getLorenzVec(), 2.0)
            .firstNotNullOfOrNull { entity ->
                timeRemainingPattern.matchMatcher(entity.name) {
                    val minutes = group("min")?.toIntOrNull() ?: 0
                    val seconds = group("sec")?.toInt() ?: 0
                    (minutes * 60 + seconds).seconds
                }
            }

    private fun getOwner(totem: EntityArmorStand): String? =
        EntityUtils.getEntitiesNearby<EntityArmorStand>(totem.getLorenzVec(), 2.0)
            .firstNotNullOfOrNull { entity ->
                ownerPattern.matchMatcher(entity.name) {
                    group("owner")
                }
            }


    private fun createDisplay() = buildList {
        val totem = getTotemToShow() ?: return@buildList
        add("§5§lTotem of Corruption")
        add("§7Remaining: §e${totem.timeRemaining.format(TimeUnit.MINUTE)}")
        add("§7Owner: §e${totem.ownerName}")
    }

    private fun getTotemToShow(): Totem? = totems
        .filter { it.distance < config.distanceThreshold }
        .maxByOrNull { it.timeRemaining }

    private fun getTotems(): List<Totem> = EntityUtils.getEntitiesNextToPlayer<EntityArmorStand>(100.0)
        .filter { totemNamePattern.matches(it.name) }.toList()
        .mapNotNull { totem ->
            val timeRemaining = getTimeRemaining(totem) ?: return@mapNotNull null
            val owner = getOwner(totem) ?: return@mapNotNull null

            val timeToWarn = config.warnWhenAboutToExpire.seconds
            if (timeToWarn > 0.seconds && timeRemaining == timeToWarn) {
                playPlingSound()
                sendTitle("§c§lTotem of Corruption §eabout to expire!", 5.seconds)
            }
            Totem(totem.getLorenzVec(), timeRemaining, owner)
        }

    private fun isOverlayEnabled() = LorenzUtils.inSkyBlock && config.showOverlay
    private fun isHideParticlesEnabled() = LorenzUtils.inSkyBlock && config.hideParticles
    private fun isEffectiveAreaEnabled() = LorenzUtils.inSkyBlock && config.outlineType != OutlineType.NONE
}

class Totem(
    val location: LorenzVec,
    val timeRemaining: Duration,
    val ownerName: String,
    val distance: Double = location.distanceToPlayer()
)
