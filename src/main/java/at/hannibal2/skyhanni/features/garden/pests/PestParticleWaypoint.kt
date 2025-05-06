package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.garden.pests.PestUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayerIgnoreY
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.ParticlePathBezierFitter
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraft.util.EnumParticleTypes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestParticleWaypoint {

    private val config get() = SkyHanniMod.feature.garden.pests.pestWaypoint

    private val bezierFitter = ParticlePathBezierFitter(3)
    private const val FIREWORK_ID = 76

    private var lastPestTrackerUse = SimpleTimeMark.farPast()
    private var lastParticle = SimpleTimeMark.farPast()

    private var guessPosition: LorenzVec? = null

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled() || !PestApi.hasVacuumInHand()) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (MinecraftCompat.localPlayer.isSneaking) return
        reset()
        lastPestTrackerUse = SimpleTimeMark.now()
    }

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true, onlyOnIsland = IslandType.GARDEN)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (lastPestTrackerUse.passedSince() > 5.seconds) return
        when {
            event.isEnchantmentTable() -> {
                if (config.hideParticles) event.cancel()
                return
            }

            !event.isVillagerAngry() -> return
        }
        if (config.hideParticles) event.cancel()

        lastParticle = SimpleTimeMark.now()
        val pos = event.location

        if (bezierFitter.isEmpty()) {
            bezierFitter.addPoint(pos)
            return
        }

        val lastPoint = bezierFitter.getLastPoint() ?: return
        val dist = lastPoint.distance(pos)
        if (dist == 0.0 || dist > 3.0) return
        bezierFitter.addPoint(pos)

        val solved = bezierFitter.solve() ?: return
        guessPosition = solved
    }

    private fun ReceiveParticleEvent.isEnchantmentTable(): Boolean =
        type == EnumParticleTypes.ENCHANTMENT_TABLE && count == 10 && speed == -2f && offset.isZero()

    private fun ReceiveParticleEvent.isVillagerAngry(): Boolean =
        type == EnumParticleTypes.VILLAGER_ANGRY && count == 1 && speed == 0f && offset.isZero()

    @HandleEvent
    fun onWorldChange() = reset()

    private fun reset() {
        lastPestTrackerUse = SimpleTimeMark.farPast()
        guessPosition = null
        bezierFitter.reset()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onFireWorkSpawn(event: PacketReceivedEvent) {
        val packet = event.packet as? S0EPacketSpawnObject ?: return
        if (!config.hideParticles) return
        if (packet.type == FIREWORK_ID) event.cancel()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (bezierFitter.isEmpty()) return
        if (lastPestTrackerUse.passedSince() > config.showForSeconds.seconds) {
            reset()
            return
        }
        val waypoint = guessPosition ?: return
        val color = LorenzColor.RED.toColor()

        event.drawWaypointFilled(waypoint, color, beacon = true)
        event.drawDynamicText(waypoint, "§aPest Guess", 1.3)
        if (config.drawLine) {
            event.drawLineToEye(
                waypoint,
                color,
                3,
                false,
            )
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick() {
        if (!isEnabled()) return
        val guessPoint = guessPosition ?: return

        if (guessPoint.distanceToPlayerIgnoreY() > 8) return
        if (lastPestTrackerUse.passedSince() !in 1.seconds..config.showForSeconds.seconds) return
        reset()
    }

    @HandleEvent
    fun onPestUpdate(event: PestUpdateEvent) {
        if (PestApi.scoreboardPests == 0) reset()
    }

    private fun isEnabled() = config.enabled

}
