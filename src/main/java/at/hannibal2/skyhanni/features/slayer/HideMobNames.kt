package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object HideMobNames {

    private val lastMobName = TimeLimitedCache<Int, String>(2.minutes)
    private val mobNamesHidden = mutableListOf<Int>()

    private enum class HideNameBossType(bossName: String) {
        ZOMBIE("Zombie"),
        ZOMBIE_VILLAGER("Zombie Villager"),
        CRYPT_GHOUL("Crypt Ghoul"),
        GRAVEYARD_ZOMBIE("Graveyard Zombie"),

        DASHER_SPIDER("Dasher Spider"),
        WEAVER_SPIDER("Weaver Spider"),
        SPLITTER_SPIDER("Splitter Spider"),
        VORACIOUS_SPIDER("Voracious Spider"),
        SILVERFISH("Silverfish"),

        WOLF("Wolf"),
        HOWLING_SPIRIT("Howling Spirit"),
        PACK_SPIRIT("Pack Spirit"),

        ENDERMAN("Enderman"),
        VOIDLING_FANATIC("Voidling Fanatic"),

        BLAZE("Blaze"),
        MUTATED_BLAZE("Mutated Blaze"),
        BEZAL("Bezal"),
        SMOLDERING_BLAZE("Smoldering Blaze"),
        FLAMING_SPIDER("Flaming Spider");

        private val patternName = bossName.lowercase().replace(" ", "-")
        val pattern by RepoPattern.pattern(
            "slayer.mobname.$patternName",
            "\\[Lv\\d+] (?<mobType>([✈☮⚓♃Ж⚙⚂♣⊙☃❄✰♨♆✿\uE018⛨\uD83E\uDDB4☽⛏༕☠⸙])+)? $bossName ae](?<min>.+)/(?<max>.+)❤",
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<ArmorStand>) {
        if (!SlayerApi.config.hideMobNames) return

        val entity = event.entity
        if (!entity.hasCustomName()) return

        val name = entity.cleanName
        val id = entity.id
        if (lastMobName[id] == name) {
            if (id in mobNamesHidden) {
                event.cancel()
            }
            return
        }

        lastMobName[id] = name
        mobNamesHidden.remove(id)

        if (shouldNameBeHidden(name)) {
            event.cancel()
            mobNamesHidden.add(id)
        }
    }

    @HandleEvent
    fun onWorldChange() {
        lastMobName.clear()
        mobNamesHidden.clear()
    }

    private fun shouldNameBeHidden(name: String): Boolean {
        for (mob in HideNameBossType.entries) {
            val pattern = mob.pattern
            pattern.matchMatcher(name) {
                val min = group("min")
                val max = group("max")
                if (min == max || min == "0") {
                    return true
                }
            }
        }

        return false
    }
}
