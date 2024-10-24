package at.hannibal2.skyhanni.features.nether.kuudra

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.KuudraCompleteEvent
import at.hannibal2.skyhanni.events.KuudraEnterEvent
import at.hannibal2.skyhanni.events.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.removePrefix
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object KuudraAPI {

    private val patternGroup = RepoPattern.group("data.kuudra")

    private val tierPattern by patternGroup.pattern(
        "scoreboard.tier",
        " §7⏣ §cKuudra's Hollow §8\\(T(?<tier>.*)\\)"
    )
    private val completePattern by patternGroup.pattern(
        "chat.complete",
        "§.\\s*(?:§.)*KUUDRA DOWN!"
    )

    /**
     * REGEX-TEST: BURNING_AURORA_CHESTPLATE
     * REGEX-TEST: CRIMSON_LEGGINGS
     * REGEX-TEST: FIERY_CRIMSON_LEGGINGS
     * REGEX-TEST: TERROR_CHESTPLATE
     */
    private val kuudraArmorPattern by patternGroup.pattern(
        "internalname.armor",
        "(?<tier>HOT|BURNING|FIERY|INFERNAL|)_?(?<type>AURORA|CRIMSON|TERROR|HOLLOW|FERVOR)_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)"
    )

    private val kuudraTiers = listOf("", "HOT", "BURNING", "FIERY", "INFERNAL")
    val kuudraSets = listOf("AURORA", "CRIMSON", "TERROR", "HOLLOW", "FERVOR")

    fun NEUInternalName.isKuudraArmor(): Boolean = kuudraArmorPattern.matches(asString())

    fun NEUInternalName.getKuudraTier(): Int? {
        val tier = kuudraArmorPattern.matchGroup(asString(), "tier") ?: return null
        return (kuudraTiers.indexOf(tier) + 1).takeIf { it != 0 }
    }

    fun NEUInternalName.removeKuudraTier(): NEUInternalName {
        val prefix = kuudraArmorPattern.matchGroup(asString(), "tier") ?: return this
        return removePrefix("${prefix}_")
    }

    var kuudraTier: Int? = null
        private set

    fun inKuudra() = kuudraTier != null

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (kuudraTier != null) return
        for (line in ScoreboardData.sidebarLinesFormatted) {
            tierPattern.matchMatcher(line) {
                val tier = group("tier").toInt()
                kuudraTier = tier
                KuudraEnterEvent(tier).post()
            }
        }
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        kuudraTier = null
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        val message = event.message
        completePattern.matchMatcher(message) {
            val tier = kuudraTier ?: return
            KuudraCompleteEvent(tier).post()
        }
    }

}
