package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CrystalHollowsProfessorRobot {

    private val pattern by RepoPattern.pattern(
        "mining.robot.missing.gfs",
        "\\[NPC\\] Professor Robot: That's not one of the components I need! Bring me one of the missing components:"
    )
    private val robotParts = setOf(
        "Electron Transmitter",
        "FTX 3070",
        "Robotron Reflector",
        "Superlite Motor",
        "Control Switch",
        "Synthetic Heart"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val cleanMessage = event.message.removeColor()

        if (!pattern.matches(cleanMessage)) return

        val itemName = event.message.removeColor().trimStart()
        if (InventoryUtils.countItemsInLowerInventory { it.name.contains(itemName) } > 0 || !robotParts.contains(itemName)) return

        GetFromSackAPI.getFromChatMessageSackItems(PrimitiveItemStack(itemName.asInternalName(), 1))
    }

    fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isInIsland() && SkyHanniMod.feature.mining.professorRobotHelper;
}
