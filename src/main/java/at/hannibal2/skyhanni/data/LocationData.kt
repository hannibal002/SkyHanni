package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import net.minecraft.util.AxisAlignedBB

object LocationData {
    private val westVillageFarmArea = AxisAlignedBB(-54.0, 69.0, -115.0, -40.0, 75.0, -127.0)
    private val howlingCaveArea = AxisAlignedBB(-401.0, 50.0, -104.0, -337.0, 90.0, 36.0)
    private val fakeZealotBruiserHideoutArea = AxisAlignedBB(-520.0, 66.0, -332.0, -558.0, 85.0, -280.0)
    private val realZealotBruiserHideoutArea = AxisAlignedBB(-552.0, 50.0, -245.0, -580.0, 72.0, -209.0)
    private val leftStronghold = AxisAlignedBB(-463.7, 94.0, -518.3, -435.3, 115.0, -547.7)

    fun fixLocation(skyBlockIsland: IslandType) = when {
        skyBlockIsland == IslandType.THE_RIFT && westVillageFarmArea.isPlayerInside() -> "Dreadfarm"
        skyBlockIsland == IslandType.THE_PARK && howlingCaveArea.isPlayerInside() -> "Howling Cave"
        skyBlockIsland == IslandType.THE_END && fakeZealotBruiserHideoutArea.isPlayerInside() -> "The End"
        skyBlockIsland == IslandType.THE_END && realZealotBruiserHideoutArea.isPlayerInside() -> "Zealot Bruiser Hideout"
        skyBlockIsland == IslandType.CRIMSON_ISLE && leftStronghold.isPlayerInside() -> "Stronghold"

        else -> null
    }
}
