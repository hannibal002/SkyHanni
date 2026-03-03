package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.LivingEntity
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CarnivalQuickStart {

    private val config get() = SkyHanniMod.feature.event.carnival.doubleClickToStart

    private val patternGroup = RepoPattern.group("carnival")

    /**
     * WRAPPED-REGEX-TEST: "Select an option: \n  ➜ [Sure thing, partner!] \n  ➜ [Could ya tell me the rules again?] \n  ➜ [I'd like to do somthin' else fer now.] "
     */
    private val chatPattern by patternGroup.pattern(
        "select.option.chat-nocolor",
        // NOTE: Do not use .* here, it doesn't match newlines.
        "Select an option:[\\s\\S]*",
    )
    private val pirate by patternGroup.pattern("npcs.pirate", "Carnival Pirateman")
    private val fisher by patternGroup.pattern("npcs.fisher", "Carnival Fisherman")
    private val cowboy by patternGroup.pattern("npcs.cowboy", "Carnival Cowboy")

    private var lastChat = SimpleTimeMark.farPast()
    private var lastClicked = SimpleTimeMark.farPast()

    @HandleEvent
    fun onEntityClick(event: EntityClickEvent) {
        if (!isEnabled()) return
        if (lastChat.passedSince() > 5.0.seconds) return
        val mob = (event.clickedEntity as? LivingEntity)?.mob ?: return
        val type = when {
            cowboy.matches(mob.name) -> "carnival_cowboy"
            fisher.matches(mob.name) -> "carnival_fisherman"
            pirate.matches(mob.name) -> "carnival_pirateman"
            else -> return
        }
        if (lastClicked.passedSince() < 1.seconds) return
        lastClicked = SimpleTimeMark.now()
        HypixelCommands.npcOption(type, "r_2_1")
        event.cancel()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (!chatPattern.matches(event.cleanMessage)) return
        lastChat = SimpleTimeMark.now()
    }

    fun isEnabled() =
        config &&
            Perk.CHIVALROUS_CARNIVAL.isActive &&
            SkyBlockUtils.graphArea == "Carnival"
}
