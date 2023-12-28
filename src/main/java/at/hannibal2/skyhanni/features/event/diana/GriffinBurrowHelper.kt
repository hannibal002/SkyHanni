package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.events.BurrowDetectEvent
import at.hannibal2.skyhanni.events.BurrowDugEvent
import at.hannibal2.skyhanni.events.BurrowGuessEvent
import at.hannibal2.skyhanni.events.EntityMoveEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.KeyboardManager
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
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.seconds

object GriffinBurrowHelper {
    private val config get() = SkyHanniMod.feature.event.diana

    private var targetLocation: LorenzVec? = null
    private var guessLocation: LorenzVec? = null
    private var particleBurrows = mapOf<LorenzVec, BurrowType>()

    private var lastGuessTime = 0L

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(1)) return

        update()
    }

    private fun update() {
        if (config.burrowsNearbyDetection) {
            checkRemoveGuess()
        }

        val locations = particleBurrows.keys.toMutableList()
        guessLocation?.let {
            locations.add(findBlock(it))
        }
        locations.addAll(InquisitorWaypointShare.waypoints.values.map { it.location })
        targetLocation = locations.minByOrNull { it.distanceToPlayer() }

        if (config.burrowNearestWarp) {
            targetLocation?.let {
                BurrowWarpHelper.shouldUseWarps(it)
            }
        }
    }

    @SubscribeEvent
    fun onBurrowGuess(event: BurrowGuessEvent) {
        EntityMovementData.addToTrack(Minecraft.getMinecraft().thePlayer)
        lastGuessTime = System.currentTimeMillis()

        guessLocation = event.guessLocation
        update()
    }

    @SubscribeEvent
    fun onBurrowDetect(event: BurrowDetectEvent) {
        EntityMovementData.addToTrack(Minecraft.getMinecraft().thePlayer)
        particleBurrows = particleBurrows.editCopy { this[event.burrowLocation] = event.type }
        update()
    }

    private fun checkRemoveGuess() {
        guessLocation?.let { guessRaw ->
            val guess = findBlock(guessRaw)
            if (particleBurrows.any { guess.distance(it.key) < 40 }) {
                guessLocation = null
            }
        }
    }

    @SubscribeEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        val location = event.burrowLocation
        particleBurrows = particleBurrows.editCopy { remove(location) }
        update()
    }

    @SubscribeEvent
    fun onPlayerMove(event: EntityMoveEvent) {
        if (!isEnabled()) return
        if (event.distance > 10 && event.entity == Minecraft.getMinecraft().thePlayer) {
            update()
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (event.message.startsWith("§c ☠ §r§7You were killed by §r")) {
            particleBurrows = particleBurrows.editCopy { keys.removeIf { this[it] == BurrowType.MOB } }
        }
        if (event.message == "§6Poof! §r§eYou have cleared your griffin burrows!") {
            guessLocation = null
            particleBurrows = emptyMap()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        guessLocation = null
        particleBurrows = emptyMap()
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
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        sendTip(event)

        val playerLocation = LocationUtils.playerLocation()
        if (config.inquisitorSharing.enabled) {
            for (inquis in InquisitorWaypointShare.waypoints.values) {
                val location = inquis.location
                event.drawColor(location, LorenzColor.LIGHT_PURPLE)
                val distance = location.distance(playerLocation)
                if (distance > 10) {
                    val formattedDistance = LorenzUtils.formatInteger(distance.toInt())
                    event.drawDynamicText(location.add(y = 1), "§d§lInquisitor §e${formattedDistance}m", 1.7)
                } else {
                    event.drawDynamicText(location.add(y = 1), "§d§lInquisitor", 1.7)
                }
                if (distance < 5) {
                    InquisitorWaypointShare.maybeRemove(inquis)
                }
                event.drawDynamicText(location.add(y = 1), "§eFrom §b${inquis.displayName}", 1.6, yOff = 9f)

                if (config.inquisitorSharing.showDespawnTime) {
                    val spawnTime = inquis.spawnTime
                    val format = (75.seconds - spawnTime.passedSince()).format()
                    event.drawDynamicText(location.add(y = 1), "§eDespawns in §b$format", 1.6, yOff = 18f)
                }
            }
        }

        if (config.lineToNext) {
            val player = event.exactPlayerEyeLocation()

            var color: LorenzColor?
            val renderLocation = if (BurrowWarpHelper.currentWarp != null) {
                color = LorenzColor.AQUA
                player.add(y = -5)
            } else {
                color = LorenzColor.WHITE
                targetLocation?.add(0.5, 0.5, 0.5) ?: return
            }

            val lineWidth = if (targetLocation in particleBurrows) {
                color = particleBurrows[targetLocation]!!.color
                3
            } else 2
            event.draw3DLine(player, renderLocation, color.toColor(), lineWidth, false)
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
                event.drawDynamicText(location.add(y = 1), burrowType.text, 1.5)
            }
        }

        if (config.burrowsSoopyGuess) {
            guessLocation?.let {
                val guessLocation = findBlock(it)
                val distance = guessLocation.distance(playerLocation)
                event.drawColor(guessLocation, LorenzColor.WHITE, distance > 10)
                val color = if (BurrowWarpHelper.currentWarp == null) "§f" else "§b"
                event.drawDynamicText(guessLocation.add(y = 1), "${color}Guess", 1.5)
                if (distance > 5) {
                    val formattedDistance = LorenzUtils.formatInteger(distance.toInt())
                    event.drawDynamicText(guessLocation.add(y = 1), "§e${formattedDistance}m", 1.7, yOff = 10f)
                }
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "diana", "event.diana")
    }

    private fun sendTip(event: LorenzRenderWorldEvent) {

        if (!config.burrowNearestWarp) return
        val warp = BurrowWarpHelper.currentWarp ?: return

        val location = event.exactPlayerEyeLocation().add(y = -5)
        val text = "§bWarp to " + warp.displayName
        val keybindSuffix = if (config.keyBindWarp != Keyboard.KEY_NONE) {
            val keyname = KeyboardManager.getKeyName(config.keyBindWarp)
            " §7(§ePress $keyname§7)"
        } else ""
        event.drawString(location, text + keybindSuffix, true)
    }

    private fun isEnabled() = DianaAPI.isDoingDiana()
}
