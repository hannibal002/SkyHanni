package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.json.toJsonArray
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonPrimitive

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

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(95, "chat.soundResponse.soundResponses") { element ->
            if (!element.isJsonArray) return@transform element
            val array = element.asJsonArray
            mapOf(
                JsonPrimitive("CAT") to lazy {
                    listOf(
                        JsonPrimitive("CATPURR"),
                        JsonPrimitive("CATPURREOW"),
                        JsonPrimitive("CATHISS"),
                    ).toJsonArray()
                },
                JsonPrimitive("DOG") to lazy {
                    listOf(
                        JsonPrimitive("DOGGROWL"),
                        JsonPrimitive("DOGHOWL"),
                    ).toJsonArray()
                },
            ).forEach {
                if (array.contains(it.key)) {
                    array.addAll(it.value.value)
                }
            }
            array
        }
    }


    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}

private const val START_PATTERN = "(?:^|^.* )(?: |§.)*(?i)"
private const val END_PATTERN = "(?: |§.|!|\\?|\\.)*(?:\$| .*\$)"

enum class SoundResponseTypes(private val displayName: String, soundLocation: String, triggersOn: List<String>) {
    CAT("Cat Meow", "mob.cat.meow", listOf("m+e*o*w+", "m+e*a+o+w+")),
    CATPURR("Cat Purr", "mob.cat.purr", listOf("p+u*rr+")),
    CATPURREOW("Cat Purreow", "mob.cat.purreow", listOf("m+r+e*o*w+", "m+r+e*a+o+w+")),
    CATHISS("Cat Hiss", "mob.cat.hiss", listOf("h+i+ss+")),
    DOG("Dog Bark", "mob.wolf.bark", listOf("bark", "a*w*r+u*f+", "w+oo+f+")),
    DOGGROWL("Dog Growl", "mob.wolf.growl", listOf("g+rr+")),
    DOGHOWL("Dog Howl", "mob.wolf.howl", listOf("a+w+oo+")),
    SHEEP("Sheep", "mob.sheep.say", listOf("baa+h*")),
    COW("Cow", "mob.cow.say", listOf("moo+")),
    PIG("Pig", "mob.pig.say", listOf("o+i+n+k+")),
    CHICKEN("Chicken", "mob.chicken.say", listOf("cl+u+c+k+")),
    ;

    val sound by lazy { SoundUtils.createSound(soundLocation, 1f) }

    // creates a pattern that looks for if the message contains any of the triggerOn strings but as a full word
    val pattern by RepoPattern.pattern(
        "chat.sound.response" + name.lowercase(),
        "$START_PATTERN(?:${triggersOn.joinToString("|")})$END_PATTERN",
    )

    override fun toString(): String = displayName
}
