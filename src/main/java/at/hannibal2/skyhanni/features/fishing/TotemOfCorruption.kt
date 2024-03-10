package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

private val config get() = SkyHanniMod.feature.fishing.totemOfCorruption

private var display = emptyList<String>()
private var totems: List<Totem> = emptyList()

class TotemOfCorruption {
    private val group = RepoPattern.group("features.fishing.totemofcorruption")
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
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isOverlayEnabled() || display.isEmpty()) return
        config.position.renderStrings(display, posLabel = "Totem of Corruption")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isOverlayEnabled()) return
        totems = getTotems()
            .filterNotNull()
            .mapNotNull { totem ->
                val timeRemaining = getTimeRemaining(totem)
                val owner = getOwner(totem)
                if (timeRemaining != null && owner != null) {
                    Totem(totem, timeRemaining, owner)
                } else {
                    null
                }
            }

        display = createLines()
    }

    @SubscribeEvent
    fun onChatPacket(event: ReceiveParticleEvent) {
        if (!isHideParticlesEnabled()) return

        for (totem in totems) {
            if (event.type == EnumParticleTypes.SPELL_WITCH && event.speed == 0.0f) {
                if (totem.totemEntity.getLorenzVec().distance(event.location) < 4.0) {
                    event.isCanceled = true
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEffectiveAreaEnabled()) return

        val color = Color(SpecialColour.specialToChromaRGB(config.color), true)
        for (totem in totems) {
            val x = totem.totemEntity.posX
            val y = totem.totemEntity.posY + 1.0 // The center of the totem is the upper part
            val z = totem.totemEntity.posZ
            RenderUtils.drawSphereInWorld(color, x, y, z, 16f, event.partialTicks)
        }
    }

    private fun getTimeRemaining(totem: EntityArmorStand) =
        EntityUtils.getEntitiesNearby<EntityArmorStand>(totem.getLorenzVec(), 2.0)
            .firstOrNull { timeRemainingPattern.matches(it.name) }
            ?.let {
                timeRemainingPattern.matchMatcher(it.name) {
                    group("min")?.toInt()?.let { min -> min * 60 + group("sec").toInt() }
                        ?: group("sec").toInt()
                }
            }

    private fun getOwner(totem: EntityArmorStand) =
        EntityUtils.getEntitiesNearby<EntityArmorStand>(totem.getLorenzVec(), 2.0)
            .firstOrNull { ownerPattern.matches(it.name) }
            ?.let {
                ownerPattern.matchMatcher(it.name) {
                    group("owner")
                }
            }

    private fun createLines() = buildList {
        val totem = getTotemToShow() ?: return@buildList
        add("§5§lTotem of Corruption")
        add("§7Remaining: §e${totem.timeRemainingFormatted()}")
        add("§7Owner: §e${totem.ownerName}")
    }

    private fun Totem.timeRemainingFormatted() = if (timeRemainingSeconds < 60) {
        "§e${timeRemainingSeconds % 60}s"
    } else {
        "§e${timeRemainingSeconds / 60}min ${timeRemainingSeconds % 60}s"
    }

    private fun getTotemToShow(): Totem? {
        return totems
            .filter { it.distance < config.distanceThreshold }
            .maxByOrNull { it.timeRemainingSeconds }
    }

    private fun getTotems(): List<EntityArmorStand?> {
        return EntityUtils.getEntitiesNextToPlayer<EntityArmorStand>(100.0)
            .filter { totemNamePattern.matches(it.name) }.toList()
    }

    private fun isOverlayEnabled() = config.showOverlay && LorenzUtils.inSkyBlock
    private fun isHideParticlesEnabled() = config.hideParticles && LorenzUtils.inSkyBlock
    private fun isEffectiveAreaEnabled() = config.showEffectiveArea && LorenzUtils.inSkyBlock
}

class Totem(
    val totemEntity: EntityArmorStand,
    val timeRemainingSeconds: Int,
    val ownerName: String,
    val distance: Double = totemEntity.getLorenzVec().distanceToPlayer()
)
