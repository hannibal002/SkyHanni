package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.hasGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import net.minecraft.world.entity.Entity
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.world.entity.boss.wither.WitherBoss
import java.awt.Color


@SkyHanniModule
object VanquisherWaypointShare {

    private val config get() = SkyHanniMod.feature.crimsonIsle.vanquisherSharing
    private val patternGroup = RepoPattern.group("vanquisher.waypoint")



    /**
     * REGEX-TEST: §9Party §8> User Name§f: x: 2.3, y: 4.5, z: -6.7 | Vanquisher
     * REGEX-TEST: §9Party §8> §b[MVP§9+§b] itsseth3§f: x: 86, y: 73, z: -29 | Vanquisher
     */
    @Suppress("MaxLineLength")
    private val vanquisherSharedPattern by patternGroup.list(
        "coords-clean",
        "(?<party>Party > )?(?:\\[.*?] )?(?<playerName>[a-zA-Z0-9_]+): x: (?<x>-?[\\d.]+),? y: (?<y>-?[\\d.]+),? z: (?<z>-?[\\d.]+) \\| Vanquisher.*"
    )

    /**
     * REGEX-TEST: §9Party §8> User Name§f: Vanquisher dead!
     */

    private val vanquisherDiedPattern by patternGroup.pattern(
        "died",
        "(?<party>§9Party §8> )?(?<playerName>.*)§f: §rVanquisher dead!",
    )

    /**
     * REGEX-TEST: A Vanquisher is spawning nearby!
     */

    private val vanquisherSpawnedPattern by patternGroup.pattern(
        "spawned",
        ".*A Vanquisher is spawning nearby!"
    )

    private var myVanquisherId = -1

    private var lastShareTime = SimpleTimeMark.farPast()

    private val vanquisherNearby = ConcurrentHashMap<Int, Entity>()

    private val sharedWaypoints = ConcurrentHashMap<String, SharedVanquisher>()

    val waypoints: Map<String, SharedVanquisher> get() = sharedWaypoints

    class SharedVanquisher(
        val fromPlayer: String,
        val playerName: String,
        val location: LorenzVec,
        val spawnTime: SimpleTimeMark,
    )

    private fun foundVanquisher(entityId: Int) {
        lastShareTime = SimpleTimeMark.farPast()
        myVanquisherId = entityId

        TitleManager.sendTitle(
            "§5§lVanquisher Spawned!",
            "§r§7You found one nearby!",
            5.seconds,
            null,
            TitleManager.TitleLocation.GLOBAL,
            TitleManager.TitleAddType.FORCE_FIRST
        )

        val entity = vanquisherNearby[entityId] ?: EntityUtils.getEntityByID(entityId)
        if (entity != null) {
            val playerName = Minecraft.getInstance().player?.name?.string ?: "You"
            sharedWaypoints[playerName] = SharedVanquisher(
                playerName,
                playerName,
                entity.getLorenzVec(),
                SimpleTimeMark.now()
            )
        }

        if (config.instantShare) {
            sendVanquisher()
        } else {
            val keyName = KeyboardManager.getKeyName(config.keybindSharing)
            val message = "You found a Vanquisher! Click here or press $keyName to share!"
            ChatUtils.clickableChat(message, onClick = ::sendVanquisher, hover = "Click to share!", oneTimeClick = true)
        }
    }

    private fun sendVanquisher() {
        if (!isEnabled()) return
        if (lastShareTime.passedSince() < 5.seconds) return
        lastShareTime = SimpleTimeMark.now()

        if (myVanquisherId == -1) {
            val closestId = vanquisherNearby.values.minByOrNull { it.distanceToPlayer() }
            if(closestId != null) {
                myVanquisherId = closestId.id
            } else {
                ChatUtils.debug("Trying to send Vanquisher via chat, but no mob found nearby.")
                return
            }
        }

        val entity = vanquisherNearby[myVanquisherId]?: EntityUtils.getEntityByID(myVanquisherId)

        if (entity == null || entity.deceased) {
            ChatUtils.chat("§cRare Mob is dead")
            return
        }

        val location = entity.getLorenzVec()
        val x = location.x.toInt()
        val y = location.y.toInt()
        val z = location.z.toInt()

        HypixelCommands.partyChat("x: $x, y: $y, z: $z | Vanquisher")
    }

    private fun sendVanquisherDeath() {
        if (!isEnabled()) return
        if (lastShareTime.passedSince() < 2.seconds) return
        if (myVanquisherId == -1) return

        myVanquisherId = -1
        HypixelCommands.partyChat("Vanquisher dead!")
    }

    private fun isEnabled() = config.enabled

    private fun Matcher.block(): Boolean = !hasGroup("party") && !config.readGlobalChat

    private fun Matcher.detectFromChat(): Boolean {
        if (block()) return false
        val playerName = group("playerName")
        val x = group("x").trim().toDoubleOrNull() ?: return false
        val y = group("y").trim().toDoubleOrNull() ?: return false
        val z = group("z").trim().toDoubleOrNull() ?: return false
        val location = LorenzVec(x, y, z)

        val name = playerName.cleanPlayerName()
        val playerDisplayName = playerName.cleanPlayerName(displayName = true)
        val yourName = Minecraft.getInstance().player?.name?.string?: ""
        val playerIsYou = name.equals(yourName, ignoreCase = true)

        if(playerIsYou) {
            sharedWaypoints[name] = SharedVanquisher(name, playerDisplayName, location, SimpleTimeMark.now())
            return false
        }
        ChatUtils.chat("$playerDisplayName found a Vanquisher at ${x.toInt()} ${y.toInt()} ${z.toInt()}!")

        TitleManager.sendTitle(
            "§5§lVanquisher from $playerDisplayName",
            null,
            5.seconds,
            null,
            TitleManager.TitleLocation.GLOBAL,
            TitleManager.TitleAddType.FORCE_FIRST
        )
        sharedWaypoints[name] = SharedVanquisher(name, playerDisplayName, location, SimpleTimeMark.now())
        return true
    }

    @HandleEvent
    fun whenChangeWorld(event: WorldChangeEvent) {
        sharedWaypoints.clear()
        vanquisherNearby.clear()
        myVanquisherId = -1
    }

    @HandleEvent
    fun keyPressEvent(event: KeyPressEvent) {
        if (!isEnabled()) return
        if (Minecraft.getInstance().screen != null) return
        if (event.keyCode == config.keybindSharing) sendVanquisher()
    }

    @HandleEvent
    fun checkVanquisherHealth(event: EntityHealthUpdateEvent) {
        if (!isEnabled()) return
        if (event.health > 0) return

        val entityId = event.entity.id
        if (entityId == myVanquisherId) {
            sendVanquisherDeath()
        }
        vanquisherNearby.remove(entityId)
    }

    @HandleEvent
    fun checkSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        sharedWaypoints.values.removeIf { it.spawnTime.passedSince() > 60.seconds }

        if (event.repeatSeconds(3)) {
            vanquisherNearby.values.removeIf { it.deceased || !it.isAlive }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE, receiveCancelled = true)
    fun readChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        val message = event.cleanMessage

        if (vanquisherSpawnedPattern.matches(message)) {
            if (myVanquisherId == -1) {
                val closestId = vanquisherNearby.values.minByOrNull {
                    it.distanceToPlayer()
                }
                if (closestId != null) { foundVanquisher(closestId.id) }
            }
        }

        vanquisherSharedPattern.matchMatchers(message) {
            if (!detectFromChat()) return@matchMatchers
            event.blockedReason = "vanquisher_waypoint"
        }

        vanquisherDiedPattern.matchMatcher(message) {
            if (block()) return
            val simpleName = group("playerName")
            val name = simpleName.cleanPlayerName()
            sharedWaypoints.remove(name)
        }
    }

    @HandleEvent
    fun onRawEntityJoin(event: EntityEnterWorldEvent<WitherBoss>) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity.name.string.equals("Wither", ignoreCase = true) || entity.name.string.contains("Vanquisher", ignoreCase = true)) {
            vanquisherNearby[entity.id] = entity

            val player = Minecraft.getInstance().player ?: return
            if (entity.distanceTo(player) < 15.0) {
                if (myVanquisherId != entity.id) {
                    foundVanquisher(entity.id)
                }
            }
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        with(WorldRenderUtils) {
            for (waypoint in waypoints.values) {
                if (waypoint.spawnTime.passedSince() > 30.seconds) continue

                val beaconColor = Color(160, 37, 191)

                event.drawWaypointFilled(
                    location = waypoint.location,
                    color = beaconColor,
                    seeThroughBlocks = true,
                    beacon = false
                )
                event.renderBeaconBeam(
                    waypoint.location,
                    beaconColor.rgb
                )
            }
        }
    }
}
