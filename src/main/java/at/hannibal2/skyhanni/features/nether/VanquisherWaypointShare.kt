package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.wither.WitherBoss
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds


@SkyHanniModule
object VanquisherWaypointShare {

    private val config get() = SkyHanniMod.feature.crimsonIsle.vanquisherShare
    private val patternGroup = RepoPattern.group("vanquisher.waypoint")

    /**
     * REGEX-TEST: Party > [MVP+] itsseth3: x: 100 y: 90 z: 10 | Vanquisher
     * REGEX-TEST: [MVP+] itsseth3: x: -10 y: 30 z: 22 | Vanquisher
     */
    @Suppress("MaxLineLength")
    private val vanquisherSharedPattern by patternGroup.list(
        "vanquisher.waypoint.share",
        "^(?:Party > |Guild > |Officer > )?([^:]+):.*?x:\\s*(-?[\\d.]+).*?y:\\s*(-?[\\d.]+).*?z:\\s*(-?[\\d.]+).*?Vanquisher.*",
    )

    /**
     * REGEX-TEST: Party > [MVP+] itsseth3: Vanquisher dead!
     * REGEX-TEST: [MVP+] itsseth3: Vanquisher dead!
     */

    private val vanquisherDiedPattern by patternGroup.pattern(
        "died",
        "^(?:.*> )?(?<playerName>[^:]+): Vanquisher dead!.*"
    )

    /**
     * REGEX-TEST: A Vanquisher is spawning nearby!
     */

    private val vanquisherSpawnedPattern by patternGroup.pattern(
        "spawned",
        ".*A Vanquisher is spawning nearby!"
    )

    private var myVanquisherId: Int? = null

    private var lastShareTime = SimpleTimeMark.farPast()

    private val vanquisherNearby = ConcurrentHashMap<Int, Entity>()

    private val sharedWaypoints = ConcurrentHashMap<String, SharedVanquisher>()

    private val waypoints: Map<String, SharedVanquisher> get() = sharedWaypoints

    data class SharedVanquisher(
        val playerName: String,
        val location: LorenzVec,
        val spawnTime: SimpleTimeMark,
    )

    private fun foundVanquisher(entityId: Int) {
        lastShareTime = SimpleTimeMark.farPast()
        myVanquisherId = entityId
        TitleManager.sendTitle("§5§lVanquisher Spawned!", "§r§7You found one nearby!")
        ChatUtils.notifyOrDisable("You Spawned a Vanquisher", config::enabled, false )

        val entity = vanquisherNearby[entityId] ?: EntityUtils.getEntityByID(entityId)
        if (entity != null) {
            val playerName = Minecraft.getInstance().player?.name?.string ?: "You"
            sharedWaypoints[playerName] = SharedVanquisher(
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

        if (myVanquisherId == null) {
            val closestId = vanquisherNearby.values.minByOrNull { it.distanceToPlayer() }
            if (closestId != null) {
                myVanquisherId = closestId.id
            } else {
                ChatUtils.debug("Trying to send Vanquisher via chat, but no mob found nearby.")
                return
            }
        }

        val entity = vanquisherNearby[myVanquisherId] ?: EntityUtils.getEntityByID(myVanquisherId!!)

        if (entity == null || entity.deceased) {
            ChatUtils.chat("No Vanquisher found")
            return
        }

        val location = entity.getLorenzVec()
        val x = location.x.toInt()
        val y = location.y.toInt()
        val z = location.z.toInt()
        if (PartyApi.isInParty()) {
            HypixelCommands.partyChat("x: $x, y: $y, z: $z | Vanquisher")
        } else if (config.readGlobalChat == isEnabled()) {
            HypixelCommands.allChat("x: $x, y: $y, z: $z | Vanquisher")
        }
    }

    private fun sendVanquisherDeath() {
        if (!isEnabled()) return
        if (lastShareTime.passedSince() < 2.seconds) return
        if (myVanquisherId == null) return

        myVanquisherId = null
        if (PartyApi.isInParty()) {
            HypixelCommands.partyChat("Vanquisher dead!")
        } else if (config.readGlobalChat == isEnabled()) {
            HypixelCommands.allChat("Vanquisher dead!")
        }
    }

    private fun isEnabled() = config.enabled

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        sharedWaypoints.clear()
        vanquisherNearby.clear()
        myVanquisherId = null

    }

    @HandleEvent
    fun onKeyPressEvent(event: KeyPressEvent) {
        if (!isEnabled()) return
        if (Minecraft.getInstance().screen != null) return
        if (event.keyCode == config.keybindSharing) sendVanquisher()
    }

    @HandleEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!isEnabled()) return
        if (event.health > 0) return

        val entityId = event.entity.id
        if (entityId == myVanquisherId) {
            sendVanquisherDeath()
        }
        vanquisherNearby.remove(entityId)
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (event.repeatSeconds(3)) {
            sharedWaypoints.values.removeIf { it.spawnTime.passedSince() > 60.seconds }
            vanquisherNearby.values.removeIf { it.deceased }

        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE, receiveCancelled = true)
    fun onReadChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        val message = event.cleanMessage

        if (vanquisherSpawnedPattern.matches(message)) {
            if (myVanquisherId == null) {
                val closestId = vanquisherNearby.values.minByOrNull {
                    it.distanceToPlayer()
                }
                if (closestId != null) { foundVanquisher(closestId.id) }
            }
        }
        vanquisherSharedPattern.matchMatchers(message) {
            val rawName = group("playerName").trim()
            val x = group("x").toDoubleOrNull() ?: return@matchMatchers
            val y = group("y").toDoubleOrNull() ?: return@matchMatchers
            val z = group("z").toDoubleOrNull() ?: return@matchMatchers

            val name = rawName.cleanPlayerName()
            val playerDisplayName = rawName.cleanPlayerName(displayName = true)

            val yourName = PlayerUtils.getName()
            val playerIsYou = name.equals(yourName, ignoreCase = true)

            val location = LorenzVec(x, y, z)

            if (playerIsYou) {
                sharedWaypoints[name] = SharedVanquisher(playerDisplayName, location, SimpleTimeMark.now())
            } else {
                ChatUtils.notifyOrDisable("$playerDisplayName§r found a Vanquisher at §b${x.toInt()} ${y.toInt()} ${z.toInt()}§r!", config::enabled, false)
                TitleManager.sendTitle("§5§lVanquisher from $playerDisplayName")

                sharedWaypoints[name] = SharedVanquisher(playerDisplayName, location, SimpleTimeMark.now())

                event.blockedReason = "vanquisher_waypoint"
            }
        }
        vanquisherDiedPattern.matchMatcher(message) {
            val simpleName = group("playerName")
            val name = simpleName.cleanPlayerName()
            sharedWaypoints.remove(name)
        }
    }

    @HandleEvent
    fun onRawEntityJoin(event: EntityEnterWorldEvent<WitherBoss>) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity.name.string.equals("Wither", ignoreCase = true)) {
            vanquisherNearby[entity.id] = entity

            if (entity.distanceToPlayer() < 15.0) {
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
                if (waypoint.spawnTime.passedSince() > 60.seconds) continue

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
                event.drawLineToEye(
                    location = waypoint.location,
                    color = beaconColor,
                    lineWidth = 3,
                    depth = false
                )
            }
        }
    }
}
