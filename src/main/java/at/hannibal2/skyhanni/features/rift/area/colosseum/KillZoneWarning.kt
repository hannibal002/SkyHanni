package at.hannibal2.skyhanni.features.rift.area.colosseum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.data.title.TitleManager.TitleAddType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object KillZoneWarning {

    private val config get() = SkyHanniMod.feature.rift.area.colosseum

    private val patternGroup = RepoPattern.group("rift.colosseum.bacte")

    /**
     * REGEX-TEST: §a⚠ §r§cGet back in the arena or you will DIE! §r§a⚠
     * REGEX-TEST: §a⚠ §r§cGet back in the arena or you will DIE!!! §r§a⚠
     * REGEX-TEST: §a⚠ §r§cGet back in the arena or you will DIE!!!!!! §r§a⚠
     * REGEX-TEST: §a⚠ §r§cGet back in the arena or you will DIE!!!!!!!!!!!! §r§a⚠
     */
    private val killZonePattern by patternGroup.pattern(
        "chat.kill-zone",
        "§a⚠ §r§cGet back in the arena or you will DIE(?<exclamation>!+) §r§a⚠"
    )

    private val sound by lazy { SoundUtils.createSound("random.orb", 0.0f) }

    private var lastMessageTime = SimpleTimeMark.farPast()
    private var killDeadline = SimpleTimeMark.farPast()
    private var title: TitleContext? = null

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onChatMessage(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        killZonePattern.matchMatcher(event.message) {
            sound.playSound()
            lastMessageTime = SimpleTimeMark.now()
            val warningLevel = group("exclamation").length
            killDeadline = lastMessageTime.plus(250.milliseconds * (12 - warningLevel))
        }
    }

    @HandleEvent(SkyHanniTickEvent::class, onlyOnIsland = IslandType.THE_RIFT)
    fun onTick() {
        if (!isEnabled()) return
        if (lastMessageTime.passedSince() > 250.milliseconds) return
        if (killDeadline.isInPast()) return
        title?.stop()
        title = TitleManager.sendTitle(
            "§cGet back in the arena!",
            String.format(
                Locale.US,
                "§7%.2fs left",
                killDeadline.timeUntil().inPartialSeconds,
            ),
            50.milliseconds,
            addType = TitleAddType.FORCE_FIRST,
        )
    }

    fun isEnabled() = RiftApi.inColosseum() && config.killZoneWarning
}
