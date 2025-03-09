package at.hannibal2.skyhanni.features.mining.routewaypoints

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.getOrNull
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.data.model.*
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.WaypointLoader
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

@SkyHanniModule
object OrderedWaypoints {

    private val config get() = SkyHanniMod.feature.mining.orderedWaypoints

    private var orderedWaypoints: MutableList<SoopyWaypoint> = mutableListOf()
    private var renderWaypoints: MutableList<Int> = mutableListOf()
    private var currentOrderedWaypointIndex = 1
    private var lastCloser = 0
    private var enabled = true

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!enabled) return
        for (i in renderWaypoints.indices) {
            var wpColor: Color? = null
            var r = 0
            var g = 0
            var b = 0
            var alpha = 0.8
            if (i == 0) {
                wpColor = config.previousWaypointColor.toSpecialColor()
            } else if (i == 1) {
                wpColor = config.currentWaypointColor.toSpecialColor()
            } else if (i == 2) {
                wpColor = config.nextWaypointColor.toSpecialColor()
            } else {
                r = 255
                alpha = 0.4
            }
            if (wpColor != null) {
                r = wpColor.red
                g = wpColor.green
                b = wpColor.blue
                alpha = wpColor.alpha / 255.0
            }
            if (orderedWaypoints.getOrNull(renderWaypoints[i]) == null) {
                ChatUtils.debug("${renderWaypoints[i]} $i")
                continue
            }
            event.drawWaypointFilled(
                orderedWaypoints[renderWaypoints[i]].toLorenzVec(),
                Color(r, g, b),
                seeThroughBlocks = true,
                beacon = false,
                minimumAlpha = alpha.toFloat()
            )
            val name = if (i < 3 && config.setupMode) {
                orderedWaypoints[renderWaypoints[i]].options["name"] ?: ""
            } else {
                orderedWaypoints[renderWaypoints[i]].options["name"] ?: ""
            }
            event.drawString(
                orderedWaypoints[renderWaypoints[i]].toLorenzVec().add(0.5, 0.5, 0.5),
                name,
                seeThroughBlocks = true
            )
        }
        val traceWP = orderedWaypoints[renderWaypoints.getOrNull(2) ?: return decideWaypoints()]
        val lineColor = config.traceLineColor.toSpecialColor()
        if (!config.setupMode && config.traceLine) {
            event.drawLineToEye(
                traceWP.toLorenzVec().add(0.5, 0.25, 0.5),
                Color(lineColor.red, lineColor.green, lineColor.blue),
                config.traceLineThickness.toInt(),
                true,
            )
        }
        val currentWP = orderedWaypoints[renderWaypoints[1]]
        if (config.setupMode) {
            event.draw3DLine(
                currentWP.toLorenzVec().add(0.5, 2.65, 0.5),
                traceWP.toLorenzVec().add(0.5, 0.5, 0.5),
                Color(lineColor.red, lineColor.green, lineColor.blue),
                config.setupModeLineThickness.toInt(),
                depth = true
            )
        }
        decideWaypoints()
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (currentOrderedWaypointIndex >= 0) currentOrderedWaypointIndex = 1
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shorderedload") {
            description = "Loads the ordered waypoints copied to your clipboard."
            category = CommandCategory.USERS_ACTIVE
            callback { load(it) }
            aliases = listOf("shorderedimport")
        }
        event.register("shorderedunload") {
            description = "Unloads the current ordered waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { unload() }
            aliases = listOf("shorderedclear")
        }
        event.register("shorderedskip") {
            description = "Skips the next <argument> number of waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { skip(it) }
        }
        event.register("shorderedskipto") {
            description = "Skips to the <argument> waypoint."
            category = CommandCategory.USERS_ACTIVE
            callback { skipTo(it) }
        }
        event.register("shorderedunskip") {
            description = "Goes back <argument> waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { unskip(it) }
        }
        event.register("shorderedenable") {
            description = "Enables the ordered waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { enable() }
        }
        event.register("shordereddisable") {
            description = "Disables the ordered waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { disable() }
        }
        event.register("shordereddelete") {
            description = "Deletes the <argument>-th waypoint."
            category = CommandCategory.USERS_ACTIVE
            callback { delete(it) }
            aliases = listOf("shorderedremove")
        }
        event.register("shorderedadd") {
            description = "Adds a waypoint."
            category = CommandCategory.USERS_ACTIVE
            callback { add(it) }
            aliases = listOf("shorderedinsert")
        }
        event.register("shorderedetherwarp") {
            description = "Marks a vein as etherwarp."
            category = CommandCategory.USERS_ACTIVE
            callback { etherwarp(it) }
        }
        event.register("shorderedexport") {
            description = "Exports the loaded ordered waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { export() }
        }
        event.register("shorderedsave") {
            description = "Saves the loaded ordered waypoints to a local file."
            category = CommandCategory.USERS_ACTIVE
            callback { save(it) }
        }
    }

    private fun load(args: Array<String>) {
        SkyHanniMod.coroutineScope.launch {
            val res = if (args.isEmpty()) WaypointLoader.getWaypoints(OSUtils.readFromClipboard() ?: "", "soopy")
            else WaypointLoader.getWaypoints(args[0], "soopy")

            if (res.success) {
                orderedWaypoints = res.waypoints?.toMutableList() ?: mutableListOf()
                ChatUtils.chat("Loaded ordered waypoints!")
            } else {
                return@launch ChatUtils.chat("There was an error parsing waypoints! ${res.message}")
            }


            orderedWaypoints.let {
                // pretty sure this is just bubble sort
                // TODO: Replace with sortedby or smth
                for (i in 0 until it.size - 1) {
                    for (j in 0 until it.size - i - 1) {
                        val currName = it[j].options["name"]?.toIntOrNull() ?: return@launch
                        val nextName = it[j + 1].options["name"]?.toIntOrNull() ?: return@launch
                        if (currName > nextName) {
                            val temp = it[j]
                            it[j] = it[j + 1]
                            it[j + 1] = temp
                        }
                    }
                }
                for (i in 1 until it.size) {
                    if (it[i].options["name"]?.toInt() != i + 1) {
                        ChatUtils.chat("Note: Waypoint ${i + 1} is not in the right order or is not a number! Current is: ${it[i].options["name"]}")
                    }
                }
            }
        }
    }

    private fun unload() {
        orderedWaypoints.clear()
        renderWaypoints.clear()
        currentOrderedWaypointIndex = 0
        lastCloser = 0
        ChatUtils.chat("Unloaded ordered waypoints!")
    }

    private fun skip(args: Array<String>) {
        val amountToSkip = args.getOrNull(0)?.toIntOrNull() ?: run {
            return ChatUtils.chat("Not an integer!")
        }
        currentOrderedWaypointIndex += amountToSkip
        if (orderedWaypoints.size > 1) currentOrderedWaypointIndex %= orderedWaypoints.size
        ChatUtils.chat("Skipped $amountToSkip ${StringUtils.pluralize(amountToSkip, "vein")}")
    }

    private fun skipTo(args: Array<String>) {
        var newOrderedWaypointIndex = args.getOrNull(0)?.toIntOrNull() ?: return
        if (newOrderedWaypointIndex > 0 && newOrderedWaypointIndex < orderedWaypoints.size) {
            if (newOrderedWaypointIndex == 1) newOrderedWaypointIndex = 2
            currentOrderedWaypointIndex = newOrderedWaypointIndex - 2
            ChatUtils.chat("Skipped to $currentOrderedWaypointIndex.")
        }
    }

    private fun unskip(args: Array<String>) {
        val decrement = args.getOrNull(0)?.toIntOrNull() ?: run {
            return ChatUtils.chat("Not an integer!")
        }
        currentOrderedWaypointIndex -= decrement
        if (orderedWaypoints.size > 1) {
            currentOrderedWaypointIndex = Math.floorMod(currentOrderedWaypointIndex, orderedWaypoints.size)
        }
    }

    private fun enable() {
        enabled = true
    }

    private fun disable() {
        enabled = false
    }

    private fun delete(args: Array<String>) {
        if (orderedWaypoints.isEmpty()) {
            return ChatUtils.chat("Waypoints have not been loaded!")
        }
        val wNum = args.getOrNull(0)?.toIntOrNull() ?: run {
            return ChatUtils.chat("Usage: /shordereddelete (number)")
        }
        if (wNum < 1 || wNum > orderedWaypoints.size) {
            return ChatUtils.chat("Invalid number! Must be in range (1 - ${orderedWaypoints.size}")
        }
        for (i in wNum - 1 until orderedWaypoints.size) {
            orderedWaypoints[i].options["name"] = ((orderedWaypoints[i].options["name"]?.toInt() ?: (i + 1)).dec()).toString()
        }
        orderedWaypoints.removeAt(wNum - 1)
        ChatUtils.chat("Removed waypoint $wNum.")
    }

    private fun add(args: Array<String>) {
        if (args.isEmpty()) {
            return ChatUtils.chat("Stand where you want to add a waypoint (will be block under you) and re-run the command.")
        }
        val player = Minecraft.getMinecraft().thePlayer.getLorenzVec()
        val wX = player.x.toInt()
        val wY = player.y.toInt() - 1
        val wZ = player.z.toInt()
        val wNum = args[0].toIntOrNull() ?: return
        if (wNum == orderedWaypoints.size - 1 || (wNum == 1 && orderedWaypoints.isEmpty())) {
            orderedWaypoints.add(min(wNum, orderedWaypoints.size - 1), SoopyWaypoint(wX, wY, wZ, options = mutableMapOf("name" to wNum.toString())))
            return
        }
        if (wNum < 1 || wNum > orderedWaypoints.size) {
            return ChatUtils.chat("Invalid number! Must be in range (1 - ${max(1, orderedWaypoints.size)}).")
        }
        for (i in wNum - 1 until orderedWaypoints.size) {
            orderedWaypoints[i].options["name"] = ((orderedWaypoints[i].options["name"]?.toIntOrNull() ?: (i + 1)).inc()).toString()
        }
        orderedWaypoints.add(wNum - 1, SoopyWaypoint(wX, wY, wZ, options = mutableMapOf("name" to wNum.toString())))
        ChatUtils.chat("Inserted waypoint $wNum at $wX, $wY, $wZ!")
    }

    private fun etherwarp(args: Array<String>) {
        if (args.isEmpty()) {
            return ChatUtils.chat("Marks a vein as etherwarp. Usage: /shorderedetherwarp (number).")
        }
        val wNum = args[0].toIntOrNull() ?: return
        if (orderedWaypoints.getOrNull(wNum) == null) {
            return ChatUtils.chat("Vein does not exist.")
        }
        orderedWaypoints[wNum].options["clip"] = (!orderedWaypoints[wNum].options["ether"].toBoolean()).toString()
        val which = if (orderedWaypoints[wNum].options["ether"].toBoolean()) "enabled"
        else "disabled"
        ChatUtils.chat("Waypoint $wNum is now $which to etherwarp.")
    }

    // TODO: skull
    private fun export() {}

    // TODO
    private fun save(args: Array<String>) {}

    private fun decideWaypoints() {
        renderWaypoints.clear()
        if (orderedWaypoints.size < 1) return
        val beforeWaypoint = orderedWaypoints.getOrNull(currentOrderedWaypointIndex - 1)
        if (beforeWaypoint != null) {
            beforeWaypoint.options["name"]?.let { renderWaypoints.add(it.toInt() - 1) }
        } else {
            orderedWaypoints[orderedWaypoints.size - 1].options["name"]?.let { renderWaypoints.add(it.toInt() - 1) }
        }
        val currentWaypoint = orderedWaypoints.getOrNull(currentOrderedWaypointIndex)
        var distanceTo1 = Double.POSITIVE_INFINITY
        if (currentWaypoint != null) {
            distanceTo1 = currentWaypoint.toLorenzVec().distanceToPlayer()
            currentWaypoint.options["name"]?.let { renderWaypoints.add(it.toInt() - 1) }
        }
        val nextWaypoint = orderedWaypoints.getOrNull(currentOrderedWaypointIndex + 1) ?: run {
            if (orderedWaypoints.getOrNull(0) != null) {
                orderedWaypoints[0]
            } else if (orderedWaypoints.getOrNull(1) != null) {
                orderedWaypoints[1]

            } else if (orderedWaypoints.getOrNull(2) != null) {
                orderedWaypoints[2]
            } else {
                null
            }
        }
        var distanceTo2 = Double.POSITIVE_INFINITY
        if (nextWaypoint != null) {
            distanceTo2 = nextWaypoint.toLorenzVec().distanceToPlayer()
            nextWaypoint.options["name"]?.let { renderWaypoints.add(it.toInt() - 1) }
            // TODO: add title
            if (nextWaypoint.options["ether"]?.toBoolean() == true) {

            }
        }
        if (lastCloser == currentOrderedWaypointIndex && distanceTo1 > distanceTo2 && distanceTo2 < config.waypointRange) {
            currentOrderedWaypointIndex++
            if (orderedWaypoints.getOrNull(currentOrderedWaypointIndex) == null) {
                currentOrderedWaypointIndex = 0
            }
            return
        }

        if (distanceTo1 < config.waypointRange.toDouble()) {
            lastCloser = currentOrderedWaypointIndex
        }

        if (distanceTo2 < config.waypointRange.toDouble()) {
            currentOrderedWaypointIndex++
            if (orderedWaypoints.getOrNull(currentOrderedWaypointIndex) == null) {
                currentOrderedWaypointIndex = 0
            }
        }

        orderedWaypoints.forEach { waypoint ->
            if (config.setupMode &&
                waypoint.options["name"]?.let { !renderWaypoints.contains(it.toInt() - 1) } == true &&
                waypoint.toLorenzVec().distanceToPlayer() < 16
            ) {
                waypoint.options["name"]?.let { renderWaypoints.add(it.toInt() - 1) }
            }
        }
    }
}
