package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.data.ElectionApi.getElectionYear
import at.hannibal2.skyhanni.data.jsonobjects.repo.DianaJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroups
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose

@SkyHanniModule
object MythologicalCreatureTracker {

    private val config get() = SkyHanniMod.feature.event.diana.mythologicalMobtracker

    private val patternGroup = RepoPattern.group("event.diana.mythological.tracker")

    /**
     * REGEX-TEST: §c§lUh oh! §r§eYou dug out a §r§2Gaia Construct§r§e!
     * REGEX-TEST: §c§lOi! §r§eYou dug out a §r§2Minos Inquisitor§r§e!
     * REGEX-TEST: §c§lOi! §r§eYou dug out §r§2Siamese Lynxes§r§e!
     */
    private val genericMythologicalSpawnPattern by patternGroup.pattern(
        "generic-spawn",
        "§c§l(?:Oh|Uh oh|Yikes|Oi|Good Grief)! §r§eYou dug out (?:a )?(?:§[a-f0-9r])*(?<creatureType>[\\w\\s]+)§r§e!",
    )

    private val tracker = SkyHanniTracker(
        "Mythological Creature Tracker", { Data() }, { it.diana.mythologicalMobTracker },
        extraDisplayModes = mapOf(
            SkyHanniTracker.DisplayMode.MAYOR to {
                it.diana.mythologicalMobTrackerPerElection.getOrPut(
                    SkyBlockTime.now().getElectionYear(), ::Data,
                )
            },
        ),
    ) { drawDisplay(it) }

    data class Data(
        @Expose var since: MutableMap<String, Int> = mutableMapOf(),
        @Expose var count: MutableMap<String, Int> = mutableMapOf(),
    ) : Resettable

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        val creatureMatch = genericMythologicalSpawnPattern.matchGroups(event.message, "creatureType")?.getOrNull(0) ?: return

        BurrowApi.lastBurrowRelatedChatMessage = SimpleTimeMark.now()

        val type = DianaApi.mythologicalCreatures[creatureMatch] ?: run {
            ErrorManager.skyHanniError(
                "Unknown mythological creature $creatureMatch",
                "message" to event.message,
            )
        }

        tracker.modify {
            it.count.addOrPut(type.trackerId, 1)
            val since = it.since
            for (creatureEntry in DianaApi.mythologicalCreatures.values) {
                val trackerId = creatureEntry.trackerId
                if (creatureEntry == type) {
                    event.chatComponent = (event.message + " §e(${since[trackerId]})").asComponent()
                    since[trackerId] = 0
                } else {
                    since.addOrPut(trackerId, 1)
                }
            }
        }
        if (config.hideChat) event.blockedReason = "mythological_creature_dug"
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§7Mythological Creature Tracker:")
        val total = data.count.sumAllValues()
        for ((creatureType, amount) in data.count.entries.sortedByDescending { it.value }) {
            val percentageSuffix = if (config.showPercentage.get()) {
                val percentage = (amount.toDouble() / total).formatPercentage()
                " §7$percentage"
            } else ""

            val typeName = DianaApi.getCreatureByTrackerName(creatureType)?.name ?: "§cUnknown type"

            addSearchString(
                " §7- §e${amount.addSeparators()} $typeName$percentageSuffix",
                searchText = creatureType,
            )
        }
        addSearchString("§7Total Mythological Creatures: §e${total.addSeparators()}")

        addSearchString("§7Creatures since:")

        for ((creatureTrackerId, since) in data.since.entries.sortedBy { it.value }) {
            val creature = DianaApi.getCreatureByTrackerName(creatureTrackerId)
            if (creature?.rare != true) continue

            addSearchString("§7- §e${creature.name}§7: §e${since.addSeparators()} ")
        }

    }

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.showPercentage) {
            tracker.update()
        }
    }

    @HandleEvent
    fun onRepoLoaded(event: RepositoryReloadEvent) {
        val dianaJson = event.getConstant<DianaJson>("events/Diana")

        tracker.modify {
            dianaJson.mythologicalCreatures.forEach { (_, creature) ->
                it.since.putIfAbsent(creature.trackerId, 0)
            }
        }
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { config.enabled && (DianaApi.isDoingDiana() || DianaApi.hasSpadeInHand()) },
            onRender = {
                if (DianaApi.hasSpadeInHand()) tracker.firstUpdate()
                tracker.renderDisplay(config.position)
            },
        )
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetmythologicalcreaturetracker") {
            description = "Resets the Mythological Creature Tracker"
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(110, "#profile.diana.mythologicalMobTracker", ::fixData)
        event.transform(110, "#profile.diana.mythologicalMobTrackerPerElection", ::fixPastData)
    }

    private fun fixPastData(jsonElement: JsonElement) =
        jsonElement.asJsonObject.also {
            it.entrySet().forEach { (k, v) -> it.add(k, fixData(v)) }
        }


    private fun fixData(jsonElement: JsonElement): JsonElement {
        val jsonObject = jsonElement.asJsonObject
        jsonObject.add(
            "since",
            ConfigManager.gson.toJsonTree(
                mapOf(
                    "MINOS_INQUISITOR" to jsonObject.get("creaturesSinceLastInquisitor").asInt,
                ),
            ),
        )
        jsonObject.remove("creaturesSinceLastInquisitor")
        return jsonElement
    }
}
