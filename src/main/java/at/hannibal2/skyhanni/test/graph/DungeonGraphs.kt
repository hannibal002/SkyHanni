package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.data.repo.RepoUtils
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import java.awt.Point
import java.io.File

@SkyHanniModule
object DungeonGraphs {

    private var currentRoomName: String? = null
    private var currentRoomDirection: String? = null
    private var currentRoomCorner: LorenzVec? = null
    private var usingDrm = true

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Dungeon Room Graph")
        if (!DungeonApi.inDungeon()) {
            event.addIrrelevant("not in dungeon.")
        }

        if (!config.dungeonRoomDetection) {
            event.addData("disabled in settings.")
        }

        if (!usingDrm) {
            event.addData("Not using Dungeon Room Mod")
        }

        event.addData {
            add("room name: $currentRoomName")
            add("room direction: $currentRoomDirection")
            add("room corner: $currentRoomCorner")
        }
    }

    private val config get() = SkyHanniMod.feature.dev

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onTick(event: SkyHanniTickEvent) {
        if (!usingDrm) return
        if (!event.isMod(2)) return
        if (!config.dungeonRoomDetection) return

        val (newRoomName, newDirection, newCorner) = readRoomData() ?: run {
            usingDrm = false
            return
        }

        val oldRoomName = currentRoomName
        if (oldRoomName == newRoomName) return

        oldRoomName?.let {
            if (currentRoomCorner != null) {
                saveRoom(it)
            }
        }

        currentRoomName = newRoomName
        currentRoomDirection = newDirection
        currentRoomCorner = newCorner

        newRoomName?.let {
            if (currentRoomCorner == null) {
                GraphEditor.clear()
            } else {
                loadRoom(it)
            }
        }
    }

    // TODO use mixin
    private fun readRoomData(): Triple<String?, String?, LorenzVec?>? =
        Class.forName("io.github.quantizr.dungeonrooms.dungeons.catacombs.RoomDetection")?.let { detection ->
            val name = detection.getField("roomName").get(null) as String?
            val direction = detection.getField("roomDirection").get(null) as String?
            val corner = (detection.getField("roomCorner").get(null) as Point?)?.let { LorenzVec(it.x, 0, it.y) }

            Triple(name, direction, corner)
        }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.oldIsland != IslandType.CATACOMBS) return
        if (currentRoomCorner != null) {
            currentRoomName?.let {
                saveRoom(it)
                GraphEditor.clear()
            }
        }

        currentRoomName = null
        currentRoomDirection = null
        currentRoomCorner = null
    }

    private fun loadRoom(roomName: String) {
        val file = File("config/skyhanni/repo/constants/island_graphs/dungeon_rooms/$roomName.json")
        if (!file.isFile) {
            GraphEditor.clear()
            return
        }
        val graph = RepoUtils.getConstant(RepoManager.repoLocation, "island_graphs/dungeon_rooms/$roomName", Graph.gson, Graph::class.java)
        if (graph.nodes.isEmpty()) {
            GraphEditor.clear()
        } else {
            IslandGraphs.loadLobby("dungeon_rooms/$roomName")
            if (!GraphEditor.isEnabled()) return
            ChatUtils.chat("loadDungeonRoom $roomName")
            GraphEditor.import(graph.revertDungeon())
        }
    }

    private fun saveRoom(roomName: String) {
        if (!GraphEditor.isEnabled()) return
        val compileGraph = GraphEditor.compileGraph()
        if (compileGraph.nodes.isEmpty()) return
        val json = compileGraph.applyDungeon().toJson()
        val file = File("config/skyhanni/repo/constants/island_graphs/dungeon_rooms/$roomName.json")
        file.parentFile?.mkdirs()
        file.writeText(json)
        ChatUtils.chat("saveDungeonRoom $roomName")
    }

    fun Graph.revertDungeon(): Graph {
        val direction = currentRoomDirection ?: return this
        val corner = currentRoomCorner ?: return this

        return transformNodes { relativeToActual(it, direction, corner) }
    }

    private fun Graph.applyDungeon(): Graph {
        val direction = currentRoomDirection ?: return this
        val corner = currentRoomCorner ?: return this

        return transformNodes { actualToRelative(it, direction, corner) }
    }

    // Taken from DRM
    private fun relativeToActual(
        relative: LorenzVec,
        cornerDirection: String,
        locationOfCorner: LorenzVec,
    ): LorenzVec = when (cornerDirection) {
        "NW" -> LorenzVec(
            x = relative.x + locationOfCorner.x,
            y = relative.y,
            z = relative.z + locationOfCorner.z,
        )

        "NE" -> LorenzVec(
            x = -(relative.z - locationOfCorner.x),
            y = relative.y,
            z = relative.x + locationOfCorner.z,
        )

        "SE" -> LorenzVec(
            x = -(relative.x - locationOfCorner.x),
            y = relative.y,
            z = -(relative.z - locationOfCorner.z),
        )

        "SW" -> LorenzVec(
            x = relative.z + locationOfCorner.x,
            y = relative.y,
            z = -(relative.x - locationOfCorner.z),
        )

        else -> throw IllegalArgumentException("Unknown corner direction: $cornerDirection")
    }

    // Taken from DRM
    private fun actualToRelative(
        actual: LorenzVec,
        cornerDirection: String,
        locationOfCorner: LorenzVec,
    ): LorenzVec = when (cornerDirection) {
        "NW" -> LorenzVec(
            x = actual.x - locationOfCorner.x,
            y = actual.y,
            z = actual.z - locationOfCorner.z,
        )

        "NE" -> LorenzVec(
            x = actual.z - locationOfCorner.z,
            y = actual.y,
            z = -(actual.x - locationOfCorner.x),
        )

        "SE" -> LorenzVec(
            x = -(actual.x - locationOfCorner.x),
            y = actual.y,
            z = -(actual.z - locationOfCorner.z),
        )

        "SW" -> LorenzVec(
            x = -(actual.z - locationOfCorner.z),
            y = actual.y,
            z = actual.x - locationOfCorner.x,
        )

        else -> throw IllegalArgumentException("Unknown corner direction: $cornerDirection")
    }
}
