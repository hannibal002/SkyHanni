package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object SoundResponse {

    private val config get() = SkyHanniMod.feature.chat.soundResponse

    init {
        SoundResponseTypes.entries.forEach { it.pattern }
    }

    @SubscribeEvent
    fun onLorenzChat(event: LorenzChatEvent) {
        for (soundType in SoundResponseTypes.entries) {
            if (!config.soundResponses.contains(soundType)) continue
            if (soundType.pattern.matches(event.message)) {
                soundType.sound.playSound()
                return
            }
        }
    }
}

private const val START_PATTERN = "(?:^|^.* )(?: |§.)*(?i)"
private const val END_PATTERN = "(?: |§.|!|\\?|\\.)*(?:\$| .*\$)"

enum class SoundResponseTypes(soundLocation: String, triggersOn: List<String>) {
    CAT("mob.cat.meow", listOf("meow")),
    DOG("mob.wolf.bark", listOf("bark", "arf", "woof")),
    SHEEP("mob.sheep.say", listOf("baa+h*")),
    COW("mob.cow.say", listOf("moo+")),
    PIG("mob.pig.say", listOf("oink")),
    CHICKEN("mob.chicken.say", listOf("cluck")),
    ;

    val sound by lazy { SoundUtils.createSound(soundLocation, 1f) }

    // creates a pattern that looks for if the message contains any of the triggerOn strings but as a full word
    val pattern by RepoPattern.pattern(
        "chat.sound.response" + name.lowercase(),
        "$START_PATTERN(?:${triggersOn.joinToString("|")})$END_PATTERN",
    )
}
