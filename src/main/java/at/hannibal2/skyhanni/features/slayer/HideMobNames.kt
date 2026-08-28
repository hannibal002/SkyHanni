package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object HideMobNames {

    private val lastMobName = TimeLimitedCache<Int, String>(2.minutes)
    private val mobNamesHidden = mutableListOf<Int>()

    // TODO: use SkyblockIcons instead of hardcoding the mob types
    private const val ALL_MOB_TYPES =
        "\uE070\uE071\uE072\uE073\uE074\uE075\uE076\uE077\uE078\uE079\uE07A\uE07B" +
            "\uE07C\uE07D\uE07E\uE018\uE07F\uE080\uE081\uE082\uE083\uE084\uE085\uE086\uE087"

    /**
     * REGEX-TEST: [Lv1]  Graveyard Zombie 100/100❤
     * REGEX-TEST: [Lv30]  Crypt Ghoul 2,000/2,000❤
     * REGEX-TEST: [Lv1]  Zombie Villager 120/120❤
     */
    private val hideMobNamePatterns by RepoPattern.list(
        "slayer.hidemobname",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Zombie (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Zombie Villager (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Crypt Ghoul (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Graveyard Zombie (?<min>.+)/(?<max>.+)❤",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Dasher Spider (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Weaver Spider (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Splitter Spider (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Voracious Spider (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Silverfish (?<min>.+)/(?<max>.+)❤",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Wolf (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Howling Spirit (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Pack Spirit (?<min>.+)/(?<max>.+)❤",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Enderman (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Voidling Fanatic (?<min>.+)/(?<max>.+)❤",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Blaze (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Mutated Blaze (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Bezal (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Smoldering Blaze (?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Flaming Spider (?<min>.+)/(?<max>.+)❤",
    )

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
        hideMobNamePatterns.matchMatchers(name) {
            val min = group("min")
            val max = group("max")
            if (min == max || min == "0") {
                return true
            }
        }
        return false
    }
}
