package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GriffinBurrowHelper {

    private var guessLocation: LorenzVec? = null
    private var particleBurrows = mutableMapOf<LorenzVec, BurrowType>()
    private var animationLocation: LorenzVec? = null
    private var lastDug: LorenzVec? = null
    private var teleportedLocation: LorenzVec? = null
    private var lastGuessTime = 0L

    @SubscribeEvent
    fun onSoopyGuessBurrow(event: SoopyGuessBurrowEvent) {
        EntityMovementData.addToTrack(Minecraft.getMinecraft().thePlayer)
        if (System.currentTimeMillis() > lastGuessTime + 1_000) {
            animationLocation = LocationUtils.playerLocation().add(-0.5, -1.0, -0.5)
        }
        lastGuessTime = System.currentTimeMillis()

        if (SkyHanniMod.feature.diana.burrowNearestWarp) {
            BurrowWarpHelper.shouldUseWarps(event.guessLocation)
        }

        guessLocation = event.guessLocation
        if (SkyHanniMod.feature.diana.burrowsNearbyDetection) {
            checkRemoveGuess(false)
        }
    }

    @SubscribeEvent
    fun onBurrowDetect(event: BurrowDetectEvent) {
        EntityMovementData.addToTrack(Minecraft.getMinecraft().thePlayer)
        particleBurrows[event.burrowLocation] = event.type

        if (SkyHanniMod.feature.diana.burrowsNearbyDetection) {
            checkRemoveGuess(true)
        }
    }

    private fun checkRemoveGuess(animation: Boolean) {
        guessLocation?.let { guessRaw ->
            val guess = findBlock(guessRaw)
            if (particleBurrows.any { guess.distance(it.key) < 20 }) {
                if (animation) {
                    animationLocation = guess
                }
                guessLocation = null
            }
        }
    }

    @SubscribeEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        val location = event.burrowLocation
        particleBurrows.remove(location)
        if (particleBurrows.isNotEmpty()) {
            animationLocation = location
        }
        lastDug = location
    }

    @SubscribeEvent
    fun onPlayerMove(event: EntityMoveEvent) {
        if (event.distance > 10) {
            if (event.entity == Minecraft.getMinecraft().thePlayer) {
                teleportedLocation = event.newLocation
            }
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message.startsWith("§c ☠ §r§7You were killed by §r")) {
            particleBurrows.keys.removeIf { particleBurrows[it] == BurrowType.MOB }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        guessLocation = null
        animationLocation = null
        lastDug = null
        particleBurrows.clear()
    }

    private fun findBlock(point: LorenzVec): LorenzVec {
        var gY = 131.0

        var searchGrass = true
        while ((if (searchGrass) LorenzVec(point.x, gY, point.z).getBlockAt() != Blocks.grass else LorenzVec(
                point.x,
                gY,
                point.z
            ).getBlockAt() == Blocks.air)
        ) {
            gY--
            if (gY < 70) {
                if (!searchGrass) {
                    break
                } else {
                    searchGrass = false
                    gY = 131.0
                }
            }
        }
        return LorenzVec(point.x, gY, point.z)
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        val playerLocation = LocationUtils.playerLocation()
        if (SkyHanniMod.feature.diana.burrowsSoopyGuess) {
            guessLocation?.let {
                val guessLocation = findBlock(it)
                val distance = guessLocation.distance(playerLocation)
                event.drawColor(guessLocation, LorenzColor.WHITE, distance > 10)
                event.drawDynamicText(guessLocation, "Guess", 1.5)
                if (distance > 5) {
                    val formattedDistance = LorenzUtils.formatDouble(distance)
                    event.drawDynamicText(guessLocation, "§e${formattedDistance}m", 1.3, yOff = 12f)
                }
            }
        }

        sendTip(event)

        if (SkyHanniMod.feature.diana.burrowsNearbyDetection) {
            for (burrow in particleBurrows) {
                val location = burrow.key
                val distance = location.distance(playerLocation)
                val burrowType = burrow.value
                event.drawColor(location, burrowType.color, distance > 10)
                if (distance < 10) {
                    event.drawString(location.add(0.5, 1.5, 0.5), burrowType.text, true)
                }
            }
        }
    }

    private fun sendTip(event: RenderWorldLastEvent) {
        teleportedLocation?.let {
            teleportedLocation = null

            if (BurrowWarpHelper.currentWarp != null) {
                BurrowWarpHelper.currentWarp = null
                if (SkyHanniMod.feature.diana.burrowNearestWarp) {
                    animationLocation = it
                    return
                }
            }
        }

        if (SkyHanniMod.feature.diana.burrowNearestWarp) {
            BurrowWarpHelper.currentWarp?.let { warp ->
                animationLocation?.let {
                    event.drawColor(it.add(0.0, 1.0, 0.0), LorenzColor.AQUA)
                    if (it.distance(LocationUtils.playerLocation()) < 10) {
                        event.drawString(it.add(0.5, 1.5, 0.5), "§bWarp to " + warp.displayName, true)
                    }
                    return
                }
            }
        }
        if (SkyHanniMod.feature.diana.burrowSmoothTransition) {
            animationLocation?.let {
                event.drawColor(it, LorenzColor.WHITE)
                animationLocation = moveAnimation(it, event)
            }
        }
    }

    private fun moveAnimation(animation: LorenzVec, event: RenderWorldLastEvent): LorenzVec? {
        val list = mutableListOf<LorenzVec>()
        if (SkyHanniMod.feature.diana.burrowsNearbyDetection) {
            list.addAll(particleBurrows.keys)
        }
        guessLocation?.let {
            val loc = findBlock(it)
            if (loc.y > 200) {
                list.add(LorenzVec(loc.x, LocationUtils.playerLocation().y, loc.z))
            } else {
                list.add(loc)
            }
        }
        val target = list.minByOrNull { it.distance(animation) } ?: return null
        val distance = target.distance(animation)
        if (distance < 0.20) return null

        var vector = target.subtract(animation)

        event.draw3DLine(animation.add(0.5, 0.5, 0.5), target.add(0.5, 0.5, 0.5), LorenzColor.WHITE.toColor(), 2, true)

        vector = vector.multiply(1 / vector.length())
        vector = vector.multiply(0.18)
        return animation.add(vector)
    }
}