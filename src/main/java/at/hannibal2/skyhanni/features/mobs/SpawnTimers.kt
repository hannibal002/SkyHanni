package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class SpawnTimers {
    private val config get() = SkyHanniMod.feature.combat.mobs

    private val arachneAltarLocation = LorenzVec(-283f, 51f, -179f)
    private var arachneSpawnTime = SimpleTimeMark.farPast()
    private val arachneFragmentMessage = "^☄ [a-z0-9_]{2,22} placed an arachne's calling! something is awakening! \\(4/4\\)\$".toRegex()
    private val arachneCrystalMessage = "^☄ [a-z0-9_]{2,22} placed an arachne crystal! something is awakening!$".toRegex()
    private var saveNextTickParticles = false
    private var particleCounter = 0
    private var tickTime: Long = 0
    private var searchTime: Long = 0

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        searchTime = 0
        tickTime = 0
        particleCounter = 0
        saveNextTickParticles = false
        arachneSpawnTime = SimpleTimeMark.farPast()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isEnabled()) return
        if (arachneSpawnTime.isInPast()) return
        val countDown = arachneSpawnTime.timeUntil()

        val format = countDown.format(showMilliSeconds = true)
        event.drawDynamicText(arachneAltarLocation, "§b$format", 1.5)
    }

    @SubscribeEvent
    fun onChatReceived(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message.removeColor().lowercase()

        if (arachneFragmentMessage.matches(message) || arachneCrystalMessage.matches(message)) {
            if (arachneCrystalMessage.matches(message)) {
                saveNextTickParticles = true
                searchTime = System.currentTimeMillis()
                particleCounter = 0
                tickTime = 0L
            } else arachneSpawnTime = SimpleTimeMark.now() + 19.seconds
        }
    }

    // All this to detect "quickspawn" vs regular arachne spawn
    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!saveNextTickParticles) return
        if (System.currentTimeMillis() <= searchTime + 3000) return

        if (particleCounter == 0 && tickTime == 0L) tickTime = System.currentTimeMillis()

        if (System.currentTimeMillis() > tickTime + 60) {
            arachneSpawnTime = if (particleCounter <= 20)  {
                SimpleTimeMark.now() + 21.seconds
            } else {
                SimpleTimeMark.now() + 37.seconds
            }
            saveNextTickParticles = false
            return
        }

        val packet = event.packet
        if (packet is S2APacketParticles) {
            val location = packet.toLorenzVec().round(2)
            if (arachneAltarLocation.distance(location) > 30) return
            if (packet.particleType == EnumParticleTypes.REDSTONE && packet.particleSpeed == 1.0f) {
                particleCounter += 1
            }
        }
    }

    fun isEnabled() = IslandType.SPIDER_DEN.isInIsland() && LorenzUtils.skyBlockArea == "Arachne's Sanctuary" && config.showArachneSpawnTimer
}