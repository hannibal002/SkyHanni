package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.system.PlatformUtils

@SkyHanniModule
object ChatSoundResponse {

    private val config get() = SkyHanniMod.feature.chat.soundResponse

    init {
        SoundResponseTypes.entries.forEach { it.pattern }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return

        for (soundType in SoundResponseTypes.entries) {
            if (!config.soundResponses.contains(soundType)) continue
            if (soundType.pattern.matches(event.message)) {
                soundType.sound.playSound()
                return
            }
        }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}

private const val START_PATTERN = "(?:^|^.* )(?: |§.)*(?i)"
private const val END_PATTERN = "(?: |§.|!|\\?|\\.)*(?:\$| .*\$)"

enum class SoundResponseTypes(legacySoundLocation: String, modernSoundLocation: String, triggersOn: List<String>) {
    CAT("mob.cat.meow", "entity.cat.ambient", listOf("meow")),
    DOG("mob.wolf.bark", "entity.wolf.ambient", listOf("bark", "arf", "woof")),
    SHEEP("mob.sheep.say", "entity.sheep.ambient", listOf("baa+h*")),
    COW("mob.cow.say", "entity.cow.ambient", listOf("moo+")),
    PIG("mob.pig.say", "entity.pig.ambient", listOf("oink")),
    CHICKEN("mob.chicken.say", "entity.chicken.ambient", listOf("cluck")),
    ;

    val sound by lazy {
        if (PlatformUtils.MC_VERSION == "1.8.9") {
            SoundUtils.createSound(legacySoundLocation, 1f)
        } else {
            SoundUtils.createSound(modernSoundLocation, 1f)
        }
    }

    // creates a pattern that looks for if the message contains any of the triggerOn strings but as a full word
    val pattern by RepoPattern.pattern(
        "chat.sound.response" + name.lowercase(),
        "$START_PATTERN(?:${triggersOn.joinToString("|")})$END_PATTERN",
    )

    override fun toString(): String = name.firstLetterUppercase()
}
