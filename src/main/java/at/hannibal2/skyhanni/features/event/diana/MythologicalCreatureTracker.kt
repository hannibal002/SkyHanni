package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ElectionApi.getElectionYear
import at.hannibal2.skyhanni.data.jsonobjects.repo.DianaJson
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper.genericMythologicalSpawnPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroups
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MythologicalCreatureTracker {

    private val config get() = SkyHanniMod.feature.event.diana.mythologicalMobtracker

    private val tracker = SkyHanniTracker(
        "Mythological Creature Tracker", ::Data, { it.diana.mythologicalMobTracker },
        extraDisplayModes = mapOf(
            SkyHanniTracker.DisplayMode.MAYOR to {
                it.diana.mythologicalMobTrackerPerElection.getOrPut(
                    SkyBlockTime.now().getElectionYear(), ::Data,
                )
            },
        ),
    ) { drawDisplay(it) }

    // TODO create a draggable list from repo one that can be done
    private val shardMobs = listOf<String>(
        "Cretan Bull",
        "Harpy",
        "Minotaur",
    )

    data class Data(
        @Expose var since: MutableMap<String, Int> = mutableMapOf(),
        @Expose var count: MutableMap<String, Int> = mutableMapOf(),
    ) : TrackerData()

    var lastSinceAmount: Int? = null

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val creatureMatch = genericMythologicalSpawnPattern.matchGroups(event.message, "creatureType")?.getOrNull(0) ?: return

        if (config.shardWarn) {
            if (shardMobs.contains(creatureMatch)) {
                TitleManager.sendTitle("Black Hole", duration = 2.seconds)
            }
        }

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
                if (creatureEntry != type) {
                    since.addOrPut(trackerId, 1)
                    lastSinceAmount = since[trackerId]
                    since[trackerId] = 0
                }
            }
        }
        if (config.hideChat) event.blockedReason = "mythological_creature_dug"
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Modify) {
        if (lastSinceAmount == null) return
        val creatureMatch = genericMythologicalSpawnPattern.matchGroups(event.message, "creatureType")?.getOrNull(0) ?: return

        val type = DianaApi.mythologicalCreatures[creatureMatch] ?: run {
            ErrorManager.skyHanniError(
                "Unknown mythological creature $creatureMatch",
                "message" to event.message,
            )
        }

        tracker.modify {
            for (creatureEntry in DianaApi.mythologicalCreatures.values) {
                if (creatureEntry == type) {
                    val newComp = event.chatComponent.copy().append(" §e($lastSinceAmount)")
                    event.replaceComponent(newComp, "diana_mobs_since")
                }
            }
        }
        lastSinceAmount = null
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§7Mythological Creature Tracker:")
        val total = data.count.sumAllValues()
        val foundCreatures = data.count.filterValues { it > 0 }.keys
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

        var addedCreaturesSince = false

        for ((creatureTrackerId, since) in data.since.entries.sortedBy { it.value }) {
            val creature = DianaApi.getCreatureByTrackerName(creatureTrackerId) ?: continue
            if (!creature.rare || creatureTrackerId !in foundCreatures) continue

            if (!addedCreaturesSince) {
                addSearchString("§7Creatures since:")
                addedCreaturesSince = true
            }
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
        event.registerBrigadier("shresetmythologicalcreaturetracker") {
            description = "Resets the Mythological Creature Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
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
