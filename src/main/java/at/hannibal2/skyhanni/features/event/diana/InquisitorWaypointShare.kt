package at.hannibal2.skyhanni.features.event.diana


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.BossHealthChangeEvent
import at.hannibal2.skyhanni.events.DamageIndicatorDetectedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import io.github.moulberry.moulconfig.internal.KeybindHelper
import net.minecraft.client.Minecraft
import net.minecraft.network.play.server.S02PacketChat
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard

object InquisitorWaypointShare {
    private val config get() = SkyHanniMod.feature.diana.inquisitorSharing
    private val partyPattern = "§9Party §8> (?<playerName>.*)§f: §rx: (?<x>.*), y: (?<y>.*), z: (?<z>.*)".toPattern()
    private val diedPattern = "§9Party §8> (?<playerName>.*)§f: §rInquisitor dead!".toPattern()

    private var time = 0L
    private var testTime = 0L
    private var lastTestMessage = ""
    private var inquisitor = -1
    private var lastShareTime = 0L

    var waypoints = mapOf<String, LorenzVec>()

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        waypoints = waypoints.editCopy { clear() }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message
        // TODO use inquisitor
//        if (message.endsWith("§r§eYou dug out §r§2a Minotaur§r§e!")) {
        if (message.endsWith("§r§eYou dug out §r§2a Minos Champion§r§e!")) {
            time = System.currentTimeMillis()
            LorenzUtils.debug("found Champion/Inquisitor")
        }
        if (message.contains("§eYou dug out")) {
            testTime = System.currentTimeMillis()
            lastTestMessage = message
        }
    }

    @SubscribeEvent
    fun onDamageIndicatorDetected(event: DamageIndicatorDetectedEvent) {
        if (!isEnabled()) return
        val bossType = event.entityData.bossType
        // TODO use inquisitor
//        if (bossType == BossType.MINOTAUR) {
        if (bossType == BossType.MINOS_INQUISITOR) {
            val diff = System.currentTimeMillis() - time
            LorenzUtils.debug("diff: $diff")
            if (diff > 100 || diff < 0) {
                val testDiff = System.currentTimeMillis() - testTime
                if (testDiff > 100 || testDiff < 0) {
                    LorenzUtils.debug("testDiff: $diff")
                    return
                } else {
                    LorenzUtils.debug("wrong Inquisitor message!")
                    println("lastTestMessage: '$lastTestMessage'")

                }
            }
            val keyName = KeybindHelper.getKeyName(config.keyBindShare)
            val message =
                "§e[SkyHanni] §l§bYou found a Inquisitor! Press §l§chere §l§bor §c$keyName to share the location!"
            LorenzUtils.clickableChat(message, "shshareinquis")
            inquisitor = event.entityData.entity.entityId
        }
    }

    @SubscribeEvent
    fun onBossHealthChange(event: BossHealthChangeEvent) {
        if (event.health <= 0) {
            val entityData = event.entityData
            val bossType = entityData.bossType
            // TODO use inquisitor
//            if (bossType == BossType.MINOTAUR) {
            if (bossType == BossType.MINOS_INQUISITOR) {
                sendDeath()
            }
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
            LorenzUtils.debug("Inquisitor is already null!")
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
                    TitleUtils.sendTitle("§dINQUISITOR §efrom §b$cleanName", 5_000)
                    SoundUtils.playBeepSound()
                }
            }
            waypoints = waypoints.editCopy { this[cleanName] = location }
            if (config.focusInquisitor) {
                GriffinBurrowHelper.setTargetLocation(location.add(0, 1, 0))
                GriffinBurrowHelper.animationLocation = LocationUtils.playerLocation()
            }

            event.isCanceled = true
        }
        diedPattern.matchMatcher(message) {
            val playerName = group("playerName").cleanPlayerName()
            waypoints = waypoints.editCopy { remove(playerName) }
            LorenzUtils.debug("Inquisitor died from '$playerName'")
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.HUB && config.enabled

    fun maybeRemove(playerName: String) {
        if (!DamageIndicatorManager.isBossSpawned(BossType.MINOTAUR)) {
            waypoints = waypoints.editCopy { remove(playerName) }
            LorenzUtils.chat("§e[SkyHanni] Inquisitor from $playerName not found, deleting.")
        }
    }
}
