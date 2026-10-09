package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.LorenzVec

data class NpcData(
    /**
     * Unique identifier for this NPC.
     */
    val id: String,

    /**
     * Position of the NPC in the world.
     */
    val position: LorenzVec,

    /**
     * Skin used by the NPC.
     */
    val skin: String,

    /**
     * Name displayed above the NPC.
     */
    val name: String,

    /**
     * Dialogue displayed when the NPC is clicked.
     */
    val dialogue: List<String> = emptyList(),
)
