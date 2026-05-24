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
import net.minecraft.network.chat.Component

@SkyHanniModule
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
        "§eIt's a §r§aDouble Hook§r§e!(?: Woot woot!)?",
    )

    /**
     * REGEX-TEST: §e> Your bottle of thunder has fully charged!
     */
    private val thunderBottleChargedPattern by patternGroup.pattern(
        "thundercharged",
        "§e> Your bottle of thunder has fully charged!",
    )

    private val config get() = SkyHanniMod.feature.fishing

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (doubleHookPattern.matches(event.message)) {
            if (config.compactDoubleHook) {
                event.blockedReason = "double_hook"
            }
            doubleHook = true
            return
        }
        if (isInterceptingColorCodeMessage(event.message)) return
        if (isInterceptingCleanMessage(event.cleanMessage)) return

        getSeaCreatureFromMessage(event.cleanMessage)?.let {
            SeaCreatureFishEvent(it, doubleHook).post()
            if (config.seaCreatureTracker.hideChat) {
                event.blockedReason = "sea_creature_tracker"
            }
            return
        }

        doubleHook = false
    }

    // if you can do it better make a pr
    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Modify) {
        if (doubleHookPattern.matches(event.message)) {
            doubleHook = true
            return
        }

        if (isInterceptingColorCodeMessage(event.message)) return
        if (isInterceptingCleanMessage(event.cleanMessage)) return

        getSeaCreatureFromMessage(event.cleanMessage)?.let {
            val original = event.chatComponent.copy()
            var edited = original

            if (config.shortenFishingMessage) {
                val name = it.displayName
                val aOrAn = StringUtils.optionalAn(name.removeColor())
                edited = "§9You caught $aOrAn $name§9!".asComponent()
            }

            if (config.compactDoubleHook && doubleHook) {
                edited = Component.literal("§e§lDOUBLE HOOK! ").append(edited)
            }

            if (original == edited) return
            event.replaceComponent(edited, "sea_creature")
        }

        doubleHook = false
    }

    /**
     * Autopet can be triggered via Sinkers as rod parts (Sponge, Prismarine, Icy) to trigger collection gain which goes between Double Hook! and the Catch message.
     * The Thunder sea Creature gives charge when hooked, which can cause thunder bottles to charge and send the full charge message between Double Hook! and Catch message.
     */
    private fun isInterceptingColorCodeMessage(message: String): Boolean =
        (PetStorageApi.isAutopetMessage(message) || thunderBottleChargedPattern.matches(message))

    // TODO Unify when both use CleanMessage.

    /**
     * Reindrakes send an empty line, the global message & another empty line between Double Hook! and Catch message.
     */
    private fun isInterceptingCleanMessage(message: String): Boolean =
        (WinterApi.isReindrakeSpawnMessage(message) || message.isEmpty())

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
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

                val creature = SeaCreature(name, fishingExperience, chatColor, rare, rarity, lootshareSphere)
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
}
