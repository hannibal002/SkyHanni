package at.hannibal2.hanni.features.fishing

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.jsonobjects.repo.SeaCreatureJson
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object SeaCreatureManager {

    private var doubleHook = false

    private val seaCreatureMap = mutableMapOf<String, SeaCreature>()
    var allFishingMobs = mapOf<String, SeaCreature>()
    var allVariants = mapOf<String, List<String>>()

    private val patternGroup = RepoPattern.group("fishing.seacreature")

    /**
     * REGEX-TEST: §eIt's a §r§aDouble Hook§r§e! Woot woot!
     * REGEX-TEST: §eIt's a §r§aDouble Hook§r§e!
     */
    private val doubleHookPattern by patternGroup.pattern(
        "doublehook",
        "§eIt's a §r§aDouble Hook§r§e!(?: Woot woot!)?"
    )

    /**
     * REGEX-TEST: §e> Your bottle of thunder has fully charged!
     */
    private val thunderBottleChargedPattern by patternGroup.pattern(
        "thundercharged",
        "§e> Your bottle of thunder has fully charged!"
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: HanniChatEvent) {
        if (doubleHookPattern.matches(event.message)) {
            if (HanniMod.feature.fishing.compactDoubleHook) {
                event.blockedReason = "double_hook"
            }
            doubleHook = true
        } else if (!doubleHook || !thunderBottleChargedPattern.matches(event.message)) {
            val seaCreature = getSeaCreatureFromMessage(event.message)
            if (seaCreature != null) {
                SeaCreatureFishEvent(seaCreature, event, doubleHook).post()
            }
            doubleHook = false
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        seaCreatureMap.clear()
        allFishingMobs = emptyMap()
        var counter = 0

        val data = event.getConstant<Map<String, SeaCreatureJson>>("SeaCreatures", SeaCreatureJson.TYPE)
        val allFishingMobs = mutableMapOf<String, SeaCreature>()

        val variants = mutableMapOf<String, List<String>>()

        for ((variantName, variant) in data) {
            val chatColor = variant.chatColor
            val variantFishes = mutableListOf<String>()
            variants[variantName] = variantFishes
            for ((name, seaCreature) in variant.seaCreatures) {
                val chatMessage = seaCreature.chatMessage
                val fishingExperience = seaCreature.fishingExperience
                val rarity = seaCreature.rarity
                val rare = seaCreature.rare

                val creature = SeaCreature(name, fishingExperience, chatColor, rare, rarity)
                seaCreatureMap[chatMessage] = creature
                for (alternateMessage in seaCreature.alternateMessages.orEmpty()) {
                    seaCreatureMap[alternateMessage] = creature
                }
                allFishingMobs[name] = creature
                variantFishes.add(name)
                counter++
            }
        }
        SeaCreatureManager.allFishingMobs = allFishingMobs
        allVariants = variants
    }

    private fun getSeaCreatureFromMessage(message: String): SeaCreature? {
        return seaCreatureMap.getOrDefault(message, null)
    }
}
