package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.misc.IslandAreas
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.EnumParticleTypes
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ArachneSpawnTimer {

    private val config get() = SkyHanniMod.feature.combat.mobs

    private val patternGroup = RepoPattern.group("combat.mobs.spawntime.arachne")

    /**
     * REGEX-TEST: ☄ littlegremlins placed an arachne's calling! something is awakening! (4/4)
     */
    private val arachneFragmentPattern by patternGroup.pattern(
        "fragment",
        "^☄ [a-z0-9_]{2,22} placed an arachne's calling! something is awakening! \\(4/4\\)\$",
    )

    /**
     * REGEX-TEST: ☄ littlegremlins placed an arachne crystal! something is awakening!
     */
    private val arachneCrystalPattern by patternGroup.pattern(
        "crystal",
        "^☄ [a-z0-9_]{2,22} placed an arachne crystal! something is awakening!$",
    )

    private val arachneAltarLocation = LorenzVec(-283f, 51f, -179f)
    private var arachneSpawnTime = SimpleTimeMark.farPast()
    private var saveNextTickParticles = false
    private var particleCounter = 0
    private var lastTickTime = SimpleTimeMark.farPast()
    private var searchTime = SimpleTimeMark.farPast()

    @HandleEvent
    fun onWorldChange() {
        searchTime = SimpleTimeMark.farPast()
        lastTickTime = SimpleTimeMark.farPast()
        particleCounter = 0
        saveNextTickParticles = false
        arachneSpawnTime = SimpleTimeMark.farPast()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (arachneSpawnTime.isInPast()) return
        val countDown = arachneSpawnTime.timeUntil()

        val format = countDown.format(showMilliSeconds = true)
        event.drawDynamicText(arachneAltarLocation, "§b$format", 1.5)
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        val message = event.message.removeColor().lowercase()

        if (arachneFragmentPattern.matches(message) || arachneCrystalPattern.matches(message)) {
            if (arachneCrystalPattern.matches(message)) {
                saveNextTickParticles = true
                searchTime = SimpleTimeMark.now()
                particleCounter = 0
                lastTickTime = SimpleTimeMark.farPast()
            } else arachneSpawnTime = SimpleTimeMark.now() + 19.seconds
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.SPIDER_DEN, priority = HandleEvent.LOW, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!saveNextTickParticles) return
        if (searchTime.passedSince() < 3.seconds) return

        if (particleCounter == 0 && lastTickTime.isFarPast()) lastTickTime = SimpleTimeMark.now()

        if (lastTickTime.passedSince() > 60.milliseconds) {
            arachneSpawnTime = if (particleCounter <= 20) {
                SimpleTimeMark.now() + 21.seconds
            } else {
                SimpleTimeMark.now() + 37.seconds
            }
            saveNextTickParticles = false
            return
        }

        val location = event.location.roundTo(2)
        if (arachneAltarLocation.distance(location) > 30) return
        if (event.type == EnumParticleTypes.REDSTONE && event.speed == 1.0f) {
            particleCounter += 1
        }
    }

    fun isEnabled() =
        IslandType.SPIDER_DEN.isInIsland() && IslandAreas.currentAreaName == "Arachne's Sanctuary" && config.showArachneSpawnTimer
}
