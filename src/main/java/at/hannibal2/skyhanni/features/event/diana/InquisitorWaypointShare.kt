package at.hannibal2.skyhanni.features.event.diana


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.play.server.S02PacketChat
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.seconds

object InquisitorWaypointShare {
    private val config get() = SkyHanniMod.feature.diana.inquisitorSharing
    private val partyPattern =
        "§9Party §8> (?<playerName>.*)§f: §rx: (?<x>-?[0-9]{1,4}), y: (?<y>-?[0-9]{1,4}), z: (?<z>-?[0-9]{1,4})\\b".toPattern()
    private val diedPattern = "§9Party §8> (?<playerName>.*)§f: §rInquisitor dead!".toPattern()

    private var time = 0L
    private var testTime = 0L
    private var lastInquisitorMessage = ""
    private var inquisitor = -1
    private var lastInquisitor = -1
    private var lastShareTime = 0L
    private var inquisitorsNearby = emptyList<EntityOtherPlayerMP>()

    private val logger = LorenzLogger("diana/waypoints")

    var waypoints = mapOf<String, SharedInquisitor>()

    class SharedInquisitor(val fromPlayer: String, val location: LorenzVec, val spawnTime: SimpleTimeMark)

    private var test = false

    fun test() {
        test = !test
        LorenzUtils.chat("§e[SkyHanni] Inquisitor Test " + if (test) "Enabled" else "Disabled")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (event.repeatSeconds(3)) {
            inquisitorsNearby = inquisitorsNearby.editCopy { removeIf { it.isDead } }
        }
        if (event.repeatSeconds(1)) {
            waypoints = waypoints.editCopy { values.removeIf { it.spawnTime.passedSince() > 75.seconds } }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        waypoints = emptyMap()
        inquisitorsNearby = emptyList()
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message
        if (message.contains("§eYou dug out")) {
            testTime = System.currentTimeMillis()
            lastInquisitorMessage = message

            val diff = System.currentTimeMillis() - time

            if (diff < 10_000) {
                logger.log(" ")
                logger.log("reverse!")
                logger.log("diff: $diff")
            }
            if (diff > 1500 || diff < -500) {
                return
            }
            foundInquisitor(lastInquisitor)
        }

        // TODO: Change the check to only one line once we have a confirmed inquis message line
        if (message.contains("§r§eYou dug out ")) {
            if (message.contains("Inquis")) {
                time = System.currentTimeMillis()
                logger.log("found Inquisitor")
            }
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

        val diff = System.currentTimeMillis() - time
        time = System.currentTimeMillis()
        lastInquisitor = entity.entityId

        logger.log("diff: $diff")
        if (diff > 1500 || diff < -500) {
            val testDiff = System.currentTimeMillis() - testTime
            if (testDiff > 1500 || testDiff < -500) {
                logger.log("testDiff: $diff")
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
            val keyName = OSUtils.getKeyName(config.keyBindShare)
            val message =
                "§e[SkyHanni] §l§bYou found a Inquisitor! Press §l§chere §l§bor §c$keyName to share the location!"
            LorenzUtils.clickableChat(message, "shshareinquis")
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
    fun onKeyBindPressed(event: InputEvent.KeyInputEvent) {
        if (!isEnabled()) return
        if (!Keyboard.getEventKeyState()) return
        val key = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        if (config.keyBindShare == key) {
            sendInquisitor()
        }
    }

    private fun sendDeath() {
        if (!isEnabled()) return
        if (lastShareTime + 5000 > System.currentTimeMillis()) return
        lastShareTime = System.currentTimeMillis()

        if (inquisitor == -1) {
            logger.log("Inquisitor is already null!")
            return
        }
        inquisitor = -1
        LorenzUtils.sendCommandToServer("pc Inquisitor dead!")
    }

    fun sendInquisitor() {
        if (!isEnabled()) return
        if (lastShareTime + 5000 > System.currentTimeMillis()) return
        lastShareTime = System.currentTimeMillis()

        if (inquisitor == -1) {
            LorenzUtils.chat("§c[SkyHanni] No Inquisitor Found!")
            return
        }

        val inquisitor = Minecraft.getMinecraft().theWorld.getEntityByID(inquisitor)
        if (inquisitor == null) {
            LorenzUtils.chat("§cInquisitor out of range!")
            return
        }

        if (inquisitor.isDead) {
            LorenzUtils.chat("§cInquisitor is ded")
            return
        }
        val location = inquisitor.getLorenzVec()
        val x = location.x.toInt()
        val y = location.y.toInt()
        val z = location.z.toInt()
        LorenzUtils.sendCommandToServer("pc x: $x, y: $y, z: $z ")
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onFirstChatEvent(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return
        val packet = event.packet
        if (packet !is S02PacketChat) return
        val messageComponent = packet.chatComponent

        val message = LorenzUtils.stripVanillaMessage(messageComponent.formattedText)
        if (packet.type.toInt() != 0) return

        partyPattern.matchMatcher(message) {
            val playerName = group("playerName")
            val x = group("x").trim().toInt()
            val y = group("y").trim().toInt()
            val z = group("z").trim().toInt()
            val location = LorenzVec(x, y, z)

            val cleanName = playerName.cleanPlayerName()
            if (!waypoints.containsKey(cleanName)) {
                LorenzUtils.chat("§e[SkyHanni] $playerName §l§efound an inquisitor at §l§c$x $y $z!")
                if (cleanName != LorenzUtils.getPlayerName()) {
                    TitleUtils.sendTitle("§dINQUISITOR §efrom §b$cleanName", 5.seconds)
                    SoundUtils.playBeepSound()
                }
            }
            val inquis = SharedInquisitor(cleanName, location, SimpleTimeMark.now())
            waypoints = waypoints.editCopy { this[cleanName] = inquis }
            if (config.focusInquisitor) {
                GriffinBurrowHelper.setTargetLocation(location.add(0, 1, 0))
                GriffinBurrowHelper.animationLocation = LocationUtils.playerLocation()
            }

            event.isCanceled = true
        }
        diedPattern.matchMatcher(message) {
            val playerName = group("playerName").cleanPlayerName()
            waypoints = waypoints.editCopy { remove(playerName) }
            logger.log("Inquisitor died from '$playerName'")
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.HUB && config.enabled

    fun maybeRemove(playerName: String) {
        if (inquisitorsNearby.isEmpty()) {
            waypoints = waypoints.editCopy { remove(playerName) }
            LorenzUtils.chat("§e[SkyHanni] Inquisitor from $playerName not found, deleting.")
        }
    }
}
