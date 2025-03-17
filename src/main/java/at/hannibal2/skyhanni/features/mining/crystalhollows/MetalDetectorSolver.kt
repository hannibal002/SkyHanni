package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MetalDetectorSolver {

    /**
     * REGEX-TEST: §7§65,453/5,078❤     §a927§a❈ Defense     §3§lTREASURE: §b79.2m§r
     */
    private val metalDetectorDistancePattern by RepoPattern.pattern(
        "mining.crystalnucleus.metaldetector.treasure",
        "§3§lTREASURE: §b(?<distance>.*)m"
    )

    /**
     * REGEX-TEST: §aYou found §r§a☘ Flawed Jade Gemstone §r§8x4 §r§awith your §r§cMetal Detector§r§a!
     */
    private val treasureFoundPattern by RepoPattern.pattern(
        "mining.crystalnucleus.metaldetector.treasurefound",
        "§aYou found .*with your §r§cMetal Detector§r§a!"
    )

    private val config get() = SkyHanniMod.feature.mining.metalDetector.metalDetectorSolver

    private var chestLocations: List<LorenzVec> = emptyList()
    private val predictedChestLocations: MutableList<LorenzVec> = mutableListOf()
    private var baseCoordinates: LorenzVec? = null
    private var ignoreLocation: LorenzVec? = null
    private var lastSearchedForBase: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastLoc: LorenzVec? = null
    private var playedPling = false

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        chestLocations = event.getConstant<List<List<Int>>>("MetalDetectorChests").map { LorenzVec(it[0], it[1] + 12, it[2]) }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        if (!isEnabled()) return
        if (predictedChestLocations.size == 1) return

        val player = Minecraft.getMinecraft().thePlayer.getLorenzVec()
        if (lastLoc != player) {
            lastLoc = player
            playedPling = false
        }

        metalDetectorDistancePattern.matchMatcher(event.actionBar) {
            val distance = group("distance").toDoubleOrNull() ?: return

            if (baseCoordinates == null) findBaseCoordinates()
            val baseCoordinatesNonNull = baseCoordinates ?: return

            predictedChestLocations.clear()
            chestLocations.forEach {
                val loc = baseCoordinatesNonNull.plus(it.negated())

                if (loc == ignoreLocation) {
                    ignoreLocation = null
                    return
                }
                if (loc.add(0, 1, 0).distanceToPlayer().roundTo(1) == distance) {
                    if (predictedChestLocations.size == 0 && !playedPling) {
                        SoundUtils.plingSound.playSound()
                        playedPling = true
                    }

                    predictedChestLocations.add(loc)
                }
            }

            if (predictedChestLocations.size == 0) {
                ChatUtils.chat(
                    "No chests found. Try standing still with the metal detector in a different spot.",
                    replaceSameMessage = true
                )
            } else if (predictedChestLocations.size > 1) {
                ChatUtils.chat(
                    "${predictedChestLocations.size} possible locations found. " +
                        "Please try standing still with the metal detector in a different spot.",
                    replaceSameMessage = true
                )
            }
        }

    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        if (!treasureFoundPattern.matches(event.message)) return

        playedPling = false
        predictedChestLocations.clear()
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        predictedChestLocations.forEach {
            event.drawColor(it, LorenzColor.GOLD)
            event.drawLineToEye(it.add(0.5, 0.5, 0.5), LorenzColor.WHITE.toColor(), 3, false)
            event.drawWaypointFilled(it, LorenzColor.RED.toColor(), true, true)
            event.drawString(it, "Treasure: §e${it.distanceToPlayer().roundTo(1)}m", true)
        }
    }

    @HandleEvent
    fun onWorldSwap(event: WorldChangeEvent) {
        baseCoordinates = null
        lastSearchedForBase = SimpleTimeMark.farPast()
        predictedChestLocations.clear()
        ignoreLocation = null
        lastLoc = null
        playedPling = false
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (predictedChestLocations.size == 1) {
            val distanceSq = predictedChestLocations[0].distanceSqToPlayer()
            if (distanceSq <= 25) {
                ignoreLocation = predictedChestLocations[0]
                predictedChestLocations.clear()
            }
        } else if (predictedChestLocations.isEmpty() && ignoreLocation?.let { it.distanceSqToPlayer() > 100 } == true) ignoreLocation = null
    }

    private fun findBaseCoordinates() {
        if (lastSearchedForBase.passedSince() < 15.seconds) return
        lastSearchedForBase = SimpleTimeMark.now()
        val player = Minecraft.getMinecraft().thePlayer.getLorenzVec()
        val playerInt = LorenzVec(player.x.toInt(), player.y.toInt(), player.z.toInt())

        for (i in -50 until 50) {
            for (j in 30 downTo -30) {
                for (k in -50 until 50) {
                    val blockPosition = playerInt.add(i, j, k)
                    val nextBlockPosition = blockPosition.add(0, 13, 0)
                    if (blockPosition.getBlockAt() == Blocks.quartz_stairs && nextBlockPosition.getBlockAt() == Blocks.barrier) {
                        baseCoordinates = getBaseCoordinates(nextBlockPosition)
                        return
                    }
                }
            }
        }
    }

    private fun getBaseCoordinates(blockPosition: LorenzVec): LorenzVec {
        var changed = true
        var currentPosition = blockPosition
        while (changed) {
            changed = false
            if (currentPosition.add(1, 0, 0).getBlockAt() == Blocks.barrier) {
                changed = true
                currentPosition = currentPosition.add(1, 0, 0)
            }
            if (currentPosition.add(0, 1, 0).getBlockAt() == Blocks.barrier) {
                changed = true
                currentPosition = currentPosition.add(0, 1, 0)
            }
            if (currentPosition.add(0, 0, 1).getBlockAt() == Blocks.barrier) {
                changed = true
                currentPosition = currentPosition.add(0, 0, 1)
            }
        }
        return currentPosition
    }

    fun isEnabled() = config && MiningApi.inMinesOfDivan()
}
