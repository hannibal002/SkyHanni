package at.hannibal2.skyhanni.events.dungeon

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.ClickedBlockType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.phys.Vec3

@PrimaryFunction("onDungeonBlockClick")
class DungeonBlockClickEvent(val position: Vec3, val blockType: ClickedBlockType) : SkyHanniEvent()

