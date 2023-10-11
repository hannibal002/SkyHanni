package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

object DungeonRoomDetection {
    var canLoadRooms = false
    var doors = mutableListOf<Door>()
    var closestEntrance: DoorEntrance? = null

    var testEntrance: DoorEntrance? = null
    var renderTestLocations = mutableMapOf<LorenzVec, String>()
//    var testClickedBlocks = mutableListOf(LorenzVec)
    var lastDetectionTime = SimpleTimeMark.farPast()

    var renderDebug = mutableMapOf<LorenzVec, String>()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        doors.clear()
        closestEntrance = null

        renderDebug.clear()
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!LorenzUtils.inDungeons) return
        testEntrance?.let {
            val clickedLocation = event.position
            val location = it.getOffset(clickedLocation) ?: return
//            println("")
            if (it.location.distance(clickedLocation) < 3) return
            if (lastDetectionTime.passedSince() < 350.milliseconds) return
            lastDetectionTime = SimpleTimeMark.now()

            val registryName = clickedLocation.getBlockAt().registryName
            val blockName = registryName.split(":")[1]
//            println("clicked at $location = $registryName")
            //RoomDetection(Blocks.dark_oak_stairs, LorenzVec(6, 2, 6)),
            val x = location.x.toInt()
            val y = location.y.toInt()
            val z = location.z.toInt()
            val text = "RoomDetection(Blocks.$blockName, LorenzVec($x, $y, $z)),"
            println(text)
//            OSUtils.copyToClipboard(text)

            renderTestLocations[clickedLocation] = "click"
        }
    }

    class Door(val center: LorenzVec, var isRealDoor: Boolean, vararg val entrances: DoorEntrance)

    class DoorEntrance(val location: LorenzVec, val direction: LorenzVec)

    @SubscribeEvent
    fun onDungeonStart(event: DungeonStartEvent) {
        canLoadRooms = true
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inDungeons) return

        if (doors.isEmpty()) {
            loadDoors()
        } else {
            checkCurrentDoor()
        }
//        if (event.isMod(10)) {
        if (Minecraft.getMinecraft().thePlayer.isSneaking) {
            if (InventoryUtils.latestItemInHand?.name == "§bMagical Map") {
                closestEntrance?.let {
                    renderTestLocations.clear()
                    if (it.location.distanceToPlayer() < 3) {
                        testEntrance = it
                        renderTestLocations[it.location.add(0, 1, 0)] = "testEntrance"
                    } else {
                        testEntrance = null
                    }
                }
            }
//            }
        }


        if (canLoadRooms) {
            if (event.isMod(20)) {
                checkRoomsNearby()
            }
        }
    }

    private fun checkRoomsNearby() {
        renderDebug.clear()
        for (door in doors) {
            val center = door.center
            if (center.distanceToPlayer() > 30) continue
//            if (center.distanceToPlayer() > 5) continue

            val blockBelow = center.add(0, -2, 0)
            if (blockBelow.getBlockAt() != Blocks.bedrock) {
//                renderDebug[center] = "no door: blockBelow"
                continue
            }

            val bloeDirectlyAbove = center.add(0, 1, 0)
            val block = bloeDirectlyAbove.getBlockAt()
            // air: open door
            // coal_block: closed wither door
            // stained_hardened_clay: closed blood door
            if (block != Blocks.air && block != Blocks.coal_block && block != Blocks.stained_hardened_clay) {
//                renderDebug[center] = "no door: blockDirectlyAbove"
                continue
            }

            door.isRealDoor = true

            renderDebug[center] = "door"
//            for (entrance in door.entrances) {
//                renderDebug[entrance.location] = "entrance"
//            }
        }
    }

    private fun loadDoors() {
        println("findMort")
        val mort = EntityUtils.getEntities<EntityArmorStand>().filter { it.name == "§bMort" }.firstOrNull()
        if (mort == null) {
            println("mort not found!")
            return
        }

        val mortLocation = mort.getLorenzVec().add(-0.5, 0.0, -0.5)
        renderDebug[mortLocation] = "mortLocation"

        loadStart(mortLocation)

//        startLocation?.let {
//            renderDebug[it.add(0, -1, 0)] = "startLocation"
//        }
    }

    private fun checkCurrentDoor() {
        val realDoors = doors.filter { it.isRealDoor }
        if (realDoors.isEmpty()) return

        val door = realDoors.map { it to it.center.distanceToPlayer() }.sorted().first().first
        val entrance =
            door.entrances.map { it to it.location.add(0.5, 0.0, 0.5).distanceToPlayer() }.sorted().first().first
        if (entrance != closestEntrance) {
            if (closestEntrance in door.entrances) {
                newRoomEntered(entrance)
            }
            closestEntrance = entrance
        }
    }

    private fun newRoomEntered(entrance: DoorEntrance) {
//        LorenzUtils.debug("newRoomEntered")

        val rooms = listOf<Room>(
            FairyRoom(
                LorenzVec(2, -1, -16),
                RoomDetection(Blocks.stone, LorenzVec(1, 0, 0)),
                RoomDetection(Blocks.stone, LorenzVec(2, 0, 0)),
                RoomDetection(Blocks.stone, LorenzVec(3, 0, 0)),
                RoomDetection(Blocks.stone, LorenzVec(4, 0, 0)),
                RoomDetection(Blocks.stone_slab, LorenzVec(5, 1, 0)),
                RoomDetection(Blocks.air, LorenzVec(5, 2, 0)),
            ),

            FairyRoom(
                LorenzVec(35, 17, -36),
                RoomDetection(Blocks.stone, LorenzVec(1, 0, 0)),
                RoomDetection(Blocks.stone, LorenzVec(2, 0, 0)),
                RoomDetection(Blocks.cobblestone, LorenzVec(3, 0, 0)),
                RoomDetection(Blocks.cobblestone, LorenzVec(4, 0, 0)),
                RoomDetection(Blocks.dirt, LorenzVec(6, 0, 0)),
                RoomDetection(Blocks.dirt, LorenzVec(7, 0, 0)),
                RoomDetection(Blocks.bedrock, LorenzVec(9, 2, -1)),
                RoomDetection(Blocks.dark_oak_stairs, LorenzVec(6, 2, 6)),
            ),

            FairyRoom(
                LorenzVec(26, 17, 4),
                RoomDetection(Blocks.dirt, LorenzVec(3, 0, -1)),
                RoomDetection(Blocks.stone, LorenzVec(4, 0, -1)),
                RoomDetection(Blocks.dirt, LorenzVec(5, 0, 0)),
                RoomDetection(Blocks.cobblestone, LorenzVec(5, 0, 1)),
                RoomDetection(Blocks.spruce_fence, LorenzVec(4, 2, 6)),
                RoomDetection(Blocks.cobblestone_wall, LorenzVec(2, 2, 5)),
                RoomDetection(Blocks.leaves, LorenzVec(4, 3, 6)),
                RoomDetection(Blocks.stone, LorenzVec(9, 2, -7)),
                RoomDetection(Blocks.dirt, LorenzVec(16, 0, -3)),
                RoomDetection(Blocks.bedrock, LorenzVec(18, 2, -1)),
            ),

        )

        for (room in rooms) {
            if (room.isRoom(entrance)) {
                println("found room: ${room.roomName}")
                if (room is FairyRoom) {
                    LorenzUtils.chat("fairy soul in this room!")
                    val fairySoul = room.soulLocation(entrance)
                    println("fairy soul at: $fairySoul")
                    renderTestLocations[fairySoul] = "soul"
                } else {
                    LorenzUtils.chat("something else in this room!")
                }
            }
        }
    }

    private fun FairyRoom.soulLocation(entrance: DoorEntrance) = entrance.getLocation(fairySoul)

    class FairyRoom(val fairySoul: LorenzVec, vararg detections: RoomDetection) : Room("Fairy Soul Room", *detections)

    abstract class Room(val roomName: String, vararg val detections: RoomDetection)

    class RoomDetection(val testBlock: Block, val offset: LorenzVec)

    fun RoomDetection.pass(entrance: DoorEntrance, roomName: String): Boolean {
        var help = entrance.getLocation(offset)

        val block = help.getBlockAt()
        val b = block == testBlock
        println("detection $b for room $roomName: $offset = ${block.registryName}")
        return b
    }

    private fun DoorEntrance.getOffset(
        otherLocation: LorenzVec
    ): LorenzVec? {
        val help = otherLocation.subtract(location)
//        println("help: $help")


//            val helpLocation = LorenzVec(otherLocation.z, otherLocation.y, otherLocation.x)
        return if (direction.x == 1.0) {
            help
        } else if (direction.x == -1.0) {
            help.rotate(180.0)
        } else if (direction.z == 1.0) {
            help.rotate(270.0)
        } else if (direction.z == -1.0) {
            help.rotate(90.0)
        } else {
//            println("direction: ${direction}")
//            helpLocation.subtract(location)
            null
        }
//        return helpLocation.subtract(location)
    }

    private fun DoorEntrance.getLocation(
        offset: LorenzVec
    ): LorenzVec {
        val nachVorne = offset.x.toInt()
        val nachOben = offset.y.toInt()
        val nachRechts = offset.z.toInt()

        var help = location.add(this.direction.multiply(nachVorne))
        help = help.add(0, nachOben, 0)
        help = help.add(this.direction.rotate(90.0).multiply(nachRechts))
        return help
    }

    fun Room.isRoom(entrance: DoorEntrance): Boolean {
        for (detection in detections) {
            if (!detection.pass(entrance, roomName)) {
                return false
            }
        }

        return true
    }

    private fun findOtherDoors(startLocation: LorenzVec, direction: LorenzVec) {
        val detectionSize = 15
//        val detectionSize = 5
        for (x in -detectionSize..detectionSize) {
            for (z in -detectionSize..detectionSize) {
                val center = startLocation.add(x * 32, 0, z * 32)

                val a = center.add(direction.multiply(2).add(0, -1, 0))
                val b = center.add(direction.multiply(-2).add(0, -1, 0))

                doors.add(
                    Door(
                        center,
                        false,
                        DoorEntrance(a, direction),
                        DoorEntrance(b, direction.multiply(-1))
                    )
                )
            }
        }
    }

    private fun loadStart(mortLocation: LorenzVec) {

        for (directionIndex in 0..3) {
            val baseDirection = LorenzVec(1, 0, 0).rotate(90.0 * directionIndex)
//        }
//
//
//        val directions = listOf(
//            LorenzVec(1, 0, 0),
//            LorenzVec(-1, 0, 0),
//            LorenzVec(0, 0, 1),
//            LorenzVec(0, 0, -1)
//        )
//        for (baseDirection in directions) {
            for (step in 1..5) {
                val testDirection = mortLocation.add(baseDirection.multiply(step)).add(0, 3, 0)
                val block = testDirection.getBlockAt()
                if (block == Blocks.iron_bars) {
                    val startLocation = mortLocation.add(baseDirection.multiply(step + 2))

                    findOtherDoors(startLocation, baseDirection)
                    findOtherDoors(startLocation.add(16, 0, 16), baseDirection.rotate(90.0))

                    return
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inDungeons) return

        for ((location, name) in renderDebug) {
            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
            event.drawDynamicText(location, name, 1.5)
        }
        for ((location, name) in renderTestLocations) {
            event.drawWaypointFilled(location, LorenzColor.GREEN.toColor())
            event.drawDynamicText(location, "§a" + name, 1.5)
        }

        closestEntrance?.let {
            val location = it.location.add(0, 2, 0)
//            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
//            event.drawDynamicText(location, "closest", 1.5)
        }
    }

    fun reset() {
//        startLocation = null
//        startDirection = null

        doors.clear()
        closestEntrance = null

        renderDebug.clear()

        canLoadRooms = true
    }
}
