package at.hannibal2.skyhanni.features.event.diana


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DamageIndicatorDetectedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.getLorenzVec
import io.github.moulberry.moulconfig.internal.KeybindHelper
import net.minecraft.entity.Entity
import net.minecraft.network.play.server.S02PacketChat
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard

object InquisitorWaypointShare {
    private val config get() = SkyHanniMod.feature.diana

    private var time = 0L
    private var inquisitor: Entity? = null
    private var lastShareTime = 0L

    var waypoints = mutableMapOf<String, LorenzVec>()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message
        if (message.endsWith("§r§eYou dug out §r§2a Minotaur§r§e!")) {
            time = System.currentTimeMillis()
            LorenzUtils.debug("found Inquisitor")
        }
    }

    @SubscribeEvent
    fun onDamageIndicatorDetected(event: DamageIndicatorDetectedEvent) {
        if (!isEnabled()) return
        val bossType = event.entityData.bossType
        if (bossType == BossType.MINOTAUR) {
            val diff = System.currentTimeMillis() - time
            LorenzUtils.debug("diff: $diff")
            if (diff > 100) return
            if (diff < 0) return
            val keyName = KeybindHelper.getKeyName(config.keyBindShare)
            val message = "§e[SkyHanni] §l§bYou found a Inquisitor! Press §l§chere §l§bor §c$keyName to share the location!"
            LorenzUtils.clickableChat(message, "shshareinquis")
            inquisitor = event.entityData.entity
        }
    }

    fun isEnabled() =
            LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.HUB && config.inquisitorWaypointShare

    @SubscribeEvent
    fun onKeyBindPressed(event: InputEvent.KeyInputEvent) {
        if (!isEnabled()) return
        if (!Keyboard.getEventKeyState()) return
        val key = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        if (config.keyBindShare == key) {
            shareWaypoint()
        }
    }

    fun shareWaypoint() {
        if (lastShareTime + 5000 > System.currentTimeMillis()) return
        lastShareTime = System.currentTimeMillis()
        val inquisitor = this.inquisitor
        if (inquisitor == null) {
            LorenzUtils.chat("§cNo Inquisitor Found!")
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
    fun onActionBarPacket(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return
        val packet = event.packet
        if (packet !is S02PacketChat) return
        val messageComponent = packet.chatComponent

        val message = LorenzUtils.stripVanillaMessage(messageComponent.formattedText)
        if (packet.type.toInt() == 0) {
            val partyPattern = "§9Party §8> (?<playerName>.*)§f: §rx: (?<x>.*), y: (?<y>.*), z: (?<z>.*)".toPattern()
            partyPattern.matchMatcher(message) {
                val playerName = group("playerName")
                val x = group("x").toInt()
                val y = group("y").toInt()
                val z = group("z").toInt()
                val location = LorenzVec(x, y, z)
                event.isCanceled = true
                LorenzUtils.chat("§e[SkyHanni] $playerName §l§dFound an Inquisitor at §l§c$x $y $z!")
            }
        }
    }
}
