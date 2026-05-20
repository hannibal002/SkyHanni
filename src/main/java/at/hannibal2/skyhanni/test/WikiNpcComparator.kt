package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.data.repo.SkyHanniRepoManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings

@SkyHanniModule
object WikiNpcComparator {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcomparewikinpc") {
            description = "Compare NPC locations from wiki (clipboard) with SkyHanni graph data."
            category = CommandCategory.DEVELOPER_TEST
            legacyCallbackArgs {
                CoroutineSettings("compare wiki npc data").launchCoroutine {
                    val result = mutableListOf<String>()
                    val wikiNpcs = customRules(loadWiki(result)) ?: return@launchCoroutine
                    val shNpcs = loadShNpcs(result) ?: return@launchCoroutine
                    compare(result, shNpcs, wikiNpcs)
                }
            }
        }
    }

    private fun customRules(raw: MutableMap<IslandType, MutableMap<String, LorenzVec>>?): MutableMap<IslandType, MutableMap<String, LorenzVec>>? {
        if (raw == null) return null
        for ((island, names) in raw) {
            for ((name, location) in names.toMutableMap()) {
                if (name.contains(" (Dwarven)")) {
                    val updatedName = name.removeSuffix(" (Dwarven)")
                    names[updatedName] = location
                    names.remove(name)
                }
            }
        }

        return raw
    }

    private fun loadWiki(result: MutableList<String>): MutableMap<IslandType, MutableMap<String, LorenzVec>>? {
        val clipboard = OSUtils.readFromClipboard() ?: run {
            ChatUtils.userError("clipboard does not contain a string")
            return null
        }
        val wikiNpcs = WikiNpcParser.parse(clipboard)
        val total = wikiNpcs.values.sumOf { it.size }
        result.add("found in total $total npcs in wiki")
        if (total == 0) {
            ChatUtils.clickableChat(
                "found no npc data in the html file, click here to copy.",
                onClick = {
                    OSUtils.openBrowser("https://hypixelskyblock.minecraft.wiki/w/NPC/List")
                },
            )
            return null
        }
        return wikiNpcs
    }

    // TODO do this once on skyblock join, then store until repo reload.
    private suspend fun loadShNpcs(result: MutableList<String>): MutableMap<IslandType, MutableMap<String, LorenzVec>>? {
        val shNpcs = mutableMapOf<IslandType, MutableMap<String, LorenzVec>>()

        val islands = mutableMapOf<String, IslandType>()
        for (islandType in IslandType.entries) {
            if (islandType in IslandTypeTag.NO_FIXED_NPC_LOCATIONS) continue
            islands[islandType.name] = islandType
        }
        islands["GLACITE_TUNNELS"] = IslandType.DWARVEN_MINES

        for ((name, type) in islands) {
            val graph = runCatching {
                SkyHanniRepoManager.getRepoDataAsync<Graph>(
                    "constants/island_graphs", name, gson = Graph.gson,
                )
            }.getOrElse {
                result.add("failed to load island graph for island $name")
                continue
            }

            val npcs = mutableMapOf<String, LorenzVec>()
            for (node in graph) {
                if (node.hasTag(GraphNodeTag.NPC)) {
                    val name = node.name ?: error("name is null for npc node at ${node.position} in island $name")
                    npcs[name] = node.position
                }
            }
            if (npcs.isNotEmpty()) {
                shNpcs.getOrPut(type) { mutableMapOf() }.putAll(npcs)
            }
        }

        val total = shNpcs.values.sumOf { it.size }
        result.add("found in total $total npcs in sh repo")
        if (total == 0) {
            ChatUtils.chat("no sh npcs loaded via local repo data!")
            return null
        }
        return shNpcs
    }

    private fun compare(
        result: MutableList<String>,
        shNpcs: Map<IslandType, Map<String, LorenzVec>>,
        wikiNpcs: Map<IslandType, Map<String, LorenzVec>>,
    ) {
        val allIslands = (shNpcs.keys + wikiNpcs.keys).sorted()

        var allFine = 0
        var issues = 0
        for (island in allIslands) {
            val sh = shNpcs[island].orEmpty()
            val wiki = wikiNpcs[island].orEmpty()
            val allNames = (sh.keys + wiki.keys).sorted()
            val islandLines = mutableMapOf<String, List<String>>()

            val onlyInSh = mutableListOf<String>()
            islandLines["Only in skyhanni"] = onlyInSh
            val onlyInWiki = mutableListOf<String>()
            islandLines["Only in wiki"] = onlyInWiki
            val mismatch = mutableListOf<String>()
            islandLines["mismatch"] = mismatch

            for (name in allNames) {
                val shPos = sh[name]
                val wikiPos = wiki[name]

                when {
                    shPos != null && wikiPos == null -> onlyInSh.add("$name: ${shPos.toChatFormat()}")

                    shPos == null && wikiPos != null -> onlyInWiki.add("$name: ${wikiPos.toChatFormat()}")

                    shPos != null && wikiPos != null -> {
                        val dist = shPos.distance(wikiPos)
                        /**
                         * this cant be a precise check.
                         * the wiki uses the block location the npc stands on,
                         * and skyhanni uses the location of where the user should stand when talking to the npc
                         * so this is almost never 0 block distance, and often even 2 blocks distance.
                         */
                        if (dist > 3.0) {
                            mismatch.add(
                                "$name: ${dist.roundTo(1)} blocks away\n" +
                                    "      sh: ${shPos.toChatFormat()}\n" +
                                    "      wiki: ${wikiPos.toChatFormat()}",
                            )
                        } else {
                            allFine++
                        }
                    }
                }
            }

            islandLines.removeIf { it.value.isEmpty() }
            if (islandLines.isNotEmpty()) {
                result.add("=== $island (sh=${sh.size}, wiki=${wiki.size}) ===")
                for ((type, list) in islandLines) {
                    if (list.isEmpty()) continue
                    result.add("  $type (${list.size})")
                    list.forEach {
                        result.add("    $it")
                        issues++
                    }
                }
            }
        }
        result.forEach { println(it) }
        val total = allFine + issues
        val percentageIdentical = (allFine.toDouble() / total) / 100
        ChatUtils.clickableChat(
            "comparison done: ${percentageIdentical.roundTo(1)}% identical.\n" +
                "total: $total, all fine: $allFine, issues: $issues.\n" +
                "see console for more infos or click here to copy to clipboard.",
            onClick = {
                CoroutineSettings("put wiki npc comparison data to clipboard").launchCoroutine {
                    OSUtils.copyToClipboardAsync(result.joinToString("\n"))
                }
            },
        )
        ChatUtils.chat()
    }
}
