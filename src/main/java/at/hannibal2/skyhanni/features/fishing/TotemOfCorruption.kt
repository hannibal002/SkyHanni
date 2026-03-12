package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.fishing.TotemOfCorruptionConfig.OutlineType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playBeepSound
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawSphereInWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawSphereWireframeInWorld
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TotemOfCorruption {

    private val config get() = SkyHanniMod.feature.fishing.totemOfCorruption

    private var display = emptyList<Renderable>()
    private var totems = emptyList<Totem>()
    private val warnedTotems = TimeLimitedSet<UUID>(2.minutes)

    private val patternGroup = RepoPattern.group("fishing.totemofcorruption")

    private val totemNamePattern by patternGroup.pattern(
        "totemname-nocolor",
        "Totem of Corruption",
    )

    /**
     * REGEX-TEST: Remaining: 2m 30s
     * REGEX-TEST: Remaining: 5s
     */
    private val timeRemainingPattern by patternGroup.pattern(
        "timeremaining-nocolor",
        "Remaining: (?:(?<min>\\d+)m )?(?<sec>\\d+)s"
    )

    /**
     * REGEX-TEST: Owner: hannibal2
     */
    private val ownerPattern by patternGroup.pattern(
        "owner-nocolor",
        "Owner: (?<owner>.+)"
    )

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isOverlayEnabled() || display.isEmpty()) return
        config.position.renderRenderables(display, posLabel = "Totem of Corruption")
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(2)) return
        if (!isOverlayEnabled()) return

        totems = getTotems()
        display = createDisplay()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!config.hideParticles) return

        for (totem in totems) {
            if (event.type == ParticleTypes.WITCH && event.speed == 0f) {
                if (totem.location.distance(event.location) < 4.0) {
                    event.cancel()
                }
            }
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEffectiveAreaEnabled()) return
        if (totems.isEmpty()) return

        val color = config.color.toColor()
        for (totem in totems) {
            // The center of the totem is the upper part of the armor stand
            when (config.outlineType) {
                OutlineType.FILLED -> {
                    event.drawSphereInWorld(color, totem.location.up(), 16f)
                }

                OutlineType.WIREFRAME -> {
                    event.drawSphereWireframeInWorld(color, totem.location.up(), 16f)
                }

                else -> return
            }
        }
    }

    @HandleEvent
    fun onConfigLoad() {
        config.showOverlay.onToggle {
            display = emptyList()
            totems = emptyList()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        display = emptyList()
        totems = emptyList()
    }

    private fun getTimeRemaining(totem: ArmorStand): Duration? =
        totem.getLorenzVec().getEntitiesNearby<ArmorStand>(2.0)
            .firstNotNullOfOrNull { entity ->
                timeRemainingPattern.matchMatcher(entity.cleanName()) {
                    val minutes = group("min")?.toIntOrNull() ?: 0
                    val seconds = group("sec")?.toInt() ?: 0
                    (minutes * 60 + seconds).seconds
                }
            }

    private fun getOwner(totem: ArmorStand): String? =
        totem.getLorenzVec().getEntitiesNearby<ArmorStand>(2.0)
            .firstNotNullOfOrNull { entity ->
                ownerPattern.matchMatcher(entity.cleanName()) {
                    group("owner")
                }
            }

    private fun createDisplay(): List<Renderable> = buildList {
        val totem = getTotemToShow() ?: return@buildList
        add(
            Component.literal("Totem of Corruption").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
        )
        add(
            componentBuilder {
                appendWithColor("Remaining: ", ChatFormatting.GRAY)
                appendWithColor(totem.timeRemaining.format(TimeUnit.MINUTE), ChatFormatting.YELLOW)
            }
        )
        add(
            componentBuilder {
                appendWithColor("Owner: ", ChatFormatting.GRAY)
                appendWithColor(totem.ownerName, ChatFormatting.YELLOW)
            }
        )
    }.map(Renderable::text)

    private fun getTotemToShow(): Totem? {
        val totems = totems.filter { it.distance < config.distanceThreshold }
        totems.firstOrNull { it.ownerName == PlayerUtils.getName() }?.let { return it }
        return totems.minByOrNull { it.distance }
    }

    private fun getTotems(): List<Totem> = getEntitiesNearby<ArmorStand>(100.0)
        .filter { totemNamePattern.matches(it.cleanName()) }.toList()
        .mapNotNull { totem ->
            val timeRemaining = getTimeRemaining(totem) ?: return@mapNotNull null
            val owner = getOwner(totem) ?: return@mapNotNull null

            if (config.ownTotemOnly && (owner != PlayerUtils.getName())) return@mapNotNull null

            val timeToWarn = config.warnWhenAboutToExpire.seconds
            if (timeToWarn > 0.seconds && timeRemaining <= timeToWarn && totem.uuid !in warnedTotems) {
                playBeepSound(0.5f)
                TitleManager.sendTitle("§c§lTotem of Corruption §eabout to expire!")
                warnedTotems.add(totem.uuid)
            }
            Totem(totem.getLorenzVec(), timeRemaining, owner)
        }

    private fun isOverlayEnabled() = SkyBlockUtils.inSkyBlock && config.showOverlay.get()
    private fun isEffectiveAreaEnabled() = SkyBlockUtils.inSkyBlock && config.outlineType != OutlineType.NONE
}

private class Totem(
    val location: LorenzVec,
    val timeRemaining: Duration,
    val ownerName: String,
    val distance: Double = location.distanceToPlayer(),
)
