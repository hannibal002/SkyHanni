package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.diana.RareDianaMobFoundEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.hasGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object RareMobWaypointShare {

    private val config get() = SkyHanniMod.feature.event.diana.inquisitorSharing

    private val patternGroup = RepoPattern.group("diana.waypoints.inquisitor")

    /**
     * REGEX-TEST: §9Party §8> User Name§f: §rx: 2.3, y: 4.5, z: -6.7
     * REGEX-TEST: §9Party §8> UserName§f: §rA MINOS INQUISITOR has spawned near [Foraging Island ] at Coords 1 2 -3
     * REGEX-TEST: §9Party §8> §b[MVP§9+§b] _088§f: §rx: 86, y: 73, z: -29 I dug up an inquisitor come over here!
     * REGEX-TEST: §9Party §8> §6[MVP§0++§6] scaryron§f: §rx: -67, y: 75, z: 116 | Minos Inquisitor spawned at [ ⏣ Mountain ]!
     * REGEX-TEST: §9Party §8> §b[MVP§5+§b] Throwpo§f: §rx: -144, y: 59, z: -119 | Sphinx
     */
    @Suppress("MaxLineLength")
    private val rareMobCoordsPattern by patternGroup.list(
        "coords",
        "(?<party>§9Party §8> )?(?<playerName>.+)§f: §rx: (?<x>[^ ,]+),? y: (?<y>[^ ,]+),? z: (?<z>[^ ,]+)(?<mobName> \\| .*)?.*",
        "(?<party>§9Party §8> )?(?<playerName>.+)§f: §rA MINOS INQUISITOR has spawned near \\[(?<area>.*)] at Coords (?<x>[^ ]+) (?<y>[^ ]+) (?<z>[^ ]+)",
    )

    /**
     * REGEX-TEST: §9Party §8> User Name§f: §rInquisitor dead!
     * REGEX-TEST: §9Party §8> User Name§f: §rRare Diana Mob dead!
     */
    private val diedPattern by patternGroup.pattern(
        "died",
        "(?<party>§9Party §8> )?(?<playerName>.*)§f: §r(?:Inquisitor|Rare Diana Mob) dead!",
    )

    /**
     * REGEX-TEST: §c§lUh oh! §r§eYou dug out a §r§2Minos Inquisitor§r§e!
     */
    private val rareMobFoundChatPattern by patternGroup.pattern(
        "dug",
        ".* §r§eYou dug out a §.§.(?:Minos Inquisitor|Sphinx|King Minos|Manticore)§.§.!",
    )


    private var rareMob = -1
    private var lastRareMob = -1
    private var lastShareTime = SimpleTimeMark.farPast()

    private val rareMobsNearby = ConcurrentHashMap<Int, RemotePlayer>()

    private val _waypoints = ConcurrentHashMap<String, SharedRareMob>()
    val waypoints: Map<String, SharedRareMob>
        get() = _waypoints

    class SharedRareMob(
        val fromPlayer: String,
        val playerDisplayName: String,
        val location: LorenzVec,
        val spawnTime: SimpleTimeMark,
        val mobName: String
    )

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        if (event.repeatSeconds(3)) {
            rareMobsNearby.removeIf { it.value.deceased }
        }

        _waypoints.removeIf { it.value.spawnTime.passedSince() > 75.seconds }
    }

    @HandleEvent
    fun onWorldChange() {
        _waypoints.clear()
        rareMobsNearby.clear()
    }

    private val rareMobTime = mutableListOf<SimpleTimeMark>()

    @HandleEvent
    fun onRareDianaMobFound(event: RareDianaMobFoundEvent) {
        val rareMob = event.entity
        rareMobsNearby[rareMob.id] = rareMob
        GriffinBurrowHelper.update()

        lastRareMob = rareMob.id
        checkRareMobFound()
    }

    // We do not know if the chat message or the entity spawn happens first.
    // We only want to run foundRareMob when both happens in under 1.5 seconds
    private fun checkRareMobFound() {
        rareMobTime.add(SimpleTimeMark.now())

        val lastTwo = rareMobTime.takeLast(2)
        if (lastTwo.size != 2) return

        if (lastTwo.all { it.passedSince() < 1.5.seconds }) {
            rareMobTime.clear()
            foundRareMob(lastRareMob)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB, receiveCancelled = true)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        val message = event.message

        if (rareMobFoundChatPattern.matches(message)) {
            checkRareMobFound()
        }

        rareMobCoordsPattern.matchMatchers(message) {
            if (!detectFromChat()) return@matchMatchers
            event.blockedReason = "rare_diana_mob_waypoint"
        }

        diedPattern.matchMatcher(message) {
            if (block()) return
            val rawName = group("playerName")
            val name = rawName.cleanPlayerName()
            _waypoints.remove(name)
            GriffinBurrowHelper.update()
        }
    }

    private fun foundRareMob(rareMobId: Int) {
        lastShareTime = SimpleTimeMark.farPast()
        rareMob = rareMobId

        if (config.instantShare) {
            // add repo kill switch
            sendRareMob()
        } else {
            val keyName = KeyboardManager.getKeyName(config.keyBindShare)
            val message = "§l§bYou found a Rare Diana Mob! Click §l§chere §l§bor press §c$keyName to share the location!"
            ChatUtils.clickableChat(
                message,
                onClick = ::sendRareMob,
                "§eClick to share!",
                oneTimeClick = true,
            )
        }
    }

    @HandleEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!isEnabled()) return
        if (event.health > 0) return

        val entityId = event.entity.id
        if (entityId == rareMob) {
            sendDeath()
        }
        rareMobsNearby.remove(entityId)
    }

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        if (!isEnabled()) return
        if (Minecraft.getInstance().screen != null) return
        if (event.keyCode == config.keyBindShare) sendRareMob()
    }

    private fun sendDeath() {
        if (!isEnabled()) return
        if (lastShareTime.passedSince() < 5.seconds) return

        // already dead
        if (rareMob == -1) return
        rareMob = -1
        HypixelCommands.partyChat("Rare Diana Mob dead!")
    }

    private fun sendRareMob() {
        if (!isEnabled()) return
        if (lastShareTime.passedSince() < 5.seconds) return
        lastShareTime = SimpleTimeMark.now()

        if (rareMob == -1) {
            ChatUtils.debug("Trying to send Rare Diana Mob via chat, but no mob found nearby.")
            return
        }

        val rareMob = EntityUtils.getEntityByID(rareMob)
        if (rareMob == null) {
            ChatUtils.chat("§cRare Mob out of range!")
            return
        }

        if (rareMob.deceased) {
            ChatUtils.chat("§cRare Mob is dead")
            return
        }
        val location = rareMob.getLorenzVec()
        val x = location.x.toInt()
        val y = location.y.toInt()
        val z = location.z.toInt()
        val mobName = rareMob.name.string.orEmpty()
        val name = if (mobName.isEmpty()) "" else "| $mobName"
        HypixelCommands.partyChat("x: $x, y: $y, z: $z $name")
    }

    private fun Matcher.block(): Boolean = !hasGroup("party") && !config.globalChat

    private fun Matcher.detectFromChat(): Boolean {
        if (block()) return false
        val rawPlayerName = group("playerName")
        val x = group("x").trim().toDoubleOrNull() ?: return false
        val y = group("y").trim().toDoubleOrNull() ?: return false
        val z = group("z").trim().toDoubleOrNull() ?: return false
        val location = LorenzVec(x, y, z)

        val rawMobName = if (hasGroup("mobName")) group("mobName").replace(" | ", "").trim().lowercase() else "Rare Mob"
        var mobName = "Rare Mob"
        for (mob in DianaApi.mythologicalCreatures.values) {
            if (!mob.rare) continue
            if (rawMobName !in mob.mobAliases) continue
            mobName = mob.cleanName
        }

        val optionalAn = StringUtils.optionalAn(mobName)

        val name = rawPlayerName.cleanPlayerName()
        val playerDisplayName = rawPlayerName.cleanPlayerName(displayName = true)
        if (!waypoints.containsKey(name)) {
            ChatUtils.chat("$playerDisplayName §l§efound $optionalAn $mobName at §l§c${x.toInt()} ${y.toInt()} ${z.toInt()}!")
            if (name != PlayerUtils.getName()) {
                TitleManager.sendTitle("§d$mobName §efrom §b$playerDisplayName")
                playUserSound()
            }
        }
        _waypoints[name] = SharedRareMob(name, playerDisplayName, location, SimpleTimeMark.now(), mobName)
        GriffinBurrowHelper.update()
        return true
    }

    private fun isEnabled() = DianaApi.isDoingDiana() && config.enabled

    fun maybeRemove(sharedMob: SharedRareMob) {
        if (rareMobsNearby.isEmpty()) {
            _waypoints.remove(sharedMob.fromPlayer)
            GriffinBurrowHelper.update()
            ChatUtils.chat("${sharedMob.mobName} from ${sharedMob.playerDisplayName} §enot found, deleting.")
        }
    }

    @JvmStatic
    fun playUserSound() {
        with(config.sound) {
            SoundUtils.createSound(name, pitch).playSound()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetdianamobs") {
            description = "Resets all saved rare Diana mob locations"
            category = CommandCategory.USERS_RESET
            callback {
                _waypoints.clear()
                rareMobsNearby.clear()
                ChatUtils.chat("Manually reset all rare mob data.")
            }
        }
    }
}
