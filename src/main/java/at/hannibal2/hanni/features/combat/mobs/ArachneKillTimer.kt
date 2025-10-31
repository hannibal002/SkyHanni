package at.hannibal2.hanni.features.combat.mobs

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.chat.ArachneChatMessageHider
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration

@HanniModule
object ArachneKillTimer {

    private val config get() = HanniMod.feature.combat.mobs

    private val patternGroup = RepoPattern.group("chat.arachne")

    /**
     * REGEX-TEST: §c[BOSS] Arachne§r§f: A befitting welcome!
     */
    private val arachneCallingSpawnedPattern by patternGroup.pattern(
        "calling.spawned",
        "§c\\[BOSS] Arachne§r§f: A befitting welcome!",
    )

    /**
     * REGEX-TEST: §c[BOSS] Arachne§r§f: With your sacrifice.
     */
    private val arachneCrystalSpawnedPattern by patternGroup.pattern(
        "crystal.spawned",
        "§c\\[BOSS] Arachne§r§f: With your sacrifice.",
    )

    /**
     * REGEX-TEST: §f                              §r§6§lARACHNE DOWN!
     */
    private val arachneDeathPattern by patternGroup.pattern(
        "dead",
        "§f.*§r§6§lARACHNE DOWN!",
    )

    /**
     * REGEX-TEST: §f                 §r§eYour Damage: §r§a1,155,000 §r§7(Position #1)
     */
    private val arachneDamagePattern by patternGroup.pattern(
        "damage",
        "§f +§r§eYour Damage: §r§a[0-9,]+ §r§7\\(Position #[0-9,]+\\)",
    )

    private var arachneSpawnedTime = SimpleTimeMark.farPast()
    private var arachneKillTime = Duration.ZERO

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return
        if (arachneCallingSpawnedPattern.matches(event.message) || arachneCrystalSpawnedPattern.matches(event.message)) {
            arachneSpawnedTime = SimpleTimeMark.now()
        }

        if (arachneDeathPattern.matches(event.message) && arachneSpawnedTime != SimpleTimeMark.farPast()) {
            arachneKillTime = arachneSpawnedTime.passedSince()
        }

        if (ArachneChatMessageHider.arachneCallingPattern.matches(event.message) ||
            ArachneChatMessageHider.arachneCrystalPattern.matches(event.message)
        ) {
            arachneSpawnedTime = SimpleTimeMark.farPast()
        }

        if (arachneKillTime.isPositive() && arachneDamagePattern.matches(event.message)) {
            val format = arachneKillTime.format(showMilliSeconds = true)
            ChatUtils.chat("                   §eArachne took §b$format§e seconds to kill.", prefix = false)
            arachneKillTime = Duration.ZERO
            arachneSpawnedTime = SimpleTimeMark.farPast()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        arachneSpawnedTime = SimpleTimeMark.farPast()
    }

    fun isEnabled() = IslandType.SPIDER_DEN.isCurrent() && config.arachneKillTimer
}
