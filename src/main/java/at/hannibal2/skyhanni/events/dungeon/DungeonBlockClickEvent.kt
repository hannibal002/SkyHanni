package at.hannibal2.hanni.events.dungeon

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.ClickedBlockType
import at.hannibal2.hanni.utils.LorenzVec

class DungeonBlockClickEvent(val position: LorenzVec, val blockType: ClickedBlockType) : HanniEvent()

