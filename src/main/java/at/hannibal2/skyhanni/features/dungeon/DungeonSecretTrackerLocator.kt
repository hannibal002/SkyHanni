package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BezierFitter
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.core.particles.ParticleTypes
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DungeonSecretTrackerLocator {
    private val config get() = SkyHanniMod.feature.dungeon.dungeonSecretCompass

    private val patternGroup = RepoPattern.group("dungeon.secrettracker")

    /**
     * REGEX-TEST: §aThere's a secret §r§e11 blocks in front of you!
     * REGEX-TEST: §aThere's a secret §r§e38 blocks in front of you and 15 blocks above you§r§a!
     */
    private val secretTrackerMessagePattern by patternGroup.pattern(
        "message",
        "There's a secret (?:§.)*(?<distance>\\d+) blocks(?:.+and (?<distance2>\\d+) blocks)?",
    )

    private val noMissingSecretsPattern by patternGroup.pattern(
        "no.missing",
        "§cThere are no missing secrets near you!",
    )

    private var lastParticle = SimpleTimeMark.farPast()
    private var lastAbilityUse = SimpleTimeMark.farPast()
    private val bezierFitter = BezierFitter(1)
    private var secretDistance: Int? = null
    private var secretLocation: LorenzVec? = null
    private val SECRET_COMPASS = "SECRET_TRACKER".toInternalName()

    @HandleEvent(receiveCancelled = true, onlyOnIsland = IslandType.CATACOMBS)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        val type = event.type
        if (type != ParticleTypes.HAPPY_VILLAGER) return
        if (event.count != 1 || event.speed != 0f) return

        lastParticle = SimpleTimeMark.now()
        val currLoc = event.location

        if (lastAbilityUse.passedSince() > 1.seconds) return
        if (bezierFitter.isEmpty()) {
            bezierFitter.addPoint(currLoc)
            return
        }

        val distToLast = bezierFitter.getLastPoint()?.distance(currLoc) ?: return

        if (distToLast == 0.0 || distToLast > 1.0) return

        bezierFitter.addPoint(currLoc)

        repredictPoint()
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        val location = secretLocation ?: return
        val distance = location.distance(event.exactPlayerEyeLocation())

        if (distance > 3) {
            val formattedDistance = distance.toInt().addSeparators()
            event.drawDynamicText(location, "§d§lSECRET", 1.7)
            event.drawDynamicText(location.add(0.0, -0.1 - distance / (12 * 1.7), 0.0), " §r§e${formattedDistance}m", 1.0)
        } else {
            reset()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onChatMessage(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        secretTrackerMessagePattern.findMatcher(event.message) {
            val distance1 = group("distance").toInt()
            val distance2 = groupOrNull("distance2")?.toInt() ?: 0
            secretDistance = sqrt((distance1 * distance1 + distance2 * distance2).toDouble()).toInt()
            repredictPoint()
        }
        if (noMissingSecretsPattern.matches(event.message)) reset()
    }

    private fun repredictPoint() {
        val curve = bezierFitter.fit()
        val knownDistance = secretDistance

        if (curve != null && knownDistance != null) {
            secretLocation = curve.at(knownDistance * 2.0)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onUseAbility(event: ItemClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val item = event.itemInHand ?: return
        if (item.getInternalNameOrNull() != SECRET_COMPASS) return
        if (lastParticle.passedSince() < 0.2.seconds) {
            event.cancel()
            return
        }
        bezierFitter.reset()
        lastAbilityUse = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onWorldChange() {
        reset()
        lastAbilityUse = SimpleTimeMark.farPast()
    }

    private fun reset() {
        secretLocation = null
        bezierFitter.reset()
    }

    private fun isEnabled() = config
}
