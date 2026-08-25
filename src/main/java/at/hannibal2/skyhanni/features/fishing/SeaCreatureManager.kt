package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.SeaCreatureJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SpecificSeaCreatures
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.inWholeTicks
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SeaCreatureManager {
    private var lastDoubleHookTime = SimpleTimeMark.farPast()

    private val seaCreatureMap = mutableMapOf<String, SeaCreature>()
    var allFishingMobs = mapOf<String, SeaCreature>()
    var allVariants = mapOf<String, List<String>>()

    private val patternGroup = RepoPattern.group("fishing.seacreature")

    /**
     * REGEX-TEST: It's a Double Hook! Woot woot!
     * REGEX-TEST: It's a Double Hook!
     */
    private val doubleHookPattern by patternGroup.pattern(
        "doublehook.colorless",
        "It's a Double Hook!(?: Woot woot!)?",
    )

    private val config get() = SkyHanniMod.feature.fishing

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage
        if (doubleHookPattern.matches(message)) {
            if (config.compactDoubleHook) {
                event.blockedReason = "double_hook"
            }
            lastDoubleHookTime = SimpleTimeMark.now()
            return
        }
        getSeaCreatureFromMessage(message)?.let {
            val isDoubleHook = isDoubleHookRecently(lastDoubleHookTime)
            SeaCreatureFishEvent(it, isDoubleHook).post()

            if (config.seaCreatureTracker.hideChat) {
                event.blockedReason = "sea_creature_tracker"
                lastDoubleHookTime = SimpleTimeMark.farPast()
            }
            return
        }
    }

    // if you can do it better make a pr
    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Modify) {
        val message = event.cleanMessage
        if (doubleHookPattern.matches(message)) {
            lastDoubleHookTime = SimpleTimeMark.now()
            return
        }

        val seaCreature = getSeaCreatureFromMessage(message) ?: run {
            return
        }

        val isDoubleHook = isDoubleHookRecently(lastDoubleHookTime)

        val original = event.chatComponent.copy()
        var edited = original

        if (config.shortenFishingMessage) {
            val name = seaCreature.displayName
            val aOrAn = StringUtils.optionalAn(name.removeColor())
            edited = "§9You caught $aOrAn $name§9!".asComponent()
        }

        if (config.compactDoubleHook && isDoubleHook) {
            edited = when (config.compactDoubleHookPosition) {
                CompactDoubleHookPosition.LEFT ->
                    "§e§lDOUBLE HOOK! ".asComponent().append(edited)

                CompactDoubleHookPosition.RIGHT ->
                    edited.append(" §e§lDOUBLE HOOK!".asComponent())
            }
        }

        lastDoubleHookTime = SimpleTimeMark.farPast()
        if (original == edited) return
        event.replaceComponent(edited, "sea_creature")
    }

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        seaCreatureMap.clear()
        allFishingMobs = emptyMap()
        var counter = 0

        val data = event.getConstant<Map<String, SeaCreatureJson>>("SeaCreatures")
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
                val lootshareSphere = seaCreature.lootshareSphereOverride
                val oldNames = seaCreature.oldNames.orEmpty()

                val creature = SeaCreature(name, fishingExperience, chatColor, rare, rarity, lootshareSphere, oldNames)
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
        SpecificSeaCreatures.saveSeaCreatures(SpecificSeaCreatures.updateList())
    }

    private fun isDoubleHookRecently(lastDoubleHookTime: SimpleTimeMark): Boolean =
        lastDoubleHookTime.passedSince().inWholeTicks <= 1

    private fun getSeaCreatureFromMessage(message: String): SeaCreature? {
        return seaCreatureMap.getOrDefault(message, null)
    }

    enum class CompactDoubleHookPosition(private val displayName: String) {
        LEFT("Left"),
        RIGHT("Right"),
        ;

        override fun toString() = displayName
    }
}
