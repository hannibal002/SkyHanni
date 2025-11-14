package at.hannibal2.skyhanni.features.nether.kuudra

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.kuudra.KuudraCompleteEvent
import at.hannibal2.skyhanni.events.kuudra.KuudraEnterEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.removePrefix
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object KuudraApi {

    private val patternGroup = RepoPattern.group("data.kuudra")

    /**
     * REGEX-TEST:  §7⏣ §cKuudra's Hollow §8(T5)
     * REGEX-TEST:  §7⏣ §cKuudra's Hollow §8(T2)
     */
    private val tierPattern by patternGroup.pattern(
        "scoreboard.tier",
        " §7⏣ §cKuudra's Hollow §8\\(T(?<tier>\\d+)\\)",
    )
    private val completePattern by patternGroup.pattern(
        "chat.complete",
        "§.\\s*(?:§.)*KUUDRA DOWN!",
    )

    /**
     * REGEX-TEST: BURNING_AURORA_CHESTPLATE
     * REGEX-TEST: CRIMSON_LEGGINGS
     * REGEX-TEST: FIERY_CRIMSON_LEGGINGS
     * REGEX-TEST: TERROR_CHESTPLATE
     */
    private val kuudraArmorPattern by patternGroup.pattern(
        "internalname.armor",
        "(?<tier>HOT|BURNING|FIERY|INFERNAL|)_?(?<type>AURORA|CRIMSON|TERROR|HOLLOW|FERVOR)_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)",
    )

    /**
     * Hypixel currently duplicate the word Chest in the inventory name (NOT ITEM STACKS) of Kuudra chests in Croesus/Vesuvius
     * REGEX-TEST: Paid Chest
     * REGEX-TEST: Paid Chest Chest
     * REGEX-TEST: Free Chest
     * REGEX-TEST: Free Chest Chest
     */
    private val kuudraChestPattern by patternGroup.pattern(
        "kuudrachest",
        "(?<chesttype>(?:Paid|Free) Chest)(?: Chest)?",
    )

    val kuudraTiers = listOf("basic", "hot", "burning", "fiery", "infernal")

    val kuudraArmorTiers = listOf("", "HOT", "BURNING", "FIERY", "INFERNAL")
    val kuudraSets = listOf("AURORA", "CRIMSON", "TERROR", "HOLLOW", "FERVOR")

    fun NeuInternalName.isKuudraArmor(): Boolean = kuudraArmorPattern.matches(asString())

    fun NeuInternalName.getArmorKuudraTier(): Int? {
        val tier = kuudraArmorPattern.matchGroup(asString(), "tier") ?: return null
        return (kuudraArmorTiers.indexOf(tier) + 1).takeIf { it != 0 }
    }

    fun NeuInternalName.removeKuudraTier(): NeuInternalName {
        val prefix = kuudraArmorPattern.matchGroup(asString(), "tier") ?: return this
        return removePrefix("${prefix}_")
    }

    var kuudraTier: Int? = null
        private set

    val inKuudra get() = SkyBlockUtils.inSkyBlock && kuudraTier != null

    enum class KuudraChest(val inventory: String) {
        FREE("Free Chest"),
        PAID("Paid Chest"),
        ;

        companion object {
            fun getByInventoryName(inventory: String): KuudraChest? {
                var realInventory = inventory
                if (kuudraChestPattern.matches(inventory)) {
                    kuudraChestPattern.matchMatcher(inventory) {
                        realInventory = group("chesttype")
                    }
                }
                return entries.firstOrNull { it.inventory == realInventory }
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        if (kuudraTier != null) return
        tierPattern.firstMatcher(event.added) {
            val tier = group("tier").toInt()
            kuudraTier = tier
            KuudraEnterEvent(tier).post()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        kuudraTier = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        completePattern.matchMatcher(event.message) {
            val tier = kuudraTier ?: return
            KuudraCompleteEvent(tier).post()
        }
    }

    fun getKuudraRunTierName(tier: Int): String {
        return kuudraTiers[tier - 1]
    }

    fun getKuudraRunTierNumber(tier: String?): Int {
        return kuudraTiers.indexOf(tier)
    }

}
