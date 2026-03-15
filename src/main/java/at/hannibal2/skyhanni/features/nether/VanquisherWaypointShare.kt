package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.VanquisherApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.combat.VanquisherEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.renderBeaconBeam
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
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
    private val sharedPattern by patternGroup.list(
        "share",
        "^(?<channel>Party > |Guild > |Officer > )?(?<playerName>[^:]+):.*?x:\\s*(?<x>-?[\\d.]+).*?y:\\s*(?<y>-?[\\d.]+).*?z:\\s*(?<z>-?[\\d.]+).*?Vanquisher.*",
    )

    /**
     * REGEX-TEST: Party > [MVP+] itsseth3: Vanquisher dead!
     * REGEX-TEST: [MVP+] itsseth3: Vanquisher dead!
     */

    private val diedPattern by patternGroup.pattern(
        "died",
        "^(?<channel>.*> )?(?<playerName>[^:]+): Vanquisher dead!.*",
    )

    private var ownVanquisherData: VanquisherApi.VanquisherData? = null

    private var lastShareTime = SimpleTimeMark.farPast()

    private val sharedWaypoints = ConcurrentHashMap<String, SharedVanquisher>()

    data class SharedVanquisher(
        val playerName: String,
        val location: LorenzVec,
        val spawnTime: SimpleTimeMark,
    )

    private fun sendSpawn() {
        if (lastShareTime.passedSince() < 5.seconds) return
        lastShareTime = SimpleTimeMark.now()

        val data = ownVanquisherData ?: run {
            ChatUtils.debug("Trying to send Vanquisher via chat, but no mob found nearby.")
            return
        }

        val location = data.mob.baseEntity.getLorenzVec()
        val x = location.x.toInt()
        val y = location.y.toInt()
        val z = location.z.toInt()

        val message = "x: $x, y: $y, z: $z | Vanquisher"

        if (PartyApi.isInParty()) {
            HypixelCommands.partyChat(message)
        } else if (config.readGlobalChat) {
            HypixelCommands.allChat(message)
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        sharedWaypoints.clear()
        ownVanquisherData = null
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onVanquisherSpawn(event: VanquisherEvent.Spawn) {
        if (!isEnabled()) return
        if (!event.vanquisher.isOwn) return

        lastShareTime = SimpleTimeMark.farPast()
        ownVanquisherData = event.vanquisher
        TitleManager.sendTitle("§5§lVanquisher Spawned!", "§r§7You found one nearby!")
        ChatUtils.notifyOrDisable("You Spawned a Vanquisher", config::enabled)
        val playerName = PlayerUtils.getName()
        sharedWaypoints[playerName] = SharedVanquisher(
            playerName,
            event.vanquisher.mob.baseEntity.getLorenzVec(),
            SimpleTimeMark.now(),
        )
        if (config.instantShare) {
            sendSpawn()
        } else {
            val keyName = KeyboardManager.getKeyName(config.keybindSharing)
            val message = "You found a Vanquisher! Click here or press $keyName to share!"
            ChatUtils.clickableChat(message, onClick = ::sendSpawn, hover = "Click to share!", oneTimeClick = true)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onVanquisherDeath(event: VanquisherEvent.Death) {
        if (!isEnabled()) return
        if (!event.vanquisher.isOwn) return
        if (lastShareTime.passedSince() < 2.seconds) return

        if (PartyApi.isInParty()) {
            HypixelCommands.partyChat("Vanquisher dead!")
        } else if (config.readGlobalChat) {
            HypixelCommands.allChat("Vanquisher dead!")
        }
    }

    @HandleEvent
    fun onVanquisherDeSpawn(event: VanquisherEvent.DeSpawn) {
        if (event.vanquisher == ownVanquisherData) ownVanquisherData = null
    }

    @HandleEvent
    fun onKeyPressEvent(event: KeyPressEvent) {
        if (!isEnabled()) return
        if (Minecraft.getInstance().screen != null) return
        if (event.keyCode == config.keybindSharing) sendSpawn()
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (event.repeatSeconds(3)) {
            sharedWaypoints.values.removeIf { it.spawnTime.passedSince() > 60.seconds }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE, receiveCancelled = true)
    fun onReadChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        val message = event.cleanMessage

        handleShared(message, event)
        handleDied(message)
    }

    private fun handleShared(message: String, event: SkyHanniChatEvent.Allow) {
        val (rawName, location) = sharedPattern.matchMatchers(message) {
            val channel = group("channel")
            val isGlobalChat = channel.isNullOrEmpty()

            if (isGlobalChat && !config.readGlobalChat) return

            val rawName = group("playerName").trim()
            val x = group("x").toDoubleOrNull() ?: return
            val y = group("y").toDoubleOrNull() ?: return
            val z = group("z").toDoubleOrNull() ?: return
            rawName to LorenzVec(x, y, z)
        } ?: return

        val name = rawName.cleanPlayerName()
        val playerDisplayName = rawName.cleanPlayerName(displayName = true)
        val yourName = PlayerUtils.getName()
        val playerIsYou = name.equals(yourName, ignoreCase = true)

        sharedWaypoints[name] = SharedVanquisher(playerDisplayName, location, SimpleTimeMark.now())

        if (!playerIsYou) {
            val (x, y, z) = location.toDoubleArray().map { it.toInt() }
            ChatUtils.notifyOrDisable(
                "$playerDisplayName§r found a Vanquisher at §b$x $y $z§r!",
                config::enabled,
            )
            TitleManager.sendTitle("§5§lVanquisher from $playerDisplayName")
            event.blockedReason = "vanquisher_waypoint"
        }
    }

    private fun handleDied(message: String) {
        diedPattern.matchMatcher(message) {
            val channel = group("channel")
            val isGlobalChat = channel.isNullOrEmpty()
            if (isGlobalChat && !config.readGlobalChat) return@matchMatcher

            val simpleName = group("playerName")
            val name = simpleName.cleanPlayerName()
            sharedWaypoints.remove(name)
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        val beaconColor = Color(160, 37, 191)
        sharedWaypoints.values.forEach { waypoint ->
            event.drawWaypoint(waypoint, beaconColor)
        }
    }

    private fun SkyHanniRenderWorldEvent.drawWaypoint(waypoint: SharedVanquisher, beaconColor: Color) {
        if (waypoint.spawnTime.passedSince() > 60.seconds) return
        drawWaypointFilled(
            location = waypoint.location,
            color = beaconColor,
            seeThroughBlocks = true,
            beacon = false,
        )
        renderBeaconBeam(
            waypoint.location,
            beaconColor.rgb,
        )
        drawLineToEye(
            location = waypoint.location,
            color = beaconColor,
            lineWidth = 3,
            depth = false,
        )
    }

    private fun isEnabled() = config.enabled
}
