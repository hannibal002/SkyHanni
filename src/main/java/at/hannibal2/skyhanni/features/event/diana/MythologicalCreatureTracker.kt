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
import at.hannibal2.skyhanni.utils.ChatUtils
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
        "genericSpawn",
        "§c§l(?:Oh|Uh Oh|Yikes|Oi|Good Grief)! §r§eYou dug out (?:a )?(?:§[a-f0-9r])*(?<creatureType>[\\w\\s]+)§r§e!",
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
        @Expose var count: MutableMap<String, Int> = mutableMapOf()
    ) : Resettable

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        val creatureMatch = genericMythologicalSpawnPattern.matchGroups(event.message, "creatureType")?.getOrNull(0) ?: return

        val type = DianaApi.mythologicalCreatures[creatureMatch]

        BurrowApi.lastBurrowRelatedChatMessage = SimpleTimeMark.now()

        if (type != null) {
            tracker.modify {
                it.count.addOrPut(type.trackerId, 1)
                for (creatureEntry in DianaApi.mythologicalCreatures.values) {
                    if (creatureEntry == type) {
                        event.chatComponent = (event.message + " §e(${it.since[creatureEntry.trackerId]})").asComponent()
                        it.since[creatureEntry.trackerId] = 0
                    } else {
                        it.since.addOrPut(creatureEntry.trackerId, 1)
                    }
                }
            }
            if (config.hideChat) event.blockedReason = "mythological_creature_dug"
        } else {
            // TODO: testing :3
            ChatUtils.userError("UNKNOWN DIANA CREATURE!!!!! $creatureMatch!!!")
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§7Mythological Creature Tracker:")
        val total = data.count.sumAllValues()
        for ((creatureType, amount) in data.count.entries.sortedByDescending { it.value }) {
            val percentageSuffix = if (config.showPercentage.get()) {
                val percentage = (amount.toDouble() / total).formatPercentage()
                " §7$percentage"
            } else ""

            val type = DianaApi.getCreatureByTrackerName(creatureType)

            addSearchString(
                " §7- §e${amount.addSeparators()} ${type?.name}$percentageSuffix",
                searchText = creatureType,
            )
        }
        addSearchString("§7Total Mythological Creatures: §e${total.addSeparators()}")

        addSearchString("§7Creatures since:")

        for ((creatureTrackerId, since) in data.since.entries.sortedBy { it.value }) {
            val creature = DianaApi.getCreatureByTrackerName(creatureTrackerId)
            if (creature == null || creature.rare != true) continue

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
        val dianaJson = event.getConstant<DianaJson>("Diana")

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
        event.transform(108, "#profile.diana.mythologicalMobTracker", ::fixData)
        event.transform(108, "#profile.diana.mythologicalMobTrackerPerElection", ::fixPastData)
    }

    private fun fixPastData(jsonElement: JsonElement): JsonElement {
        val jsonObject = jsonElement.asJsonObject

        for ((key, value) in jsonObject.entrySet()) {
            jsonObject.add(key, fixData(value))
        }

        return jsonObject
    }

    private fun fixData(jsonElement: JsonElement): JsonElement {
        println(jsonElement)
        val jsonObject = jsonElement.asJsonObject
        jsonObject.add(
            "since",
            ConfigManager.gson.toJsonTree(
                mapOf(
                    "MINOS_INQUISITOR" to jsonObject.get("creaturesSinceLastInquisitor").asInt
                )
            )
        )
        jsonObject.remove("creaturesSinceLastInquisitor")
        return jsonElement
    }
}
