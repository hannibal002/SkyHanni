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
            "\uE07C\uE07D\uE07E\uE018\uE080\uE081\uE082\uE083\uE084\uE085\uE086\uE087"

    private val hideMobNamePatterns by RepoPattern.list(
        "slayer.hidemobname",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Zombie ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Zombie Villager ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Crypt Ghoul ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Graveyard Zombie ae](?<min>.+)/(?<max>.+)❤",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Dasher Spider ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Weaver Spider ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Splitter Spider ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Voracious Spider ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Silverfish ae](?<min>.+)/(?<max>.+)❤",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Wolf ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Howling Spirit ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Pack Spirit ae](?<min>.+)/(?<max>.+)❤",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Enderman ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Voidling Fanatic ae](?<min>.+)/(?<max>.+)❤",

        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Blaze ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Mutated Blaze ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Bezal ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Smoldering Blaze ae](?<min>.+)/(?<max>.+)❤",
        "\\[Lv\\d+] (?<mobType>([$ALL_MOB_TYPES])+)? Flaming Spider ae](?<min>.+)/(?<max>.+)❤",
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
