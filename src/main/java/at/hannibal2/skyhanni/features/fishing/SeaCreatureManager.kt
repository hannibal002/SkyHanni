package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.PetStorageApi
import at.hannibal2.skyhanni.data.WinterApi
import at.hannibal2.skyhanni.data.jsonobjects.repo.SeaCreatureJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SpecificSeaCreatures
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SeaCreatureManager {

    private var doubleHook = false

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

    /**
     * REGEX-TEST: > Your bottle of thunder has fully charged!
     */
    private val thunderBottleChargedPattern by patternGroup.pattern(
        "thundercharged.colorless",
        "> Your bottle of thunder has fully charged!",
    )

    private val config get() = SkyHanniMod.feature.fishing

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage
        if (doubleHookPattern.matches(message)) {
            if (config.compactDoubleHook) {
                event.blockedReason = "double_hook"
            }
            doubleHook = true
            return
        }
        if (isInterceptingMessage(message)) return

        getSeaCreatureFromMessage(message)?.let {
            SeaCreatureFishEvent(it, doubleHook).post()

            if (config.seaCreatureTracker.hideChat) {
                event.blockedReason = "sea_creature_tracker"
                doubleHook = false
            }
            return
        }

        doubleHook = false
    }

    // if you can do it better make a pr
    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Modify) {
        val message = event.cleanMessage
        if (doubleHookPattern.matches(message)) {
            doubleHook = true
            return
        }

        if (isInterceptingMessage(message)) return

        val seaCreature = getSeaCreatureFromMessage(message) ?: run {
            doubleHook = false
            return
        }

        val wasDoubleHook = doubleHook
        doubleHook = false

        val original = event.chatComponent.copy()
        var edited = original

        if (config.shortenFishingMessage) {
            val name = seaCreature.displayName
            val aOrAn = StringUtils.optionalAn(name.removeColor())
            edited = "§9You caught $aOrAn $name§9!".asComponent()
        }

        if (config.compactDoubleHook && wasDoubleHook) {
            edited = when (config.compactDoubleHookPosition) {
                CompactDoubleHookPosition.LEFT ->
                    "§e§lDOUBLE HOOK! ".asComponent().append(edited)

                CompactDoubleHookPosition.RIGHT ->
                    edited.append(" §e§lDOUBLE HOOK!".asComponent())
            }
        }

        if (original == edited) return
        event.replaceComponent(edited, "sea_creature")
    }

    /**
     * Autopet can be triggered via Sinkers as rod parts (Sponge, Prismarine, Icy) to trigger collection gain which goes between Double Hook! and the Catch message.
     * The Thunder sea Creature gives charge when hooked, which can cause thunder bottles to charge and send the full charge message between Double Hook! and Catch message.
     * Reindrakes send an empty line, the global message & another empty line between Double Hook! and Catch message.
     */
    private fun isInterceptingMessage(message: String): Boolean =
        WinterApi.isReindrakeSpawnMessage(message) ||
            message.isEmpty() ||
            PetStorageApi.isAutopetMessage(message) ||
            thunderBottleChargedPattern.matches(message)

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
