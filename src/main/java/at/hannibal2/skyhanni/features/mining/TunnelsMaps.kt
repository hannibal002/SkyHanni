package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.findShortestDistance
import at.hannibal2.skyhanni.data.model.findShortestPathAsGraphWithDistance
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.filterNotNullKeys
import at.hannibal2.skyhanni.utils.ColorUtils.getFirstColorCode
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DPathWithWaypoint
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class TunnelsMaps {

    private val config get() = SkyHanniMod.feature.mining.tunnelMaps

    private var graph: Graph = Graph(emptyList())
    private lateinit var campfire: GraphNode

    private var goalReached = false
    private var prevGoal: GraphNode? = null
    private var goal: GraphNode? = null
        set(value) {
            prevGoal = field
            field = value
        }

    private var closedNote: GraphNode? = null
    private var path: Pair<Graph, Double>? = null

    private var possibleLocations = mapOf<String, List<GraphNode>>()
    private val cooldowns = mutableMapOf<GraphNode, SimpleTimeMark>()
    private var active: String = ""

    private lateinit var fairySouls: Map<String, GraphNode>
    private lateinit var newGemstones: Map<String, List<GraphNode>>
    private lateinit var oldGemstones: Map<String, List<GraphNode>>
    private lateinit var normalLocations: Map<String, List<GraphNode>>

    private fun getNext(name: String = active): GraphNode? {
        fairySouls[name]?.let {
            goalReached = false
            return it
        }

        val closed = closedNote ?: return null
        val list = possibleLocations[name] ?: return null

        val offCooldown = list.filter { cooldowns[it]?.isInPast() != false }
        val goodOnes = offCooldown.filter { it.position.distanceSqToPlayer() > 400.0 }
        val best = goodOnes.minByOrNull { graph.findShortestDistance(closed, it) } ?: list.minBy {
            cooldowns[it] ?: SimpleTimeMark.farPast()
        }

        cooldowns[best] = 25.0.seconds.fromNow()
        goalReached = false
        return best
    }

    private fun hasNext(name: String = active): Boolean {
        val list = possibleLocations[name] ?: return false
        return list.size > 1
    }

    private val oldGemstonePattern by RepoPattern.pattern(
        "mining.tunnels.maps.gem.old", ".*(?:Ruby|Amethyst|Jade|Sapphire|Amber|Topaz).*"
    )
    private val newGemstonePattern by RepoPattern.pattern(
        "mining.tunnels.maps.gem.new", ".*(?:Aquamarine|Onyx|Citrine|Peridot).*"
    )

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        graph = event.getConstant<Graph>("TunnelsGraph", gson = Graph.gson)
        possibleLocations = graph.groupBy { it.name }.filterNotNullKeys().mapValues { (_, value) ->
            value
        }
        val fairy = mutableMapOf<String, GraphNode>()
        val oldGemstone = mutableMapOf<String, List<GraphNode>>()
        val newGemstone = mutableMapOf<String, List<GraphNode>>()
        val other = mutableMapOf<String, List<GraphNode>>()
        possibleLocations.forEach { (key, value) ->
            when {
                key.contains("Campfire") -> campfire = value.first()
                key.contains("Fairy") -> fairy[key] = value.first()
                newGemstonePattern.matches(key) -> newGemstone[key] = value
                oldGemstonePattern.matches(key) -> oldGemstone[key] = value
                else -> other[key] = value
            }
        }
        fairySouls = fairy
        this.newGemstones = newGemstone
        this.oldGemstones = oldGemstone
        normalLocations = other
    }

    @SubscribeEvent
    fun onRenderDisplay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val display = buildList<Renderable> {
            if (active.isNotEmpty()) {
                if (goal == campfire && active != campfire.name) {
                    add(Renderable.string("§6Override for ${campfire.name}"))
                    add(Renderable.clickable(Renderable.string("§eMake §f$active §eactive"), onClick = {
                        goal = getNext()
                    }))
                } else {
                    add(Renderable.string("§6Active: §f$active"))
                    if (hasNext()) {
                        add(Renderable.clickable(Renderable.string("§eNext Spot"), onClick = {
                            goal = getNext()
                        }))
                    } else {
                        add(Renderable.string(""))
                    }
                }
            } else {
                add(Renderable.string(""))
                add(Renderable.string(""))
            }
            add(Renderable.string("§6Loactions:"))
            add(
                Renderable.multiClickAndHover(
                    campfire.name!!, listOf(
                        "§eLeft Click to set active", "§eRight Click for override"
                    ), click = mapOf(
                        0 to guiSetActive(campfire.name!!), 1 to ::campfireOverride
                    )
                )
            )
            add(Renderable.hoverable(Renderable.horizontalContainer(listOf(Renderable.string("§dFairy Souls")) + fairySouls.map {
                val name = it.key.removePrefix("§dFairy Soul ")
                Renderable.clickable(Renderable.string("§d[${name}]"), onClick = guiSetActive(it.key))
            }

            ), Renderable.string("§dFairy Souls")))
            if (config.compactGemstone) {
                add(
                    Renderable.table(
                        listOf(
                            newGemstones.map(::toCompactGemstoneName), oldGemstones.map(::toCompactGemstoneName)
                        )
                    )
                )
            } else {
                addAll(newGemstones.map {
                    Renderable.clickable(Renderable.string(it.key), onClick = guiSetActive(it.key))
                })
                addAll(oldGemstones.map {
                    Renderable.clickable(Renderable.string(it.key), onClick = guiSetActive(it.key))
                })
            }
            addAll(normalLocations.map {
                Renderable.clickable(Renderable.string(it.key), onClick = guiSetActive(it.key))
            })
        }
        config.position.renderRenderables(display, posLabel = "TunnelsMaps")
    }

    private fun toCompactGemstoneName(it: Map.Entry<String, List<GraphNode>>): Renderable = Renderable.clickAndHover(
        Renderable.string((it.key.getFirstColorCode()?.let { "§$it" } ?: "") + ("ROUGH_".plus(
            it.key.removeColor().removeSuffix("stone")
        ).asInternalName().itemName.takeWhile { it != ' ' }.removeColor()),
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
        ),
        tips = listOf(it.key),
        onClick = guiSetActive(it.key),
    )

    private fun campfireOverride() {
        goalReached = false
        goal = campfire
    }

    private fun guiSetActive(it: String): () -> Unit = {
        active = it
        goal = getNext()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (checkGoalReached()) return
        val prevClosed = closedNote
        closedNote = graph.minBy { it.position.distanceSqToPlayer() }
        val closest = closedNote ?: return
        val goal = goal ?: return
        if (closest == prevClosed && goal == prevGoal) return
        val (path, distance) = graph.findShortestPathAsGraphWithDistance(closest, goal)
        val first = path.firstOrNull()
        val second = path.getOrNull(1)

        val playerPosition = LocationUtils.playerLocation()
        val nodeDistance = first?.let { playerPosition.distance(it.position) } ?: 0.0
        if (first != null && second != null) {
            val direct = playerPosition.distance(second.position)
            val firstPath = first.neighbours[second] ?: 0.0
            val around = nodeDistance + firstPath
            if (direct < around) {
                this.path = Graph(path.drop(1)) to (distance - firstPath + direct)
                return
            }
        }
        this.path = path to (distance + nodeDistance)
    }

    private fun checkGoalReached(): Boolean {
        if (goalReached) return true
        val distance = goal?.position?.distanceSqToPlayer() ?: return false
        goalReached = distance < 36.0
        if (goalReached) {
            path = null
            return true
        }
        return false
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        val path = path?.takeIf { it.first.isNotEmpty() } ?: return
        event.draw3DPathWithWaypoint(path.first, getPathColor(), 4, true, bezierPoint = 2.0)
        event.drawDynamicText(path.first.last().position, "§e${path.second.roundToInt()}m", 1.0, yOff = 10f)
    }

    private fun getPathColor(): Color = if (config.dynamicPathColour) {
        goal?.name?.getFirstColorCode()?.toLorenzColor()?.takeIf { it != LorenzColor.WHITE }?.toColor()
    } else {
        null
    } ?: config.pathColour.toChromaColor()

    @SubscribeEvent
    fun onKeyPress(event: LorenzKeyPressEvent) {
        if (!isEnabled()) return
        campfireKey(event)
        nextSpotKey(event)
    }

    private fun campfireKey(event: LorenzKeyPressEvent) {
        if (event.keyCode != config.campfireKey) return
        if (config.travelScroll) {
            ChatUtils.sendMessageToServer("/warp basecamp")
        } else {
            campfireOverride()
        }
    }

    private var nextSpotDelay = SimpleTimeMark.farPast()

    private fun nextSpotKey(event: LorenzKeyPressEvent) {
        if (event.keyCode != config.nextSpotHotkey) return
        if (!nextSpotDelay.isInPast()) return
        nextSpotDelay = 1.0.seconds.fromNow()
        goal = getNext()
    }

    val areas = setOf(
        "Glacite Tunnels", "Dwarven Base Camp", "Glacite Lake", "Fossil Research Center"
    )

    private fun isEnabled() =
        IslandType.DWARVEN_MINES.isInIsland() && config.enable && areas.contains(LorenzUtils.skyBlockArea)
}
