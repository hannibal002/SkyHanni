package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class SpawnTimers {
    private val config get() = SkyHanniMod.feature.combat.mobs

    private val arachneAltarLocation = LorenzVec(-283f, 51f, -179f)
    private var arachneSpawnTime = SimpleTimeMark.farPast()
    private val arachneFragmentMessage = "^☄ [a-z0-9_]{2,22} placed an arachne's calling! something is awakening! \\(4/4\\)\$".toRegex()
    private val arachneCrystalMessage = "^☄ [a-z0-9_]{2,22} placed an arachne crystal! something is awakening!$".toRegex()

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
            arachneSpawnTime = if (arachneCrystalMessage.matches(message))
                SimpleTimeMark.now() + 24.seconds
            else
                SimpleTimeMark.now() + 19.seconds
        }
    }

    fun isEnabled() = IslandType.SPIDER_DEN.isInIsland() && LorenzUtils.skyBlockArea == "Arachne's Sanctuary" && config.showArachneSpawnTimer
}