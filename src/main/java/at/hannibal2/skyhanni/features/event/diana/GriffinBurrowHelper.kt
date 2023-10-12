package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.events.BurrowDetectEvent
import at.hannibal2.skyhanni.events.BurrowDugEvent
import at.hannibal2.skyhanni.events.EntityMoveEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SoopyGuessBurrowEvent
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object GriffinBurrowHelper {
    private val config get() = SkyHanniMod.feature.event.diana

    private var guessLocation: LorenzVec? = null
    private var targetLocation: LorenzVec? = null
    private var particleBurrows = mapOf<LorenzVec, BurrowType>()
    var animationLocation: LorenzVec? = null
    private var lastDug: LorenzVec? = null
    private var teleportedLocation: LorenzVec? = null
    private var lastGuessTime = 0L
    private var lastAnimationTime = 0L

    @SubscribeEvent
    fun onSoopyGuessBurrow(event: SoopyGuessBurrowEvent) {
        EntityMovementData.addToTrack(Minecraft.getMinecraft().thePlayer)
        if (System.currentTimeMillis() > lastGuessTime + 1_000) {
            animationLocation = LocationUtils.playerLocation().add(-0.5, -1.0, -0.5)
        }
        lastGuessTime = System.currentTimeMillis()

        guessLocation = event.guessLocation
        setTargetLocation(event.guessLocation)
    }

    fun setTargetLocation(location: LorenzVec) {
        targetLocation = location

        if (config.burrowNearestWarp) {
            BurrowWarpHelper.shouldUseWarps(location)
        }
        if (config.burrowsNearbyDetection) {
            checkRemoveGuess(false)
        }
    }

    @SubscribeEvent
    fun onBurrowDetect(event: BurrowDetectEvent) {
        EntityMovementData.addToTrack(Minecraft.getMinecraft().thePlayer)
        particleBurrows = particleBurrows.editCopy { this[event.burrowLocation] = event.type }

        if (config.burrowsNearbyDetection) {
            checkRemoveGuess(true)
        }
    }

    private fun checkRemoveGuess(animation: Boolean) {
        guessLocation?.let { guessRaw ->
            val guess = findBlock(guessRaw)
            if (particleBurrows.any { guess.distance(it.key) < 40 }) {
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
        particleBurrows = particleBurrows.editCopy { remove(location) }
        if (particleBurrows.isNotEmpty()) {
            animationLocation = location
        }
        lastDug = location
    }

    @SubscribeEvent
    fun onPlayerMove(event: EntityMoveEvent) {
        if (event.distance > 10 && event.entity == Minecraft.getMinecraft().thePlayer) {
            teleportedLocation = event.newLocation
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message.startsWith("§c ☠ §r§7You were killed by §r")) {
            particleBurrows = particleBurrows.editCopy { keys.removeIf { this[it] == BurrowType.MOB } }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        guessLocation = null
        targetLocation = null
        animationLocation = null
        lastDug = null
        particleBurrows = particleBurrows.editCopy { clear() }
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
        sendTip(event)

        val playerLocation = LocationUtils.playerLocation()
        if (config.inquisitorSharing.enabled) {
            for (inquis in InquisitorWaypointShare.waypoints.values) {
                val playerName = inquis.fromPlayer
                val location = inquis.location
                event.drawColor(location, LorenzColor.LIGHT_PURPLE)
                val distance = location.distance(playerLocation)
                if (distance > 10) {
                    val formattedDistance = LorenzUtils.formatInteger(distance.toInt())
                    event.drawDynamicText(location.add(0, 1, 0), "§d§lInquisitor §e${formattedDistance}m", 1.7)
                } else {
                    event.drawDynamicText(location.add(0, 1, 0), "§d§lInquisitor", 1.7)
                }
                if (distance < 5) {
                    InquisitorWaypointShare.maybeRemove(playerName)
                }
                event.drawDynamicText(location.add(0, 1, 0), "§eFrom §b$playerName", 1.6, yOff = 9f)

                if (config.inquisitorSharing.showDespawnTime) {
                    val spawnTime = inquis.spawnTime
                    val format = TimeUtils.formatDuration(75.seconds - spawnTime.passedSince())
                    event.drawDynamicText(location.add(0, 1, 0), "§eDespawns in §b$format", 1.6, yOff = 18f)
                }
            }
        }

        if (InquisitorWaypointShare.waypoints.isNotEmpty() && config.inquisitorSharing.focusInquisitor) {
            return
        }

        if (config.burrowsNearbyDetection) {
            for (burrow in particleBurrows) {
                val location = burrow.key
                val distance = location.distance(playerLocation)
                val burrowType = burrow.value
                event.drawColor(location, burrowType.color, distance > 10)
                event.drawDynamicText(location.add(0, 1, 0), burrowType.text, 1.5)
            }
        }

        if (config.burrowsSoopyGuess) {
            guessLocation?.let {
                val guessLocation = findBlock(it)
                val distance = guessLocation.distance(playerLocation)
                event.drawColor(guessLocation, LorenzColor.WHITE, distance > 10)
                event.drawDynamicText(guessLocation.add(0, 1, 0), "Guess", 1.5)
                if (distance > 5) {
                    val formattedDistance = LorenzUtils.formatInteger(distance.toInt())
                    event.drawDynamicText(guessLocation.add(0, 1, 0), "§e${formattedDistance}m", 1.7, yOff = 10f)
                }
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "diana", "event.diana")
    }

    private fun sendTip(event: RenderWorldLastEvent) {
        teleportedLocation?.let {
            teleportedLocation = null

            if (BurrowWarpHelper.currentWarp != null) {
                BurrowWarpHelper.currentWarp = null
                if (config.burrowNearestWarp) {
                    animationLocation = it
                    return
                }
            }
        }

        if (config.burrowNearestWarp) {
            BurrowWarpHelper.currentWarp?.let { warp ->
                animationLocation?.let {
                    event.drawColor(it.add(0.0, 1.0, 0.0), LorenzColor.AQUA)
                    if (it.distanceToPlayer() < 10) {
                        event.drawString(it.add(0.5, 1.5, 0.5), "§bWarp to " + warp.displayName, true)
                    }
                    return
                }
            }
        }
        if (config.burrowSmoothTransition) {
            animationLocation?.let {
                event.drawColor(it, LorenzColor.WHITE)
                animationLocation = moveAnimation(it, event)
            }
        }
    }

    private fun moveAnimation(animation: LorenzVec, event: RenderWorldLastEvent): LorenzVec? {
        val list = mutableListOf<LorenzVec>()
        if (config.burrowsNearbyDetection) {
            list.addAll(particleBurrows.keys)
        }
        targetLocation?.let {
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

        return if (System.currentTimeMillis() > lastAnimationTime + 25) {
            lastAnimationTime = System.currentTimeMillis()
            vector = vector.multiply(1 / vector.length())
            vector = vector.multiply(0.18)
            animation.add(vector)
        } else {
            animation
        }
    }
}