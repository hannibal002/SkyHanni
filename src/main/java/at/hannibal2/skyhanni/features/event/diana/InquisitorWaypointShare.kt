package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.hasGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.play.server.S02PacketChat
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object InquisitorWaypointShare {

    private val config get() = SkyHanniMod.feature.event.diana.inquisitorSharing

    private val patternGroup = RepoPattern.group("diana.waypoints")

    /**
     * REGEX-TEST: §9Party §8> User Name§f: §rx: 2.3, y: 4.5, z: 6.7
     */
    private val partyOnlyCoordsPattern by patternGroup.pattern(
        "party.onlycoords",
        "(?<party>§9Party §8> )?(?<playerName>.+)§f: §rx: (?<x>[^ ,]+),? y: (?<y>[^ ,]+),? z: (?<z>[^ ,]+)"
    )

    //Support for https://www.chattriggers.com/modules/v/inquisitorchecker
    /**
     * REGEX-TEST: §9Party §8> UserName§f: §rA MINOS INQUISITOR has spawned near [Foraging Island ] at Coords 1 2 3
     */
    private val partyInquisitorCheckerPattern by patternGroup.pattern(
        "party.inquisitorchecker",
        "(?<party>§9Party §8> )?(?<playerName>.+)§f: §rA MINOS INQUISITOR has spawned near \\[(?<area>.*)] at Coords (?<x>[^ ]+) (?<y>[^ ]+) (?<z>[^ ]+)"
    )
    private val diedPattern by patternGroup.pattern(
        "died",
        "(?<party>§9Party §8> )?(?<playerName>.*)§f: §rInquisitor dead!"
    )

    private var inquisitorTime = SimpleTimeMark.farPast()
    private var testTime = SimpleTimeMark.farPast()
    private var lastInquisitorMessage = ""
    private var inquisitor = -1
    private var lastInquisitor = -1
    private var lastShareTime = SimpleTimeMark.farPast()
    private var inquisitorsNearby = emptyList<EntityOtherPlayerMP>()
    private val soonRange = (-500).milliseconds..1.5.seconds

    private val logger = LorenzLogger("diana/waypoints")

    var waypoints = mapOf<String, SharedInquisitor>()

    class SharedInquisitor(
        val fromPlayer: String,
        val displayName: String,
        val location: LorenzVec,
        val spawnTime: SimpleTimeMark,
    )

    private var test = false

    fun test() {
        test = !test
        ChatUtils.chat("Inquisitor Test " + if (test) "Enabled" else "Disabled")
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        if (event.repeatSeconds(3)) {
            inquisitorsNearby = inquisitorsNearby.editCopy { removeIf { it.isDead } }
        }

        waypoints = waypoints.editCopy { values.removeIf { it.spawnTime.passedSince() > 75.seconds } }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        waypoints = emptyMap()
        inquisitorsNearby = emptyList()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message
        // TODO repo pattern
        if (message.contains("§eYou dug out")) {
            testTime = SimpleTimeMark.now()
            lastInquisitorMessage = message

            val passedSince = inquisitorTime.passedSince()

            if (passedSince < 10.seconds) {
                logger.log(" ")
                logger.log("reverse!")
                logger.log("diff: $passedSince")
            }
            if (passedSince in soonRange) return
            foundInquisitor(lastInquisitor)
        }

        // TODO: Change the check to only one line once we have a confirmed inquis message line
        if (message.contains("§r§eYou dug out ") && message.contains("Inquis")) {
            inquisitorTime = SimpleTimeMark.now()
            logger.log("found Inquisitor")
        }
    }

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entity !is EntityOtherPlayerMP) return
        val name = entity.name
        if (test) {
            if (name != "Minos Inquisitor" && name != "Minotaur " && name != "Minos Champion") return
        } else {
            if (name != "Minos Inquisitor") return
        }
        logger.log("FOUND: $name")

        inquisitorsNearby = inquisitorsNearby.editCopy { add(entity) }
        GriffinBurrowHelper.update()

        val passedSince = inquisitorTime.passedSince()
        inquisitorTime = SimpleTimeMark.now()
        lastInquisitor = entity.entityId

        logger.log("diff: $passedSince")
        if (passedSince in soonRange) {
            val testDiff = testTime.passedSince()
            if (testDiff in soonRange) {
                logger.log("testDiff: $passedSince")
                return
            } else {
                logger.log("wrong Inquisitor message!")
            }
        }
        foundInquisitor(entity.entityId)
    }

    private fun foundInquisitor(inquisId: Int) {
        logger.log("lastInquisitorMessage: '$lastInquisitorMessage'")
        inquisitor = inquisId

        if (config.instantShare) {
            // add repo kill switch
            sendInquisitor()
        } else {
            val keyName = KeyboardManager.getKeyName(config.keyBindShare)
            val message = "§l§bYou found a Inquisitor! Press §l§chere §l§bor §c$keyName to share the location!"
            ChatUtils.clickableChat(
                message, onClick = {
                    sendInquisitor()
                },
                oneTimeClick = true
            )
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!isEnabled()) return
        if (event.health > 0) return

        val entityId = event.entity.entityId
        if (entityId == inquisitor) {
            sendDeath()
        }
        inquisitorsNearby.find { it.entityId == entityId }?.let {
            inquisitorsNearby = inquisitorsNearby.editCopy { remove(it) }
        }
    }

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!isEnabled()) return
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (event.keyCode == config.keyBindShare) sendInquisitor()
    }

    private fun sendDeath() {
        if (!isEnabled()) return
        if (lastShareTime.passedSince() > 5.seconds) return
        lastShareTime = SimpleTimeMark.now()

        if (inquisitor == -1) {
            logger.log("Inquisitor is already null!")
            return
        }
        inquisitor = -1
        HypixelCommands.partyChat("Inquisitor dead!")
    }

    fun sendInquisitor() {
        if (!isEnabled()) return
        if (lastShareTime.passedSince() > 5.seconds) return
        lastShareTime = SimpleTimeMark.now()

        if (inquisitor == -1) {
            ChatUtils.error("No Inquisitor Found!")
            return
        }

        val inquisitor = EntityUtils.getEntityByID(inquisitor)
        if (inquisitor == null) {
            ChatUtils.chat("§cInquisitor out of range!")
            return
        }

        if (inquisitor.isDead) {
            ChatUtils.chat("§cInquisitor is dead")
            return
        }
        val location = inquisitor.getLorenzVec()
        val x = location.x.toInt()
        val y = location.y.toInt()
        val z = location.z.toInt()
        HypixelCommands.partyChat("x: $x, y: $y, z: $z ")
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onFirstChatEvent(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return
        val packet = event.packet
        if (packet !is S02PacketChat) return
        val messageComponent = packet.chatComponent

        val message = LorenzUtils.stripVanillaMessage(messageComponent.formattedText)
        if (packet.type.toInt() != 0) return

        partyInquisitorCheckerPattern.matchMatcher(message) {
            if (detectFromChat()) {
                event.isCanceled = true
            }
        }

        partyOnlyCoordsPattern.matchMatcher(message) {
            if (detectFromChat()) {
                event.isCanceled = true
            }
        }
        diedPattern.matchMatcher(message) {
            if (block()) return
            val rawName = group("playerName")
            val name = rawName.cleanPlayerName()
            val displayName = rawName.cleanPlayerName(displayName = true)
            waypoints = waypoints.editCopy { remove(name) }
            GriffinBurrowHelper.update()
            logger.log("Inquisitor died from '$displayName'")
        }
    }

    private fun Matcher.block(): Boolean = !hasGroup("party") && !config.globalChat

    private fun Matcher.detectFromChat(): Boolean {
        if (block()) return false
        val rawName = group("playerName")
        val x = group("x").trim().toDoubleOrNull() ?: return false
        val y = group("y").trim().toDoubleOrNull() ?: return false
        val z = group("z").trim().toDoubleOrNull() ?: return false
        val location = LorenzVec(x, y, z)

        val name = rawName.cleanPlayerName()
        val displayName = rawName.cleanPlayerName(displayName = true)
        if (!waypoints.containsKey(name)) {
            ChatUtils.chat("$displayName §l§efound an inquisitor at §l§c${x.toInt()} ${y.toInt()} ${z.toInt()}!")
            if (name != LorenzUtils.getPlayerName()) {
                LorenzUtils.sendTitle("§dINQUISITOR §efrom §b$displayName", 5.seconds)
                SoundUtils.playBeepSound()
            }
        }
        val inquis = SharedInquisitor(name, displayName, location, SimpleTimeMark.now())
        waypoints = waypoints.editCopy { this[name] = inquis }
        GriffinBurrowHelper.update()
        return true
    }

    private fun isEnabled() = DianaAPI.isDoingDiana() && config.enabled

    fun maybeRemove(inquis: SharedInquisitor) {
        if (inquisitorsNearby.isEmpty()) {
            waypoints = waypoints.editCopy { remove(inquis.fromPlayer) }
            GriffinBurrowHelper.update()
            ChatUtils.chat("Inquisitor from ${inquis.displayName} §enot found, deleting.")
        }
    }
}
